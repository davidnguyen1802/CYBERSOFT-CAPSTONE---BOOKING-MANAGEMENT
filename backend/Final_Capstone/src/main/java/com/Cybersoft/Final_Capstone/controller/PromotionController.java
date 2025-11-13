package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.dto.PromotionDTO;
import com.Cybersoft.Final_Capstone.dto.PromotionPreviewDTO;
import com.Cybersoft.Final_Capstone.payload.request.PromotionRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    public ResponseEntity<?> getAllPromotions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 9;
        String sb = sortBy != null ? sortBy : "id";
        String sd = sortDirection != null ? sortDirection : "DESC";
        
        Sort sort = sd.equalsIgnoreCase("ASC") ? Sort.by(sb).ascending() : Sort.by(sb).descending();
        Pageable pageable = PageRequest.of(p, s, sort);

        PageResponse<PromotionDTO> pageResponse = promotionService.findAll(pageable);
        
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get all promotions successfully");
        response.setData(pageResponse);
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

    /**
     * Validate promotion and calculate discount BEFORE booking creation
     * Called by frontend when user selects a promotion on booking form
     * 
     * GET /promotions/validate?code=SUMMER2025&propertyId=123&checkIn=2025-12-01T15:00:00&checkOut=2025-12-05T11:00:00
     * 
     * Flow:
     * 1. User fills booking form (property, dates, guests)
     * 2. User selects promotion from their claimed promotions
     * 3. FE calls this endpoint to preview discount
     * 4. FE displays original price + discount + final price
     * 5. User submits booking with promotionCode + originalAmount
     * 
     * @param code Promotion code to validate
     * @param propertyId Property ID
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return PromotionPreviewDTO with discount calculation or error message
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validatePromotion(
            @RequestParam String code,
            @RequestParam Integer propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut) {
        
        PromotionPreviewDTO preview = promotionService.validatePromotionForBooking(code, propertyId, checkIn, checkOut);
        
        BaseResponse response = new BaseResponse();
        if (preview.isValid()) {
            response.setCode(200);
            response.setMessage("Promotion validated successfully");
        } else {
            response.setCode(400);
            response.setMessage("Promotion validation failed");
        }
        response.setData(preview);
        
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
