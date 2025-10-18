package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.Enum.PropertyType;
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
import com.Cybersoft.Final_Capstone.specification.PropertySpecification;
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
    private UserAccountRepository userAccountRepository;

    private static final String AVAILABLE_STATUS = "AVAILABLE";

    @Transactional
    @Override
    public PropertyDTO insertProperty(PropertyRequest propertyRequest) {
        Property property = new Property();
        /*Lấy userId từ Token
        * Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();*/
        // Use hostId from request if provided, otherwise use hardcoded value (or from SecurityContext in production)
        int userId = propertyRequest.getHostId() != null ? propertyRequest.getHostId() : 2; //Thay tạm userId = 2
        UserAccount host = userAccountRepository.findById(userId)
                        .orElseThrow(()-> new DataNotFoundException("User not found with id: " + userId));
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

        return PropertyMapper.toDTO(propertyRepository.save(property));
    }

    @Override
    public PropertyDTO updateProperty(int id, PropertyRequest propertyRequest) {
        Property property = propertyRepository.findByIdForUpdate(id)
                .orElseThrow(()-> new DataNotFoundException("Property not found with id: " + id));
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
        try{
            property.setStatus(new Status(3)); //Chuyển sang trạng thái Inactive
            propertyRepository.save(property);
        } catch (Exception e){
            throw new RuntimeException("Delete property failed");
        }

    }

    @Override
    public PropertyDTO getPropertyByName(String name) {
        return propertyRepository.findByPropertyNameContainingIgnoreCaseAndStatus_Name(name, AVAILABLE_STATUS)
                .map(PropertyMapper::toDTO)
                .orElseThrow(()-> new DataNotFoundException("Property not found with name: " + name));
    }

    @Override
    public List<PropertyDTO> getByCity(String cityName) {
        List<Property> properties = propertyRepository.findByLocation_City_CityNameAndStatus_Name(cityName, AVAILABLE_STATUS);
        try {
            if (properties.isEmpty()) {
                throw new DataNotFoundException("No properties found in city: " + cityName);
            }
        } catch (Exception e){
            throw new RuntimeException("Get properties by city failed");
        }
       return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByLocation(String locationName) {
        List<Property> properties = propertyRepository.findByLocation_LocationNameAndStatus_Name(locationName, AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found in location: " + locationName);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }
    // Dùng cache
    @Override
    public List<PropertyDTO> getAllProperties() {
        // Use the repository-level "visible" filter to exclude Pending and Inactive properties in SQL
        List<Property> properties = propertyRepository.findAllVisibleProperties();
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found");
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByPropertyType(int propertyType) {
        PropertyType type;
        try{
            type = PropertyType.fromValue(propertyType);
        } catch (IllegalArgumentException e){
            throw new DataNotFoundException("Invalid property type: " + propertyType);
        }
        List<Property> properties = propertyRepository.findByPropertyTypeAndStatus_Name(type, AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found with type: " + type);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());

    }

    @Override
    public List<PropertyDTO> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        List<Property> properties = propertyRepository.searchByPriceBetween(minPrice, maxPrice);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found in price range: " + minPrice + " - " + maxPrice);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByNumBedRooms(Integer numberOfBedrooms) {
        List<Property> properties = propertyRepository.findByNumberOfBedroomsAndStatus_Name(numberOfBedrooms, AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found with number of bedrooms: " + numberOfBedrooms);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByNumBathrooms(Integer numberOfBathrooms) {
        List<Property> properties = propertyRepository.findByNumberOfBathroomsAndStatus_Name(numberOfBathrooms, AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found with number of bathrooms: " + numberOfBathrooms);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByAmenities(List<Integer> amenities) {
        List<Property> properties = propertyRepository.findDistinctByAmenities_IdInAndStatus_Name(amenities, AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found with given amenities");
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByFacilities(List<Integer> facilities) {
        List<Property> properties = propertyRepository.findDistinctByFacilities_IdInAndStatus_Name(facilities, AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found with given facilities");
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByHostId(Integer hostId) {
        List<Property> properties = propertyRepository.findByHostIdAndStatus_Name(hostId, AVAILABLE_STATUS);
        if(properties.isEmpty()){
            throw new DataNotFoundException("No properties found for host with id: " + hostId);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByMaxAdults(Integer maxAdults) {
        return propertyRepository.findByMaxAdultsGreaterThanEqualAndStatus_Name(maxAdults, AVAILABLE_STATUS)
                .stream().map(PropertyMapper::toDTO).toList();
    }

    @Override
    public List<PropertyDTO> getByMaxChildren(Integer maxChildren) {
        return propertyRepository.findByMaxChildrenGreaterThanEqualAndStatus_Name(maxChildren, AVAILABLE_STATUS)
                .stream().map(PropertyMapper::toDTO).toList();
    }

    @Override
    public List<PropertyDTO> getByMaxInfants(Integer maxInfants) {
        return propertyRepository.findByMaxInfantsGreaterThanEqualAndStatus_Name(maxInfants, AVAILABLE_STATUS)
                .stream().map(PropertyMapper::toDTO).toList();
    }

    @Override
    public List<PropertyDTO> getByMaxPets(Integer maxPets) {
        return propertyRepository.findByMaxPetsGreaterThanEqualAndStatus_Name(maxPets, AVAILABLE_STATUS)
                .stream().map(PropertyMapper::toDTO).toList();
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
    public List<PropertyDTO> getByPropertyTypeAndCity(int propertyType, String cityName) {
        PropertyType type;
        try {
            type = PropertyType.fromValue(propertyType);
        } catch (IllegalArgumentException e) {
            throw new DataNotFoundException("Invalid property type: " + propertyType);
        }
        List<Property> properties = propertyRepository.findByPropertyTypeAndLocation_City_CityNameAndStatus_Name(type, cityName, AVAILABLE_STATUS);
        if (properties.isEmpty()) {
            throw new DataNotFoundException("No properties found with type: " + type + " in city: " + cityName);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByPropertyTypeAndLocation(int propertyType, String locationName) {
        PropertyType type;
        try {
            type = PropertyType.fromValue(propertyType);
        } catch (IllegalArgumentException e) {
            throw new DataNotFoundException("Invalid property type: " + propertyType);
        }
        List<Property> properties = propertyRepository.findByPropertyTypeAndLocation_LocationNameAndStatus_Name(type, locationName, AVAILABLE_STATUS);
        if (properties.isEmpty()) {
            throw new DataNotFoundException("No properties found with type: " + type + " in location: " + locationName);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
    }

    @Override
    public List<PropertyDTO> getByPropertyTypeAndMaxPets(int propertyType, Integer maxPets) {
        PropertyType type;
        try {
            type = PropertyType.fromValue(propertyType);
        } catch (IllegalArgumentException e) {
            throw new DataNotFoundException("Invalid property type: " + propertyType);
        }
        List<Property> properties = propertyRepository.findByPropertyTypeAndMaxPetsGreaterThanEqualAndStatus_Name(type, maxPets, AVAILABLE_STATUS);
        if (properties.isEmpty()) {
            throw new DataNotFoundException("No properties found with type: " + type + " and max pets >= " + maxPets);
        }
        return properties.stream().map(PropertyMapper::toDTO).collect(toList());
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
}
