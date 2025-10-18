package com.Cybersoft.Final_Capstone.dto;

import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.Enum.Gender;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserAccountDTO extends SocialAccountDTO {
    private Integer id;

    @JsonProperty("fullname")
    private String fullName;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    private String address;
    private String avatar;
    private Gender gender;
    private LocalDate dob;
    private String roleName;
    private String statusName;

    private LocalDate createDate;

    @NotBlank(message = "Password cannot be blank")
    private String password = "";

    @JsonProperty("facebook_account_id")
    private String facebookAccountId;

    @JsonProperty("google_account_id")
    private String googleAccountId;

    public void setStatusName(String name) {
    }
}
