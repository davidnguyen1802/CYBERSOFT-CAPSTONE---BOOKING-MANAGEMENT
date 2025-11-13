package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Promotion;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.Entity.UserPromotion;
import com.Cybersoft.Final_Capstone.dto.UserPromotionDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.InvalidException;
import com.Cybersoft.Final_Capstone.mapper.UserPromotionMapper;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.repository.PromotionRepository;
import com.Cybersoft.Final_Capstone.repository.StatusRepository;
import com.Cybersoft.Final_Capstone.repository.UserAccountRepository;
import com.Cybersoft.Final_Capstone.repository.UserPromotionRepository;
import com.Cybersoft.Final_Capstone.service.UserPromotionService;
import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserPromotionServiceImp implements UserPromotionService {
    @Autowired
    private UserPromotionRepository userPromotionRepository;
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private StatusRepository statusRepository;
    
    @Autowired
    private SecurityUtil securityUtil;

    @Override
    public Integer countActivePromotionsByUserId(int userId) {
        return userPromotionRepository.countByUserAccount_IdAndStatus_Name(userId, "ACTIVE");
    }

    @Override
    public List<UserPromotionDTO> getPromotionsByUserId(int userId) {
        return userPromotionRepository.findByUserAccount_Id(userId)
                .stream().map(UserPromotionMapper::toDTO).toList();
    }

    @Override
    public PageResponse<UserPromotionDTO> getPromotionsByUserId(int userId, Pageable pageable) {
        log.info("📄 Fetching promotions for userId: {} with pageable: {}", userId, pageable);
        Page<UserPromotion> promotionPage = userPromotionRepository.findByUserAccount_Id(userId, pageable);
        
        log.info("📊 Page info - totalElements: {}, totalPages: {}, currentPage: {}, size: {}", 
                promotionPage.getTotalElements(), 
                promotionPage.getTotalPages(), 
                promotionPage.getNumber(), 
                promotionPage.getSize());
        
        List<UserPromotionDTO> promotionDTOs = promotionPage.getContent().stream()
                .map(UserPromotionMapper::toDTO)
                .toList();
        
        PageResponse<UserPromotionDTO> response = PageResponse.<UserPromotionDTO>builder()
                .content(promotionDTOs)
                .currentPage(promotionPage.getNumber())
                .pageSize(promotionPage.getSize())
                .totalElements(promotionPage.getTotalElements())
                .totalPages(promotionPage.getTotalPages())
                .first(promotionPage.isFirst())
                .last(promotionPage.isLast())
                .empty(promotionPage.isEmpty())
                .build();
        
        log.info("✅ PageResponse built - totalElements in response: {}", response.getTotalElements());
        return response;
    }

    @Override
    public void deletePromotions(int id, int userId) throws IllegalAccessException {
        List<UserPromotion> promotions = userPromotionRepository.findByUserAccount_Id(userId);
        boolean exists = promotions.stream().anyMatch(promotion -> promotion.getId() == id);
        if (!exists) {
            throw new IllegalAccessException("User does not have permission to delete this promotion");
        }
        try {
            userPromotionRepository.deleteById(id);
        } catch (Exception e){
            throw new DataNotFoundException("Promotion not found with id: " + id);
        }
    }
    
    @Override
    @Transactional
    public UserPromotionDTO claimPromotion(String code) {
        log.info("🎟️ User claiming promotion with code: {}", code);
        
        // 1. Get current authenticated user
        UserAccount currentUser = securityUtil.getLoggedInUser();
        if (currentUser == null) {
            throw new InvalidException("User not authenticated");
        }
        
        // 2. Find promotion by code
        Promotion promotion = promotionRepository.findByCodeAndStatus_Name(code, "ACTIVE")
                .orElseThrow(() -> new DataNotFoundException("Promotion not found or inactive with code: " + code));
        
        // 3. Validate promotion dates
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate())) {
            throw new InvalidException("Promotion has not started yet. Start date: " + promotion.getStartDate());
        }
        if (now.isAfter(promotion.getEndDate())) {
            throw new InvalidException("Promotion has expired. End date: " + promotion.getEndDate());
        }
        
        // 4. Check if user already claimed this promotion (unique constraint will catch this too)
        // But we provide a better error message
        boolean alreadyClaimed = userPromotionRepository.findByUserAccountAndPromotionCode(currentUser.getId(), code).isPresent();
        if (alreadyClaimed) {
            throw new InvalidException("You have already claimed this promotion");
        }
        
        // 5. Create UserPromotion
        UserPromotion userPromotion = new UserPromotion();
        userPromotion.setPromotion(promotion);
        userPromotion.setUserAccount(currentUser);
        userPromotion.setStatus(new Status(1)); // ACTIVE
        userPromotion.setIsLocked(false);
        // assignedAt and expiresAt will be set by @PrePersist
        
        try {
            UserPromotion saved = userPromotionRepository.save(userPromotion);
            log.info("✅ User {} successfully claimed promotion: {}", currentUser.getId(), code);
            return UserPromotionMapper.toDTO(saved);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation
            log.error("❌ Duplicate claim attempt for user {} and promotion {}", currentUser.getId(), code);
            throw new InvalidException("You have already claimed this promotion");
        }
    }
    
    @Override
    @Transactional
    public UserPromotionDTO assignPromotionToUser(Integer userId, String promotionCode) {
        log.info("🎟️ Admin assigning promotion {} to user {}", promotionCode, userId);
        
        // 1. Find target user
        UserAccount targetUser = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));
        
        // 2. Find promotion by code
        Promotion promotion = promotionRepository.findByCodeAndStatus_Name(promotionCode, "ACTIVE")
                .orElseThrow(() -> new DataNotFoundException("Promotion not found or inactive with code: " + promotionCode));
        
        // 3. Validate promotion dates
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate())) {
            throw new InvalidException("Promotion has not started yet. Start date: " + promotion.getStartDate());
        }
        if (now.isAfter(promotion.getEndDate())) {
            throw new InvalidException("Promotion has expired. End date: " + promotion.getEndDate());
        }
        
        // 4. Check if user already has this promotion
        boolean alreadyAssigned = userPromotionRepository.findByUserAccountAndPromotionCode(userId, promotionCode).isPresent();
        if (alreadyAssigned) {
            throw new InvalidException("User already has this promotion assigned");
        }
        
        // 5. Create UserPromotion
        UserPromotion userPromotion = new UserPromotion();
        userPromotion.setPromotion(promotion);
        userPromotion.setUserAccount(targetUser);
        userPromotion.setStatus(new Status(1)); // ACTIVE
        userPromotion.setIsLocked(false);
        
        try {
            UserPromotion saved = userPromotionRepository.save(userPromotion);
            log.info("✅ Admin successfully assigned promotion {} to user {}", promotionCode, userId);
            return UserPromotionMapper.toDTO(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("❌ Duplicate assignment attempt for user {} and promotion {}", userId, promotionCode);
            throw new InvalidException("User already has this promotion assigned");
        }
    }
}
