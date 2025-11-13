package com.Cybersoft.Final_Capstone.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();

        // Read from CorsProperties with null-safe fallbacks
        config.setAllowedOrigins(
            corsProperties.getAllowedOrigins() != null 
                ? corsProperties.getAllowedOrigins()
                : List.of("http://localhost:3000", "http://localhost:4200", "http://localhost:5173")
        );

        config.setAllowedMethods(
            corsProperties.getAllowedMethods() != null 
                ? corsProperties.getAllowedMethods()
                : List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        );

        config.setAllowedHeaders(
            corsProperties.getAllowedHeaders() != null 
                ? corsProperties.getAllowedHeaders()
                : List.of("Authorization", "Content-Type", "X-CSRF-Check", "X-Device-Id")
        );

        config.setExposedHeaders(
            corsProperties.getExposedHeaders() != null 
                ? corsProperties.getExposedHeaders()
                : List.of("X-Total-Count", "X-Device-Id")
        );

        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(corsProperties.getMaxAge() > 0 ? corsProperties.getMaxAge() : 3600L);

        // Apply to all endpoints
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
