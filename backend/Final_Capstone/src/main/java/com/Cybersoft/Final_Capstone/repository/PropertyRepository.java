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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer>, JpaSpecificationExecutor<Property> {
    Optional<Property> findByPropertyNameContainingIgnoreCaseAndStatus_Name(String name, String statusName);

    List<Property> findByLocation_City_CityNameAndStatus_Name(String cityName, String statusName);
    List<Property> findByLocation_LocationNameAndStatus_Name(String locationName, String statusName);
    List<Property> findByPropertyTypeAndStatus_Name(PropertyType propertyType, String statusName);

    @Query("SELECT p FROM Property p WHERE p.pricePerNight BETWEEN :minPrice AND :maxPrice AND p.status.name = 'AVAILABLE'")
    List<Property> searchByPriceBetween(@Param("minPrice") BigDecimal minPrice,
                                        @Param("maxPrice") BigDecimal maxPrice);

    List<Property> findByNumberOfBedroomsAndStatus_Name(Integer numberOfBedrooms, String statusName);
    List<Property> findByNumberOfBathroomsAndStatus_Name(Integer numberOfBathrooms, String statusName);

    List<Property> findDistinctByAmenities_IdInAndStatus_Name(Collection<Integer> amenityIds, String statusName);

    List<Property> findDistinctByFacilities_IdInAndStatus_Name(Collection<Integer> facilityIds, String statusName);

    List<Property> findByHostIdAndStatus_Name(Integer hostId, String statusName);
    
    // Get all properties by host (regardless of status)
    List<Property> findByHostId(Integer hostId);

    List<Property> findByMaxAdultsGreaterThanEqualAndStatus_Name(Integer maxAdults, String statusName);
    List<Property> findByMaxChildrenGreaterThanEqualAndStatus_Name(Integer maxChildren, String statusName);
    List<Property> findByMaxInfantsGreaterThanEqualAndStatus_Name(Integer maxInfants, String statusName);
    List<Property> findByMaxPetsGreaterThanEqualAndStatus_Name(Integer maxPets, String statusName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Property p where p.id = :id")
    Optional<Property> findByIdForUpdate(@Param("id") Integer id);

    // New: return only properties visible to normal users (exclude Pending and Inactive/Unactive)
    @Query("SELECT p FROM Property p WHERE p.status.name = 'AVAILABLE'")
    List<Property> findAllVisibleProperties();

    // Top-N queries ordered by overallRating (descending). Use 7 as requested.
    List<Property> findTop7ByStatus_NameOrderByOverallRatingDesc(String statusName);
    List<Property> findTop4ByStatus_NameAndPropertyTypeOrderByOverallRatingDesc(String statusName, PropertyType propertyType);

    // Find by id and status name (used to ensure property is AVAILABLE when requested)
    Optional<Property> findByIdAndStatus_Name(Integer id, String statusName);

    // Find by property type and city name with AVAILABLE status
    List<Property> findByPropertyTypeAndLocation_City_CityNameAndStatus_Name(PropertyType propertyType, String cityName, String statusName);

    // Find by property type and location name with AVAILABLE status
    List<Property> findByPropertyTypeAndLocation_LocationNameAndStatus_Name(PropertyType propertyType, String locationName, String statusName);

    List<Property> findByPropertyTypeAndMaxPetsGreaterThanEqualAndStatus_Name(PropertyType propertyType, Integer maxPets, String statusName);

    // Bulk update review_count based on user_review table counts (native SQL for efficiency)
    @Modifying
    @Transactional
    @Query(value = "UPDATE property p SET review_count = (SELECT COALESCE(COUNT(ur.id),0) FROM user_review ur WHERE ur.property_id = p.id)", nativeQuery = true)
    int updateReviewCountsFromReviews();
}
