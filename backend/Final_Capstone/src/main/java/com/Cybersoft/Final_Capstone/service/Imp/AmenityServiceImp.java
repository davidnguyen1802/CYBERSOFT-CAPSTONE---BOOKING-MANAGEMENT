package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Amenity;
import com.Cybersoft.Final_Capstone.Entity.Image;
import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.dto.AmenityDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.mapper.AmenityMapper;
import com.Cybersoft.Final_Capstone.payload.request.AmenityInsertRequest;
import com.Cybersoft.Final_Capstone.payload.request.AmenityRequest;
import com.Cybersoft.Final_Capstone.repository.AmenityRepository;
import com.Cybersoft.Final_Capstone.repository.PropertyRepository;
import com.Cybersoft.Final_Capstone.service.AmenityService;
import com.Cybersoft.Final_Capstone.service.FileStorageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AmenityServiceImp implements AmenityService {
    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional
    public void addAmenityToProperty(AmenityRequest amenityRequest) {
        List<Amenity> amenities = amenityRepository.findAllById(amenityRequest.getIds());
        Property property = propertyRepository.findById(amenityRequest.getIdProperty())
                .orElseThrow(()-> new DataNotFoundException("Property not found"));
        try {
            amenities.forEach(amenity -> property.getAmenities().add(amenity));
            propertyRepository.save(property);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add amenities");
        }
    }
    @Override
    @Transactional
    public void updateAmenityOfProperty(AmenityRequest amenityRequest) {
        Property property = propertyRepository.findById(amenityRequest.getIdProperty())
                .orElseThrow(()-> new DataNotFoundException("Property not found"));

        Set<Integer> targetAmenityIds = Set.copyOf(amenityRequest.getIds());
        Set<Integer> existingAmenityIds = property.getAmenities().stream()
                .map(Amenity::getId)
                .collect(java.util.stream.Collectors.toSet());

        Set<Integer> toAdd    = new HashSet<>(targetAmenityIds);  toAdd.removeAll(existingAmenityIds);
        Set<Integer> toRemove = new HashSet<>(existingAmenityIds); toRemove.removeAll(targetAmenityIds);

        // validate toAdd tồn tại thật
        if (!toAdd.isEmpty()) {
            List<Amenity> valid = amenityRepository.findAllById(toAdd);
            if (valid.size() != toAdd.size()) throw new IllegalArgumentException("Some amenities not found");

            // bulk insert
            property.getAmenities().addAll(amenityRepository.findAllById(toAdd));
        }
        // bulk delete
        if (!toRemove.isEmpty()) {
            property.getAmenities().removeIf(amenity -> toRemove.contains(amenity.getId()));
        }
        try {
            propertyRepository.save(property);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update amenities");
        }

    }

    @Override
    @Transactional
    public void deleteAmenity(AmenityRequest amenityRequest) {
        List<Amenity> amenities = amenityRepository.findAllById(amenityRequest.getIds());
        try {
            amenityRepository.deleteAll(amenities);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete amenities");
        }

    }

    @Override
    public void insertAmenity(AmenityInsertRequest amenityInsertRequest) {
        if(amenityRepository.existsByAmenityName(amenityInsertRequest.getName())){
            throw new IllegalArgumentException("Amenity name already exists");
        }
        Amenity amenity = new Amenity();
        amenity.setAmenityName(amenityInsertRequest.getName());
        amenity.setDescription(amenityInsertRequest.getDescription());

        MultipartFile icon = amenityInsertRequest.getIcon();
        fileStorageService.saveFile(icon);
        String iconUrl = "/files/" + icon.getOriginalFilename();
        amenity.setIconUrl(iconUrl);
        try {
            amenityRepository.save(amenity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert amenity");
        }

    }

    @Override
    public List<AmenityDTO> getAllAmenities() {
        return amenityRepository.findAll().stream().map(AmenityMapper::toDTO).toList();
    }

    @Override
    public List<AmenityDTO> getAmenitiesByPropertyId(int propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(()-> new DataNotFoundException("Property not found"));
        return property.getAmenities().stream().map(AmenityMapper::toDTO).toList();
    }
}
