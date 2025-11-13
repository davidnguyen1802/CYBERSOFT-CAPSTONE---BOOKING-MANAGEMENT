package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingDTO {
    private Integer id;
    private Integer userId;
    private Integer propertyId;
    private String propertyName;
    private BigDecimal propertyPricePerNight;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private BigDecimal totalPrice;
    private Integer numAdults;
    private Integer numChildren;
    private Integer num_infant;
    private Integer numPet;
    private String notes;
    private String status;
    
    // Promotion fields (if promotion was applied)
    private String promotionCode;          // Promotion code used (null if no promotion)
    private BigDecimal originalAmount;     // Original price before discount (null if no promotion)
    private BigDecimal discountAmount;     // Discount amount (null if no promotion)

    // New fields for booking flow
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String cancelReason;
    
    // Computed fields
    private LocalDateTime paymentDeadline; // confirmedAt + 24h
    private Boolean isPaymentExpired;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

