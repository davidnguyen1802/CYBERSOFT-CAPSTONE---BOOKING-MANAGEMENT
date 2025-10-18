package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.PromotionUsage;
import com.Cybersoft.Final_Capstone.dto.PromotionUsageDTO;
import lombok.Data;

@Data
public class PromotionUsageMapper {
    public static PromotionUsageDTO toDTO(PromotionUsage promotionUsage){
        PromotionUsageDTO dto = new PromotionUsageDTO();
        dto.setId(promotionUsage.getId().getUserPromotionId());
        dto.setIdBooking(promotionUsage.getBooking().getId());
        dto.setIdUserPromotion(promotionUsage.getUserPromotion().getId());
        dto.setDiscountAmount(promotionUsage.getDiscountAmount());
        dto.setUsedDate(promotionUsage.getUsedAt());
        return dto;
    }
}
