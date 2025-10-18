package com.Cybersoft.Final_Capstone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserLoginDTO extends SocialAccountDTO {
    private String username;

    private String email;

    // Password may not be needed for social login but required for traditional login
    private String password;

    // Facebook Account Id, not mandatory, can be blank
    private String facebookAccountId;

    // Google Account Id, not mandatory, can be blank
    private String googleAccountId;

    //For Google, Facebook login
    // Full name, not mandatory, can be blank
    private String fullname;

    // Profile image URL, not mandatory, can be blank
    private String avatar;
    // Kiểm tra facebookAccountId có hợp lệ không
    public boolean isFacebookAccountIdValid() {
        return facebookAccountId != null && !facebookAccountId.isEmpty();
    }

    // Kiểm tra googleAccountId có hợp lệ không
    public boolean isGoogleAccountIdValid() {
        return googleAccountId != null && !googleAccountId.isEmpty();
    }
}

