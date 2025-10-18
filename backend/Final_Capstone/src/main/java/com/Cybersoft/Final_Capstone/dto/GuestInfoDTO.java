package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GuestInfoDTO {
    private Integer bookingId;
    private Integer guestId;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private Integer propertyId;
    private String propertyName;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Integer numAdults;
    private Integer numChildren;
    private String bookingStatus;
    private String notes;
}

