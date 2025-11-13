package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.UserPromotionDTO;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserPromotionService {
    Integer countActivePromotionsByUserId(int userId);
    List<UserPromotionDTO> getPromotionsByUserId(int userId);
    PageResponse<UserPromotionDTO> getPromotionsByUserId(int userId, Pageable pageable);
    void deletePromotions(int id, int userId) throws IllegalAccessException;
    
    /**
     * User claims a promotion by entering promotion code
     * Creates UserPromotion with ACTIVE status and unlocked state
     * Does NOT increment Promotion.timesUsed (only increments on payment success)
     * 
     * @param code Promotion code to claim
     * @return Created UserPromotionDTO
     */
    UserPromotionDTO claimPromotion(String code);
    
    /**
     * Admin assigns a promotion to a specific user
     * Similar to claim but initiated by admin
     * 
     * @param userId Target user ID
     * @param promotionCode Promotion code to assign
     * @return Created UserPromotionDTO
     */
    UserPromotionDTO assignPromotionToUser(Integer userId, String promotionCode);

}
