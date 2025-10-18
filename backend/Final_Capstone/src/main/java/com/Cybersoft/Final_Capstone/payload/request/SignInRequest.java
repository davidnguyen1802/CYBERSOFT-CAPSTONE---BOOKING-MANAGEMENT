package com.Cybersoft.Final_Capstone.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInRequest {
    
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;

    // Facebook Account Id, not mandatory, can be blank
    private String facebookAccountId;

    // Google Account Id, not mandatory, can be blank
    private String googleAccountId;;

    //For Google, Facebook login
    // Full name, not mandatory, can be blank
    private String fullname;

    // Profile image URL, not mandatory, can be blank
    private String profileImage;

    // Kiểm tra facebookAccountId có hợp lệ không
    public boolean isFacebookAccountIdValid() {
        return facebookAccountId != null && !facebookAccountId.isEmpty();
    }

    // Kiểm tra googleAccountId có hợp lệ không
    public boolean isGoogleAccountIdValid() {
        return googleAccountId != null && !googleAccountId.isEmpty();
    }
}

