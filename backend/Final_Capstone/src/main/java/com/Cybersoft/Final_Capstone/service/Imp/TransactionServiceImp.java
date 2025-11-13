package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.InvalidException;
import com.Cybersoft.Final_Capstone.payload.request.PaymentInitRequest;
import com.Cybersoft.Final_Capstone.payload.response.PaymentInitResponse;
import com.Cybersoft.Final_Capstone.payload.response.PayOSPaymentData;
import com.Cybersoft.Final_Capstone.repository.BookingRepository;
import com.Cybersoft.Final_Capstone.repository.PromotionUsageRepository;
import com.Cybersoft.Final_Capstone.repository.TransactionRepository;
import com.Cybersoft.Final_Capstone.service.PayOSService;
import com.Cybersoft.Final_Capstone.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionServiceImp implements TransactionService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PromotionUsageRepository promotionUsageRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private PayOSService payOSService;

    @Override
    @Transactional
    public synchronized PaymentInitResponse initPayment(PaymentInitRequest request) {
        log.info("🚀 Initiating payment for booking {}", request.getBookingId());

        // 1. Find booking with details
        Booking booking = bookingRepository.findByIdWithDetails(request.getBookingId())
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + request.getBookingId()));

        // 2. Validate booking belongs to current user
        UserAccount currentUser = securityUtil.getLoggedInUser();
        if (currentUser == null) {
            throw new InvalidException("User not authenticated");
        }

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new InvalidException("This booking does not belong to the current user");
        }

        // 3. Validate booking status = CONFIRMED
        if (!"CONFIRMED".equals(booking.getStatus().getName())) {
            throw new InvalidException("Only CONFIRMED bookings can proceed to payment. Current status: " + booking.getStatus().getName());
        }

        // 4. Check 24h deadline
        if (booking.isPaymentExpired()) {
            throw new InvalidException("Payment deadline has passed (24h after confirmation). Booking will be auto-cancelled.");
        }

        // ========== CHECK EXISTING PENDING TRANSACTION ==========
        List<Transaction> existingTransactions = transactionRepository.findByBookingId(booking.getId());

        // Tìm transaction PENDING (nếu có)
        Transaction pendingTransaction = existingTransactions.stream()
                .filter(t -> "PENDING".equals(t.getPaymentStatus()))
                .findFirst()
                .orElse(null);

        if (pendingTransaction != null) {
            // Có transaction PENDING rồi
            log.info("⚠️ Found existing PENDING transaction {} for booking {}",
                pendingTransaction.getId(), booking.getId());

            // Check xem payUrl còn valid không (PayOS payment link expires sau 60 phút)
            LocalDateTime createdAt = pendingTransaction.getTransactionDate();
            LocalDateTime expiresAt = createdAt.plusMinutes(60); // ✅ Match với PayOS config

            if (LocalDateTime.now().isBefore(expiresAt)) {
                // PayUrl còn valid, reuse transaction cũ
                log.info("✅ Reusing existing transaction. PayUrl still valid until {}", expiresAt);

                return PaymentInitResponse.builder()
                        .transactionId(pendingTransaction.getId())
                        .orderId(pendingTransaction.getOrderId())
                        .payUrl(pendingTransaction.getPayUrl())
                        .amount(pendingTransaction.getTotalAmount())
                        .paymentMethod(pendingTransaction.getPaymentMethod())
                        .expiresAt(expiresAt)
                        .build();
            } else {
                // PayUrl đã expired (> 60 phút), update transaction thành EXPIRED
                log.warn("⚠️ Existing transaction expired (> 60 min). Creating new one.");
                pendingTransaction.setPaymentStatus("EXPIRED");
                pendingTransaction.setMessage("Payment link expired after 60 minutes");
                transactionRepository.save(pendingTransaction);
            }
        }

        // ========== TẠO TRANSACTION MỚI ==========
        // Count retry attempts (chỉ đếm FAILED/EXPIRED, không đếm PENDING)
        int retryCount = (int) existingTransactions.stream()
                .filter(t -> "FAILED".equals(t.getPaymentStatus())
                          || "EXPIRED".equals(t.getPaymentStatus()))
                .count();

        // Generate unique orderCode for PayOS
        // First attempt: bookingId (e.g., "123")
        // Retry 1: bookingId + "Retry1" (e.g., "123Retry1")
        // Retry 2: bookingId + "Retry2" (e.g., "123Retry2")
        String orderCode = retryCount == 0
            ? String.valueOf(booking.getId())
            : booking.getId() + "Retry" + retryCount;

        log.info("💳 Creating new transaction. Retry attempt: {}, OrderCode: {}", retryCount, orderCode);

        // 6. Use booking's total price (promotion already applied during booking creation)
        BigDecimal finalAmount = booking.getTotalPrice();

        // Get promotion info if used (prevent NullPointerException if no promotion)
        BigDecimal discountAmount = BigDecimal.ZERO;
        String promotionAppliedCode = null;

        Optional<PromotionUsage> usageOpt = promotionUsageRepository.findByBookingId(booking.getId());
        if (usageOpt.isPresent()) {
            PromotionUsage usage = usageOpt.get();
            discountAmount = usage.getDiscountAmount();
            promotionAppliedCode = usage.getUserPromotion().getPromotion().getCode();
            log.info("📋 Promotion applied: code={}, discount={}", promotionAppliedCode, discountAmount);
        } else {
            log.info("ℹ️ No promotion used for this booking");
        }

        // 7. Call payment gateway (PayOS) with custom orderCode
        if (!"PAYOS".equals(request.getPaymentMethod())) {
            throw new InvalidException("Unsupported payment method: " + request.getPaymentMethod() + ". Only PAYOS is currently supported.");
        }

        // Pass custom orderCode to PayOS (supports retry with unique orderCode)
        PayOSPaymentData paymentData = payOSService.createPaymentForBooking(booking, orderCode);

        // 8. Find the transaction created by PayOSService (using custom orderCode)
        Transaction transaction = transactionRepository.findByOrderId(orderCode)
                .orElseThrow(() -> new RuntimeException("Transaction not found after PayOS call with orderCode: " + orderCode));

        // Update transaction with final amount
        transaction.setTotalAmount(finalAmount);
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setPromotionCode(promotionAppliedCode);
        transaction.setDiscountAmount(discountAmount);
        transaction.setCounterAccountNumber(paymentData.getAccountNumber());
        transaction.setCounterAccountName(paymentData.getAccountName());
        transactionRepository.save(transaction);

        log.info("✅ Payment initialized successfully. Transaction ID: {}, Amount: {}", transaction.getId(), finalAmount);

        // 9. Return payment data
        return PaymentInitResponse.builder()
                .transactionId(transaction.getId())
                .orderId(transaction.getOrderId())
                .payUrl(paymentData.getCheckoutUrl())
                .amount(finalAmount)
                .paymentMethod(request.getPaymentMethod())
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // PayOS default timeout
                .build();
    }
}




