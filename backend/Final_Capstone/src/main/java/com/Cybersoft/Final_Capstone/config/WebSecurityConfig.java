package com.Cybersoft.Final_Capstone.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.Cybersoft.Final_Capstone.filter.JwtTokenFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))  // Apply CORS configuration
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(customizer->
                        customizer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(requests -> {
                    requests
                            // Authentication endpoints - public
                            .requestMatchers("/auth/login", "/auth/signup", "/auth/refresh", "/auth/logout").permitAll()
                            .requestMatchers("/auth/social-login", "/auth/social/callback").permitAll()
                            .requestMatchers("/auth/forgot-password", "/auth/reset-password", "/auth/validate-reset-token").permitAll()
                            .requestMatchers("/auth/me").permitAll()

                            // Public endpoints - no authentication required
                            .requestMatchers("/properties/**", "/property/**", "/promotions/**", "/images/**", "/files/**").permitAll()
                            
                            // User profile endpoints - requires authentication
                            .requestMatchers("/users/**").authenticated()
                            
                            // Booking endpoints - requires authentication
                            .requestMatchers("/bookings/**").authenticated()
                            
                            // Host endpoints - requires HOST role
                            .requestMatchers("/host/**").hasRole("HOST")
                            .requestMatchers("/hosts/**").authenticated()
                            .requestMatchers("/locations/**").permitAll()
                            .requestMatchers("/cities/**").permitAll()
                            // All other requests require authentication
                            .anyRequest().authenticated();
                })
                .oauth2Login(Customizer.withDefaults());

        return http.build();
    }
}
