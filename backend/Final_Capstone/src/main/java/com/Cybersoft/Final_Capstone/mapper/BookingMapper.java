package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.PromotionUsage;
import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.repository.PromotionUsageRepository;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
public class BookingMapper {

    /**
     * Convert Booking entity to BookingDTO
     * Note: This does NOT include promotion info. Use toDTOWithPromotion() if needed.
     */
    public static BookingDTO toDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setPropertyId(booking.getProperty().getId());
        dto.setPropertyName(booking.getProperty().getPropertyName());
        dto.setPropertyPricePerNight(booking.getProperty().getPricePerNight());
        dto.setCheckIn(booking.getCheckIn());
        dto.setCheckOut(booking.getCheckOut());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setNumAdults(booking.getNumAdults());
        dto.setNumChildren(booking.getNumChildren());
        dto.setNum_infant(booking.getNum_infant());
        dto.setNumPet(booking.getNumPet());
        dto.setNotes(booking.getNotes());
        dto.setStatus(booking.getStatus().getName());
        
        // Promotion fields - will be null if not populated
        dto.setPromotionCode(null);
        dto.setOriginalAmount(null);
        dto.setDiscountAmount(null);

        // New fields for booking flow
        dto.setConfirmedAt(booking.getConfirmedAt());
        dto.setCancelledAt(booking.getCancelledAt());
        dto.setCancelledBy(booking.getCancelledBy());
        dto.setCancelReason(booking.getCancelReason());
        
        // Computed fields
        dto.setPaymentDeadline(booking.getPaymentDeadline());
        dto.setIsPaymentExpired(booking.isPaymentExpired());
        
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }

    /**
     * Convert Booking to BookingDTO with promotion info
     * @param booking Booking entity
     * @param promotionUsageRepository Repository to fetch promotion usage
     * @return BookingDTO with promotion fields populated
     */
    public static BookingDTO toDTOWithPromotion(Booking booking, PromotionUsageRepository promotionUsageRepository) {
        BookingDTO dto = toDTO(booking);

        // Fetch promotion usage for this booking
        Optional<PromotionUsage> usageOpt = promotionUsageRepository.findByBookingId(booking.getId());

        if (usageOpt.isPresent()) {
            PromotionUsage usage = usageOpt.get();
            String promoCode = usage.getUserPromotion().getPromotion().getCode();
            BigDecimal discountAmount = usage.getDiscountAmount();
            BigDecimal finalPrice = booking.getTotalPrice();
            BigDecimal originalAmount = finalPrice.add(discountAmount);

            dto.setPromotionCode(promoCode);
            dto.setOriginalAmount(originalAmount);
            dto.setDiscountAmount(discountAmount);
        }

        return dto;
    }
}


