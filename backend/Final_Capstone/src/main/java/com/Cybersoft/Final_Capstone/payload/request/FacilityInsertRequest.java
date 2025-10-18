package com.Cybersoft.Final_Capstone.payload.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FacilityInsertRequest {
    private String name;
    private Integer quantity;
    private MultipartFile icon;
}
