package com.Cybersoft.Final_Capstone.filter;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.repository.TokenRepository;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.data.util.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // CRITICAL: Bypass ALL OPTIONS requests (CORS preflight) FIRST
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if this endpoint should bypass authentication
        if(isBypassToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"MISSING_TOKEN\",\"message\":\"Authorization header missing or invalid\"}");
                return;
            }

            final String token = authHeader.substring(7);

            // Check token in database first
            Token dbToken = tokenRepository.findByToken(token);

            if (dbToken == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Token not found\"}");
                return;
            }

            if (dbToken.isRevoked()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"TOKEN_REVOKED\",\"message\":\"Token has been revoked\"}");
                return;
            }

            if (dbToken.isExpired()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"Access token expired. Please call /auth/refresh.\"}");
                return;
            }

            // Parse and validate JWT
            String subject;
            try {
                subject = jwtTokenUtil.getSubject(token);
            } catch (ExpiredJwtException e) {
                // Mark as expired in DB
                dbToken.setExpired(true);
                tokenRepository.save(dbToken);

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"Access token expired. Please call /auth/refresh.\"}");
                return;
            }

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserAccount userDetails = (UserAccount) userDetailsService.loadUserByUsername(subject);

                if(jwtTokenUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    logger.debug("User authenticated: {}", userDetails.getUsername());
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Token validation failed\"}");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"Access token expired. Please call /auth/refresh.\"}");
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"AUTHENTICATION_ERROR\",\"message\":\"Authentication failed\"}");
        }
    }

    private boolean isBypassToken(@NonNull HttpServletRequest request) {
        final List<Pair<String, String>> bypassTokens = Arrays.asList(
                // Authentication endpoints
                Pair.of("/auth/login", "POST"),
                Pair.of("/auth/signup", "POST"),
                Pair.of("/auth/refresh", "POST"),
                Pair.of("/auth/forgot-password", "POST"),
                Pair.of("/auth/reset-password", "POST"),
                Pair.of("/auth/validate-reset-token", "GET"),

                // Public content endpoints
                Pair.of("/properties**", "GET"),
                Pair.of("/property**", "GET"),
                Pair.of("/promotions**", "GET"),
                Pair.of("/images**", "GET"),
                Pair.of("/files**", "GET"),

                // Social login endpoints
                Pair.of("/auth/social-login**", "GET"),
                Pair.of("/auth/social/callback**", "GET"),
                Pair.of("/oauth2/**", "GET")
        );

        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();

        logger.debug("Checking bypass for path: {} method: {}", requestPath, requestMethod);

        for (Pair<String, String> token : bypassTokens) {
            String path = token.getFirst();
            String method = token.getSecond();

            // Check if method matches
            if (!requestMethod.equalsIgnoreCase(method)) {
                continue;
            }

            // Check path - handle wildcards
            boolean pathMatches = false;
            if (path.contains("**")) {
                // Wildcard path - use startsWith with the prefix
                String prefix = path.substring(0, path.indexOf("**"));
                pathMatches = requestPath.startsWith(prefix);
            } else {
                // Exact path match
                pathMatches = requestPath.equals(path);
            }

            if (pathMatches) {
                logger.debug("Bypassing authentication for: {} {}", requestMethod, requestPath);
                return true;
            }
        }

        logger.debug("NOT bypassing authentication for: {} {}", requestMethod, requestPath);
        return false;
    }
}
