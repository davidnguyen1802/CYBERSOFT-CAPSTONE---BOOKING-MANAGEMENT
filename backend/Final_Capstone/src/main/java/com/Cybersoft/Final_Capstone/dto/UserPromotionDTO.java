package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserPromotionDTO {
    private int id;
    private LocalDateTime assignedDate;
    private LocalDateTime expiresDate;
    private boolean isActive;
    private String promotionName;
    private String promotionCode;
    private int userId;
    private String userName;
    private List<PromotionUsageDTO> promotionUsages;
}
