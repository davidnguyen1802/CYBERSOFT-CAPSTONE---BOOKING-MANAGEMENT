package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.dto.CityDTO;
import com.Cybersoft.Final_Capstone.mapper.CityMapper;
import com.Cybersoft.Final_Capstone.repository.CityRepository;
import com.Cybersoft.Final_Capstone.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityServiceImp implements CityService {

    private final CityRepository cityRepository;

    @Override
    public List<CityDTO> getAllCityNames() {
        return cityRepository.findAll().stream()
                .map(CityMapper::toDTO)
                .collect(Collectors.toList());
    }
}

