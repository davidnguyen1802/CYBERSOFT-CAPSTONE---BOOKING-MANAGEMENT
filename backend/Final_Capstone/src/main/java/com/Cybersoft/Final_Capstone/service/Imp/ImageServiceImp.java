package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Image;
import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.components.SecurityUtil;
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
import org.springframework.security.access.AccessDeniedException;

@Service
public class ImageServiceImp implements ImageService {
    @Autowired
    ImageRepository imageRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    private void checkHostOwnership(Property property) {
        UserAccount currentUser = securityUtil.getLoggedInUser();
        
        if (currentUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }
        
        if (!"HOST".equals(currentUser.getRole().getName())) {
            throw new AccessDeniedException("Only HOST users can manage property images");
        }
        
        if (!property.getHost().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only manage images for your own properties");
        }
    }

    @Override
    public void addImageToProperty(ImageRequest imageRequest) {
        Property property = propertyRepository.findById(imageRequest.getPropertyId())
                .orElseThrow(()-> new DataNotFoundException("Property not found with id: " + imageRequest.getPropertyId()));
        
        checkHostOwnership(property);
        
        // Kiểm tra xem số lượng hình ảnh và mô tả có khớp không
        if (imageRequest.getFile().size() != imageRequest.getImageDescription().size()) {
            throw new IllegalArgumentException("The number of images and descriptions must match.");
        }

        for (int i = 0; i < imageRequest.getFile().size(); i++) {
            // Tạo đối tượng Image và gán các thuộc tính
            MultipartFile file = imageRequest.getFile().get(i);
            String description = imageRequest.getImageDescription().get(i);  // Mô tả tương ứng

            // Tạo custom filename: image_of_{propertyId}_{index}
            String customFileName = "image_of_" + imageRequest.getPropertyId() + "_" + i;
            
            Image image = new Image();
            // Lưu file với custom name và nhận lại đường dẫn
            String imagePath = fileStorageService.saveFileWithCustomName(file, customFileName);
            image.setImagePath(imagePath);
            image.setImageDescription(description);
            image.setProperty(property);
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
        
        // Kiểm tra quyền HOST và ownership
        checkHostOwnership(property);
        
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
        
        Property property = image.getProperty();
        if (property == null) {
            throw new DataNotFoundException("Property not found for this image");
        }
        
        // Kiểm tra quyền HOST và ownership
        checkHostOwnership(property);
        
        // Cập nhật mô tả
        image.setImageDescription(imageUpdateRequest.getDescription());
        
        // Xóa file cũ
        fileStorageService.deleteFile(image.getImagePath());
        
        // Tạo custom filename dựa trên propertyId và imageId
        String customFileName = "image_of_property_" + property.getId() + "_" + image.getId();
        
        // Lưu file mới với custom name
        String newImagePath = fileStorageService.saveFileWithCustomName(imageUpdateRequest.getFile(), customFileName);
        image.setImagePath(newImagePath);
        
        try {
            imageRepository.save(image);
        } catch (Exception e){
            throw new RuntimeException("Update image failed");
        }

    }
}
