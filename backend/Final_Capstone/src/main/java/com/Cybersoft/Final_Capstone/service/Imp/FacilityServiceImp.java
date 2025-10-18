package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Facility;
import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.dto.FacilityDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.mapper.FacilityMapper;
import com.Cybersoft.Final_Capstone.payload.request.FacilityInsertRequest;
import com.Cybersoft.Final_Capstone.payload.request.FacilityRequest;
import com.Cybersoft.Final_Capstone.repository.FacilityRepository;
import com.Cybersoft.Final_Capstone.repository.PropertyRepository;
import com.Cybersoft.Final_Capstone.service.FacilityService;
import com.Cybersoft.Final_Capstone.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FacilityServiceImp implements FacilityService {
    @Autowired
    private final FacilityRepository facilityRepository;

    @Autowired
    private final PropertyRepository propertyRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional
    public void addFacilityToProperty(FacilityRequest facilityRequest) {
        if (facilityRequest.getPropertyId() == null || facilityRequest.getIds() == null || facilityRequest.getIds().isEmpty()) {
            throw new IllegalArgumentException("Property and facility ids must not be null or empty");
        }

        Property property = propertyRepository.findById(facilityRequest.getPropertyId())
                .orElseThrow(() -> new DataNotFoundException("Property not found"));

        List<Facility> facilities = facilityRepository.findAllById(facilityRequest.getIds());

        try {
            property.getFacilities().addAll(facilities);
            propertyRepository.save(property);
        } catch (Exception e) {
            throw new RuntimeException("Error adding facilities to property: " + e.getMessage());
        }

    }

    @Override
    @Transactional
    public void updateFacilityOfProperty(FacilityRequest facilityRequest) {
        Property property = propertyRepository.findById(facilityRequest.getPropertyId())
                .orElseThrow(() -> new DataNotFoundException("Property not found"));

        Set<Integer> targetIds = Set.copyOf(facilityRequest.getIds());
        Set<Integer> existingIds = property.getFacilities().stream()
                .map(Facility::getId)
                .collect(java.util.stream.Collectors.toSet());

        Set<Integer> toAdd = new HashSet<>(targetIds);      toAdd.removeAll(existingIds);
        Set<Integer> toRemove = new HashSet<>(existingIds);  toRemove.removeAll(targetIds);

        // Validate toAdd
        if (!toAdd.isEmpty()) {
            List<Facility> toAddEntities = facilityRepository.findAllById(toAdd);
            if (toAddEntities.size() != toAdd.size()) {
                throw new IllegalArgumentException("Some facilities not found");
            }
            property.getFacilities().addAll(toAddEntities);
        }

        // Remove
        if (!toRemove.isEmpty()) {
            property.getFacilities().removeIf(f -> toRemove.contains(f.getId()));
        }
        try {
            propertyRepository.save(property);
        } catch (Exception e) {
            throw new RuntimeException("Error updating facilities of property: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteFacility(FacilityRequest facilityRequest) {
        List<Facility> facilities = facilityRepository.findAllById(facilityRequest.getIds());
        try {
            facilityRepository.deleteAll(facilities);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting facilities: " + e.getMessage());
        }
    }

    @Override
    public void insertFacility(FacilityInsertRequest req) {
        if(facilityRepository.existsByFacilityName(req.getName())){
            throw new IllegalArgumentException("Amenity name already exists");
        }
        Facility f = new Facility();
        f.setFacilityName(req.getName());
        f.setQuantity(req.getQuantity());
        f.setIconUrl("/files" + req.getIcon().getOriginalFilename());
        fileStorageService.saveFile(req.getIcon());
        facilityRepository.save(f);
    }

    @Override
    public List<FacilityDTO> getAllFacilities() {
        // Giữ tên method theo interface bạn đã khai báo, nhưng nên đổi tên thành getAllFacilities()
        return facilityRepository.findAll().stream()
                .map(FacilityMapper::toDTO)
                .toList();
    }

    @Override
    public List<FacilityDTO> getFacilitiesByPropertyId(int propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found"));
        return property.getFacilities().stream()
                .map(FacilityMapper::toDTO)
                .toList();
    }
}
