package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.FacilityDTO;
import com.Cybersoft.Final_Capstone.payload.request.FacilityInsertRequest;
import com.Cybersoft.Final_Capstone.payload.request.FacilityRequest;

import java.util.List;

public interface FacilityService {
    void addFacilityToProperty(FacilityRequest facilityRequest);
    void updateFacilityOfProperty(FacilityRequest facilityRequest);
    void deleteFacility(FacilityRequest facilityRequest);
    void insertFacility(FacilityInsertRequest facilityInsertRequest);
    List<FacilityDTO> getAllFacilities();
    List<FacilityDTO> getFacilitiesByPropertyId(int propertyId);
}
