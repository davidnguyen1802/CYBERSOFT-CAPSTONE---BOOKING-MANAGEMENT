package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HostStatisticsDTO {
    private Integer hostId;
    private String hostName;
    private Integer totalProperties;
    private Integer totalBookings;
    private Integer pendingBookings;
    private Integer confirmedBookings;
    private Integer completedBookings;
    private Integer cancelledBookings;
    private BigDecimal totalRevenue;
    private BigDecimal averageRating;
}

