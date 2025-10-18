package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Image;
import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.payload.request.ImageRequest;
import com.Cybersoft.Final_Capstone.payload.request.ImageUpdateRequest;
import com.Cybersoft.Final_Capstone.repository.ImageRepository;
import com.Cybersoft.Final_Capstone.repository.PropertyRepository;
import com.Cybersoft.Final_Capstone.service.FileStorageService;
import com.Cybersoft.Final_Capstone.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class ImageServiceImp implements ImageService {
    @Autowired
    ImageRepository imageRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public void addImageToProperty(ImageRequest imageRequest) {
        Property property = propertyRepository.findById(imageRequest.getPropertyId())
                .orElseThrow(()-> new DataNotFoundException("Property not found with id: " + imageRequest.getPropertyId()));
        // Kiểm tra xem số lượng hình ảnh và mô tả có khớp không
        if (imageRequest.getFile().size() != imageRequest.getImageDescription().size()) {
            throw new IllegalArgumentException("The number of images and descriptions must match.");
        }

        for (int i = 0; i < imageRequest.getFile().size(); i++) {
            // Tạo đối tượng Image và gán các thuộc tính
            MultipartFile file = imageRequest.getFile().get(i);
            String description = imageRequest.getImageDescription().get(i);  // Mô tả tương ứng

            Image image = new Image();
            image.setImagePath("/files/" + file.getOriginalFilename());
            image.setImageDescription(description);
            image.setProperty(property);
            fileStorageService.saveFile(file);
            property.getImages().add(image);
        }
        try {
            imageRepository.saveAll(property.getImages());
        } catch (Exception e){
            throw new RuntimeException("Add image failed");
        }
    }

    @Override
    public void removeImageOfProperty(ImageRequest imageRequest) {
        int propertyId = imageRequest.getPropertyId();
        int imageId = imageRequest.getImageId();
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(()-> new DataNotFoundException("Property not found with id: " + propertyId));
        Image image = imageRepository.findById(imageId)
                .orElseThrow(()-> new DataNotFoundException("Image not found with id: " + imageId));
        if(!image.getProperty().getId().equals(property.getId())){
            throw new RuntimeException("Image does not belong to the property");
        }
        try {
            property.getImages().remove(image);
            fileStorageService.deleteFile(image.getImagePath());
            imageRepository.delete(image);
        } catch (Exception e){
            throw new RuntimeException("Remove image failed");
        }
    }

    @Override
    public void updateImage(ImageUpdateRequest imageUpdateRequest) {
        Image image = imageRepository.findById(imageUpdateRequest.getImageId())
                .orElseThrow(()-> new DataNotFoundException("Image not found with id: " + imageUpdateRequest.getImageId()));
        image.setImageDescription(imageUpdateRequest.getDescription());
        fileStorageService.deleteFile(image.getImagePath());
        image.setImagePath("files" + imageUpdateRequest.getFile().getOriginalFilename());
        fileStorageService.saveFile(imageUpdateRequest.getFile());
        try {
            imageRepository.save(image);
        } catch (Exception e){
            throw new RuntimeException("Update image failed");
        }

    }
}
