package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingDTO {
    private Integer id;
    private Integer userId;
    private String userName;
    private Integer propertyId;
    private String propertyName;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private BigDecimal totalPrice;
    private Integer numAdults;
    private Integer numChildren;
    private Integer num_teenager;
    private Integer num_infant;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

