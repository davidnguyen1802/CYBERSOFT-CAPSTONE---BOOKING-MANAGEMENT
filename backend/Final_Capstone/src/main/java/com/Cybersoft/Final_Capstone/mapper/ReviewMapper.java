package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.UserReview;
import com.Cybersoft.Final_Capstone.dto.ReviewDTO;

public class ReviewMapper {
    public static ReviewDTO toDTO(UserReview review){
        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(review.getId());
        dto.setPropertyName(review.getProperty().getPropertyName());
        dto.setPropertyId(review.getProperty().getId());
        dto.setComment(review.getComment());
        dto.setRating(review.getRating());
        dto.setReviewDate(review.getReviewDate());
        return dto;
    }
}
