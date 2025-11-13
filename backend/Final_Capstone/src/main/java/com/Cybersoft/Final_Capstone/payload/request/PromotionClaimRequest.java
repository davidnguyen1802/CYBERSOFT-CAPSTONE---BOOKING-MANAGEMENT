package com.Cybersoft.Final_Capstone.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user claiming a promotion by code
 * POST /api/promotions/claim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionClaimRequest {
    
    @NotBlank(message = "Promotion code is required")
    @Size(min = 3, max = 50, message = "Promotion code must be between 3 and 50 characters")
    private String code;
}












