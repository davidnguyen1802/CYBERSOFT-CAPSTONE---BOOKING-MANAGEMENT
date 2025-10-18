package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageDTO {
    private int imageId;
    private String imageUrl;
    private String description;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
}
