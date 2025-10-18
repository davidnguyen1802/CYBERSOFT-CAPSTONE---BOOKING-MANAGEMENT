package com.Cybersoft.Final_Capstone.payload.request;

import com.Cybersoft.Final_Capstone.Enum.PropertyType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertyRequest {
    private Integer hostId; // Optional: If provided, will override the authenticated user
    private String fullAddress;
    private String propertyName;
    private BigDecimal pricePerNight;
    private Integer numberOfBedrooms;
    private Integer numberOfBathrooms;
    private String description;
    private Integer propertyType;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxInfants;
    private Integer maxPets;
//    private List<MultipartFile> file;//
//    private List<String> imageDescription;//
    private Integer locationId;//
//    private List<String> listAmenities;//
//    private List<String> listFacilities;//
}
