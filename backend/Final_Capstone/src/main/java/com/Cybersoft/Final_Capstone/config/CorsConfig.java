package com.Cybersoft.Final_Capstone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.security.allowed-origins:http://localhost:5501,http://127.0.0.1:5501,http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Configuration for /auth/refresh endpoint (strict)
        CorsConfiguration refreshConfig = new CorsConfiguration();

        // Parse allowed origins for refresh endpoint
        if (allowedOrigins.length == 1 && allowedOrigins[0].contains(",")) {
            String[] origins = allowedOrigins[0].split(",");
            refreshConfig.setAllowedOrigins(Arrays.asList(origins));
        } else {
            refreshConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
        }

        refreshConfig.setAllowedMethods(List.of("POST", "OPTIONS"));
        refreshConfig.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "Cookie",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        refreshConfig.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        refreshConfig.setAllowCredentials(true);
        refreshConfig.setMaxAge(3600L);

        source.registerCorsConfiguration("/auth/refresh", refreshConfig);

        // General configuration for all other endpoints
        CorsConfiguration generalConfig = new CorsConfiguration();

        // Parse allowed origins for general endpoints
        if (allowedOrigins.length == 1 && allowedOrigins[0].contains(",")) {
            String[] origins = allowedOrigins[0].split(",");
            generalConfig.setAllowedOrigins(Arrays.asList(origins));
        } else {
            generalConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
        }

        generalConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        generalConfig.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "Cookie",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        generalConfig.setExposedHeaders(List.of("Authorization", "Content-Type", "Set-Cookie"));
        generalConfig.setAllowCredentials(true);
        generalConfig.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", generalConfig);

        return source;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:4200",
                        "http://127.0.0.1:4200",
                        "http://localhost:5501",
                        "http://127.0.0.1:5501",
                        "http://localhost:3000",
                        "http://localhost:5173"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization", "Content-Type", "Set-Cookie")
                .maxAge(3600);
    }
}

