package com.Cybersoft.Final_Capstone.payload.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ImageUpdateRequest {
    private Integer imageId;
    private String description;
    private MultipartFile file;
}
