package com.Cybersoft.Final_Capstone.payload.request;

import com.Cybersoft.Final_Capstone.Entity.Facility;
import lombok.Data;

import java.util.List;

@Data
public class FacilityRequest {
    private List<Integer> ids;
    private Integer propertyId;

}
