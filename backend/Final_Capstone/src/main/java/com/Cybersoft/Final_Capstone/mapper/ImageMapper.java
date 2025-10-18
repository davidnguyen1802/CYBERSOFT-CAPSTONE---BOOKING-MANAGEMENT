package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Image;
import com.Cybersoft.Final_Capstone.dto.ImageDTO;
import lombok.Data;

public class ImageMapper {
    public static ImageDTO toDTO(Image image){
        ImageDTO dto = new ImageDTO();
        dto.setImageId(image.getId());
        dto.setDescription(image.getImageDescription());
        dto.setImageUrl(image.getImagePath());
        dto.setCreateDate(image.getCreateDate());
        dto.setUpdateDate(image.getUpdateDate());
        return dto;
    }
}
