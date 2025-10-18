package com.Cybersoft.Final_Capstone.payload.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AmenityInsertRequest {
    private String name;
    private String description;
    MultipartFile icon;
}
