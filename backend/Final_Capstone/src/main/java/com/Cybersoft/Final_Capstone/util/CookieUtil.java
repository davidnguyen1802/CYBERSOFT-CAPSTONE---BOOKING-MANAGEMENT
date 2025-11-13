package com.Cybersoft.Final_Capstone.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * NOTE: Utility để quản lý refresh token cookie
 * HttpOnly=true, Secure=true, SameSite=Lax, Path=/auth
 */
@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/auth"; // NOTE: Restrict cookie path
    private static final boolean SECURE = false; // NOTE: Set true for HTTPS in production
    private static final boolean HTTP_ONLY = true;

    /**
     * NOTE: Set refresh token cookie
     * rememberMe = true → Max-Age = 30 days (persistent)
     * rememberMe = false → Session cookie (Max-Age = -1)
     */
    public void setRefreshTokenCookie(HttpServletResponse response,
                                      String refreshToken,
                                      boolean rememberMe,
                                      long rememberDays) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath(COOKIE_PATH);

        // NOTE: SameSite=Lax - balance security vs usability
        // Lax allows top-level navigation (redirect after login)
        // Strict would block cross-site requests completely
        cookie.setAttribute("SameSite", "Lax");

        if (rememberMe) {
            cookie.setMaxAge((int) (rememberDays * 24 * 3600)); // Persistent cookie
        } else {
            cookie.setMaxAge(-1); // Session cookie - expires when browser closes
        }

        response.addCookie(cookie);
    }

    /**
     * NOTE: Get refresh token từ cookie
     */
    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * NOTE: Delete refresh token cookie (logout)
     * Set Max-Age=0 để xóa ngay lập tức
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0); // Delete immediately

        response.addCookie(cookie);
    }
}

