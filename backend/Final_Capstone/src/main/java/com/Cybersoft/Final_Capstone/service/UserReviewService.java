package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.Entity.UserReview;
import com.Cybersoft.Final_Capstone.dto.ReviewDTO;
import com.Cybersoft.Final_Capstone.payload.request.UserReviewReuquest;

import java.util.List;

public interface UserReviewService {
    ReviewDTO createReview(UserReviewReuquest request);
    ReviewDTO updateReview(int id, UserReviewReuquest request);
    void deleteReview(int id, Integer userId);
    List<ReviewDTO> getReviewsByUserId(int userId);
}
