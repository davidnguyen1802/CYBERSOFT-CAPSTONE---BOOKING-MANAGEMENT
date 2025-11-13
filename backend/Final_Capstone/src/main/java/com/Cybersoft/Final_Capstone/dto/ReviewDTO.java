package com.Cybersoft.Final_Capstone.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private int reviewId;
    private String propertyName;
    private Integer propertyId;
    private String comment;
    private Integer rating;
    private LocalDateTime reviewDate;
}
