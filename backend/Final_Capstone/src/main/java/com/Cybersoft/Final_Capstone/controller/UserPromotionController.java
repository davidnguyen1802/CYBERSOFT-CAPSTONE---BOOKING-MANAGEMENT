package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.UserPromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
public class UserPromotionController {
    @Autowired
    private UserPromotionService userPromotionService;

    @GetMapping("/count/{userId}")
    public ResponseEntity<?> countUserPromotions(@PathVariable int userId) {
        int count = userPromotionService.countActivePromotionsByUserId(userId);
        return ResponseEntity.ok(new BaseResponse(200, "Count retrieved successfully", count));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserPromotions(@PathVariable int userId) {
        return ResponseEntity.ok(new BaseResponse(200, "Promotions retrieved successfully",
                userPromotionService.getActivePromotionsByUserId(userId)));
    }
}
