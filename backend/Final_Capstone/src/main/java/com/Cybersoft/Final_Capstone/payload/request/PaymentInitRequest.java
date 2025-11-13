package com.Cybersoft.Final_Capstone.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initializing payment for a confirmed booking
 * Note: Promotion handling is done during booking creation, not during payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitRequest {
    @NotNull(message = "Booking ID is required")
    private Integer bookingId;
    
    @NotNull(message = "Payment method is required")
    private String paymentMethod; // PAYOS, VNPAY, STRIPE, etc.
}




