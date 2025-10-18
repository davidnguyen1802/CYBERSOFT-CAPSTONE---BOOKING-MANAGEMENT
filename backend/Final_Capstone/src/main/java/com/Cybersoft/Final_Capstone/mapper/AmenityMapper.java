package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Amenity;
import com.Cybersoft.Final_Capstone.dto.AmenityDTO;
import lombok.Data;

@Data
public class AmenityMapper {
    public static AmenityDTO toDTO(Amenity amenity){
        AmenityDTO dto = new AmenityDTO();
        dto.setId(amenity.getId());
        dto.setAmenityName(amenity.getAmenityName());
        dto.setDescription(amenity.getDescription());
        dto.setIconUrl(amenity.getIconUrl());
        return dto;
    }
}
