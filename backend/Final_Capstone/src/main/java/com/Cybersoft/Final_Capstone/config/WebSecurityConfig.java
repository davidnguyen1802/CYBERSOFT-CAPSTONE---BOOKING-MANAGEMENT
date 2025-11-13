package com.Cybersoft.Final_Capstone.config;

import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.filter.JwtTokenFilter;
import com.Cybersoft.Final_Capstone.filter.RefreshGuardFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final CorsConfigurationSource corsConfigurationSource;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final CorsProperties corsProperties;

    /**
     * Create JwtTokenFilter bean for manual registration in SecurityFilterChain
     */
    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter(userDetailsService, jwtTokenUtil);
    }

    /**
     * Create RefreshGuardFilter bean for manual registration in SecurityFilterChain
     */
    @Bean
    public RefreshGuardFilter refreshGuardFilter() {
        return new RefreshGuardFilter(corsProperties);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                // ========================================
                // 1. CORS Configuration
                // ========================================
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // ========================================
                // 2. CSRF - Disabled for stateless JWT
                // ========================================
                .csrf(AbstractHttpConfigurer::disable)
                
                // ========================================
                // 3. Session Management - STATELESS
                // ========================================
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // ========================================
                // 4. Exception Handling
                // ========================================
                .exceptionHandling(customizer ->
                    customizer.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                
                // ========================================
                // 5. Authorization Rules
                // ========================================
                .authorizeHttpRequests(requests -> {
                    requests
                            // Allow CORS preflight requests (OPTIONS)
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            
                            // Authentication endpoints - public
                            .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/signup").permitAll()
                            .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()  // ✅ POST method for refresh
                            .requestMatchers(HttpMethod.POST, "/auth/forgot-password", "/auth/reset-password").permitAll()
                            .requestMatchers(HttpMethod.GET, "/auth/validate-reset-token").permitAll()
                            .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()
                            
                            // OAuth/Social login endpoints - public
                            .requestMatchers("/auth/social-login", "/auth/social/callback").permitAll()
                            .requestMatchers("/oauth2/**").permitAll()
                            
                            // Logout endpoint - requires authentication
                            .requestMatchers("/auth/logout").authenticated()
                            
                            // Public content endpoints (GET only)
                            .requestMatchers(HttpMethod.GET, "/locations/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/cities/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/files/**").permitAll()
                            
                            // Property endpoints - specific public endpoints only
                            .requestMatchers(HttpMethod.GET, "/property/filter").permitAll()  // Search/filter
                            .requestMatchers(HttpMethod.POST, "/property/search").permitAll()  // Search POST
                            .requestMatchers(HttpMethod.GET, "/property/top7").permitAll()  // Top 7 properties
                            .requestMatchers(HttpMethod.GET, "/property/top4/type/*").permitAll()  // Top 4 by type
                            .requestMatchers(HttpMethod.GET, "/property/{id:[0-9]+}").permitAll()  // Get property by ID (number only)
                            // All other /property/** endpoints require authentication (managed by @PreAuthorize)

                            // Image endpoints - requires HOST role
                            .requestMatchers("/images/**").hasRole("HOST")
                            
                            // Amenity endpoints - requires HOST or ADMIN role
                            .requestMatchers("/amenities/**").hasAnyRole("ADMIN", "HOST")
                            
                            // Facility endpoints - requires HOST or ADMIN role
                            .requestMatchers("/facilities/**").hasAnyRole("ADMIN", "HOST")
                            
            // PayOS calls this endpoint without authentication, and we allow it for security reasons
                .requestMatchers("/api/payment/webhook").permitAll()
                .requestMatchers("/payment/webhook").permitAll()

                // Payment endpoints - other payment endpoints need authentication
                .requestMatchers("/payment/success", "/payment/cancel").permitAll()
                .requestMatchers("/payment/**", "/api/payment/**").authenticated()

                            // User profile endpoints - requires authentication
                            .requestMatchers("/users/**").authenticated()
                            
                            // Booking endpoints - requires authentication
                            .requestMatchers("/bookings/**").authenticated()
                            
                            // Host endpoints - requires HOST role
                            .requestMatchers("/host/**").hasRole("HOST")
                            .requestMatchers("/hosts/**").authenticated()

                            // All other requests require authentication
                            .anyRequest().authenticated();
                })
                
                // ========================================
                // 6. OAuth2 Login (for social login)
                // ========================================
                .oauth2Login(Customizer.withDefaults());

                // ========================================
                // 7. Add Filters (manual registration)
                // ========================================
        // RefreshGuardFilter runs FIRST to protect /auth/refresh
        http.addFilterBefore(refreshGuardFilter(), UsernamePasswordAuthenticationFilter.class);
        
        // JwtTokenFilter runs AFTER RefreshGuardFilter for token validation
        http.addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
