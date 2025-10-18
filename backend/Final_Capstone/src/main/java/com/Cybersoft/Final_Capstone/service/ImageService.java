package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.payload.request.ImageRequest;
import com.Cybersoft.Final_Capstone.payload.request.ImageUpdateRequest;


public interface ImageService{
    void addImageToProperty(ImageRequest imageRequest);
    void removeImageOfProperty(ImageRequest imageRequest);
    void updateImage(ImageUpdateRequest imageUpdateRequest);
}
