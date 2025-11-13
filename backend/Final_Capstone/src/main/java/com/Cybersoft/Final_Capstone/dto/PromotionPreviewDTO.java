package com.Cybersoft.Final_Capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for previewing promotion discount without consuming the promotion
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionPreviewDTO {
    private String promotionCode;
    private String promotionName;
    private String description;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String discountType; // PERCENTAGE or FIXED
    private BigDecimal discountValue; // e.g., 10 for 10% or $10
    private boolean isValid;
    private String errorMessage; // if not valid
}




