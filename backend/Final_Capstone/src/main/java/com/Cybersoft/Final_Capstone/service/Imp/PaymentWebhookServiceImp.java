package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.PromotionUsage;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.Entity.Transaction;
import com.Cybersoft.Final_Capstone.Entity.UserPromotion;
import com.Cybersoft.Final_Capstone.config.PayOSConfig;
import com.Cybersoft.Final_Capstone.payload.request.PayOSWebhookRequest;
import com.Cybersoft.Final_Capstone.repository.BookingRepository;
import com.Cybersoft.Final_Capstone.repository.PromotionUsageRepository;
import com.Cybersoft.Final_Capstone.repository.StatusRepository;
import com.Cybersoft.Final_Capstone.repository.TransactionRepository;
import com.Cybersoft.Final_Capstone.repository.UserPromotionRepository;
import com.Cybersoft.Final_Capstone.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookServiceImp implements PaymentWebhookService {

    private final TransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;
    private final StatusRepository statusRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final UserPromotionRepository userPromotionRepository;
    private final PayOSConfig payOSConfig;

    @Override
    @Transactional
    public void processWebhook(PayOSWebhookRequest webhookRequest) {

        try {
            // ⚠️ Handle PayOS test webhook (data is null)
            if (webhookRequest.getData() == null) {
                return; // Return gracefully without throwing exception
            }

            // Verify webhook success status
            // PayOS webhook success is determined by:
            // 1. code = "00" means success
            // 2. success field can be true, null, or missing (for test webhooks)
            boolean isSuccess = "00".equals(webhookRequest.getCode()) || Boolean.TRUE.equals(webhookRequest.getSuccess());

            // ✅ SIGNATURE VERIFICATION ENABLED
            // Note: If testing locally and signature verification fails,
            // you can temporarily disable by commenting out the block below
            if (!verifySignature(webhookRequest)) {
                log.error("❌ Invalid webhook signature for orderCode: {}", webhookRequest.getData().getOrderCode());
                throw new RuntimeException("Invalid webhook signature");
            }
            log.info("✅ Webhook signature verified successfully");

            // Find transaction by orderCode
            String orderCode = String.valueOf(webhookRequest.getData().getOrderCode());

            Transaction transaction = null;

            // Try find by orderId first
            Optional<Transaction> transactionOpt = transactionRepository.findByOrderId(orderCode);

            if (transactionOpt.isPresent()) {
                transaction = transactionOpt.get();
            } else {
                // Fallback: try find by bookingId if orderId is null (legacy data)
                log.warn("⚠️ Transaction not found by orderCode: {}. Trying fallback to bookingId...", orderCode);
                try {
                    Integer bookingId = Integer.parseInt(orderCode);
                    List<Transaction> transactionsByBooking = transactionRepository.findByBookingId(bookingId);

                    if (!transactionsByBooking.isEmpty()) {
                        transaction = transactionsByBooking.get(0);
                        log.info("✅ Found transaction by bookingId fallback: {}", transaction.getId());

                        // Fix null orderId for future webhook calls
                        if (transaction.getOrderId() == null || transaction.getOrderId().isEmpty()) {
                            transaction.setOrderId(orderCode);
                            transactionRepository.save(transaction);
                            log.info("🔧 Fixed null orderId for transaction {}", transaction.getId());
                        }
                    } else {
                        log.error("❌ No transaction found for orderCode: {} (tried both orderId and bookingId)", orderCode);
                        return; // Gracefully skip this webhook
                    }
                } catch (NumberFormatException e) {
                    log.error("❌ OrderCode cannot be parsed as Integer: {}", orderCode);
                    return; // Gracefully skip this webhook
                }
            }

            if (transaction == null) {
                log.error("❌ Transaction is null after all attempts for orderCode: {}", orderCode);
                return;
            }


            // Check if already processed
            if ("SUCCESS".equals(transaction.getPaymentStatus())) {
                log.info("ℹ️ Transaction {} already processed successfully", transaction.getId());
                return;
            }

            // ========== HANDLE PAYMENT SUCCESS ==========
            if (isSuccess) {
                log.info("✅ Processing SUCCESSFUL payment for orderCode: {}", orderCode);

                // Update transaction with webhook data
                PayOSWebhookRequest.PayOSWebhookData data = webhookRequest.getData();

                transaction.setPaymentStatus("SUCCESS");
                transaction.setAccountNumber(data.getAccountNumber());
                transaction.setReference(data.getReference());
                transaction.setPaymentLinkId(data.getPaymentLinkId());
                transaction.setCounterAccountBankId(data.getCounterAccountBankId());
                transaction.setCounterAccountBankName(data.getCounterAccountBankName());
                transaction.setCounterAccountName(data.getCounterAccountName());
                transaction.setCounterAccountNumber(data.getCounterAccountNumber());
                transaction.setVirtualAccountName(data.getVirtualAccountName());
                transaction.setVirtualAccountNumber(data.getVirtualAccountNumber());
                transaction.setWebhookSignature(webhookRequest.getSignature());
                transaction.setMessage("Payment completed successfully via PayOS");

                // Parse transaction date from PayOS
                if (data.getTransactionDateTime() != null && !data.getTransactionDateTime().isEmpty()) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        transaction.setTransactionDateTime(LocalDateTime.parse(data.getTransactionDateTime(), formatter));
                    } catch (Exception e) {
                        transaction.setTransactionDateTime(LocalDateTime.now());
                    }
                } else {
                    transaction.setTransactionDateTime(LocalDateTime.now());
                }

                transactionRepository.save(transaction);

                // Update booking status to PAID (payment successful)
                Booking booking = transaction.getBooking();

                booking.setStatus(new Status(8)); // PAID
                bookingRepository.save(booking);

                log.info("✅ Booking {} status updated to PAID", booking.getId());

                // ========== MARK PROMOTION AS USED ==========
                // When payment is successful, change UserPromotion.status from INACTIVE → USED
                // This prevents refund if user cancels after payment
                markPromotionAsUsed(booking);

                // TODO: Send notification to user (payment confirmed)
                // TODO: Send notification to host (booking confirmed + paid)

            } else {
                // ========== HANDLE PAYMENT FAILED ==========
                log.warn("⚠️ Processing FAILED payment for orderCode: {}", orderCode);
                log.warn("⚠️ Reason: {}", webhookRequest.getDesc());

                transaction.setPaymentStatus("FAILED");
                transaction.setMessage("Payment failed: " + webhookRequest.getDesc());
                transaction.setWebhookSignature(webhookRequest.getSignature());
                transactionRepository.save(transaction);

                // DO NOT change booking status - it stays CONFIRMED
                // User can retry payment by calling /transactions/init again
                log.info("💡 User can retry payment for booking {}", transaction.getBooking().getId());

                // TODO: Send notification to user (payment failed, please retry)
            }


        } catch (Exception e) {
            log.error("Error in processWebhook: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Map<String, Object> getPaymentStatus(Integer bookingId) {
        log.info("📊 Getting payment status for booking ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        Transaction transaction = transactionRepository.findByBookingIdAndPaymentStatus(bookingId, "SUCCESS");

        if (transaction == null) {
            // Check for pending transaction
            List<Transaction> transactions = transactionRepository.findByBookingId(bookingId);
            transaction = transactions.isEmpty() ? null : transactions.get(0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("bookingStatus", booking.getStatus().getName());

        if (transaction != null) {
            result.put("paymentStatus", transaction.getPaymentStatus());
            result.put("transactionId", transaction.getId());
            result.put("orderId", transaction.getOrderId());
            result.put("amount", transaction.getTotalAmount());
            result.put("paymentMethod", transaction.getPaymentMethod());
            result.put("reference", transaction.getReference());
            result.put("transactionDate", transaction.getTransactionDateTime());
        } else {
            result.put("paymentStatus", "NOT_FOUND");
        }

        return result;
    }

    /**
     * Verify webhook signature using HMAC SHA256
     * According to PayOS documentation, signature is calculated from sorted webhook data fields
     */
    private boolean verifySignature(PayOSWebhookRequest webhookRequest) {
        try {
            PayOSWebhookRequest.PayOSWebhookData data = webhookRequest.getData();

            // Build signature data string from webhook data
            // Use TreeMap for automatic alphabetical sorting
            Map<String, String> signatureData = new TreeMap<>();

            // Add all fields - always include, convert null/'null'/'undefined' to ""
            addForSignature(signatureData, "accountNumber", data.getAccountNumber());
            addForSignature(signatureData, "amount", data.getAmount());
            addForSignature(signatureData, "code", data.getCode());
            addForSignature(signatureData, "counterAccountBankId", data.getCounterAccountBankId());
            addForSignature(signatureData, "counterAccountBankName", data.getCounterAccountBankName());
            addForSignature(signatureData, "counterAccountName", data.getCounterAccountName());
            addForSignature(signatureData, "counterAccountNumber", data.getCounterAccountNumber());
            addForSignature(signatureData, "currency", data.getCurrency());
            addForSignature(signatureData, "desc", data.getDesc());
            addForSignature(signatureData, "description", data.getDescription());
            addForSignature(signatureData, "orderCode", data.getOrderCode());
            addForSignature(signatureData, "paymentLinkId", data.getPaymentLinkId());
            addForSignature(signatureData, "reference", data.getReference());
            addForSignature(signatureData, "transactionDateTime", data.getTransactionDateTime());
            addForSignature(signatureData, "virtualAccountName", data.getVirtualAccountName());
            addForSignature(signatureData, "virtualAccountNumber", data.getVirtualAccountNumber());

            // Build signature string: key1=value1&key2=value2...
            String signatureString = signatureData.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));

            log.info("Signature string: {}", signatureString);

            // Calculate HMAC SHA256
            String calculatedSignature = calculateHmacSha256(signatureString, payOSConfig.getChecksumKey());

            log.info("Calculated signature: {}", calculatedSignature);
            log.info("Received signature: {}", webhookRequest.getSignature());

            boolean isValid = calculatedSignature != null
                    && calculatedSignature.equalsIgnoreCase(webhookRequest.getSignature());

            if (!isValid) {
                log.error("Signature mismatch. Expected: {}, Got: {}", calculatedSignature, webhookRequest.getSignature());
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Helper method to add field to signature map
     * Handles null/'null'/'undefined' by converting to empty string
     * Always adds the field to map (required by PayOS)
     */
    private void addForSignature(Map<String, String> map, String key, Object value) {
        String valueStr;
        if (value == null) {
            valueStr = "";
        } else {
            String tmp = String.valueOf(value);
            if ("null".equalsIgnoreCase(tmp) || "undefined".equalsIgnoreCase(tmp)) {
                valueStr = "";
            } else {
                valueStr = tmp;
            }
        }
        map.put(key, valueStr);
    }

    /**
     * Calculate HMAC SHA256 hash
     * Compatible with Java 8+
     */
    private String calculateHmacSha256(String data, String key) {
        try {
            if (key == null) {
                throw new IllegalArgumentException("Checksum key is null");
            }
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Manual hex conversion for Java 8+ compatibility
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error calculating HMAC SHA256: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Mark UserPromotion as USED when payment is successful
     * This prevents refund if user cancels after payment
     *
     * Flow:
     * - Before payment: UserPromotion.status = INACTIVE (locked, can refund if cancel)
     * - After payment success: UserPromotion.status = USED (cannot refund even if cancel)
     *
     * @param booking The booking that was successfully paid
     */
    private void markPromotionAsUsed(Booking booking) {
        try {
            // Find PromotionUsage for this booking
            Optional<PromotionUsage> usageOpt = promotionUsageRepository.findByBookingId(booking.getId());

            if (usageOpt.isEmpty()) {
                log.info("ℹ️ No promotion used for booking {}. Nothing to mark.", booking.getId());
                return;
            }

            PromotionUsage usage = usageOpt.get();
            UserPromotion userPromotion = usage.getUserPromotion();

            // Check current status
            if ("USED".equals(userPromotion.getStatus().getName())) {
                log.info("ℹ️ UserPromotion {} already marked as USED", userPromotion.getId());
                return;
            }

            log.info("🔄 Marking UserPromotion {} as USED for paid booking {}",
                userPromotion.getId(), booking.getId());

            // Update UserPromotion status to USED
            userPromotion.setStatus(new Status(12)); // USED
            // Keep isLocked = true (still locked, but now USED)
            userPromotionRepository.save(userPromotion);

            log.info("✅ UserPromotion {} marked as USED. Payment successful, promotion consumed permanently.",
                userPromotion.getId());

        } catch (Exception e) {
            log.error("❌ Error marking promotion as USED for booking {}: {}",
                booking.getId(), e.getMessage(), e);
            // Don't throw - payment success should not be affected by promotion status update
        }
    }
}
