package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.request.PromotionRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/promotions")

public class PromotionController {
    @Autowired
    private PromotionService promotionService;

    @PostMapping("/insert")
    public ResponseEntity<?> insertPromotion(@RequestBody PromotionRequest promotionRequest) {
        promotionService.insertPromotion(promotionRequest);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Insert promotion successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllPromotions() {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get all promotions successfully");
        response.setData(promotionService.findAll());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/findCode/{code}")
    public ResponseEntity<?> getPromotionByCode(@PathVariable("code") String code) {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get promotion by code successfully");
        response.setData(promotionService.getPromotionByCode(code));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/findName/{name}")
    public ResponseEntity<?> getPromotionByName(@PathVariable("name") String name) {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get promotion by name successfully");
        response.setData(promotionService.getPromotionByName(name));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{name}")
    public ResponseEntity<?> updatePromotion(@PathVariable("name") String name, @RequestBody PromotionRequest promotionRequest) {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Update promotion successfully");
        response.setData(promotionService.updatePromotion(name, promotionRequest));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{name}")
    public ResponseEntity<?> deletePromotion(@PathVariable("name") String name) {
        promotionService.deletePromotion(name);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Delete promotion successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }
}
