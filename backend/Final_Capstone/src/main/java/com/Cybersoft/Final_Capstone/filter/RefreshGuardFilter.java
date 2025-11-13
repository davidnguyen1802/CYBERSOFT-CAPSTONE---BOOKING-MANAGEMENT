package com.Cybersoft.Final_Capstone.filter;

import com.Cybersoft.Final_Capstone.config.CorsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * NOTE: Cross-site protection cho /auth/refresh
 * Bảo vệ khỏi CSRF attack mặc dù csrf().disable()
 *
 * Yêu cầu:
 * - POST method only
 * - Origin/Referer phải thuộc whitelist (hoặc null trong dev mode cho Postman)
 * - Header X-CSRF-Check: 1 (có thể bỏ qua trong dev mode)
 * - Rate limiting cơ bản (5 requests/minute)
 */
@Slf4j
@RequiredArgsConstructor
public class RefreshGuardFilter extends OncePerRequestFilter {

    private final CorsProperties corsProperties;

    @Value("${rate-limit.refresh-endpoint.max-requests:5}")
    private int maxRequests;

    @Value("${rate-limit.refresh-endpoint.window-seconds:60}")
    private long windowSeconds;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // NOTE: Simple in-memory rate limiter
    // TODO: Production nên dùng Redis cho distributed rate limiting
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // NOTE: Chỉ áp dụng cho /auth/refresh
        if (!requestURI.equals("/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("RefreshGuardFilter: Protecting /auth/refresh request");

        // 1. CHECK: POST method only (refresh uses POST for better security)
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            log.warn("RefreshGuardFilter: Method not allowed: {}", request.getMethod());
            sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                     "Method not allowed. Use POST.");
            return;
        }

        // 2. CHECK: Origin hoặc Referer phải thuộc whitelist
        // NOTE: Allow Postman/dev tools khi không có Origin (dev mode only)
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        boolean isDevelopment = "dev".equalsIgnoreCase(activeProfile) || activeProfile == null;

        if (!isAllowedOrigin(origin) && !isAllowedReferer(referer)) {
            // Allow requests without Origin/Referer in development (e.g., Postman)
            if (!isDevelopment || (origin != null && !origin.isEmpty())) {
                log.warn("RefreshGuardFilter: Forbidden origin/referer. Origin: {}, Referer: {}",
                         origin, referer);
                sendError(response, HttpServletResponse.SC_FORBIDDEN,
                         "Forbidden: Invalid origin");
                return;
            }
            log.debug("RefreshGuardFilter: Dev mode - allowing request without Origin header (Postman)");
        }

        // 3. CHECK: Custom header X-CSRF-Check: 1
        // NOTE: Relax in dev mode for Postman testing
        String csrfCheck = request.getHeader("X-CSRF-Check");
        if (!"1".equals(csrfCheck)) {
            if (!isDevelopment) {
                log.warn("RefreshGuardFilter: Missing or invalid X-CSRF-Check header");
                sendError(response, HttpServletResponse.SC_FORBIDDEN,
                         "Forbidden: Missing CSRF check header");
                return;
            }
            log.debug("RefreshGuardFilter: Dev mode - allowing request without X-CSRF-Check header");
        }

        // 4. CHECK: Rate limiting
        String clientKey = getClientKey(request);
        if (!checkRateLimit(clientKey)) {
            log.warn("RefreshGuardFilter: Rate limit exceeded for client: {}", clientKey);
            sendError(response, 429, // Too Many Requests
                     "Too many requests. Please try again later.");
            return;
        }

        // NOTE: All checks passed → allow request
        log.debug("RefreshGuardFilter: All checks passed, proceeding");
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isEmpty()) return false;
        var allowedOrigins = corsProperties.getAllowedOrigins();
        if (allowedOrigins == null) return false;
        return allowedOrigins.stream()
                .anyMatch(allowed -> origin.trim().startsWith(allowed.trim()));
    }

    private boolean isAllowedReferer(String referer) {
        if (referer == null || referer.isEmpty()) return false;
        var allowedOrigins = corsProperties.getAllowedOrigins();
        if (allowedOrigins == null) return false;
        return allowedOrigins.stream()
                .anyMatch(allowed -> referer.trim().startsWith(allowed.trim()));
    }

    private String getClientKey(HttpServletRequest request) {
        // NOTE: Combine IP + User-Agent để tạo unique key
        String ip = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        return ip + "_" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }

    private String getClientIP(HttpServletRequest request) {
        // NOTE: Handle proxy/load balancer
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Get first IP if multiple (proxy chain)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean checkRateLimit(String clientKey) {
        long now = System.currentTimeMillis();

        rateLimitMap.compute(clientKey, (key, entry) -> {
            if (entry == null || (now - entry.windowStart) > TimeUnit.SECONDS.toMillis(windowSeconds)) {
                // New window
                return new RateLimitEntry(now, 1);
            } else {
                // Within window
                entry.count++;
                return entry;
            }
        });

        // NOTE: Cleanup old entries (prevent memory leak)
        rateLimitMap.entrySet().removeIf(e ->
            (now - e.getValue().windowStart) > TimeUnit.SECONDS.toMillis(windowSeconds * 2)
        );

        RateLimitEntry entry = rateLimitMap.get(clientKey);
        return entry != null && entry.count <= maxRequests;
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            String.format("{\"error\":\"%s\",\"message\":\"%s\"}",
                         getErrorCode(status), message)
        );
    }

    private String getErrorCode(int status) {
        return switch (status) {
            case 403 -> "FORBIDDEN";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 429 -> "TOO_MANY_REQUESTS";
            default -> "ERROR";
        };
    }

    private static class RateLimitEntry {
        long windowStart;
        int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
