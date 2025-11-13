package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.dto.UserPromotionDTO;
import com.Cybersoft.Final_Capstone.payload.request.PromotionClaimRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.service.UserPromotionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> getUserPromotions(
            @PathVariable int userId,
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

        PageResponse<UserPromotionDTO> pageResponse = userPromotionService.getPromotionsByUserId(userId, pageable);
        
        return ResponseEntity.ok(new BaseResponse(200, "Promotions retrieved successfully", pageResponse));
    }

    @DeleteMapping("/{id}/user/{userId}")
    public ResponseEntity<?> deleteUserPromotion(@PathVariable int id, @PathVariable int userId) throws IllegalAccessException {
        try {
            userPromotionService.deletePromotions(id, userId);
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403)
                    .body(new BaseResponse(403, "You do not have permission to delete this promotion", null));
        }
        return ResponseEntity.ok(new BaseResponse(200, "Promotion deleted successfully", null));
    }
    
    /**
     * User claims a promotion by entering promotion code
     * POST /promotions/claim
     * 
     * Flow:
     * 1. Validate promotion exists and is ACTIVE
     * 2. Check timesUsed < usageLimit (GLOBAL)
     * 3. Check promotion date range valid
     * 4. Create UserPromotion (status=ACTIVE, isLocked=false)
     * 5. Does NOT increment Promotion.timesUsed (only increments on payment success)
     * 
     * @param request Contains promotion code
     * @return Created UserPromotionDTO
     */
    @PostMapping("/claim")
    public ResponseEntity<?> claimPromotion(@Valid @RequestBody PromotionClaimRequest request) {
        UserPromotionDTO userPromotion = userPromotionService.claimPromotion(request.getCode());
        BaseResponse response = new BaseResponse();
        response.setCode(201);
        response.setMessage("Promotion claimed successfully");
        response.setData(userPromotion);
        return ResponseEntity.status(201).body(response);
    }
    
}
