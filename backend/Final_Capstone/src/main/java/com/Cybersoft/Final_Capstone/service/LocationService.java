package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.LocationDTO;

import java.util.List;

public interface LocationService {
    List<LocationDTO> getAllLocationNames();
    List<LocationDTO> getLocationsByCityName(String cityName);
}

