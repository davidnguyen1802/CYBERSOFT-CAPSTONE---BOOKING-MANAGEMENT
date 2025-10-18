package com.Cybersoft.Final_Capstone.controller;


import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;
import com.Cybersoft.Final_Capstone.dto.PropertyDTO;
import com.Cybersoft.Final_Capstone.dto.PropertyListItemDTO;
import com.Cybersoft.Final_Capstone.payload.request.PropertyRequest;
import com.Cybersoft.Final_Capstone.payload.request.PropertySearchRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.service.PropertyService;
import com.Cybersoft.Final_Capstone.util.PageableBuilder;
import jakarta.validation.Valid;
// import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/property")

public class PropertyController {

    private final PropertyService propertyService;

    // Constructor injection (preferred over field injection)
    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // ========================================
    // NEW UNIFIED SEARCH ENDPOINTS (RECOMMENDED)
    // ========================================

    /**
     * Unified property search endpoint (GET with query parameters).
     * Now always returns paginated results by default.
     * Frontend paging is 0-based (page=0 is first page). Default page=0, size=10.
     */
    @GetMapping("/filter")
    public ResponseEntity<?> filterProperties(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) Integer maxAdults,
            @RequestParam(required = false) Integer maxChildren,
            @RequestParam(required = false) Integer maxInfants,
            @RequestParam(required = false) Integer maxPets,
            @RequestParam(required = false) List<Integer> amenities,
            @RequestParam(required = false) List<Integer> facilities,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        // Always return paginated results. Use 0-based page indexing: page=0 -> first page
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 10;
        String sb = sortBy != null ? sortBy : "overallRating";
        String sd = sortDirection != null ? sortDirection : "DESC";
        Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

        PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(
                type, city, location, minPrice, maxPrice,
                bedrooms, bathrooms, maxAdults, maxChildren,
                maxInfants, maxPets, amenities, facilities, name, pageable
        );

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Filter properties successfully");
        response.setData(pageResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * NEW: Unified property search endpoint (POST with request body).
     * Same functionality as GET /property/filter but accepts JSON body.
     * Useful for complex filter combinations or when URL length limits are a concern.
     *
     * Example body:
     * {
     *   "type": 1,
     *   "city": "HaNoi",
     *   "minPrice": 100,
     *   "maxPrice": 500,
     *   "amenities": [1, 2, 3],
     *   "page": 0,
     *   "size": 20,
     *   "sortBy": "pricePerNight",
     *   "sortDirection": "ASC"
     * }
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchPropertiesPost(@Valid @RequestBody PropertySearchRequest request) {
        // Ensure request.page uses 0-based indexing in service; if PropertySearchRequest uses 1-based, service should handle it.
        PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(request);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Search properties successfully");
        response.setData(pageResponse);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // CRUD OPERATIONS
    // ========================================

    @PostMapping
    public ResponseEntity<?> insertProperty(@ModelAttribute PropertyRequest propertyRequest) {
        PropertyDTO dto = propertyService.insertProperty(propertyRequest);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Insert property successfully");
        response.setData(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProperty(@PathVariable int id, @ModelAttribute PropertyRequest propertyRequest) {
        PropertyDTO dto = propertyService.updateProperty(id, propertyRequest);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Update property successfully");
        response.setData(dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable int id) {
        propertyService.deleteProperty(id);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Delete property successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // SPECIFIC ENDPOINTS (Still active for backward compatibility)
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getAvailablePropertyById(@PathVariable Integer id) {
        PropertyDTO dto = propertyService.getAvailablePropertyById(id);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get available property by id successfully");
        response.setData(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top7")
    public ResponseEntity<?> getTop7Properties() {
        List<PropertyDTO> dtos = propertyService.getTop7Properties();
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get top 7 properties successfully");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top4/type/{propertyType}")
    public ResponseEntity<?> getTop4PropertiesBaseOnType(@PathVariable int propertyType) {
        List<PropertyDTO> dtos = propertyService.getTop4PropertiesBaseOnType(propertyType);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get top 4 properties by type successfully");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // LEGACY ENDPOINTS (Deprecated - use /property/filter or /property/search instead)
    // These endpoints are kept for backward compatibility but may be removed in future versions.
    // ========================================

    /**
     * @deprecated Use GET /property/filter with query parameters instead.
     * This endpoint will be removed in a future version.
     */
    @Deprecated
    @GetMapping("/name/{name}")
    public ResponseEntity<?> getPropertyByName(@PathVariable String name,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) String sortBy,
                                               @RequestParam(required = false) String sortDirection) {
        if (page != null || size != null || sortBy != null || sortDirection != null) {
            int p = (page != null && page >= 0) ? page : 0;
            int s = (size != null && size > 0) ? size : 10;
            String sb = sortBy != null ? sortBy : "overallRating";
            String sd = sortDirection != null ? sortDirection : "DESC";
            Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

            PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, name, pageable
            );

            BaseResponse response = new BaseResponse();
            response.setCode(200);
            response.setMessage("Get property by name successfully");
            response.setData(pageResponse);
            return ResponseEntity.ok(response);
        }

        PropertyDTO dto = propertyService.getPropertyByName(name);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get property by name successfully (DEPRECATED: use /property/filter?name=...) ");
        response.setData(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?city=... instead
     */
    @Deprecated
    @GetMapping("/city/{cityName}")
    public ResponseEntity<?> getByCity(@PathVariable String cityName,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size,
                                       @RequestParam(required = false) String sortBy,
                                       @RequestParam(required = false) String sortDirection) {
        if (page != null || size != null || sortBy != null || sortDirection != null) {
            int p = (page != null && page >= 0) ? page : 0;
            int s = (size != null && size > 0) ? size : 10;
            String sb = sortBy != null ? sortBy : "overallRating";
            String sd = sortDirection != null ? sortDirection : "DESC";
            Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

            PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(
                    null, cityName, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, pageable
            );

            BaseResponse response = new BaseResponse();
            response.setCode(200);
            response.setMessage("Get properties by city successfully");
            response.setData(pageResponse);
            return ResponseEntity.ok(response);
        }

        List<PropertyDTO> dtos = propertyService.getByCity(cityName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by city successfully (DEPRECATED: use /property/filter?city=...)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?location=... instead
     */
    @Deprecated
    @GetMapping("/location/{locationName}")
    public ResponseEntity<?> getByLocation(@PathVariable String locationName,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size,
                                           @RequestParam(required = false) String sortBy,
                                           @RequestParam(required = false) String sortDirection) {
        if (page != null || size != null || sortBy != null || sortDirection != null) {
            int p = (page != null && page >= 0) ? page : 0;
            int s = (size != null && size > 0) ? size : 10;
            String sb = sortBy != null ? sortBy : "overallRating";
            String sd = sortDirection != null ? sortDirection : "DESC";
            Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

            PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(
                    null, null, locationName, null, null,
                    null, null, null, null,
                    null, null, null, null, null, pageable
            );

            BaseResponse response = new BaseResponse();
            response.setCode(200);
            response.setMessage("Get properties by location successfully");
            response.setData(pageResponse);
            return ResponseEntity.ok(response);
        }

        List<PropertyDTO> dtos = propertyService.getByLocation(locationName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by location successfully (DEPRECATED: use /property/filter?location=...)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter with multiple parameters instead
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<?> searchProperties(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) Integer maxAdults,
            @RequestParam(required = false) Integer maxChildren,
            @RequestParam(required = false) Integer maxInfants,
            @RequestParam(required = false) Integer maxPets,
            @RequestParam(required = false) List<Integer> amenities,
            @RequestParam(required = false) List<Integer> facilities,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        if (page != null || size != null || sortBy != null || sortDirection != null) {
            int p = (page != null && page >= 0) ? page : 0;
            int s = (size != null && size > 0) ? size : 10;
            String sb = sortBy != null ? sortBy : "overallRating";
            String sd = sortDirection != null ? sortDirection : "DESC";
            Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

            PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(
                    type, city, location, minPrice, maxPrice,
                    bedrooms, bathrooms, maxAdults, maxChildren,
                    maxInfants, maxPets, amenities, facilities, null, pageable
            );

            BaseResponse response = new BaseResponse();
            response.setCode(200);
            response.setMessage("Search properties successfully (DEPRECATED: use /property/filter for paginated results)");
            response.setData(pageResponse);
            return ResponseEntity.ok(response);
        }

        List<PropertyDTO> dtos = propertyService.searchProperties(
                type, city, location, minPrice, maxPrice,
                bedrooms, bathrooms, maxAdults, maxChildren,
                maxInfants, maxPets, amenities, facilities
        );
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Search properties successfully (DEPRECATED: use /property/filter for paginated results)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter instead
     */
    @Deprecated
    @GetMapping("/all")
    public ResponseEntity<?> getAllProperties(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) String sortBy,
                                              @RequestParam(required = false) String sortDirection) {
        if (page != null || size != null || sortBy != null || sortDirection != null) {
            int p = (page != null && page >= 0) ? page : 0;
            int s = (size != null && size > 0) ? size : 10;
            String sb = sortBy != null ? sortBy : "priority";
            String sd = sortDirection != null ? sortDirection : "DESC";
            Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

            PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, pageable
            );

            BaseResponse response = new BaseResponse();
            response.setCode(200);
            response.setMessage("Get all properties successfully (DEPRECATED: use /property/filter)");
            response.setData(pageResponse);
            return ResponseEntity.ok(response);
        }

        List<PropertyDTO> dtos = propertyService.getAllProperties();
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get all properties successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?type=... instead
     */
    @Deprecated
    @GetMapping("/type/{propertyType}")
    public ResponseEntity<?> getByPropertyType(@PathVariable int propertyType,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) String sortBy,
                                               @RequestParam(required = false) String sortDirection) {
        if (page != null || size != null || sortBy != null || sortDirection != null) {
            int p = (page != null && page >= 0) ? page : 0;
            int s = (size != null && size > 0) ? size : 10;
            String sb = sortBy != null ? sortBy : "priority";
            String sd = sortDirection != null ? sortDirection : "DESC";
            Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

            PageResponse<PropertyListItemDTO> pageResponse = propertyService.searchPropertiesPaginated(
                    propertyType, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, pageable
            );

            BaseResponse response = new BaseResponse();
            response.setCode(200);
            response.setMessage("Get properties by type successfully (DEPRECATED: use /property/filter?type=...)");
            response.setData(pageResponse);
            return ResponseEntity.ok(response);
        }

        List<PropertyDTO> dtos = propertyService.getByPropertyType(propertyType);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by type successfully (DEPRECATED: use /property/filter?type=...)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?minPrice=...&maxPrice=... instead
     */
    @Deprecated
    @GetMapping("/price")
    public ResponseEntity<?> getByPriceRange(@RequestParam BigDecimal minPrice, @RequestParam BigDecimal maxPrice) {
        List<PropertyDTO> dtos = propertyService.getByPriceRange(minPrice, maxPrice);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by price range successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?bedrooms=... instead
     */
    @Deprecated
    @GetMapping("/bedrooms/{numberOfBedrooms}")
    public ResponseEntity<?> getByNumRooms(@PathVariable Integer numberOfBedrooms) {
        List<PropertyDTO> dtos = propertyService.getByNumBedRooms(numberOfBedrooms);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by number of bedrooms successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?bathrooms=... instead
     */
    @Deprecated
    @GetMapping("/bathrooms/{numberOfBathrooms}")
    public ResponseEntity<?> getByNumBathrooms(@PathVariable Integer numberOfBathrooms) {
        List<PropertyDTO> dtos = propertyService.getByNumBathrooms(numberOfBathrooms);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by number of bathrooms successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?amenities=1,2,3 instead
     */
    @Deprecated
    @GetMapping("/amenities")
    public ResponseEntity<?> getByAmenities(@RequestParam List<Integer> ids) {
        List<PropertyDTO> dtos = propertyService.getByAmenities(ids);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by amenities successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?facilities=1,2,3 instead
     */
    @Deprecated
    @GetMapping("/facilities")
    public ResponseEntity<?> getByFacilities(@RequestParam List<Integer> ids) {
        List<PropertyDTO> dtos = propertyService.getByFacilities(ids);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by facilities successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter with host filter (if implemented) instead
     */
    @Deprecated
    @GetMapping("/host/{hostId}")
    public ResponseEntity<?> getByHostId(@PathVariable Integer hostId) {
        List<PropertyDTO> dtos = propertyService.getByHostId(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by host successfully");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?maxAdults=... instead
     */
    @Deprecated
    @GetMapping("/max-adults/{maxAdults}")
    public ResponseEntity<?> getByMaxAdults(@PathVariable Integer maxAdults) {
        List<PropertyDTO> dtos = propertyService.getByMaxAdults(maxAdults);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by max adults successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?maxChildren=... instead
     */
    @Deprecated
    @GetMapping("/max-children/{maxChildren}")
    public ResponseEntity<?> getByMaxChildren(@PathVariable Integer maxChildren) {
        List<PropertyDTO> dtos = propertyService.getByMaxChildren(maxChildren);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by max children successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?maxInfants=... instead
     */
    @Deprecated
    @GetMapping("/max-infants/{maxInfants}")
    public ResponseEntity<?> getByMaxInfants(@PathVariable Integer maxInfants) {
        List<PropertyDTO> dtos = propertyService.getByMaxInfants(maxInfants);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by max infants successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?maxPets=... instead
     */
    @Deprecated
    @GetMapping("/max-pets/{maxPets}")
    public ResponseEntity<?> getByMaxPets(@PathVariable Integer maxPets) {
        List<PropertyDTO> dtos = propertyService.getByMaxPets(maxPets);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by max pets successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?type=...&city=... instead
     */
    @Deprecated
    @GetMapping("/type/{propertyType}/city/{cityName}")
    public ResponseEntity<?> getByPropertyTypeAndCity(@PathVariable int propertyType, @PathVariable String cityName) {
        List<PropertyDTO> dtos = propertyService.getByPropertyTypeAndCity(propertyType, cityName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by type and city successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use GET /property/filter?type=...&location=... instead
     */
    @Deprecated
    @GetMapping("/type/{propertyType}/location/{locationName}")
    public ResponseEntity<?> getByPropertyTypeAndLocation(@PathVariable int propertyType, @PathVariable String locationName) {
        List<PropertyDTO> dtos = propertyService.getByPropertyTypeAndLocation(propertyType, locationName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by type and location successfully (DEPRECATED: use /property/filter)");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }
}
