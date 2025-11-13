package com.Cybersoft.Final_Capstone.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    @JsonProperty("message")
    private String message;

    @JsonProperty("token")
    private String token;

    private String tokenType = "Bearer";
    //user's detail
    private int id;
    private String username;

    private String role; // Changed from List<String> to String - each user has only one role
}
