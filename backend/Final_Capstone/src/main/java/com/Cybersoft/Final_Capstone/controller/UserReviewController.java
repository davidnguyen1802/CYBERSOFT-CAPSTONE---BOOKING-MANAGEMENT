package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.request.UserReviewReuquest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.UserReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
public class UserReviewController {

    @Autowired
    private UserReviewService userReviewService;

    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody UserReviewReuquest request) {
        BaseResponse response = new BaseResponse();
        try {
            response.setCode(201);
            response.setMessage("Review created successfully");
            response.setData(userReviewService.createReview(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.setCode(400);
            response.setMessage("Failed to create review: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable("id") int id, @RequestBody UserReviewReuquest request) {
        BaseResponse response = new BaseResponse();
        try {
            response.setCode(200);
            response.setMessage("Review updated successfully");
            response.setData(userReviewService.updateReview(id, request));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setCode(400);
            response.setMessage("Failed to update review: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{id}/user/{userId}")
    public ResponseEntity<?> deleteReview(@PathVariable("id") int id,
                                          @PathVariable("userId") Integer userId) {
        BaseResponse response = new BaseResponse();
        try {
            userReviewService.deleteReview(id, userId);
            response.setCode(200);
            response.setMessage("Review deleted successfully");
            response.setData(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setCode(400);
            response.setMessage("Failed to delete review: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getReviewsByUserId(@PathVariable("userId") int userId) {
        BaseResponse response = new BaseResponse();
        try {
            response.setCode(200);
            response.setMessage("Reviews retrieved successfully");
            response.setData(userReviewService.getReviewsByUserId(userId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setCode(400);
            response.setMessage("Failed to retrieve reviews: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
