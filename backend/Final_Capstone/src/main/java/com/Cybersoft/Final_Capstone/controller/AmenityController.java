package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.dto.AmenityDTO;
import com.Cybersoft.Final_Capstone.payload.request.AmenityInsertRequest;
import com.Cybersoft.Final_Capstone.payload.request.AmenityRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.Imp.AmenityServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/amenities")
public class AmenityController {

    @Autowired
    private AmenityServiceImp amenityService;

    @PostMapping
    public ResponseEntity<?> insertAmenity(@RequestBody AmenityInsertRequest request) {
        amenityService.insertAmenity(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Insert amenity successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllAmenities() {
        List<AmenityDTO> dtos = amenityService.getAllAmenities();
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get all amenities successfully");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getAmenitiesByProperty(@PathVariable int propertyId) {
        List<AmenityDTO> dtos = amenityService.getAmenitiesByPropertyId(propertyId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get amenities by property successfully");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/property")
    public ResponseEntity<?> addAmenityToProperty(@RequestBody AmenityRequest request) {
        amenityService.addAmenityToProperty(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Add amenities to property successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/property")
    public ResponseEntity<?> updateAmenityOfProperty(@RequestBody AmenityRequest request) {
        amenityService.updateAmenityOfProperty(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Update amenities of property successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAmenity(@RequestBody AmenityRequest request) {
        amenityService.deleteAmenity(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Delete amenities successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }
}
