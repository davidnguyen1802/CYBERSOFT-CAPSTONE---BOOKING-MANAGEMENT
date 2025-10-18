package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.FavouriteListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/favorites")
public class FavouriteListController {

    @Autowired
    private FavouriteListService favouriteListService;

    /**
     * Get all available favorite properties for a user
     * GET /user/favorites/{userId}/available
     */
    @GetMapping("/{userId}/available")
    public ResponseEntity<?> getAvailableFavoriteProperties(@PathVariable Integer userId) {
        BaseResponse response = new BaseResponse();
        try {
            response.setCode(200);
            response.setMessage("Get available favorite properties successfully");
            response.setData(favouriteListService.getAvailableFavoriteProperties(userId));
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
