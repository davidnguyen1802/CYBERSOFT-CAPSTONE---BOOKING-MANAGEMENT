package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.PromotionUsage;
import com.Cybersoft.Final_Capstone.Entity.PromotionUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PromotionUsage entity
 * Tracks actual consumption of UserPromotions in successful bookings
 */
@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, PromotionUsageId> {
    
    /**
     * Find all usages for a specific UserPromotion
     * Used to track usage history of a promotion assignment
     */
    @Query("SELECT pu FROM PromotionUsage pu " +
           "WHERE pu.userPromotion.id = :userPromotionId " +
           "ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findByUserPromotionId(@Param("userPromotionId") Integer userPromotionId);
    
    /**
     * Find promotion usage for a specific booking
     * Used to check if a booking already consumed a promotion
     */
    @Query("SELECT pu FROM PromotionUsage pu " +
           "WHERE pu.booking.id = :bookingId")
    Optional<PromotionUsage> findByBookingId(@Param("bookingId") Integer bookingId);
    
    /**
     * Check if a booking already has a promotion usage record
     * Prevents duplicate promotion consumption
     */
    boolean existsByBooking_Id(Integer bookingId);
    
    /**
     * Count total usages for a specific user promotion
     */
    long countByUserPromotion_Id(Integer userPromotionId);
}












