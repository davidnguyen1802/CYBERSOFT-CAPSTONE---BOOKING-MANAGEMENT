package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.UserPromotionDTO;

import java.util.List;

public interface UserPromotionService {
    Integer countActivePromotionsByUserId(int userId);
    List<UserPromotionDTO> getActivePromotionsByUserId(int userId);

}
