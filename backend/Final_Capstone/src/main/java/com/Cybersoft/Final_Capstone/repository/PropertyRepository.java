package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Enum.PropertyType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer>, JpaSpecificationExecutor<Property> {
    // Keep only actively used methods
    List<Property> findByHostId(Integer hostId); // Used by HostService

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Property p where p.id = :id")
    Optional<Property> findByIdForUpdate(@Param("id") Integer id);

    // Top-N queries ordered by overallRating (descending)
    List<Property> findTop7ByStatus_NameOrderByOverallRatingDesc(String statusName);
    List<Property> findTop4ByStatus_NameAndPropertyTypeOrderByOverallRatingDesc(String statusName, PropertyType propertyType);

    // Find by id and status name (used to ensure property is AVAILABLE when requested)
    Optional<Property> findByIdAndStatus_Name(Integer id, String statusName);
    // Bulk update review_count based on user_review table counts (native SQL for efficiency)
    @Modifying
    @Transactional
    @Query(value = "UPDATE property p SET review_count = (SELECT COALESCE(COUNT(ur.id),0) FROM user_review ur WHERE ur.property_id = p.id)", nativeQuery = true)
    int updateReviewCountsFromReviews();
}
