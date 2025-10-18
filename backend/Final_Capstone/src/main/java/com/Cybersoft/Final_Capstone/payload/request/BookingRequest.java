package com.Cybersoft.Final_Capstone.payload.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {

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

    @Min(value = 0, message = "Number of teenager cannot be negative")
    private Integer num_teenager;

    @Min(value = 0, message = "Number of infant cannot be negative")
    private Integer num_infant;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    // Optional: Promotion code if you want to apply promotions during booking
    private String promotionCode;
}

