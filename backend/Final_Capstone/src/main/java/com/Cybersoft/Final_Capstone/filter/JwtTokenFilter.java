package com.Cybersoft.Final_Capstone.filter;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.constants.PublicEndpoints;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * NOTE: JWT Authentication Filter
 * - Access Token: STATELESS validation (chỉ verify JWT signature, không check DB)
 * - Không track/revoke Access Token trong DB
 * - Đăng ký thủ công trong WebSecurityConfig (không dùng @Component)
 */
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    // NOTE: Removed TokenRepository - không cần check DB cho Access Token

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();

        logger.info("========================================");
        logger.info("JwtTokenFilter - Path: {}, Method: {}", requestPath, requestMethod);

        // CRITICAL: Bypass ALL OPTIONS requests (CORS preflight) FIRST
        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            logger.info("✅ Bypassing OPTIONS request");
            filterChain.doFilter(request, response);
            return;
        }

        // Check if this endpoint should bypass authentication
        if(isBypassToken(request)) {
            logger.info("✅ BYPASSED - Endpoint does not require authentication");
            logger.info("========================================");
            filterChain.doFilter(request, response);
            return;
        }

        logger.info("🔒 Endpoint REQUIRES authentication");

        try {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("❌ Missing or invalid Authorization header");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"MISSING_TOKEN\",\"message\":\"Authorization header missing or invalid\"}");
                return;
            }

            final String token = authHeader.substring(7);

            // NOTE: STATELESS validation - chỉ verify JWT, KHÔNG check DB
            String subject;
            try {
                subject = jwtTokenUtil.getSubject(token);
            } catch (ExpiredJwtException e) {
                logger.warn("❌ JWT token expired");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"Access token expired. Please call /auth/refresh.\"}");
                return;
            }

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserAccount userDetails = (UserAccount) userDetailsService.loadUserByUsername(subject);

                // NOTE: validateToken chỉ check JWT signature, expiration, user status
                // KHÔNG check DB (stateless)
                if(jwtTokenUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    logger.info("✅ User authenticated (stateless): {}", userDetails.getUsername());
                } else {
                    logger.warn("❌ Token validation failed");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Token validation failed\"}");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            logger.warn("❌ JWT expired exception", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"Access token expired. Please call /auth/refresh.\"}");
        } catch (Exception e) {
            logger.error("❌ Authentication error", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"AUTHENTICATION_ERROR\",\"message\":\"Authentication failed: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if endpoint bypasses JWT authentication
     * Uses centralized PublicEndpoints configuration
     */
    private boolean isBypassToken(@NonNull HttpServletRequest request) {
        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();

        logger.info("🔍 Checking bypass - Path: '{}', Method: '{}'", requestPath, requestMethod);

        // Use centralized PublicEndpoints configuration
        boolean isPublic = PublicEndpoints.isPublicEndpoint(requestMethod, requestPath);

        if (isPublic) {
            logger.info("✅ MATCH: Public endpoint - {} {}", requestMethod, requestPath);
        } else {
            logger.warn("❌ NO MATCH - Requires authentication: {} {}", requestMethod, requestPath);
        }

        return isPublic;
    }
}
