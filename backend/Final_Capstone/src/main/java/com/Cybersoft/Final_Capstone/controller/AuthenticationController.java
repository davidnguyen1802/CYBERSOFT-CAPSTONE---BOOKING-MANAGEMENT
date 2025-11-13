package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.payload.request.SignInRequest;
import com.Cybersoft.Final_Capstone.payload.request.SignUpRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.RefreshTokenResponse;
import com.Cybersoft.Final_Capstone.payload.response.ResponseObject;
import com.Cybersoft.Final_Capstone.service.AuthService;
import com.Cybersoft.Final_Capstone.service.AuthenticationService;
import com.Cybersoft.Final_Capstone.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    private final JwtTokenUtil jwtTokenUtil;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthenticationController.class);

    @PostMapping("/login")
    public ResponseEntity<ResponseObject> login(
            @Valid @RequestBody SignInRequest signInRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        // ========== STEP 1: Validate X-Device-Id header (REQUIRED) ==========
        String deviceId = extractDeviceId(request);
        if (deviceId == null) {
            logger.warn("⚠️ [LOGIN] Missing or invalid X-Device-Id header");
            return ResponseEntity.status(428).body(
                    ResponseObject.builder()
                            .message("X-Device-Id header is required. Generate UUID v4 on client.")
                            .status(HttpStatus.valueOf(428))
                            .build()
            );
        }

        // ========== STEP 2: Authenticate user ==========
        String accessToken = authService.login(signInRequest);
        UserAccount userDetail = authService.getUserDetailsFromToken(accessToken);
        boolean rememberMe = signInRequest.getRememberMe() != null ? signInRequest.getRememberMe() : false;

        // ========== STEP 3: Generate and store refresh token ==========
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetail, rememberMe, null);
        tokenService.addRefreshToken(userDetail, refreshToken, rememberMe, request);

        // ========== STEP 4: Set refresh token cookie ==========
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                .sameSite("Lax")
                .path("/");

        if (rememberMe) {
            cookieBuilder.maxAge(Duration.ofMillis(jwtTokenUtil.getRefreshExpirationMs(true)));
        }

        ResponseCookie refreshCookie = cookieBuilder.build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // ========== STEP 5: Build response (return access token directly as string) ==========
        return ResponseEntity.ok().body(
                ResponseObject.builder()
                        .message("Login successfully")
                        .data(accessToken)  // Return access token directly (same as signup)
                        .status(HttpStatus.OK)
                        .build()
        );
    }
    
    /**
     * Extract and validate X-Device-Id header
     * Returns null if missing or invalid format
     */
    private String extractDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        
        if (deviceId == null || deviceId.trim().isEmpty() || "Unknown".equalsIgnoreCase(deviceId)) {
            return null;
        }
        
        // Validate UUID v4 format (strict)
        if (!deviceId.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) {
            logger.warn("⚠️ Invalid X-Device-Id format: {}", deviceId);
            return null;
        }
        
        return deviceId.trim();
    }

    /**
     * Refresh Token endpoint
     * POST /auth/refresh
     *
     * Reads refresh token from HttpOnly cookie, validates it, and issues new tokens
     * Implements token rotation for security
     *
     * NEW: Requires X-Device-Id header (strict validation)
     *
     * Status Codes:
     * - 200 OK: Token refreshed successfully (returns new access token only)
     * - 400 BAD_REQUEST: X-Device-Id header missing or invalid UUID v4
     * - 449 RETRY_WITH: Refresh token missing (client didn't send cookie)
     * - 498 INVALID_TOKEN: Invalid JWT signature or token not found
     * - 499 TOKEN_REVOKED: Token revoked (logout or security issue)
     * - 419 AUTHENTICATION_TIMEOUT: Token expired (need to re-login)
     * - 503 SERVICE_UNAVAILABLE: Temporary server error
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            // Case 1: Missing refresh token → 449 RETRY_WITH
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(449).body(
                        ResponseObject.builder()
                                .message("Token missing")
                                .status(HttpStatus.valueOf(449))
                                .build()
                );
            }
            
            // Case 2: Validate X-Device-Id header (REQUIRED for refresh)
            String deviceId = extractDeviceId(request);
            if (deviceId == null) {
                logger.warn("⚠️ [REFRESH] Missing or invalid X-Device-Id header");
                return ResponseEntity.status(428).body(
                        ResponseObject.builder()
                                .message("X-Device-Id header is required. Generate UUID v4 on client.")
                                .status(HttpStatus.valueOf(428))
                                .build()
                );
            }

            // Refresh and rotate tokens (pass request for device metadata update)
            RefreshTokenResponse refreshResponse = tokenService.refreshToken(refreshToken, request);
            UserAccount user = refreshResponse.getTokenEntity().getUser();
            boolean rememberMe = refreshResponse.getTokenEntity().isRememberMe();

            // Generate new access token (stateless - không lưu DB)
            String newAccessToken = jwtTokenUtil.generateAccessToken(user);

            // Set new refresh token cookie with rememberMe-aware expiry
            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshResponse.getJwtToken())
                    .httpOnly(true)
                    .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                    .sameSite("Lax")  // Changed to Lax for local development, use Strict in production
                    .path("/");

            // Only set maxAge if rememberMe is true (persistent cookie)
            // If rememberMe is false, omit maxAge to make it a session cookie
            if (rememberMe) {
                cookieBuilder.maxAge(Duration.ofMillis(jwtTokenUtil.getRefreshExpirationMs(true)));
            }

            ResponseCookie newRefreshCookie = cookieBuilder.build();
            response.addHeader("Set-Cookie", newRefreshCookie.toString());

            // Return only access token → 200 OK
            return ResponseEntity.ok().body(
                    ResponseObject.builder()
                            .message("Token refreshed")
                            .data(newAccessToken)
                            .status(HttpStatus.OK)
                            .build()
            );

        } catch (com.Cybersoft.Final_Capstone.exception.ExpiredTokenException e) {
            // Case 2: Token expired → 419 AUTHENTICATION_TIMEOUT
            if (e.getMessage().contains("expired")) {
                return ResponseEntity.status(419).body(
                        ResponseObject.builder()
                                .message("Token expired")
                                .status(HttpStatus.valueOf(419))
                                .build()
                );
            }

            // Case 3: Token revoked (logout/security) → 499 TOKEN_REVOKED
            if (e.getMessage().contains("revoked") || e.getMessage().contains("reuse attack")) {
                return ResponseEntity.status(499).body(
                        ResponseObject.builder()
                                .message("Token revoked")
                                .status(HttpStatus.valueOf(499))
                                .build()
                );
            }

            // Case 4: Invalid signature or other JWT errors → 498 INVALID_TOKEN
            return ResponseEntity.status(498).body(
                    ResponseObject.builder()
                            .message("Invalid token")
                            .status(HttpStatus.valueOf(498))
                            .build()
            );

        } catch (com.Cybersoft.Final_Capstone.exception.DataNotFoundException e) {
            // Case 5: Token not found in DB → 498 INVALID_TOKEN
            return ResponseEntity.status(498).body(
                    ResponseObject.builder()
                            .message("Token not found")
                            .status(HttpStatus.valueOf(498))
                            .build()
            );

        } catch (Exception e) {
            // Case 6: Unknown error → 503 SERVICE_UNAVAILABLE
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ResponseObject.builder()
                            .message("Service temporarily unavailable")
                            .status(HttpStatus.SERVICE_UNAVAILABLE)
                            .build()
            );
        }
    }

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signUp(
            @Valid @ModelAttribute SignUpRequest signUpRequest,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        // ========== STEP 1: Validate X-Device-Id header (REQUIRED) ==========
        String deviceId = extractDeviceId(request);
        if (deviceId == null) {
            logger.warn("⚠️ [SIGNUP] Missing or invalid X-Device-Id header");
            return ResponseEntity.status(428).body(
                    ResponseObject.builder()
                            .message("X-Device-Id header is required. Generate UUID v4 on client.")
                            .status(HttpStatus.valueOf(428))
                            .build()
            );
        }

        // ========== STEP 2: Create user account ==========
        UserAccount newUser = authService.signUp(signUpRequest, avatar);

        // Get rememberMe flag from request (default false if null)
        boolean rememberMe = signUpRequest.getRememberMe() != null ? signUpRequest.getRememberMe() : false;

        // ========== STEP 3: Generate tokens with rememberMe flag ==========
        String accessToken = jwtTokenUtil.generateAccessToken(newUser);
        String refreshToken = jwtTokenUtil.generateRefreshToken(newUser, rememberMe, null);

        // NOTE: Access Token is stateless - KHÔNG lưu DB
        // Chỉ lưu Refresh Token with device tracking
        tokenService.addRefreshToken(newUser, refreshToken, rememberMe, request);

        // ========== STEP 4: Set refresh token cookie ==========
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                .sameSite("Lax")  // Changed to Lax for local development, use Strict in production
                .path("/");

        // Only set maxAge if rememberMe is true (persistent cookie)
        // If rememberMe is false, omit maxAge to make it a session cookie
        if (rememberMe) {
            cookieBuilder.maxAge(Duration.ofMillis(jwtTokenUtil.getRefreshExpirationMs(true)));
        }

        ResponseCookie refreshCookie = cookieBuilder.build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // ========== STEP 5: Return only access token ==========
        return ResponseEntity.ok().body(
                ResponseObject.builder()
                        .message("Sign up successfully")
                        .data(accessToken)
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
//    @GetMapping("/me")
//    public ResponseEntity<?> getCurrentUser(
//            @RequestHeader(name = "Authorization", required = false) String authHeader
//    ) {
//        try {
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
//                        ResponseObject.builder()
//                                .message("No authentication token found")
//                                .status(HttpStatus.UNAUTHORIZED)
//                                .build()
//                );
//            }
//
//            String token = authHeader.substring(7);
//
//            // Validate token
//            if (jwtTokenUtil.isTokenExpired(token)) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
//                        ResponseObject.builder()
//                                .message("Access token expired. Please call /auth/refresh.")
//                                .status(HttpStatus.UNAUTHORIZED)
//                                .build()
//                );
//            }
//
//            Integer userId = jwtTokenUtil.getUserId(token);
//
//            if (userId == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
//                        ResponseObject.builder()
//                                .message("Invalid token")
//                                .status(HttpStatus.UNAUTHORIZED)
//                                .build()
//                );
//            }
//
//            return ResponseEntity.ok().body(
//                    ResponseObject.builder()
//                            .message("User ID retrieved successfully")
//                            .data(userId)
//                            .status(HttpStatus.OK)
//                            .build()
//            );
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
//                    ResponseObject.builder()
//                            .message("Invalid or expired token")
//                            .status(HttpStatus.UNAUTHORIZED)
//                            .build()
//            );
//        }
//    }

    /**
     * Sign out endpoint
     * POST /auth/logout
     *
     * Revokes refresh token for the current device and clears the cookie
     * 
     * ⭐ IMPORTANT CHANGES:
     * - Now requires authentication (Authorization header)
     * - Accepts X-Device-Id header for accurate device tracking
     * - Revokes token for specific device only
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseObject> signOut(
            @AuthenticationPrincipal UserAccount user,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @RequestHeader(name = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // Validate user is authenticated
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ResponseObject.builder()
                                .message("Authentication required")
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
            }

            if (refreshToken != null && !refreshToken.isEmpty()) {
                // Revoke refresh token in database
                // The token service will use device_id if provided, otherwise User-Agent
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
        
        // ========== Validate X-Device-Id header (REQUIRED) ==========
        String deviceId = extractDeviceId(request);
        if (deviceId == null) {
            logger.warn("⚠️ [SOCIAL_LOGIN] Missing or invalid X-Device-Id header");
            String errorUrl = "http://localhost:4200/auth/callback?error=" + 
                URLEncoder.encode("X-Device-Id header is required. Generate UUID v4 on client.", StandardCharsets.UTF_8) +
                "&error_code=428";
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
            UserAccount userDetail = authService.getUserDetailsFromToken(accessToken);

            // Generate refresh token (no rememberMe for social login - default to false)
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetail, false, null);

            // NOTE: REMOVED revokeAllUserRefreshTokens() - Same bug as regular login!
            // The 3-device limit logic in addRefreshToken() handles per-device token management

            // NOTE: Access Token is stateless - KHÔNG lưu DB
            // Chỉ lưu Refresh Token with device tracking
            tokenService.addRefreshToken(userDetail, refreshToken, false, request);

            // Set refresh token cookie (session cookie - no maxAge since rememberMe = false)
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(false)  // Set to false for local development (http://), true in production with HTTPS
                    .sameSite("Lax")  // Changed to Lax for local development, use Strict in production
                    .path("/")
                    // No maxAge → session cookie for social login
                    .build();
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // Get single role (user has only one role)
            String role = userDetail.getRole().getName();

            // Build redirect URL to Angular frontend with auth data (only access token, not refresh)
            String frontendCallbackUrl = "http://localhost:4200/auth/callback?" +
                "token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                "&id=" + userDetail.getId() +
                "&username=" + URLEncoder.encode(userDetail.getUsername(), StandardCharsets.UTF_8) +
                "&role=" + URLEncoder.encode(role, StandardCharsets.UTF_8); // Single role, not roles array

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
