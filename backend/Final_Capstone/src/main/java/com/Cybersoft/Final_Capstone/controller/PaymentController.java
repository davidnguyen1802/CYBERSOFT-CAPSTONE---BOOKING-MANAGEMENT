package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.request.PayOSWebhookRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentWebhookService paymentWebhookService;

    /**
     * Webhook endpoint for PayOS payment notifications
     * This endpoint is called by PayOS when a payment status changes
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handlePaymentWebhook(@RequestBody PayOSWebhookRequest webhookRequest) {
        log.info("📥 Received PayOS webhook: orderCode={}, status={}",
                webhookRequest.getData() != null ? webhookRequest.getData().getOrderCode() : "N/A",
                webhookRequest.getData() != null ? "SUCCESS" : "FAILED");

        try {
            // Verify signature and process payment
            paymentWebhookService.processWebhook(webhookRequest);

            BaseResponse response = new BaseResponse();
            response.setCode(HttpStatus.OK.value());
            response.setMessage("Webhook processed successfully");
            response.setData(null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error processing webhook: {}", e.getMessage(), e);

            BaseResponse response = new BaseResponse();
            response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to process webhook: " + e.getMessage());
            response.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Check payment status for a booking
     */
    @GetMapping("/status/{bookingId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Integer bookingId) {
        log.info("📊 Checking payment status for booking ID: {}", bookingId);

        try {
            Map<String, Object> paymentStatus = paymentWebhookService.getPaymentStatus(bookingId);

            BaseResponse response = new BaseResponse();
            response.setCode(HttpStatus.OK.value());
            response.setMessage("Payment status retrieved successfully");
            response.setData(paymentStatus);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting payment status: {}", e.getMessage());

            BaseResponse response = new BaseResponse();
            response.setCode(HttpStatus.NOT_FOUND.value());
            response.setMessage("Payment status not found: " + e.getMessage());
            response.setData(null);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}
