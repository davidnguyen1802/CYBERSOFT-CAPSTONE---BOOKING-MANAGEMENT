package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.payload.request.SignInRequest;
import com.Cybersoft.Final_Capstone.payload.request.SignUpRequest;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    /**
     * Sign in with username/email and password
     * @param signInRequest Sign in credentials
     * @return Authentication response with JWT token and user info
     */
//    AuthResponse signIn(SignInRequest signInRequest);
//
    /**
     * Sign up a new user account
     * @param signUpRequest User registration details
     * @param avatar Optional user avatar image
     * @return UserAccount entity (already saved to database)
     */
    UserAccount signUp(SignUpRequest signUpRequest, MultipartFile avatar) throws Exception;
//
//    /**
//     * Get user ID from JWT token
//     * @param token JWT token string
//     * @return User ID as string
//     */
    String getUserIdFromToken(String token);
    
    /**
     * Upload avatar for existing user
     * @param userId User's ID
     * @param avatar Avatar image file
     * @return Avatar URL path
     */
    String uploadAvatar(Integer userId, MultipartFile avatar);
    
    /**
     * Initiate password reset process by sending reset email
     * @param email User's email address
     * @return Success message
     */
    String forgotPassword(String email);
    
    /**
     * Reset password using valid reset token
     * @param token Password reset token
     * @param newPassword New password to set
     * @return Success message
     */
    String resetPassword(String token, String newPassword);
    
    /**
     * Validate password reset token
     * @param token Password reset token
     * @return true if valid, false otherwise
     */
    boolean validatePasswordResetToken(String token);
    
    /**
     * Sign out user by revoking their current token
     * @param token JWT token to revoke
     * @return Success message
     */
    String signOut(String token);
    
    /**
     * Sign out user from all devices by revoking all their tokens
     * @param token JWT token to identify the user
     * @return Success message
     */
    String signOutAllDevices(String token);

    /**
     * Login with username/email and password
     * @param signInRequest Sign in credentials
     * @return JWT token string
     */
    String login(SignInRequest signInRequest) throws Exception;

    /**
     * Get user details from JWT token
     * @param token JWT token string
     * @return UserAccount entity
     */
    UserAccount getUserDetailsFromToken(String token) throws Exception;
    
    /**
     * Get user details from refresh token
     * @param refreshToken Refresh token string
     * @return UserAccount entity
     */
    UserAccount getUserDetailsFromRefreshToken(String refreshToken) throws Exception;

    String loginSocial(SignInRequest signInRequest) throws Exception;
}
