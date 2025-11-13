package com.Cybersoft.Final_Capstone.config;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserAccountRepository userAccountRepository;
    private final WebClient userInfoClient;

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return subject -> {
            logger.info("UserDetailsService: Loading user with subject={}", subject);

            // 1) Priority: Try to parse subject as userId (Integer) - NEW TOKENS
            if (subject != null && !subject.isEmpty()) {
                try {
                    Integer userId = Integer.parseInt(subject);
                    Optional<UserAccount> userById = userAccountRepository.findById(userId);
                    if (userById.isPresent()) {
                        logger.info("✅ User loaded by ID: {}", userId);
                        return userById.get();
                    }
                    logger.warn("User not found by ID: {}", userId);
                } catch (NumberFormatException e) {
                    // Subject is not a number, continue with legacy lookups
                    logger.debug("Subject '{}' is not a numeric ID, trying legacy lookups", subject);
                }
            }

            // 2) Legacy fallback: Find by email
            Optional<UserAccount> userByEmail = userAccountRepository.findByEmail(subject);
            if (userByEmail.isPresent()) {
                logger.info("✅ User loaded by email (legacy): {}", subject);
                return userByEmail.get();
            }

            // 3) Legacy fallback: Find by username
            Optional<UserAccount> userByUsername = userAccountRepository.findByUsername(subject);
            if (userByUsername.isPresent()) {
                logger.info("✅ User loaded by username (legacy): {}", subject);
                return userByUsername.get();
            }

            // 4) Legacy fallback: Find by phone (if available)
            Optional<UserAccount> userByPhone = userAccountRepository.findByPhone(subject);
            if (userByPhone.isPresent()) {
                logger.info("✅ User loaded by phone (legacy): {}", subject);
                return userByPhone.get();
            }

            // If user not found by any method, throw exception
            logger.error("❌ User not found with subject: {}", subject);
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