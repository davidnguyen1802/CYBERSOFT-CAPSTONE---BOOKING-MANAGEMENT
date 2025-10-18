package com.Cybersoft.Final_Capstone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient userInfoClient() {
        // Use Google's userinfo endpoint directly
        return WebClient.builder()
                .baseUrl("https://www.googleapis.com")
                .build();
    }

    @Bean
    public WebClient facebookUserInfoClient() {
        // Use Facebook's graph API endpoint
        return WebClient.builder()
                .baseUrl("https://graph.facebook.com")
                .build();
    }
}
