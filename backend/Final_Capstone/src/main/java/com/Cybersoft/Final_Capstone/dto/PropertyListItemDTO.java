package com.Cybersoft.Final_Capstone.dto;

import com.Cybersoft.Final_Capstone.Enum.PropertyType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Lightweight DTO for property list/card view.
 * Contains only essential fields needed for search results and property cards.
 * Reduces N+1 queries and payload size compared to full PropertyDTO.
 */
@Data
public class PropertyListItemDTO {
    private int id;
    private String name;
    private BigDecimal rating;
    private String hostName;
    private String locationName;
    private String cityName;
    private BigDecimal pricePerNight;
    private Integer numberOfBedrooms;
    private Integer numberOfBathrooms;
    private Integer maxAdults;
    private Integer maxPets;
    private PropertyType propertyType;
    private String thumbnailImageUrl; // First image URL only
}
