package com.Cybersoft.Final_Capstone.dto;

import com.Cybersoft.Final_Capstone.Enum.PropertyType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PropertyDTO {
    private int id;
    private String name;
    private BigDecimal rating;
    private String hostName;
    private String address;
    private String locationName;
    private String cityName;
    private BigDecimal pricePerNight;
    private Integer numberOfBedrooms;
    private Integer numberOfBathrooms;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxInfants;
    private Integer maxPets;
    private PropertyType propertyType;
    private String description;
    private boolean isAvailable;
    private List<ImageDTO> images;
    private List<ReviewDTO> reviews;
    private List<AmenityDTO> amenities;
    private List<FacilityDTO> facilities;
    private List<String> nameUserFavorites;
    private LocalDateTime createDate;

}
