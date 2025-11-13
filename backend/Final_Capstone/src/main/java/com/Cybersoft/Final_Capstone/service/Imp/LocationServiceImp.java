package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.City;
import com.Cybersoft.Final_Capstone.dto.LocationDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.mapper.LocationMapper;
import com.Cybersoft.Final_Capstone.repository.CityRepository;
import com.Cybersoft.Final_Capstone.repository.LocationRepository;
import com.Cybersoft.Final_Capstone.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationServiceImp implements LocationService {

    private final LocationRepository locationRepository;
    private final CityRepository cityRepository;

    @Override
    public List<LocationDTO> getAllLocationNames() {
        return locationRepository.findAll().stream()
                .map(LocationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationDTO> getLocationsByCityName(String cityName) {
        City city = cityRepository.findByCityName(cityName)
                .orElseThrow(() -> new DataNotFoundException("City not found: " + cityName));

        return locationRepository.findByCityId(city.getId()).stream()
                .map(LocationMapper::toDTO)
                .collect(Collectors.toList());
    }
}

