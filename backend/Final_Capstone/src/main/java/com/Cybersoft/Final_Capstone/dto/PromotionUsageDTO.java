package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionUsageDTO {
    private int id;
    private int idBooking;
    private int idUserPromotion;
    private BigDecimal discountAmount;
    private LocalDateTime usedDate;
}
