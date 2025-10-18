package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.City;
import com.Cybersoft.Final_Capstone.dto.CityDTO;

public class CityMapper {

    public static CityDTO toDTO(City city) {
        if (city == null) {
            return null;
        }

        CityDTO dto = new CityDTO();
        dto.setId(city.getId());
        dto.setCityName(city.getCityName());
        return dto;
    }

    public static City toEntity(CityDTO dto) {
        if (dto == null) {
            return null;
        }

        City city = new City();
        city.setId(dto.getId());
        city.setCityName(dto.getCityName());
        return city;
    }
}

