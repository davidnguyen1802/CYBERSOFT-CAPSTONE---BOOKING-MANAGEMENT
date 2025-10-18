package com.Cybersoft.Final_Capstone.payload.request;

import com.Cybersoft.Final_Capstone.Enum.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class PromotionRequest {
    private String code;
    private String name;
    private String description;
    private BigDecimal discountValue;
    private Integer discountType;
    private BigDecimal minPurchaseLimit;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private Integer remainingDays;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private boolean isActive;
}
