package com.Cybersoft.Final_Capstone.config;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

//import com.Cybersoft.Final_Capstone.filter.AuthorizationFilter;
//import com.Cybersoft.Final_Capstone.service.CustomOAuth2UserService;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Autowired
//    private AuthorizationFilter authorizationFilter;
//
//    @Autowired
//    private CustomOAuth2UserService customOAuth2UserService;
//
//    @Autowired
//    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        return http
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(requests -> {
//                    // Public auth endpoints - no authentication required
//                    requests.requestMatchers("/auth/signup", "/auth/signin", "/auth/health").permitAll();
//                    requests.requestMatchers("/auth/forgot-password", "/auth/reset-password", "/auth/validate-reset-token").permitAll();
//
//                    // OAuth2 endpoints - allow access
//                    requests.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
//
//                    // Protected auth endpoints - require authentication
//                    requests.requestMatchers("/auth/upload-avatar", "/auth/signout", "/auth/me").authenticated();
//
//                    // Payment webhook
//                    requests.requestMatchers("/api/payment/momo/ipn-handler").permitAll();
//
//                    // Public content
//                    requests.requestMatchers("/properties/**").permitAll();
//                    requests.requestMatchers("/property/**").permitAll();
//                    requests.requestMatchers("/promotions/**").permitAll();
//                    requests.requestMatchers("/images/**").permitAll();
//                    requests.requestMatchers("/files/**").permitAll();
//
//                    // Protected endpoints - require authentication
//                    requests.requestMatchers("/bookings/**").authenticated();
//                    requests.requestMatchers("/hosts/**").authenticated();
//
//                    // All other endpoints require authentication
//                    requests.anyRequest().authenticated();
//                })
//                // Configure OAuth2 login
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)
//                        )
//                        .successHandler(oauth2AuthenticationSuccessHandler)
//                        // Disable automatic redirect to OAuth2 login
//                        // Only allow OAuth2 when explicitly initiated
//                        .authorizationEndpoint(authorization -> authorization
//                                .baseUri("/oauth2/authorization")
//                        )
//                )
//                // Configure exception handling to return 401 instead of redirecting
//                .exceptionHandling(exception -> exception
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            // Return 401 Unauthorized for API requests
//                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                            response.setContentType("application/json");
//                            response.getWriter().write("{\"statusCode\":401,\"message\":\"Unauthorized - Authentication required\",\"data\":null}");
//                        })
//                )
//                .addFilterBefore(authorizationFilter, UsernamePasswordAuthenticationFilter.class)
//                .build();
//    }
//}

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserAccountRepository userAccountRepository;
    private final WebClient userInfoClient;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return subject -> {
            // Attempt to find user by phone number
            Optional<UserAccount> userByPhone = userAccountRepository.findByPhone(subject);
            if (userByPhone.isPresent()) {
                return userByPhone.get(); // Return UserDetails if found
            }

            // If user not found by phone number, attempt to find by email
            Optional<UserAccount> userByEmail = userAccountRepository.findByEmail(subject);
            if (userByEmail.isPresent()) {
                return userByEmail.get(); // Return UserDetails if found
            }

            // If user not found by phone or email, attempt to find by username
            Optional<UserAccount> userByUsername = userAccountRepository.findByUsername(subject);
            if (userByUsername.isPresent()) {
                return userByUsername.get(); // Return UserDetails if found
            }

            // If user not found by phone, email, or username, throw exception
            throw new UsernameNotFoundException("User not found with subject: " + subject);
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public OpaqueTokenIntrospector introspector() {
        return new GoogleOpaqueTokenIntrospector(userInfoClient);
    }
}