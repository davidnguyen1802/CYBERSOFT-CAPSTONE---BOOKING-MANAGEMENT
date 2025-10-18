package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.dto.FacilityDTO;
import com.Cybersoft.Final_Capstone.payload.request.FacilityInsertRequest;
import com.Cybersoft.Final_Capstone.payload.request.FacilityRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.Imp.FacilityServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facilities")
public class FacilityController {

    @Autowired
    private FacilityServiceImp facilityService;

    @PostMapping
    public ResponseEntity<?> insertFacility(@RequestBody FacilityInsertRequest request) {
        facilityService.insertFacility(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Insert facility successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllFacilities() {
        List<FacilityDTO> dtos = facilityService.getAllFacilities();
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get all facilities successfully");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getFacilitiesByProperty(@PathVariable int propertyId) {
        List<FacilityDTO> dtos = facilityService.getFacilitiesByPropertyId(propertyId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get facilities by property successfully");
        response.setData(dtos);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/property")
    public ResponseEntity<?> addFacilityToProperty(@RequestBody FacilityRequest request) {
        facilityService.addFacilityToProperty(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Add facilities to property successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/property")
    public ResponseEntity<?> updateFacilityOfProperty(@RequestBody FacilityRequest request) {
        facilityService.updateFacilityOfProperty(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Update facilities of property successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFacility(@RequestBody FacilityRequest request) {
        facilityService.deleteFacility(request);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Delete facilities successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }
}
