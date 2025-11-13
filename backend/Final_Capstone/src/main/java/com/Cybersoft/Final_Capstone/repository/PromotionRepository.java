package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    Optional<Promotion> findByCode(String code);
    Optional<Promotion> findByCodeAndStatus_Name(String code, String statusName);
    Optional<Promotion> findByNameAndStatus_Name(String name, String statusName);

    // ==================== NEW: PROMOTION USAGE TRACKING ====================

    /**
     * Atomically increment promotion usage count with optimistic locking
     * Returns number of rows updated (0 if version mismatch, 1 if success)
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.timesUsed = p.timesUsed + 1, p.version = p.version + 1 " +
            "WHERE p.id = :promotionId AND p.version = :currentVersion")
    int incrementUsageAtomic(@Param("promotionId") Integer promotionId,
                             @Param("currentVersion") Integer currentVersion);

    /**
     * Decrement promotion usage (rollback - if needed for cancel scenarios)
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.timesUsed = p.timesUsed - 1 " +
            "WHERE p.id = :promotionId AND p.timesUsed > 0")
    int decrementUsage(@Param("promotionId") Integer promotionId);
}
