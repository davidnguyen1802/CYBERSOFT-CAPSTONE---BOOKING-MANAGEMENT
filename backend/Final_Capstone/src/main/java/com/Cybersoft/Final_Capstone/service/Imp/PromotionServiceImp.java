package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Promotion;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.Enum.DiscountType;
import com.Cybersoft.Final_Capstone.dto.PromotionDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.InvalidException;
import com.Cybersoft.Final_Capstone.mapper.PromotionMapper;
import com.Cybersoft.Final_Capstone.payload.request.PromotionRequest;
import com.Cybersoft.Final_Capstone.repository.PromotionRepository;
import com.Cybersoft.Final_Capstone.repository.StatusRepository;
import com.Cybersoft.Final_Capstone.service.PromotionService;
import com.Cybersoft.Final_Capstone.util.UpdateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromotionServiceImp implements PromotionService {
    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private StatusRepository statusRepository;
    @Override
    public void insertPromotion(PromotionRequest promotionRequest) {
        Promotion promotion = new Promotion();
        promotion.setName(promotionRequest.getName());
        promotion.setCode(promotionRequest.getCode());
        promotion.setDiscountValue(promotionRequest.getDiscountValue());
        promotion.setDescription(promotionRequest.getDescription());
        promotion.setStartDate(promotionRequest.getStartDate());
        promotion.setRemainingDays(promotionRequest.getRemainingDays());
        promotion.setUsageLimit(promotionRequest.getUsageLimit());
        promotion.setDiscountType(DiscountType.fromValue(promotionRequest.getDiscountType()));
        promotion.setMinPurchaseLimit(promotionRequest.getMinPurchaseLimit());
        promotion.setMaxDiscountAmount(promotionRequest.getMaxDiscountAmount());
        promotion.setStatus(new Status(1));
        try {
            promotionRepository.save(promotion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert promotion: " + e.getMessage());
        }

    }

    @Override
    public PromotionDTO getPromotionByCode(String code) {
        Promotion promotion = promotionRepository.findByCodeAndStatus_Name(code, "ACTIVE")
                .orElseThrow(()-> new DataNotFoundException("Promotion not found with code: " + code));
        return PromotionMapper.toDTO(promotion);
    }

    @Override
    public PromotionDTO getPromotionByName(String name) {
        Promotion promotion = promotionRepository.findByNameAndStatus_Name(name, "ACTIVE")
                .orElseThrow(()-> new DataNotFoundException("Promotion not found with name: " + name));
        if(promotion.getStatus().getName().equals("Inactive")){
            throw new InvalidException("Promotion is inactive with name: " + name);
        }
        return PromotionMapper.toDTO(promotion);
    }

    @Override
    public List<PromotionDTO> findAll() {
        return promotionRepository.findAll()
                .stream()
                .map(PromotionMapper::toDTO)
                .filter(PromotionDTO::isActive)
                .toList();
    }
    @Override
    public void deletePromotion(String name) {
        Promotion promotion = promotionRepository.findByNameAndStatus_Name(name, "ACTIVE")
                .orElseThrow(()-> new DataNotFoundException("Promotion not found with name: " + name));
        promotion.setStatus(new Status(1));
        try {
            promotionRepository.save(promotion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete promotion: " + e.getMessage());
        }

    }

    @Override
    public PromotionDTO updatePromotion(String code, PromotionRequest promotionRequest) {
        Promotion promotion = promotionRepository.findByCodeAndStatus_Name(code, "ACTIVE")
                .orElseThrow(()-> new DataNotFoundException("Promotion not found with code: " + code));
        UpdateHelper.copyNonNullChangedFields(promotionRequest, promotion, "isActive");
        if(promotionRequest.isActive()){
            promotion.setStatus(new Status(1));
        } else {
            promotion.setStatus(new Status(2));
        }
        try {
            promotionRepository.save(promotion);
            return PromotionMapper.toDTO(promotion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update promotion: " + e.getMessage());
        }

    }
}
