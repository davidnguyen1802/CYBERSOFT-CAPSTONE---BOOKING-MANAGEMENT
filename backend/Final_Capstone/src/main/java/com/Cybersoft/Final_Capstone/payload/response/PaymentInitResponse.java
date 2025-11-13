package com.Cybersoft.Final_Capstone.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment initialization
 * Contains payment URL and transaction details
 * Note: Promotion discount is already included in the amount
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitResponse {
    private Long transactionId;
    private String orderId;
    private String payUrl; // URL to redirect user for payment
    private BigDecimal amount; // Amount to pay (already includes any promotion discount from booking)
    private String paymentMethod;
    private LocalDateTime expiresAt; // Payment link expiration time
}
