package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.Enum.PropertyType;
import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import com.Cybersoft.Final_Capstone.dto.PropertyDTO;
import com.Cybersoft.Final_Capstone.dto.PropertyListItemDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.mapper.PropertyMapper;
import com.Cybersoft.Final_Capstone.mapper.PropertyListItemMapper;
import com.Cybersoft.Final_Capstone.payload.request.PropertyRequest;
import com.Cybersoft.Final_Capstone.payload.request.PropertySearchRequest;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.repository.*;
import com.Cybersoft.Final_Capstone.service.PropertyService;
import com.Cybersoft.Final_Capstone.payload.request.ImageRequest;
import com.Cybersoft.Final_Capstone.payload.request.AmenityRequest;
import com.Cybersoft.Final_Capstone.payload.request.FacilityRequest;
import com.Cybersoft.Final_Capstone.specification.PropertySpecification;
import org.springframework.web.multipart.MultipartFile;
import com.Cybersoft.Final_Capstone.util.UpdateHelper;
import com.Cybersoft.Final_Capstone.util.PageableBuilder;
import com.Cybersoft.Final_Capstone.util.PageResponseMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class PropertyServiceImp implements PropertyService {
    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private ImageServiceImp imageService;

    @Autowired
    private AmenityServiceImp amenityService;

    @Autowired
    private FacilityServiceImp facilityService;

    private static final String AVAILABLE_STATUS = "AVAILABLE";

    @Transactional
    @Override
    public int insertProperty(PropertyRequest propertyRequest) {
        Property property = new Property();
        
        // Lấy host từ authenticated user (đã được bảo vệ bởi @PreAuthorize("hasRole('HOST')"))
        UserAccount host = securityUtil.getLoggedInUser();
        if (host == null) {
            throw new DataNotFoundException("No authenticated user found");
        }
        
        property.setHost(host);
        property.setPropertyName(propertyRequest.getPropertyName());
        property.setFullAddress(propertyRequest.getFullAddress());
        property.setPricePerNight(propertyRequest.getPricePerNight());
        property.setNumberOfBedrooms(propertyRequest.getNumberOfBedrooms());
        property.setNumberOfBathrooms(propertyRequest.getNumberOfBathrooms());
        property.setMaxAdults(propertyRequest.getMaxAdults());
        property.setMaxChildren(propertyRequest.getMaxChildren());
        property.setMaxInfants(propertyRequest.getMaxInfants());
        property.setMaxPets(propertyRequest.getMaxPets());
        property.setDescription(propertyRequest.getDescription());
        property.setPropertyType(PropertyType.fromValue(propertyRequest.getPropertyType()));
        property.setStatus(new Status(4));
        Location location = locationRepository.findById(propertyRequest.getLocationId())
                .orElseThrow(()-> new DataNotFoundException("Location not found with id: " + propertyRequest.getLocationId()));
        property.setLocation(location);

        return propertyRepository.save(property).getId();
    }

    @Override
    public PropertyDTO updateProperty(int id, PropertyRequest propertyRequest) {
        Property property = propertyRepository.findByIdForUpdate(id)
                .orElseThrow(()-> new DataNotFoundException("Property not found with id: " + id));
        
        // ⚠️ SECURITY: Verify ownership before updating
        verifyOwnership(property);
        
        UpdateHelper.copyNonNullChangedFields(propertyRequest, property, "locationId");
        if(propertyRequest.getLocationId() != null){
            Location location = locationRepository.findById(propertyRequest.getLocationId())
                    .orElseThrow(()-> new DataNotFoundException("Location not found with id: " + propertyRequest.getLocationId()));
            property.setLocation(location);
        }
        try{
            return PropertyMapper.toDTO(propertyRepository.save(property));
        } catch (Exception e){
            throw new RuntimeException("Update property failed");
        }

    }

    @Override
    public void deleteProperty(int id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(()-> new DataNotFoundException("Property not found with id: " + id));
        
        // ⚠️ SECURITY: Verify ownership before deleting
        verifyOwnership(property);
        
        try{
            property.setStatus(new Status(3)); //Chuyển sang trạng thái Inactive
            propertyRepository.save(property);
        } catch (Exception e){
            throw new RuntimeException("Delete property failed");
        }

    }

    @Override
    public List<PropertyDTO> getByHostId(Integer hostId) {
        List<Property> properties = propertyRepository.findByHostId(hostId);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found for host with id: " + hostId);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public PageResponse<PropertyDTO> getByHostId(Integer hostId, Pageable pageable) {
        Page<Property> propertyPage = propertyRepository.findAll(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("host").get("id"), hostId),
            pageable
        );
        
        List<PropertyDTO> propertyDTOs = propertyPage.getContent().stream()
                .map(PropertyMapper::toDTO)
                .toList();
        
        return PageResponse.<PropertyDTO>builder()
                .content(propertyDTOs)
                .currentPage(propertyPage.getNumber())
                .pageSize(propertyPage.getSize())
                .totalElements(propertyPage.getTotalElements())
                .totalPages(propertyPage.getTotalPages())
                .first(propertyPage.isFirst())
                .last(propertyPage.isLast())
                .empty(propertyPage.isEmpty())
                .build();
    }

    @Override
    public List<PropertyDTO> getTop7Properties() {
        List<Property> properties = propertyRepository.findTop7ByStatus_NameOrderByOverallRatingDesc(AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No top properties found");
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getTop4PropertiesBaseOnType(int propertyType) {
        PropertyType type;
        try{
            type = PropertyType.fromValue(propertyType);
        } catch (IllegalArgumentException e){
            throw new DataNotFoundException("Invalid property type: " + propertyType);
        }
        List<Property> properties = propertyRepository.findTop4ByStatus_NameAndPropertyTypeOrderByOverallRatingDesc("AVAILABLE", type);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No top properties found with type: " + type);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public PropertyDTO getAvailablePropertyById(Integer id) {
        Property property = propertyRepository.findByIdAndStatus_Name(id, AVAILABLE_STATUS)
                .orElseThrow(() -> new DataNotFoundException("Property not found or not available with id: " + id));
        return PropertyMapper.toDTO(property);
    }

    @Override
    public List<PropertyDTO> searchProperties(
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
    ) {
        Specification<Property> spec = PropertySpecification.filterProperties(
                type, city, location, minPrice, maxPrice,
                bedrooms, bathrooms, maxAdults, maxChildren,
                maxInfants, maxPets, amenities, facilities, null
        );

        List<Property> properties = propertyRepository.findAll(spec);

        if (properties.isEmpty()) {
            throw new DataNotFoundException("No properties found matching the search criteria");
        }

        return properties.stream()
                .map(PropertyMapper::toDTO)
                .collect(toList());
    }

    /**
     * NEW: Unified paginated search - accepts PropertySearchRequest DTO.
     * This is the main implementation; all search logic flows through here.
     */
    @Override
    public PageResponse<PropertyListItemDTO> searchPropertiesPaginated(PropertySearchRequest request) {
        // Build Pageable with validation
        // Convert frontend 1-based page to 0-based for Spring Data
        int page = (request.getPage() != null && request.getPage() > 0) ? request.getPage() - 1 : 0;
        int size = (request.getSize() != null && request.getSize() > 0) ? request.getSize() : 10;
        Pageable pageable = PageableBuilder.buildPropertyPageable(
                page,
                size,
                request.getSortBy(),
                request.getSortDirection()
        );

        // Delegate to the core search method
        return searchPropertiesPaginated(
                request.getType(),
                request.getCity(),
                request.getLocation(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getBedrooms(),
                request.getBathrooms(),
                request.getMaxAdults(),
                request.getMaxChildren(),
                request.getMaxInfants(),
                request.getMaxPets(),
                request.getAmenities(),
                request.getFacilities(),
                request.getName(),
                pageable
        );
    }

    /**
     * NEW: Core paginated search implementation.
     * Used by both GET (query params) and POST (body) endpoints.
     * Returns lightweight DTOs to reduce payload size and avoid N+1 queries.
     */
    @Override
    public PageResponse<PropertyListItemDTO> searchPropertiesPaginated(
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
    ) {
        // Build specification with all filters (reusing existing logic)
        Specification<Property> spec = PropertySpecification.filterProperties(
                type, city, location, minPrice, maxPrice,
                bedrooms, bathrooms, maxAdults, maxChildren,
                maxInfants, maxPets, amenities, facilities, name
        );

        // Execute paginated query
        Page<Property> page = propertyRepository.findAll(spec, pageable);

        // Convert to PageResponse with lightweight DTOs (DRY - single mapping point)
        return PageResponseMapper.toPageResponse(page, PropertyListItemMapper::toListItemDTO);
    }

    // Create property with images, amenities, facilities in one transaction. Returns propertyId.
    @Override
    @Transactional
    public int createCompleteProperty(
            PropertyRequest propertyRequest,
            List<MultipartFile> imageFiles,
            List<String> imageDescriptions,
            List<Integer> amenityIds,
            List<Integer> facilityIds
    ) {
        int propertyId = insertProperty(propertyRequest);
        
        if (imageFiles != null && !imageFiles.isEmpty()) {
            ImageRequest imageRequest = new ImageRequest();
            imageRequest.setPropertyId(propertyId);
            imageRequest.setFile(imageFiles);
            imageRequest.setImageDescription(imageDescriptions);
            imageService.addImageToProperty(imageRequest);
        }
        
        if (amenityIds != null && !amenityIds.isEmpty()) {
            AmenityRequest amenityRequest = new AmenityRequest();
            amenityRequest.setIdProperty(propertyId);
            amenityRequest.setIds(amenityIds);
            amenityService.addAmenityToProperty(amenityRequest);
        }
        
        if (facilityIds != null && !facilityIds.isEmpty()) {
            FacilityRequest facilityRequest = new FacilityRequest();
            facilityRequest.setPropertyId(propertyId);
            facilityRequest.setIds(facilityIds);
            facilityService.addFacilityToProperty(facilityRequest);
        }
        
        return propertyId;
    }

    /**
     * ⚠️ SECURITY: Verify that the logged-in user owns the property
     * Throws SecurityException if user is not the owner
     * ADMIN role can bypass this check
     */
    private void verifyOwnership(Property property) {
        UserAccount currentUser = securityUtil.getLoggedInUser();
        if (currentUser == null) {
            throw new SecurityException("No authenticated user found");
        }
        
        // Allow ADMIN to bypass ownership check
        if ("ADMIN".equals(currentUser.getRole().getName())) {
            return;
        }
        
        // Check if current user is the owner
        if (!currentUser.getId().equals(property.getHost().getId())) {
            throw new SecurityException("You are not authorized to modify this property. Only the owner can modify it.");
        }
    }
}
