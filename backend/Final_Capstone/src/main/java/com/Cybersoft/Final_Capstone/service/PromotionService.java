package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.PromotionDTO;
import com.Cybersoft.Final_Capstone.payload.request.PromotionRequest;

import java.util.List;

public interface PromotionService {
    void insertPromotion(PromotionRequest promotionRequest);
    PromotionDTO getPromotionByCode(String code);
    PromotionDTO getPromotionByName(String name);
    List<PromotionDTO>findAll();
    void deletePromotion(String name);
    PromotionDTO updatePromotion(String name, PromotionRequest promotionRequest);

}
