package com.Cybersoft.Final_Capstone.dto;

import com.Cybersoft.Final_Capstone.Enum.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HostDTO {
    private Integer id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String address;
    private Gender gender;
    private LocalDate dob;
    private Integer totalProperties;
    private Integer totalBookings;
    private LocalDate joinDate;
}

