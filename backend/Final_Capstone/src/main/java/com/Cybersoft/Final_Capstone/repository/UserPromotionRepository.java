package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.UserPromotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Integer> {
    Integer countByUserAccount_IdAndStatus_Name(int userId, String statusName);
    List<UserPromotion> findByUserAccount_Id(int userId);
    Page<UserPromotion> findByUserAccount_Id(int userId, Pageable pageable);

    // ==================== NEW: PROMOTION PAYMENT SUPPORT ====================

    /**
     * Find ACTIVE user promotion by user ID and promotion code with pessimistic lock
     * Lock prevents race conditions when multiple payment requests use same promotion
     */
    @Query("SELECT up FROM UserPromotion up " +
            "JOIN FETCH up.promotion p " +
            "WHERE up.userAccount.id = :userId " +
            "AND p.code = :promotionCode " +
            "AND up.status.name = 'ACTIVE'")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserPromotion> findByUserAndCodeForUpdate(@Param("userId") Integer userId,
                                                        @Param("promotionCode") String promotionCode);

    /**
     * Find user promotion by user ID and promotion code (no lock)
     * Used for queries that don't modify data
     */
    @Query("SELECT up FROM UserPromotion up " +
            "JOIN FETCH up.promotion p " +
            "WHERE up.userAccount.id = :userId " +
            "AND p.code = :promotionCode")
    Optional<UserPromotion> findByUserAccountAndPromotionCode(@Param("userId") Integer userId,
                                                               @Param("promotionCode") String promotionCode);

    /**
     * Find user promotion by user ID and promotion ID
     * Used during booking creation to check if user has claimed the promotion
     */
    @Query("SELECT up FROM UserPromotion up " +
            "WHERE up.userAccount.id = :userId " +
            "AND up.promotion.id = :promotionId")
    Optional<UserPromotion> findByUserIdAndPromotionId(@Param("userId") Integer userId,
                                                        @Param("promotionId") Integer promotionId);

    /**
     * Find all UserPromotions by Promotion and Status
     * Used to deactivate all unused (ACTIVE) UserPromotions when promotion reaches usage limit
     */
    @Query("SELECT up FROM UserPromotion up " +
            "WHERE up.promotion = :promotion " +
            "AND up.status = :status")
    List<UserPromotion> findByPromotionAndStatus(@Param("promotion") com.Cybersoft.Final_Capstone.Entity.Promotion promotion,
                                                   @Param("status") com.Cybersoft.Final_Capstone.Entity.Status status);
}
