package com.Cybersoft.Final_Capstone.payload.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserReviewReuquest {
    private Integer userId;
    private Integer propertyId;
    private String comment;
    private Integer rating;
}
