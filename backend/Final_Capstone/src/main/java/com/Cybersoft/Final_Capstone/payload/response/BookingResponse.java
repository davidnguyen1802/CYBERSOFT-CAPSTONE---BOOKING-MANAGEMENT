package com.Cybersoft.Final_Capstone.payload.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class BookingResponse {
    private Integer id;
    private Integer userId;
    private Integer propertyId;
    private String propertyName;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private BigDecimal totalPrice;
    private Integer numAdults;
    private Integer numChildren;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
