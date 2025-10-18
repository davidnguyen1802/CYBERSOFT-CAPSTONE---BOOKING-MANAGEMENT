package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.UserPromotion;
import com.Cybersoft.Final_Capstone.dto.UserPromotionDTO;

public class UserPromotionMapper {
    public static UserPromotionDTO toDTO (UserPromotion userPromotion){
        UserPromotionDTO dto = new UserPromotionDTO();
        dto.setId(userPromotion.getId());
        dto.setPromotionCode(userPromotion.getPromotion().getCode());
        dto.setPromotionName(userPromotion.getPromotion().getName());
        dto.setUserId(userPromotion.getUserAccount().getId());
        dto.setUserName(userPromotion.getUserAccount().getFullName());
        dto.setActive(userPromotion.getStatus().getName().equals("ACTIVE"));
        dto.setAssignedDate(userPromotion.getAssignedAt());
        dto.setExpiresDate(userPromotion.getExpiresAt());
        dto.setPromotionUsages(userPromotion.getUsages().stream().map(PromotionUsageMapper::toDTO).toList());
        return dto;
    }
}
