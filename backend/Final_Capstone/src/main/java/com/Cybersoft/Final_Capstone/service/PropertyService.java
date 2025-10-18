package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.PropertyDTO;
import com.Cybersoft.Final_Capstone.dto.PropertyListItemDTO;
import com.Cybersoft.Final_Capstone.payload.request.PropertyRequest;
import com.Cybersoft.Final_Capstone.payload.request.PropertySearchRequest;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyService {
    PropertyDTO insertProperty(PropertyRequest propertyRequest);
    PropertyDTO updateProperty(int id, PropertyRequest propertyRequest);
    void deleteProperty(int id);
    PropertyDTO getPropertyByName(String name);
    List<PropertyDTO> getByCity(String cityName);
    List<PropertyDTO> getByLocation(String locationName);
    List<PropertyDTO> getAllProperties();
    List<PropertyDTO> getByPropertyType(int propertyType);
    List<PropertyDTO> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    List<PropertyDTO> getByNumBedRooms(Integer numberOfBedrooms);
    List<PropertyDTO> getByNumBathrooms(Integer numberOfBathrooms);
    List<PropertyDTO> getByAmenities(List<Integer> amenities);
    List<PropertyDTO> getByFacilities(List<Integer> facilities);
    List<PropertyDTO> getByHostId(Integer hostId);

    List<PropertyDTO> getByMaxAdults(Integer maxAdults);
    List<PropertyDTO> getByMaxChildren(Integer maxChildren);
    List<PropertyDTO> getByMaxInfants(Integer maxInfants);
    List<PropertyDTO> getByMaxPets(Integer maxPets);
    List<PropertyDTO> getTop4PropertiesBaseOnType(int propertyType);
    /**
     * Backward-compatible declarations for older callers. Implementations should delegate to getTop4*.
     * Deprecated: prefer getTop4Properties()/getTop4PropertiesBaseOnType(int)
     */
    List<PropertyDTO> getTop7Properties();

    // Get a property by id only if its status is AVAILABLE
    PropertyDTO getAvailablePropertyById(Integer id);

    // Get properties by type and city name (AVAILABLE only)
    List<PropertyDTO> getByPropertyTypeAndCity(int propertyType, String cityName);

    // Get properties by type and location name (AVAILABLE only)
    List<PropertyDTO> getByPropertyTypeAndLocation(int propertyType, String locationName);

    // Get properties by type and max pets
    List<PropertyDTO> getByPropertyTypeAndMaxPets(int propertyType, Integer maxPets);

    // Dynamic search with multiple filters
    List<PropertyDTO> searchProperties(
            Integer type,
            String city,
            String location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            Integer maxAdults,
            Integer maxChildren,
            Integer maxInfants,
            Integer maxPets,
            List<Integer> amenities,
            List<Integer> facilities
    );

    /**
     * NEW: Unified paginated search with multiple filters.
     * Returns lightweight DTOs optimized for list/card view.
     * Supports pagination and sorting.
     * This method consolidates all search/filter functionality.
     */
    PageResponse<PropertyListItemDTO> searchPropertiesPaginated(PropertySearchRequest request);

    /**
     * NEW: Overload for direct Pageable usage (used by GET endpoint).
     * All filter parameters are optional.
     */
    PageResponse<PropertyListItemDTO> searchPropertiesPaginated(
            Integer type,
            String city,
            String location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            Integer maxAdults,
            Integer maxChildren,
            Integer maxInfants,
            Integer maxPets,
            List<Integer> amenities,
            List<Integer> facilities,
            String name,
            Pageable pageable
    );

    // Bulk recalculation of review counts from user_review table. Returns number of rows updated.
}
