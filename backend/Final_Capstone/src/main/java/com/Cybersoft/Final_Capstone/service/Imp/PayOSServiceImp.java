package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.Transaction;
import com.Cybersoft.Final_Capstone.config.PayOSConfig;
import com.Cybersoft.Final_Capstone.exception.PayOSException;
import com.Cybersoft.Final_Capstone.payload.request.PayOSItemRequest;
import com.Cybersoft.Final_Capstone.payload.request.PayOSPaymentRequest;
import com.Cybersoft.Final_Capstone.payload.response.PayOSPaymentData;
import com.Cybersoft.Final_Capstone.payload.response.PayOSPaymentResponse;
import com.Cybersoft.Final_Capstone.repository.BookingRepository;
import com.Cybersoft.Final_Capstone.repository.TransactionRepository;
import com.Cybersoft.Final_Capstone.service.PayOSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
public class PayOSServiceImp implements PayOSService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayOSServiceImp.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestTemplate restTemplate;
    private final PayOSConfig payOSConfig;
    private final TransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;

    public PayOSServiceImp(RestTemplate restTemplate,
                           PayOSConfig payOSConfig,
                           TransactionRepository transactionRepository,
                           BookingRepository bookingRepository) {
        this.restTemplate = restTemplate;
        this.payOSConfig = payOSConfig;
        this.transactionRepository = transactionRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public PayOSPaymentData createPaymentForBooking(Booking booking) {
        // Default: use booking.getId() as orderCode
        return createPaymentForBooking(booking, String.valueOf(booking.getId()));
    }

    @Override
    public PayOSPaymentData createPaymentForBooking(Booking booking, String orderCode) {
        if (booking == null || booking.getId() == null) {
            throw new PayOSException("Booking must be persisted before creating a PayOS payment request");
        }

        validateConfiguration();

        // Fetch booking with all relationships to avoid lazy loading issues
        Booking fullBooking = bookingRepository.findByIdWithDetails(booking.getId())
                .orElseThrow(() -> new PayOSException("Booking not found with id: " + booking.getId()));

        PayOSPaymentRequest paymentRequest = buildRequestPayload(fullBooking.getId(), orderCode);
        HttpHeaders headers = buildHeaders();

        try {
            LOGGER.info("Creating PayOS payment for booking ID: {}, orderCode: {}, amount: {}",
                fullBooking.getId(), orderCode, fullBooking.getTotalPrice());
            LOGGER.info("PayOS endpoint: {}", payOSConfig.getEndpoint());
            LOGGER.info("PayOS request payload: {}", paymentRequest);

            ResponseEntity<PayOSPaymentResponse> response = restTemplate.exchange(
                    payOSConfig.getEndpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(paymentRequest, headers),
                    PayOSPaymentResponse.class
            );

            LOGGER.info("PayOS response status: {}", response.getStatusCode());
            PayOSPaymentResponse responseBody = response.getBody();

            // ✅ CHECK NULL
            if (responseBody == null) {
                throw new PayOSException("PayOS returned null response");
            }

            // ✅ CHECK RESPONSE CODE
            String responseCode = String.valueOf(responseBody.getCode());
            LOGGER.info("PayOS response: code={}, desc={}", responseCode, responseBody.getDesc());

            if (!"00".equals(responseCode) && !"0".equals(responseCode)) {
                String errorMsg = String.format("PayOS error [%s]: %s", responseCode, responseBody.getDesc());
                LOGGER.error(errorMsg);

                // Map common errors
                switch (responseCode) {
                    case "ORDER_FOUND":
                        throw new PayOSException("OrderCode already exists (duplicate request)");
                    case "PAYMENT_REQUEST_DATA_SIGNATURE_INCORRECT":
                        throw new PayOSException("Signature incorrect. Check checksumKey");
                    case "AMOUNT_NOT_INTEGER":
                        throw new PayOSException("Amount must be integer (no decimal)");
                    case "INVALID_PARAM":
                        throw new PayOSException("Invalid parameters. Check required fields");
                    case "401":
                        throw new PayOSException("Authentication failed. Check x-client-id and x-api-key");
                    default:
                        throw new PayOSException(errorMsg);
                }
            }

            // ✅ CHECK DATA FIELD (chỉ khi code = "00")
            if (responseBody.getData() == null) {
                throw new PayOSException("PayOS response missing data field (code=00 but no data)");
            }

            LOGGER.info("✅ PayOS payment created successfully. OrderCode: {}", responseBody.getData().getOrderCode());
            LOGGER.info("PayOS checkout URL: {}", responseBody.getData().getCheckoutUrl());

            PayOSPaymentData paymentData = responseBody.getData();
            if (!StringUtils.hasText(paymentData.getMessage())) {
                paymentData.setMessage(responseBody.getDesc());
            }

            persistTransaction(fullBooking, paymentData, orderCode);

            return paymentData;
        } catch (HttpStatusCodeException ex) {
            String message = String.format("PayOS request failed with status %s: %s", ex.getStatusCode(), ex.getResponseBodyAsString());
            LOGGER.error(message, ex);
            throw new PayOSException(message, ex);
        } catch (RestClientException ex) {
            String message = "Failed to execute PayOS request";
            LOGGER.error(message, ex);
            throw new PayOSException(message, ex);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while creating PayOS payment", ex);
            throw new PayOSException("Unexpected error: " + ex.getMessage(), ex);
        }

    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSConfig.getClientId());
        headers.set("x-api-key", payOSConfig.getApiKey());
        return headers;
    }


    private void validateConfiguration() {
        if (!StringUtils.hasText(payOSConfig.getClientId()) || !StringUtils.hasText(payOSConfig.getApiKey())
                || !StringUtils.hasText(payOSConfig.getChecksumKey())) {
            throw new PayOSException("PayOS configuration is incomplete. Please verify clientId, apiKey, and checksumKey");
        }

        if (!StringUtils.hasText(payOSConfig.getCancelUrl()) || !StringUtils.hasText(payOSConfig.getReturnUrl())) {
            throw new PayOSException("PayOS configuration must include cancel and return URLs");
        }

        if (!StringUtils.hasText(payOSConfig.getEndpoint())) {
            throw new PayOSException("PayOS endpoint configuration is missing");
        }
    }

    private PayOSPaymentRequest buildRequestPayload(Integer bookingId, String orderCode) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new PayOSException("Booking not found"));

        // ✅ Convert orderCode to int for PayOS (PayOS requires integer type)
        int orderCodeInt;
        try {
            if (orderCode.contains("Retry")) {
                // Extract bookingId from orderCode string
                String bookingIdStr = orderCode.replaceAll("\\D.*", "");
                int bookingIdNum = Integer.parseInt(bookingIdStr);

                // ✅ Generate unique suffix with timestamp (4 digits) - optimized for many bookings
                // 4 digits (0-9999) = ~10 seconds cycle, enough to distinguish retry attempts
                int timestamp = (int) (System.currentTimeMillis() % 10000);

                // Combine: bookingId * 10000 + timestamp
                // Example: bookingId=123, timestamp=5678 → orderCode=1235678
                // Supports up to ~214,000 bookings (still < Integer.MAX_VALUE = 2,147,483,647)
                orderCodeInt = bookingIdNum * 10000 + timestamp;

                LOGGER.info("Generated retry orderCode: {} (bookingId={}, timestamp={})",
                    orderCodeInt, bookingIdNum, timestamp);
            } else {
                orderCodeInt = Integer.parseInt(orderCode);
            }
        } catch (NumberFormatException | ArithmeticException e) {
            // Fallback: Use timestamp only
            orderCodeInt = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            LOGGER.warn("Cannot parse orderCode '{}', using timestamp: {}", orderCode, orderCodeInt);
        }

        long amount = toLong(booking.getTotalPrice());

        // ✅ Short description (to comply with 9 char limit for non-linked accounts)
        String description = "BK" + bookingId;  // Example: "BK123"

        // Validate length (optional - only needed if account not linked via PayOS)
        if (description.length() > 9) {
            description = description.substring(0, 9);
            LOGGER.warn("Description truncated to 9 chars: {}", description);
        }

        long expiredAt = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();

        // Calculate number of nights from check-in to check-out
        long nights = ChronoUnit.DAYS.between(
            booking.getCheckIn().toLocalDate(),
            booking.getCheckOut().toLocalDate()
        );

        LOGGER.info("Building PayOS request - BookingId: {}, OrderCode: {}, Amount: {}, Description: '{}', Nights: {}",
                    bookingId, orderCodeInt, amount, description, nights);

        PayOSItemRequest item = PayOSItemRequest.builder()
                .name(booking.getProperty().getPropertyName())
                .price(toLong(booking.getProperty().getPricePerNight()))
                .quantity((int) nights)  // ✅ Calculate from booking dates
                // .taxPercentage(8)     // ⚠️ COMMENTED - Tax not implemented yet
                .unit("night")
                .build();

        // ✅ Generate signature with int orderCode
        String signature = generateSignature(amount, payOSConfig.getCancelUrl(), description, orderCodeInt, payOSConfig.getReturnUrl());

        return PayOSPaymentRequest.builder()
                .orderCode(orderCodeInt)  // ✅ Integer type
                .amount(amount)
                .description(description)
                .cancelUrl(payOSConfig.getCancelUrl())
                .returnUrl(payOSConfig.getReturnUrl())
                .expiredAt(expiredAt)
                .signature(signature)
                .items(List.of(item))
                .build();
    }

    private long toLong(BigDecimal value) {
        if (value == null) {
            return 0L;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private void persistTransaction(Booking booking, PayOSPaymentData paymentData, String orderCode) {
        if (booking.getUser() == null) {
            LOGGER.warn("Booking {} does not include user information. Transaction will not be persisted", booking.getId());
            return;
        }

        // ⚠️ IMPORTANT: orderId now uses custom orderCode (booking.getId() or booking.getId()+"Retry"+N)
        // This ensures PayOS receives unique orderCode for each retry attempt
        LOGGER.info("💾 Creating transaction for booking #{} with orderId: {}", booking.getId(), orderCode);

        Transaction transaction = new Transaction();
        transaction.setUser(booking.getUser());
        transaction.setBooking(booking);
        transaction.setOrderId(orderCode);  // ✅ Use custom orderCode (supports retry)
        transaction.setPaymentStatus("PENDING");
        transaction.setPayUrl(paymentData.getCheckoutUrl());
        transaction.setMessage(paymentData.getMessage());

        Transaction savedTransaction = transactionRepository.save(transaction);
        LOGGER.info("✅ Transaction saved: ID={}, OrderId={}, Status={}",
            savedTransaction.getId(), savedTransaction.getOrderId(), savedTransaction.getPaymentStatus());
    }

    private String generateSignature(long amount, String cancelUrl, String description, int orderCode, String returnUrl) {
        // ✅ PayOS yêu cầu KHÔNG encode các giá trị khi tạo signature
        // Chỉ cần sắp xếp theo thứ tự alphabet và nối chuỗi
        try {
            // Build signature string (alphabetically sorted) - KHÔNG encode
            String rawData = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    amount, cancelUrl, description, orderCode, returnUrl);

            LOGGER.info("🔐 Signature raw data: {}", rawData);
            LOGGER.info("🔑 Checksum key length: {}", payOSConfig.getChecksumKey().length());

            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    payOSConfig.getChecksumKey().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));

            // Convert to lowercase hex (PayOS requires lowercase)
            String signature = HexFormat.of().formatHex(hmac);
            LOGGER.info("🔐 Generated signature: {}", signature);

            return signature;
        } catch (Exception ex) {
            throw new PayOSException("Failed to generate PayOS signature", ex);
        }
    }

}
