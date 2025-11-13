package com.Cybersoft.Final_Capstone.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ========================================
 * PUBLIC ENDPOINTS CONFIGURATION
 * ========================================
 *
 * Centralized management of all public endpoints that bypass JWT authentication.
 * These endpoints can be accessed without Authorization header.
 *
 * CATEGORIES:
 * 1. Authentication Endpoints
 * 2. OAuth/Social Login Endpoints
 * 3. Public Content Endpoints (Read-only)
 * 4. System Endpoints
 *
 * USAGE:
 * - JwtTokenFilter.isBypassToken() uses this to determine if endpoint requires auth
 * - Update this file when adding new public endpoints
 *
 * SECURITY NOTES:
 * - All POST endpoints for user actions (bookings, reviews) require authentication
 * - All admin endpoints (/admin/**) require authentication + ADMIN role
 * - All host endpoints (/host/**) require authentication + HOST role
 */
public final class PublicEndpoints {

    // ========================================
    // 1. AUTHENTICATION ENDPOINTS
    // ========================================

    /**
     * POST endpoints for authentication (PUBLIC - no JWT required)
     * X-Device-Id header required for login/signup (frontend must generate UUID v4)
     */
    public static final Set<String> AUTH_POST_ENDPOINTS = new HashSet<>(Arrays.asList(
            "/auth/login",              // Traditional login (username/email + password)
            "/auth/signup",             // User registration
            "/auth/refresh",            // ✅ Refresh access token (reads refresh_token cookie) - CHANGED TO POST
            "/auth/forgot-password",    // Request password reset email
            "/auth/reset-password",     // Reset password with token
            "/api/payment/webhook",     // ✅ PayOS webhook callback (no auth - verified by signature)
            "/payment/webhook"          // ✅ PayOS webhook callback (alternative path)

    ));

    /**
     * GET endpoints for authentication
     * These endpoints read cookies/tokens and perform validation
     */
    public static final Set<String> AUTH_GET_ENDPOINTS = new HashSet<>(Arrays.asList(
            "/auth/validate-reset-token",   // Validate password reset token
            "/auth/me",                     // Get current user info (optional - can be made protected)
            "/favicon.ico"                  // Browser favicon request (bypass authentication)
    ));

    // ========================================
    // 2. OAUTH/SOCIAL LOGIN ENDPOINTS
    // ========================================

    /**
     * OAuth and social login endpoints
     * Handles Google, Facebook, etc. login flows
     */
    public static final Set<String> OAUTH_ENDPOINTS = new HashSet<>(Arrays.asList(
            "/auth/social-login",       // Initiate OAuth flow (GET)
            "/auth/social/callback",    // OAuth callback handler (GET)
            "/oauth2/"                  // Spring Security OAuth2 internal endpoints (prefix match)
    ));

    // ========================================
    // 3. PUBLIC CONTENT ENDPOINTS (READ-ONLY)
    // ========================================

    /**
     * Public read-only endpoints for browsing content
     * Authentication NOT required, but some features may be limited
     */
    public static final Set<String> PUBLIC_CONTENT_PREFIXES = new HashSet<>(Arrays.asList(
            "/files",       // Access uploaded files (images, etc.)
            "/locations",   // Browse locations/cities
            "/cities"       // Browse cities
    ));

    /**
     * Specific public property endpoints (GET only)
     * Other /property/** endpoints require authentication
     */
    public static final Set<String> PUBLIC_PROPERTY_ENDPOINTS = new HashSet<>(Arrays.asList(
            "/property/filter",         // Search/filter properties
            "/property/search",         // Search properties (POST allowed in isPublicEndpoint)
            "/property/top7",           // Top 7 properties
            "/property/top4/type/",     // Top 4 by type (prefix match)
            // Note: /property/{id} (where id is number) is handled by regex in isPublicEndpoint()
            // Pattern: /property/\d+ (e.g., /property/123, /property/456)
            "/property/"                // Property detail by ID (handled separately by regex)
    ));

    // ========================================
    // 4. SYSTEM ENDPOINTS
    // ========================================

    /**
     * System and health check endpoints
     */
    public static final Set<String> SYSTEM_ENDPOINTS = new HashSet<>(Arrays.asList(
            "/actuator",    // Spring Boot Actuator (if exposed)
            "/health",      // Health check
            "/swagger-ui",  // Swagger UI (if enabled)
            "/api-docs"     // API documentation
    ));

    // ========================================
    // PROTECTED ENDPOINTS (NOT PUBLIC)
    // ========================================

    /**
     * These endpoints REQUIRE JWT authentication:
     * - /auth/logout (POST) - Sign out (requires JWT + refresh_token cookie)
     * - /auth/logout-all (POST) - Sign out from all devices (requires JWT)
     * - /bookings/** (POST, PUT, DELETE) - Create/manage bookings
     * - /reviews/** (POST, PUT, DELETE) - Create/manage reviews
     * - /users/profile (GET, PUT) - View/edit profile
     * - /auth/upload-avatar (POST) - Upload avatar
     * - /admin/** (ALL) - Admin panel (requires ADMIN role)
     * - /host/** (ALL) - Host management (requires HOST role)
     * - /images/** (ALL) - Image upload/management (requires HOST role)
     * 
     * PUBLIC ENDPOINTS (bypass JWT auth):
     * - /auth/login, /auth/signup, /auth/refresh, /auth/forgot-password, /auth/reset-password
     * - /payment/webhook (POST) - PayOS webhook (verified by signature, not JWT)
     * - /api/payment/webhook (POST) - PayOS webhook (alternative path)
     * - /properties, /locations, /cities (GET) - Browse public content
     */

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Check if endpoint is public (bypasses authentication)
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param path   Request path (/auth/login, /properties, etc.)
     * @return true if endpoint is public, false if requires authentication
     */
    public static boolean isPublicEndpoint(String method, String path) {
        if (method == null || path == null) {
            return false;
        }

        method = method.toUpperCase();
        path = path.toLowerCase();

        // 1. Check exact POST matches
        if ("POST".equals(method) && AUTH_POST_ENDPOINTS.contains(path)) {
            return true;
        }

        // 2. Check exact GET matches
        if ("GET".equals(method) && AUTH_GET_ENDPOINTS.contains(path)) {
            return true;
        }

        // 3. Check OAuth endpoints (any method)
        for (String endpoint : OAUTH_ENDPOINTS) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }

        // 4. Check specific public property endpoints
        if ("GET".equals(method)) {
            // Exact matches
            for (String endpoint : PUBLIC_PROPERTY_ENDPOINTS) {
                if (path.equals(endpoint)) {
                    return true;
                }
                // Prefix match for /property/top4/type/*
                if (endpoint.endsWith("/") && path.startsWith(endpoint)) {
                    return true;
                }
            }

            // Special: /property/{id} where id is a number (property detail)
            if (path.matches("/property/\\d+")) {
                return true;
            }
        }

        // 5. POST /property/search is also public
        if ("POST".equals(method) && path.equals("/property/search")) {
            return true;
        }

        // 6. Check public content prefixes (GET only for safety)
        if ("GET".equals(method)) {
            for (String prefix : PUBLIC_CONTENT_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }

            // System endpoints
            for (String endpoint : SYSTEM_ENDPOINTS) {
                if (path.startsWith(endpoint)) {
                    return true;
                }
            }
        }

        // Default: require authentication
        return false;
    }

    /**
     * Get all public endpoints as documentation
     *
     * @return Set of all public endpoint patterns
     */
    public static Set<String> getAllPublicEndpoints() {
        Set<String> allEndpoints = new HashSet<>();
        allEndpoints.addAll(AUTH_POST_ENDPOINTS);
        allEndpoints.addAll(AUTH_GET_ENDPOINTS);
        allEndpoints.addAll(OAUTH_ENDPOINTS);
        allEndpoints.addAll(PUBLIC_CONTENT_PREFIXES);
        allEndpoints.addAll(SYSTEM_ENDPOINTS);
        return allEndpoints;
    }

    // Prevent instantiation
    private PublicEndpoints() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}







