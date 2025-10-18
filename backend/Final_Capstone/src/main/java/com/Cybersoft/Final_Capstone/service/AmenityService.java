package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.AmenityDTO;
import com.Cybersoft.Final_Capstone.payload.request.AmenityInsertRequest;
import com.Cybersoft.Final_Capstone.payload.request.AmenityRequest;

import java.util.List;

public interface AmenityService {
    void addAmenityToProperty(AmenityRequest amenityRequest);
    void updateAmenityOfProperty(AmenityRequest amenityRequest);
    void deleteAmenity(AmenityRequest amenityRequest);
    void insertAmenity(AmenityInsertRequest amenityInsertRequest);
    List<AmenityDTO> getAllAmenities();
    List<AmenityDTO> getAmenitiesByPropertyId(int propertyId);
}
