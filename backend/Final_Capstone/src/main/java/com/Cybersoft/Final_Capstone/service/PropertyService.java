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
    int insertProperty(PropertyRequest propertyRequest);
    PropertyDTO updateProperty(int id, PropertyRequest propertyRequest);
    void deleteProperty(int id);

    // Keep only methods that are actively used
    List<PropertyDTO> getByHostId(Integer hostId); // Used by HostService
    PageResponse<PropertyDTO> getByHostId(Integer hostId, Pageable pageable); // Paginated version
    List<PropertyDTO> getTop4PropertiesBaseOnType(int propertyType);
    List<PropertyDTO> getTop7Properties();

    // Get a property by id only if its status is AVAILABLE
    PropertyDTO getAvailablePropertyById(Integer id);

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
    // Create property with images, amenities, facilities in one transaction. Returns propertyId.

    int createCompleteProperty(
            PropertyRequest propertyRequest,
            List<org.springframework.web.multipart.MultipartFile> imageFiles,
            List<String> imageDescriptions,
            List<Integer> amenityIds,
            List<Integer> facilityIds
    );
}
