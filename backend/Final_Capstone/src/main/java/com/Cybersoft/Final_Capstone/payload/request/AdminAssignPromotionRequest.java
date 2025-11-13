package com.Cybersoft.Final_Capstone.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin assigning a promotion to a user
 * POST /api/promotions/admin/user-promotions/assign
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAssignPromotionRequest {
    
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Integer userId;
    
    @NotBlank(message = "Promotion code is required")
    @Size(min = 3, max = 50, message = "Promotion code must be between 3 and 50 characters")
    private String promotionCode;
}












