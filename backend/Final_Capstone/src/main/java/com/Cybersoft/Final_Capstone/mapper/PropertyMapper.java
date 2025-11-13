package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.dto.ImageDTO;
import com.Cybersoft.Final_Capstone.dto.PropertyDTO;
import com.Cybersoft.Final_Capstone.dto.ReviewDTO;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PropertyMapper {

    public static PropertyDTO toDTO(Property property){
        PropertyDTO dto = new PropertyDTO();
        dto.setId(property.getId());
        AtomicInteger totalStars = new AtomicInteger();
        property.getReviews().forEach(review -> totalStars.addAndGet(review.getRating()));

        dto.setRating(property.getOverallRating());
        dto.setName(property.getPropertyName());
        dto.setHostName(property.getHost().getFullName());
        dto.setAddress(property.getFullAddress());
        dto.setLocationName(property.getLocation().getLocationName());
        dto.setCityName(property.getLocation().getCity().getCityName());
        dto.setPricePerNight(property.getPricePerNight());
        dto.setNumberOfBedrooms(property.getNumberOfBedrooms());
        dto.setNumberOfBathrooms(property.getNumberOfBathrooms());
        dto.setMaxAdults(property.getMaxAdults());
        dto.setMaxChildren(property.getMaxChildren());
        dto.setMaxInfants(property.getMaxInfants());
        dto.setMaxPets(property.getMaxPets());
        dto.setPropertyType(property.getPropertyType());
        dto.setDescription(property.getDescription());
        dto.setAvailable(property.getStatus().getName().equals("AVAILABLE"));
        List<ImageDTO> images = property.getImages().stream().map(ImageMapper::toDTO).toList();
        dto.setImages(images);
        List<ReviewDTO> reviews = property.getReviews().stream().map(ReviewMapper::toDTO).toList();
        dto.setReviews(reviews);
        dto.setAmenities(property.getAmenities().stream().map(AmenityMapper::toDTO).toList());
        dto.setFacilities(property.getFacilities().stream().map(FacilityMapper::toDTO).toList());
        dto.setNameUserFavorites(property.getFavoriteBy().stream().map(UserAccount::getUsername).toList());
        dto.setCreateDate(property.getCreateDate());
        return dto;
    }
}
