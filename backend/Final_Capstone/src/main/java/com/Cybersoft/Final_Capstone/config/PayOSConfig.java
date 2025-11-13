package com.Cybersoft.Final_Capstone.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Getter
@Setter
@Configuration
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.endpoint}")
    private String endpoint;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey, endpoint);
    }
}
