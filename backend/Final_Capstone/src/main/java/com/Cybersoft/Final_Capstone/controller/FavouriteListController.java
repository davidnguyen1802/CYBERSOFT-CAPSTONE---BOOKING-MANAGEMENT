package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.dto.PropertyDTO;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.service.FavouriteListService;
import com.Cybersoft.Final_Capstone.util.PageableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/favorites")
public class  FavouriteListController {

    @Autowired
    private FavouriteListService favouriteListService;

    /**
     * Get all available favorite properties for a user (paginated)
     * GET /user/favorites/{userId}/available
     */
    @GetMapping("/{userId}/available")
    public ResponseEntity<?> getAvailableFavoriteProperties(
            @PathVariable Integer userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        BaseResponse response = new BaseResponse();
        try {
            int p = (page != null && page >= 0) ? page : 0;
            int s = (size != null && size > 0) ? size : 9;
            String sb = sortBy != null ? sortBy : "id";
            String sd = sortDirection != null ? sortDirection : "DESC";
            Pageable pageable = PageableBuilder.buildPropertyPageable(p, s, sb, sd);

            PageResponse<PropertyDTO> pageResponse = favouriteListService.getAvailableFavoriteProperties(userId, pageable);
            
            response.setCode(200);
            response.setMessage("Get available favorite properties successfully");
            response.setData(pageResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setCode(404);
            response.setMessage("Failed to get favorite properties: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Add a property to user's favorite list
     * POST /user/favorites/{userId}/property/{propertyId}
     */
    @PostMapping("/{userId}/property/{propertyId}")
    public ResponseEntity<?> addPropertyToFavorites(@PathVariable Integer userId, @PathVariable Integer propertyId) {
        BaseResponse response = new BaseResponse();
        try {
            favouriteListService.addPropertyToFavorites(userId, propertyId);
            response.setCode(200);
            response.setMessage("Property added to favorites successfully");
            response.setData(null);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.setCode(400);
            response.setMessage(e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setCode(404);
            response.setMessage("Failed to add property to favorites: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Remove a property from user's favorite list
     * DELETE /user/favorites/{userId}/property/{propertyId}
     */
    @DeleteMapping("/{userId}/property/{propertyId}")
    public ResponseEntity<?> removePropertyFromFavorites(@PathVariable Integer userId, @PathVariable Integer propertyId) {
        BaseResponse response = new BaseResponse();
        try {
            favouriteListService.removePropertyFromFavorites(userId, propertyId);
            response.setCode(200);
            response.setMessage("Property removed from favorites successfully");
            response.setData(null);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.setCode(400);
            response.setMessage(e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setCode(404);
            response.setMessage("Failed to remove property from favorites: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Check if a property is in user's favorite list
     * GET /user/favorites/{userId}/property/{propertyId}/check
     */
    @GetMapping("/{userId}/property/{propertyId}/check")
    public ResponseEntity<?> checkIfFavorite(@PathVariable Integer userId, @PathVariable Integer propertyId) {
        BaseResponse response = new BaseResponse();
        try {
            boolean isFavorite = favouriteListService.isFavorite(userId, propertyId);
            response.setCode(200);
            response.setMessage(isFavorite ? "Property is in favorites" : "Property is not in favorites");
            response.setData(isFavorite);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setCode(500);
            response.setMessage("Failed to check favorite status: " + e.getMessage());
            response.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
