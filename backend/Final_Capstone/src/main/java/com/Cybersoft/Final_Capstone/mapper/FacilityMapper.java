package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Facility;
import com.Cybersoft.Final_Capstone.dto.FacilityDTO;
import lombok.Data;

@Data
public class FacilityMapper {
    public static FacilityDTO toDTO(Facility facility) {
        FacilityDTO dto = new FacilityDTO();
        dto.setId(facility.getId());
        dto.setFacilityName(facility.getFacilityName());
        dto.setQuantity(facility.getQuantity());
        dto.setIconUrl(facility.getIconUrl());
        return dto;
    }
}
