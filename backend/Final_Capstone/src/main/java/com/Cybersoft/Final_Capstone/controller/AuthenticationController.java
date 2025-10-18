package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import com.Cybersoft.Final_Capstone.payload.request.SignInRequest;
import com.Cybersoft.Final_Capstone.payload.request.SignUpRequest;
import com.Cybersoft.Final_Capstone.payload.response.AuthResponse;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.ResponseObject;
import com.Cybersoft.Final_Capstone.service.AuthService;
import com.Cybersoft.Final_Capstone.service.AuthenticationService;
import com.Cybersoft.Final_Capstone.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Authentication Controller
 * Handles user sign in and sign up operations
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationService authenticationService;

    private final TokenService tokenService;
    private final SecurityUtil securityUtil;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public ResponseEntity<ResponseObject> login(
            @Valid @RequestBody SignInRequest signInRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        // Call login service for traditional login
        String accessToken = authService.login(signInRequest);

        // Get user details
        String userAgent = request.getHeader("User-Agent");
        UserAccount userDetail = authService.getUserDetailsFromToken(accessToken);

        // Generate refresh token
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetail);

        // Revoke previous refresh tokens for this user
        tokenService.revokeAllUserRefreshTokens(userDetail);

        // Save tokens to database
        tokenService.addAccessToken(userDetail, accessToken, isMobileDevice(userAgent));
        tokenService.addRefreshToken(userDetail, refreshToken);

        // Set refresh token as secure HttpOnly cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                .sameSite("Lax")  // Changed to Lax for local development, use Strict in production
                .path("/")
                .maxAge(Duration.ofMillis(jwtTokenUtil.getRefreshExpirationMs()))
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Create response with access token (NOT in cookie)
        AuthResponse authResponse = AuthResponse.builder()
                .message("Login Successfully.")
                .token(accessToken)
                .tokenType("Bearer")
                .refreshToken(null)  // Don't expose refresh token in response body
                .username(userDetail.getUsername())
                .roles(userDetail.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .id(userDetail.getId())
                .build();

        return ResponseEntity.ok().body(
                ResponseObject.builder()
                        .message("Login successfully")
                        .data(authResponse)
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    /**
     * Refresh Token endpoint
     * POST /auth/refresh
     *
     * Reads refresh token from HttpOnly cookie, validates it, and issues new tokens
     * Implements token rotation for security
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ResponseObject.builder()
                                .message("Refresh token missing")
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
            }

            // Refresh and rotate tokens
            Token newRefreshTokenEntity = tokenService.refreshToken(refreshToken);
            UserAccount user = newRefreshTokenEntity.getUser();

            // Generate new access token
            String newAccessToken = jwtTokenUtil.generateAccessToken(user);
            String userAgent = request.getHeader("User-Agent");
            tokenService.addAccessToken(user, newAccessToken, isMobileDevice(userAgent));

            // Set new refresh token cookie
            ResponseCookie newRefreshCookie = ResponseCookie.from("refresh_token", newRefreshTokenEntity.getToken())
                    .httpOnly(true)
                    .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                    .sameSite("Lax")  // Changed to Lax for local development, use Strict in production
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtTokenUtil.getRefreshExpirationMs()))
                    .build();
            response.addHeader("Set-Cookie", newRefreshCookie.toString());

            // Return new access token
            AuthResponse authResponse = AuthResponse.builder()
                    .message("Token refreshed successfully")
                    .token(newAccessToken)
                    .tokenType("Bearer")
                    .username(user.getUsername())
                    .roles(user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                    .id(user.getId())
                    .build();

            return ResponseEntity.ok().body(
                    ResponseObject.builder()
                            .message("Token refreshed successfully")
                            .data(authResponse)
                            .status(HttpStatus.OK)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ResponseObject.builder()
                            .message("Refresh token invalid or expired: " + e.getMessage())
                            .status(HttpStatus.UNAUTHORIZED)
                            .build()
            );
        }
    }

    private boolean isMobileDevice(String userAgent) {
        return userAgent != null && userAgent.toLowerCase().contains("mobile");
    }

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signUp(
            @Valid @ModelAttribute SignUpRequest signUpRequest,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        // Create user account (includes validation, avatar saving, etc.)
        UserAccount newUser = authService.signUp(signUpRequest, avatar);

        // Generate tokens
        String accessToken = jwtTokenUtil.generateAccessToken(newUser);
        String refreshToken = jwtTokenUtil.generateRefreshToken(newUser);

        // Get user agent for device detection
        String userAgent = request.getHeader("User-Agent");

        // Save tokens to database
        tokenService.addAccessToken(newUser, accessToken, isMobileDevice(userAgent));
        tokenService.addRefreshToken(newUser, refreshToken);

        // Set refresh token as secure HttpOnly cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                .sameSite("Lax")  // Changed to Lax for local development, use Strict in production
                .path("/")
                .maxAge(Duration.ofMillis(jwtTokenUtil.getRefreshExpirationMs()))
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Create response
        AuthResponse authResponse = AuthResponse.builder()
                .message("Sign up Successfully.")
                .token(accessToken)
                .tokenType("Bearer")
                .username(newUser.getUsername())
                .roles(newUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .id(newUser.getId())
                .build();

        return ResponseEntity.ok().body(
                ResponseObject.builder()
                        .message("Sign up successfully")
                        .data(authResponse)
                        .status(HttpStatus.OK)
                        .build()
        );
    }

    /**
     * Get Current User endpoint
     * GET /auth/me
     *
     * Returns the current authenticated user's info from Bearer token
     * NO cookie reading - only Authorization header
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ResponseObject.builder()
                                .message("No authentication token found")
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
            }

            String token = authHeader.substring(7);

            // Validate token
            if (jwtTokenUtil.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ResponseObject.builder()
                                .message("Access token expired. Please call /auth/refresh.")
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
            }

            Integer userId = jwtTokenUtil.getUserId(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ResponseObject.builder()
                                .message("Invalid token")
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
            }

            return ResponseEntity.ok().body(
                    ResponseObject.builder()
                            .message("User ID retrieved successfully")
                            .data(userId)
                            .status(HttpStatus.OK)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ResponseObject.builder()
                            .message("Invalid or expired token")
                            .status(HttpStatus.UNAUTHORIZED)
                            .build()
            );
        }
    }

    /**
     * Sign out endpoint
     * POST /auth/logout
     *
     * Revokes refresh token and clears the cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseObject> signOut(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        try {
            if (refreshToken != null && !refreshToken.isEmpty()) {
                // Revoke refresh token in database
                tokenService.revokeToken(refreshToken);
            }

            // Clear the refresh token cookie
            ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader("Set-Cookie", clearCookie.toString());

            return ResponseEntity.ok(
                    ResponseObject.builder()
                            .message("Signed out successfully")
                            .status(HttpStatus.OK)
                            .build()
            );
        } catch (Exception e) {
            // Still clear cookie even if validation fails
            ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader("Set-Cookie", clearCookie.toString());

            return ResponseEntity.ok(
                    ResponseObject.builder()
                            .message("Signed out successfully")
                            .status(HttpStatus.OK)
                            .build()
            );
        }
    }

    /**
     * Sign out from all devices endpoint
     * POST /auth/logout-all
     *
     * Revokes all tokens for the authenticated user
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ResponseObject> signOutAllDevices(
            @AuthenticationPrincipal UserAccount user,
            HttpServletResponse response) {
        try {
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ResponseObject.builder()
                                .message("No authentication found")
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
            }

            // Revoke all tokens for this user
            tokenService.revokeAllUserTokens(user);

            // Clear the refresh token cookie
            ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader("Set-Cookie", clearCookie.toString());

            return ResponseEntity.ok(
                    ResponseObject.builder()
                            .message("Successfully signed out from all devices")
                            .status(HttpStatus.OK)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ResponseObject.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .build()
            );
        }
    }

    /**
     * Upload/Update avatar endpoint
     * POST /auth/upload-avatar
     *
     * Allows users to upload or update their avatar after signup
     * Uses Bearer token from Authorization header
     */
    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> uploadAvatar(
            @RequestPart("avatar") MultipartFile avatar,
            @AuthenticationPrincipal UserAccount user) {
        try {
            if (avatar == null || avatar.isEmpty()) {
                throw new IllegalArgumentException("Avatar file is required");
            }

            // Validate file is an image
            String contentType = avatar.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("File must be an image");
            }

            if (user == null) {
                throw new IllegalArgumentException("Authentication required");
            }

            // Upload avatar
            String avatarUrl = authService.uploadAvatar(user.getId(), avatar);

            BaseResponse responseData = new BaseResponse(
                    HttpStatus.OK.value(),
                    "Avatar uploaded successfully!",
                    avatarUrl
            );
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            BaseResponse responseData = new BaseResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseData);
        }
    }

    /**
     * Social Login - Step 1: Initiate OAuth flow
     * GET /auth/social-login?login_type=google
     * Returns the OAuth authorization URL for the specified provider
     */
    @GetMapping("/social-login")
    public ResponseEntity<String> socialAuth(
            @RequestParam("login_type") String loginType,
            HttpServletRequest request
    ){
        loginType = loginType.trim().toLowerCase();
        String url = authenticationService.generateAuthUrl(loginType);
        return ResponseEntity.ok(url);
    }

    /**
     * Social Login - Step 2: OAuth Callback Handler
     * GET /auth/social/callback?code=xxx&state=google
     * Processes OAuth callback and redirects to Angular frontend with auth data
     * The 'state' parameter contains the login_type (google/facebook)
     */
    @GetMapping("/social/callback")
    public ResponseEntity<?> callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "login_type", required = false) String loginType,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        // Use state parameter if available, otherwise fall back to login_type parameter
        if (state != null && !state.isEmpty()) {
            loginType = state;
        }
        
        // Validate that we have a login type
        if (loginType == null || loginType.isEmpty()) {
            String errorUrl = "http://localhost:4200/auth/callback?error=" + 
                URLEncoder.encode("Missing login type in callback", StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
        
        try {
            // Call the AuthService to get user info
            Map<String, Object> userInfo = authenticationService.authenticateAndFetchProfile(code, loginType);

            if (userInfo == null) {
                String errorUrl = "http://localhost:4200/auth/callback?error=" + 
                    URLEncoder.encode("Failed to authenticate", StandardCharsets.UTF_8);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(errorUrl))
                        .build();
            }

            // Extract user information from userInfo map
            String accountId = "";
            String name = "";
            String picture = "";
            String email = "";

            if (loginType.trim().equals("google")) {
                accountId = (String) Objects.requireNonNullElse(userInfo.get("sub"), "");
                name = (String) Objects.requireNonNullElse(userInfo.get("name"), "");
                picture = (String) Objects.requireNonNullElse(userInfo.get("picture"), "");
                email = (String) Objects.requireNonNullElse(userInfo.get("email"), "");
            } else if (loginType.trim().equals("facebook")) {
                accountId = (String) Objects.requireNonNullElse(userInfo.get("id"), "");
                name = (String) Objects.requireNonNullElse(userInfo.get("name"), "");
                email = (String) Objects.requireNonNullElse(userInfo.get("email"), "");
                // Get picture URL from Facebook data structure
                Object pictureObj = userInfo.get("picture");
                if (pictureObj instanceof Map) {
                    Map<?, ?> pictureData = (Map<?, ?>) pictureObj;
                    Object dataObj = pictureData.get("data");
                    if (dataObj instanceof Map) {
                        Map<?, ?> dataMap = (Map<?, ?>) dataObj;
                        Object urlObj = dataMap.get("url");
                        if (urlObj instanceof String) {
                            picture = (String) urlObj;
                        }
                    }
                }
            }

            // Create SignInRequest object
            SignInRequest userLoginDTO = SignInRequest.builder()
                    .email(email)
                    .fullname(name)
                    .password("")
                    .profileImage(picture)
                    .build();

            if (loginType.trim().equals("google")) {
                userLoginDTO.setGoogleAccountId(accountId);
            } else if (loginType.trim().equals("facebook")) {
                userLoginDTO.setFacebookAccountId(accountId);
            }

            // Perform social login to get authentication tokens
            String accessToken = authService.loginSocial(userLoginDTO);
            String userAgent = request.getHeader("User-Agent");
            UserAccount userDetail = authService.getUserDetailsFromToken(accessToken);

            // Generate refresh token
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetail);

            // Revoke previous refresh tokens
            tokenService.revokeAllUserRefreshTokens(userDetail);

            // Save tokens
            tokenService.addAccessToken(userDetail, accessToken, isMobileDevice(userAgent));
            tokenService.addRefreshToken(userDetail, refreshToken);

            // Set refresh token cookie
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                    .sameSite("Lax")  // Changed to Lax for local development, use Strict in production
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtTokenUtil.getRefreshExpirationMs()))
                    .build();
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // Convert roles to JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String rolesJson = objectMapper.writeValueAsString(
                userDetail.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList()
            );

            // Build redirect URL to Angular frontend with auth data (only access token, not refresh)
            String frontendCallbackUrl = "http://localhost:4200/auth/callback?" +
                "token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                "&id=" + userDetail.getId() +
                "&username=" + URLEncoder.encode(userDetail.getUsername(), StandardCharsets.UTF_8) +
                "&roles=" + URLEncoder.encode(rolesJson, StandardCharsets.UTF_8);

            // Redirect to Angular frontend
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendCallbackUrl))
                    .build();
                    
        } catch (Exception e) {
            // Redirect to frontend with error message
            String errorUrl = "http://localhost:4200/auth/callback?error=" + 
                URLEncoder.encode("Authentication failed: " + e.getMessage(), StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
    }

}
