package com.Cybersoft.Final_Capstone.controller;


import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.multipart.MultipartFile;
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
    // PROPERTY CREATION (Use /complete endpoint)
    // ========================================
    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/complete")
    public ResponseEntity<?> createCompleteProperty(
            @RequestPart("property") PropertyRequest propertyRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "imageDescriptions", required = false) List<String> imageDescriptions,
            @RequestPart(value = "amenityIds", required = false) List<Integer> amenityIds,
            @RequestPart(value = "facilityIds", required = false) List<Integer> facilityIds
    ) {
        int propertyId = propertyService.createCompleteProperty(
                propertyRequest,
                images,
                imageDescriptions,
                amenityIds,
                facilityIds
        );
        
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Create complete property successfully");
        response.setData(Collections.singletonMap("propertyId", propertyId));
        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasRole('HOST')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProperty(@PathVariable int id, @ModelAttribute PropertyRequest propertyRequest) {
        PropertyDTO dto = propertyService.updateProperty(id, propertyRequest);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Update property successfully");
        response.setData(dto);
        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasAnyRole('HOST, ADMIN')")
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
    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/{hostId}")
    public ResponseEntity<?> getByHostId(
            @PathVariable Integer hostId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 9;
        String sb = sortBy != null ? sortBy : "id";
        String sd = sortDirection != null ? sortDirection : "DESC";
        Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

        PageResponse<PropertyDTO> pageResponse = propertyService.getByHostId(hostId, pageable);
        
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get properties by host successfully");
        response.setData(pageResponse);
        return ResponseEntity.ok(response);
    }
}
