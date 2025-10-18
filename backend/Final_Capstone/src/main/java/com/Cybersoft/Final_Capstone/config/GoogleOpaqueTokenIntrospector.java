package com.Cybersoft.Final_Capstone.config;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class GoogleOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    private final WebClient userInfoClient;


    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        UserAccount userInfo = userInfoClient.get()
                .uri( uriBuilder -> uriBuilder
                        .path("/oauth2/v3/userinfo")
                        .queryParam("access_token", token)
                        .build())
                .retrieve()
                .bodyToMono(UserAccount.class)
                .block();
        Map<String, Object> attributes = new HashMap<>();
        assert userInfo != null;
        attributes.put("userId", userInfo.getId());
        return new OAuth2IntrospectionAuthenticatedPrincipal(userInfo.getUsername(), attributes, null);
    }
}
