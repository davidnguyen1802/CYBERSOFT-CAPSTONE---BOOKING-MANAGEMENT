package com.Cybersoft.Final_Capstone.payload.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ImageRequest {
    private int imageId;
    private int propertyId;
    private List<MultipartFile> file;
    private List<String> imageDescription;
}
