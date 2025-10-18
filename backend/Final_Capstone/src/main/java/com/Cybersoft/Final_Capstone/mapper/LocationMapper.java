package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Location;
import com.Cybersoft.Final_Capstone.dto.LocationDTO;

public class LocationMapper {

    public static LocationDTO toDTO(Location location) {
        if (location == null) {
            return null;
        }

        LocationDTO dto = new LocationDTO();
        dto.setId(location.getId());
        dto.setLocationName(location.getLocationName());
        dto.setCityName(location.getCity() != null ? location.getCity().getCityName() : null);
        return dto;
    }

    public static Location toEntity(LocationDTO dto) {
        if (dto == null) {
            return null;
        }

        Location location = new Location();
        location.setId(dto.getId());
        location.setLocationName(dto.getLocationName());
        return location;
    }
}

