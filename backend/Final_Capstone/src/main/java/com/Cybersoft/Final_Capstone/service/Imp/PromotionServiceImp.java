package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.Promotion;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.Entity.UserPromotion;
import com.Cybersoft.Final_Capstone.Enum.DiscountType;
import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import com.Cybersoft.Final_Capstone.dto.PromotionDTO;
import com.Cybersoft.Final_Capstone.dto.PromotionPreviewDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.InvalidException;
import com.Cybersoft.Final_Capstone.mapper.PromotionMapper;
import com.Cybersoft.Final_Capstone.payload.request.PromotionRequest;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.repository.BookingRepository;
import com.Cybersoft.Final_Capstone.repository.PromotionRepository;
import com.Cybersoft.Final_Capstone.repository.StatusRepository;
import com.Cybersoft.Final_Capstone.repository.UserPromotionRepository;
import com.Cybersoft.Final_Capstone.service.PromotionService;
import com.Cybersoft.Final_Capstone.util.UpdateHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PromotionServiceImp implements PromotionService {
    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private StatusRepository statusRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserPromotionRepository userPromotionRepository;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @Autowired
    private com.Cybersoft.Final_Capstone.repository.PropertyRepository propertyRepository;
    
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
    public PageResponse<PromotionDTO> findAll(Pageable pageable) {
        Page<Promotion> promotionPage = promotionRepository.findAll(pageable);
        
        List<PromotionDTO> promotionDTOs = promotionPage.getContent().stream()
                .map(PromotionMapper::toDTO)
                .filter(PromotionDTO::isActive)
                .toList();
        
        return PageResponse.<PromotionDTO>builder()
                .content(promotionDTOs)
                .currentPage(promotionPage.getNumber())
                .pageSize(promotionPage.getSize())
                .totalElements(promotionPage.getTotalElements())
                .totalPages(promotionPage.getTotalPages())
                .first(promotionPage.isFirst())
                .last(promotionPage.isLast())
                .empty(promotionPage.isEmpty())
                .build();
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

    /**
     * NEW: Validate promotion BEFORE booking creation
     * Called by FE when user selects promotion on booking form
     */
    @Override
    public PromotionPreviewDTO validatePromotionForBooking(String code, Integer propertyId, 
                                                           LocalDateTime checkIn, LocalDateTime checkOut) {
        try {
            // 1. Get current user
            UserAccount currentUser = securityUtil.getLoggedInUser();
            if (currentUser == null) {
                throw new InvalidException("User not authenticated");
            }
            
            // 2. Get property and calculate original price
            com.Cybersoft.Final_Capstone.Entity.Property property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
            
            BigDecimal originalAmount = calculateOriginalPrice(property.getPricePerNight(), checkIn, checkOut);
            
            // 3. Find UserPromotion for current user with this promotion code
            UserPromotion userPromotion = userPromotionRepository.findByUserAccountAndPromotionCode(
                    currentUser.getId(), 
                    code
            ).orElse(null);
            
            if (userPromotion == null) {
                return PromotionPreviewDTO.builder()
                        .isValid(false)
                        .errorMessage("You don't have this promotion assigned. Please claim it first.")
                        .build();
            }

            // 4. Validate UserPromotion status = ACTIVE
            if (!"ACTIVE".equals(userPromotion.getStatus().getName())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(userPromotion.getPromotion().getName())
                        .isValid(false)
                        .errorMessage("This promotion is no longer active. Status: " + userPromotion.getStatus().getName())
                        .build();
            }

            // 5. Validate UserPromotion is not locked
            if (Boolean.TRUE.equals(userPromotion.getIsLocked())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(userPromotion.getPromotion().getName())
                        .isValid(false)
                        .errorMessage("This promotion is currently locked for another pending payment")
                        .build();
            }

            // 6. Validate UserPromotion expiration
            if (userPromotion.getExpiresAt() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(userPromotion.getExpiresAt())) {
                    return PromotionPreviewDTO.builder()
                            .promotionCode(code)
                            .promotionName(userPromotion.getPromotion().getName())
                            .isValid(false)
                            .errorMessage("Your promotion assignment has expired on: " + userPromotion.getExpiresAt())
                            .build();
                }
            }

            // Get underlying Promotion for validation and calculation
            Promotion promotion = userPromotion.getPromotion();

            // 7. Validate GLOBAL promotion usage limit
            // usage_limit = -1 means UNLIMITED
            // usage_limit = null also treated as UNLIMITED for backward compatibility
            if (promotion.getUsageLimit() != null && promotion.getUsageLimit() != -1) {
                // Check with timesUsed + 1 (simulate this usage)
                int potentialUsage = promotion.getTimesUsed() + 1;

                if (potentialUsage > promotion.getUsageLimit()) {
                    return PromotionPreviewDTO.builder()
                            .promotionCode(code)
                            .promotionName(promotion.getName())
                            .isValid(false)
                            .errorMessage(String.format(
                                "Promotion has reached its global usage limit (%d/%d used)",
                                promotion.getTimesUsed(),
                                promotion.getUsageLimit()
                            ))
                            .build();
                }

                // ⚠️ Warning if this will be the LAST usage
                if (potentialUsage == promotion.getUsageLimit()) {
                    log.warn("⚠️ Promotion '{}' will reach limit after this usage ({}/{})",
                        promotion.getCode(), potentialUsage, promotion.getUsageLimit());
                }
            }

            // 8. Validate promotion date range
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartDate())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(promotion.getName())
                        .isValid(false)
                        .errorMessage("Promotion has not started yet. Starts at: " + promotion.getStartDate())
                        .build();
            }
            
            if (now.isAfter(promotion.getEndDate())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(promotion.getName())
                        .isValid(false)
                        .errorMessage("Promotion has expired on: " + promotion.getEndDate())
                        .build();
            }

            // 9. Validate minimum purchase limit
            if (promotion.getMinPurchaseLimit() != null && 
                originalAmount.compareTo(promotion.getMinPurchaseLimit()) < 0) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(promotion.getName())
                        .originalAmount(originalAmount)
                        .isValid(false)
                        .errorMessage(String.format(
                            "Booking amount (%.2f) does not meet minimum purchase requirement (%.2f)", 
                            originalAmount, promotion.getMinPurchaseLimit()
                        ))
                        .build();
            }

            // 10. Calculate discount
            BigDecimal discountAmount = calculatePromoDiscount(promotion, originalAmount);
            BigDecimal finalAmount = originalAmount.subtract(discountAmount);
            
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }

            // ✅ Return valid preview
            return PromotionPreviewDTO.builder()
                    .isValid(true)
                    .promotionCode(code)
                    .promotionName(promotion.getName())
                    .originalAmount(originalAmount)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .build();

        } catch (DataNotFoundException | InvalidException e) {
            return PromotionPreviewDTO.builder()
                    .isValid(false)
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            return PromotionPreviewDTO.builder()
                    .isValid(false)
                    .errorMessage("Unexpected error: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Helper: Calculate original price from property price and dates
     */
    private BigDecimal calculateOriginalPrice(BigDecimal pricePerNight, LocalDateTime checkIn, LocalDateTime checkOut) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(
            checkIn.toLocalDate(), 
            checkOut.toLocalDate()
        );
        
        if (nights <= 0) {
            throw new InvalidException("Check-out must be after check-in");
        }
        
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }
    
    /**
     * Helper: Calculate discount amount based on promotion type
     */
    private BigDecimal calculatePromoDiscount(Promotion promotion, BigDecimal originalAmount) {
        BigDecimal discount;
        
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = originalAmount.multiply(
                promotion.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            );
            
            // Apply max discount cap if set
            if (promotion.getMaxDiscountAmount() != null && 
                discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();
            }
        } else {
            // FIXED discount
            discount = promotion.getDiscountValue();
        }
        
        // Ensure discount doesn't exceed original amount
        if (discount.compareTo(originalAmount) > 0) {
            discount = originalAmount;
        }
        
        return discount;
    }

    @Override
    public PromotionPreviewDTO previewPromotion(String code, Integer bookingId) {
        try {
            // 1. Find booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + bookingId));

            // 2. Validate booking belongs to current user
            UserAccount currentUser = securityUtil.getLoggedInUser();
            if (currentUser == null) {
                throw new InvalidException("User not authenticated");
            }
            
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                throw new InvalidException("This booking does not belong to the current user");
            }

            // 3. Validate booking status = CONFIRMED
            if (!"CONFIRMED".equals(booking.getStatus().getName())) {
                return PromotionPreviewDTO.builder()
                        .isValid(false)
                        .errorMessage("Only CONFIRMED bookings can apply promotion. Current status: " + booking.getStatus().getName())
                        .build();
            }

            // ==================== NEW LOGIC: Check UserPromotion ====================
            // 4. Find UserPromotion for current user with this promotion code
            UserPromotion userPromotion = userPromotionRepository.findByUserAccountAndPromotionCode(
                    currentUser.getId(), 
                    code
            ).orElse(null);
            
            if (userPromotion == null) {
                return PromotionPreviewDTO.builder()
                        .isValid(false)
                        .errorMessage("You don't have this promotion assigned. Please claim it first using /promotions/claim")
                        .build();
            }

            // 5. Validate UserPromotion status = ACTIVE
            if (!"ACTIVE".equals(userPromotion.getStatus().getName())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(userPromotion.getPromotion().getName())
                        .isValid(false)
                        .errorMessage("This promotion is no longer active. Status: " + userPromotion.getStatus().getName())
                        .build();
            }

            // 6. Validate UserPromotion is not locked
            if (Boolean.TRUE.equals(userPromotion.getIsLocked())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(userPromotion.getPromotion().getName())
                        .isValid(false)
                        .errorMessage("This promotion is currently locked for another pending payment")
                        .build();
            }

            // 7. Validate UserPromotion expiration
            if (userPromotion.getExpiresAt() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(userPromotion.getExpiresAt())) {
                    return PromotionPreviewDTO.builder()
                            .promotionCode(code)
                            .promotionName(userPromotion.getPromotion().getName())
                            .isValid(false)
                            .errorMessage("Your promotion assignment has expired on: " + userPromotion.getExpiresAt())
                            .build();
                }
            }

            // Get underlying Promotion for validation and calculation
            Promotion promotion = userPromotion.getPromotion();

            // 8. Validate GLOBAL promotion usage limit
            if (promotion.getUsageLimit() != null && promotion.getUsageLimit() != -1) {
                if (promotion.getTimesUsed() >= promotion.getUsageLimit()) {
                    return PromotionPreviewDTO.builder()
                            .promotionCode(code)
                            .promotionName(promotion.getName())
                            .isValid(false)
                            .errorMessage("Promotion has reached its global usage limit")
                            .build();
                }
            }

            // 9. Validate promotion date range
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(promotion.getStartDate())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(promotion.getName())
                        .isValid(false)
                        .errorMessage("Promotion has not started yet. Starts at: " + promotion.getStartDate())
                        .build();
            }
            
            if (now.isAfter(promotion.getEndDate())) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(promotion.getName())
                        .isValid(false)
                        .errorMessage("Promotion has expired. Ended at: " + promotion.getEndDate())
                        .build();
            }

            // 10. Validate min purchase
            if (promotion.getMinPurchaseLimit() != null && 
                booking.getTotalPrice().compareTo(promotion.getMinPurchaseLimit()) < 0) {
                return PromotionPreviewDTO.builder()
                        .promotionCode(code)
                        .promotionName(promotion.getName())
                        .isValid(false)
                        .errorMessage(String.format("Booking amount (%.2f) does not meet minimum purchase requirement (%.2f)", 
                            booking.getTotalPrice(), promotion.getMinPurchaseLimit()))
                        .build();
            }

            // 11. Calculate discount
            BigDecimal originalAmount = booking.getTotalPrice();
            BigDecimal discountAmount = calculateDiscount(promotion, originalAmount);
            BigDecimal finalAmount = originalAmount.subtract(discountAmount);
            
            // Ensure final amount doesn't go negative
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }

            // 12. Return valid preview (DO NOT increment timesUsed!)
            return PromotionPreviewDTO.builder()
                    .promotionCode(code)
                    .promotionName(promotion.getName())
                    .description(promotion.getDescription())
                    .originalAmount(originalAmount)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .discountType(promotion.getDiscountType().name())
                    .discountValue(promotion.getDiscountValue())
                    .isValid(true)
                    .errorMessage(null)
                    .build();

        } catch (DataNotFoundException | InvalidException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            // Wrap unexpected exceptions
            throw new RuntimeException("Error previewing promotion: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate discount amount based on promotion type
     */
    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal originalAmount) {
        BigDecimal discount;
        
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            // Percentage discount: amount * (discountValue / 100)
            discount = originalAmount
                    .multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // Fixed discount
            discount = promotion.getDiscountValue();
        }
        
        // Apply max discount cap if set
        if (promotion.getMaxDiscountAmount() != null && 
            discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discount = promotion.getMaxDiscountAmount();
        }
        
        // Discount cannot exceed original amount
        if (discount.compareTo(originalAmount) > 0) {
            discount = originalAmount;
        }
        
        return discount.setScale(2, RoundingMode.HALF_UP);
    }
}
