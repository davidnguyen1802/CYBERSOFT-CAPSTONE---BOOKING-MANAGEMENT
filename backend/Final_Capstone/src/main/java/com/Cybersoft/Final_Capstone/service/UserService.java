package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.UpdateUserDTO;
import com.Cybersoft.Final_Capstone.dto.UserProfileDTO;

/**
 * User Service Interface
 * Handles user profile and account operations for GUEST and HOST roles
 */
public interface   UserService {
    
    /**
     * Get user's own profile information
     * Returns role-specific data (GUEST vs HOST)
     * 
     * @param userId The authenticated user's ID
     * @return UserProfileDTO with basic profile info and statistics
     */
    UserProfileDTO getMyProfile(Integer userId);
    
    /**
     * Update user's own profile information
     * Users can only update their own profile
     * 
     * @param userId The authenticated user's ID
     * @param updateUserDTO The updated user information
     * @return Updated UserProfileDTO
     */
    UserProfileDTO updateMyProfile(Integer userId, UpdateUserDTO updateUserDTO);
    
    /**
     * Check if user exists and is active
     * 
     * @param userId The user ID to check
     * @return true if user exists and is active
     */
    boolean isUserActive(Integer userId);
}
