package com.Cybersoft.Final_Capstone.components;

import jakarta.servlet.FilterChain;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component("customCorsFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    // List of allowed origins
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:4200",
            "http://127.0.0.1:4200",
            "http://localhost:5501",
            "http://127.0.0.1:5501"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");

        // Check if the origin is in our allowed list
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);  // Use exact origin, NOT wildcard
            response.setHeader("Access-Control-Allow-Credentials", "true");  // Required for cookies
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "authorization, content-type, xsrf-token, x-requested-with");
        response.setHeader("Access-Control-Expose-Headers", "xsrf-token, set-cookie, authorization");

        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
