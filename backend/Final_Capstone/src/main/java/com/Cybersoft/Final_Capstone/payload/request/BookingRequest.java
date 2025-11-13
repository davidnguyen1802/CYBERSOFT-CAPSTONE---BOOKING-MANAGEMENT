package com.Cybersoft.Final_Capstone.payload.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull(message = "User ID is required" )
    private Integer userId;
    @NotNull(message = "Property ID is required")
    private Integer propertyId;

    @NotNull(message = "Check-in date is required")
    @Future(message = "Check-in date must be in the future")
    private LocalDateTime checkIn;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDateTime checkOut;

    @NotNull(message = "Number of adults is required")
    @Min(value = 1, message = "At least 1 adult is required")
    private Integer numAdults;

    @Min(value = 0, message = "Number of children cannot be negative")
    private Integer numChildren;

    @Min(value = 0, message = "Number of infant cannot be negative")
    private Integer num_infant;

    @Min(value = 0, message = "Number of pet cannot be negative")
    private Integer num_pet;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    // ========== PROMOTION FIELDS (OPTIONAL) ==========
    // User can optionally apply a promotion code when creating booking
    // Frontend validates via GET /promotions/validate before submitting
    
    @Size(max = 50, message = "Promotion code cannot exceed 50 characters")
    private String promotionCode;
    
    // Original booking amount BEFORE discount (calculated by FE, verified by BE)
    @DecimalMin(value = "0.0", inclusive = false, message = "Original amount must be positive")
    private BigDecimal originalAmount;
}

