package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.dto.PropertyListItemDTO;

/**
 * Mapper for lightweight PropertyListItemDTO.
 * Used in search results to avoid loading full entity graphs (reviews, images, amenities, etc.).
 */
public class PropertyListItemMapper {

    /**
     * Convert Property entity to lightweight list item DTO.
     * Only maps essential fields for list/card view.
     */
    public static PropertyListItemDTO toListItemDTO(Property property) {
        PropertyListItemDTO dto = new PropertyListItemDTO();
        dto.setId(property.getId());
        dto.setName(property.getPropertyName());
        dto.setRating(property.getOverallRating());
        dto.setHostName(property.getHost() != null ? property.getHost().getFullName() : null);
        dto.setLocationName(property.getLocation() != null ? property.getLocation().getLocationName() : null);
        dto.setCityName(property.getLocation() != null && property.getLocation().getCity() != null
                ? property.getLocation().getCity().getCityName() : null);
        dto.setPricePerNight(property.getPricePerNight());
        dto.setNumberOfBedrooms(property.getNumberOfBedrooms());
        dto.setNumberOfBathrooms(property.getNumberOfBathrooms());
        dto.setMaxAdults(property.getMaxAdults());
        dto.setMaxPets(property.getMaxPets());
        dto.setPropertyType(property.getPropertyType());

        // Get first image URL only (avoid loading all images)
        if (property.getImages() != null && !property.getImages().isEmpty()) {
            dto.setThumbnailImageUrl(property.getImages().get(0).getImagePath());
        }

        return dto;
    }
}
