package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionDTO {
    private int id;
    private String name;
    private String code;
    private String description;
    private String discountValue;
    private String discountType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
    private int usageLimit;
    private int timesUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UserPromotionDTO> userPromotions;

}
