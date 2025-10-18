package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import com.Cybersoft.Final_Capstone.dto.UpdateUserDTO;
import com.Cybersoft.Final_Capstone.dto.UserProfileDTO;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 * Handles user profile and account management for authenticated users
 * Both GUEST and HOST roles can access these endpoints
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    /**
     * Get current user's profile
     * GET /users/me
     * 
     * Returns basic profile information with role-specific statistics
     * - GUEST: bookings count, favorites count, promotions count
     * - HOST: all GUEST info + hosted properties, earnings, ratings
     * 
     * @return UserProfileDTO with user information and statistics
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'ADMIN')")
    public ResponseEntity<BaseResponse> getMyProfile() {
        try {
            Integer userId = securityUtil.getLoggedInUser().getId();
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BaseResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "User not authenticated",
                                null
                        )
                );
            }

            UserProfileDTO profile = userService.getMyProfile(userId);

            return ResponseEntity.ok(
                    new BaseResponse(
                            HttpStatus.OK.value(),
                            "Profile retrieved successfully",
                            profile
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BaseResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Error retrieving profile: " + e.getMessage(),
                            null
                    )
            );
        }
    }

    /**
     * Get current user's detailed profile
     * GET /users/me/details
     * 
     * Returns basic profile information (same as /me endpoint)
     *
     * @return UserProfileDTO with user information and statistics
     */
    @GetMapping("/me/details")
    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'ADMIN')")
    public ResponseEntity<BaseResponse> getMyDetailedProfile() {
        try {
            Integer userId = securityUtil.getLoggedInUser().getId();
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BaseResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "User not authenticated",
                                null
                        )
                );
            }

            UserProfileDTO profile = userService.getMyProfile(userId);

            return ResponseEntity.ok(
                    new BaseResponse(
                            HttpStatus.OK.value(),
                            "Profile retrieved successfully",
                            profile
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BaseResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Error retrieving profile: " + e.getMessage(),
                            null
                    )
            );
        }
    }

    /**
     * Update current user's profile
     * PUT /users/me
     * 
     * Allows users to update their own profile information
     * Users can only update their own profile, not others
     * 
     * @param updateUserDTO Updated user information
     * @return Updated UserProfileDTO
     */
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'ADMIN')")
    public ResponseEntity<BaseResponse> updateMyProfile(@Valid @RequestBody UpdateUserDTO updateUserDTO) {
        try {
            Integer userId = securityUtil.getLoggedInUser().getId();
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BaseResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "User not authenticated",
                                null
                        )
                );
            }

            UserProfileDTO updatedProfile = userService.updateMyProfile(userId, updateUserDTO);

            return ResponseEntity.ok(
                    new BaseResponse(
                            HttpStatus.OK.value(),
                            "Profile updated successfully",
                            updatedProfile
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new BaseResponse(
                            HttpStatus.BAD_REQUEST.value(),
                            "Error updating profile: " + e.getMessage(),
                            null
                    )
            );
        }
    }

    /**
     * Get user profile by ID
     * GET /users/{userId}
     * 
     * Allows users to view another user's basic profile
     * Only returns public information (no sensitive data)
     * 
     * @param userId The user ID to retrieve
     * @return UserProfileDTO with public profile information
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'ADMIN')")
    public ResponseEntity<BaseResponse> getUserProfile(@PathVariable Integer userId) {
        try {
            Integer currentUserId = securityUtil.getLoggedInUser().getId();
            
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BaseResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                "User not authenticated",
                                null
                        )
                );
            }

            // Users can view their own profile or others' public profiles
            UserProfileDTO profile = userService.getMyProfile(userId);
            
            // Remove sensitive information if viewing another user's profile
            if (!currentUserId.equals(userId)) {
                profile.setEmail(null); // Hide email
                profile.setPhone(null); // Hide phone
                profile.setAddress(null); // Hide address
                
                // Remove detailed statistics for other users
                profile.setTotalBookings(null);
                profile.setFavoritePropertiesCount(null);
                profile.setActivePromotionsCount(null);
                profile.setTotalEarnings(null);
            }

            return ResponseEntity.ok(
                    new BaseResponse(
                            HttpStatus.OK.value(),
                            "Profile retrieved successfully",
                            profile
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new BaseResponse(
                            HttpStatus.NOT_FOUND.value(),
                            "User not found: " + e.getMessage(),
                            null
                    )
            );
        }
    }

    /**
     * Check if user account is active
     * GET /users/{userId}/status
     * 
     * @param userId The user ID to check
     * @return Boolean indicating if user is active
     */
    @GetMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'ADMIN')")
    public ResponseEntity<BaseResponse> checkUserStatus(@PathVariable Integer userId) {
        try {
            boolean isActive = userService.isUserActive(userId);

            return ResponseEntity.ok(
                    new BaseResponse(
                            HttpStatus.OK.value(),
                            "User status retrieved successfully",
                            isActive
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new BaseResponse(
                            HttpStatus.NOT_FOUND.value(),
                            "Error checking user status: " + e.getMessage(),
                            null
                    )
            );
        }
    }
}
