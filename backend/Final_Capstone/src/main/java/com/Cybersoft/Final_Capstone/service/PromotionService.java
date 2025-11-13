package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.PromotionDTO;
import com.Cybersoft.Final_Capstone.dto.PromotionPreviewDTO;
import com.Cybersoft.Final_Capstone.payload.request.PromotionRequest;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PromotionService {
    void insertPromotion(PromotionRequest promotionRequest);
    PromotionDTO getPromotionByCode(String code);
    PromotionDTO getPromotionByName(String name);
    List<PromotionDTO>findAll();
    PageResponse<PromotionDTO> findAll(Pageable pageable);
    void deletePromotion(String name);
    PromotionDTO updatePromotion(String name, PromotionRequest promotionRequest);
    
    /**
     * Validate promotion BEFORE booking creation (FE preview)
     * Called when user selects promotion on booking form
     * 
     * @param code Promotion code
     * @param propertyId Property ID
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return PromotionPreviewDTO with discount calculation
     */
    PromotionPreviewDTO validatePromotionForBooking(String code, Integer propertyId, 
                                                     java.time.LocalDateTime checkIn, 
                                                     java.time.LocalDateTime checkOut);
    
    /**
     * OLD METHOD: Preview promotion for EXISTING booking (kept for backward compatibility)
     * 
     * @param code Promotion code to validate
     * @param bookingId Booking ID (must be CONFIRMED status)
     * @return PromotionPreviewDTO with discount calculation or error message
     */
    @Deprecated
    PromotionPreviewDTO previewPromotion(String code, Integer bookingId);

}
