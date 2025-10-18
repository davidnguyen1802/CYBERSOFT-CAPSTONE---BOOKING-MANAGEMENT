package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Promotion;
import com.Cybersoft.Final_Capstone.dto.PromotionDTO;
import com.Cybersoft.Final_Capstone.repository.StatusRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Data
public class PromotionMapper {
    public static PromotionDTO toDTO(Promotion promotion) {
        PromotionDTO dto = new PromotionDTO();
        dto.setId(promotion.getId());
        dto.setName(promotion.getName());
        dto.setCode(promotion.getCode());
        dto.setDescription(promotion.getDescription());
        BigDecimal discountValue = promotion.getDiscountValue();

        if (discountValue != null
                && discountValue.compareTo(BigDecimal.ZERO) > 0
                && discountValue.compareTo(BigDecimal.valueOf(100)) < 0) {
            dto.setDiscountValue(discountValue.toString() + "%");
            dto.setDiscountType(promotion.getDiscountType().toString());
        }
        else if (discountValue != null) {
            dto.setDiscountValue(discountValue.toString());
            dto.setDiscountType(promotion.getDiscountType().toString());
        }
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setActive(promotion.getStatus().getName().equals("Active"));
        dto.setUsageLimit(promotion.getUsageLimit());
        dto.setTimesUsed(promotion.getTimesUsed());
        dto.setCreatedAt(promotion.getCreatedAt());
        dto.setUpdatedAt(promotion.getUpdatedAt());
        dto.setUserPromotions(promotion.getUserPromotions().stream().map(UserPromotionMapper::toDTO).toList());
        return dto;
    }
}
