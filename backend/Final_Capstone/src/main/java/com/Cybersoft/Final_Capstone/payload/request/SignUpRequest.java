package com.Cybersoft.Final_Capstone.payload.request;

import com.Cybersoft.Final_Capstone.Enum.Gender;
import com.Cybersoft.Final_Capstone.dto.SocialAccountDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest extends SocialAccountDTO {
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores and hyphens")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phone;
    
    private String address;
    
    private Gender gender;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    // Remember Me flag for persistent cookie (optional, default false)
    private Boolean rememberMe;

    // Custom setter to trim string values before conversion
    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : null;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim() : null;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }

    public void setAddress(String address) {
        this.address = address != null ? address.trim() : null;
    }

    @JsonSetter("dob")
    public void setDob(String dobStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) {
            this.dob = null;
            return;
        }

        String s = dobStr.trim();

        // Các định dạng đầu vào chấp nhận
        String[] patterns = {
                "yyyy-MM-dd",   // 2025-10-19
                "dd/MM/yyyy",   // 19/10/2025
                "dd-MM-yyyy",   // 19-10-2025
                "MM-dd-yyyy"    // 10-19-2025
        };

        LocalDate parsed = null;
        for (String p : patterns) {
            try {
                parsed = LocalDate.parse(s, DateTimeFormatter.ofPattern(p));
                break;
            } catch (DateTimeParseException ignore) {}
        }

        if (parsed == null) {
            throw new IllegalArgumentException("Invalid dob format. Use one of: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy, MM-dd-yyyy");
        }

        // (Tuỳ chọn) Không cho phép ngày sinh ở tương lai
        if (parsed.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dob cannot be in the future");
        }

        this.dob = parsed;
    }

    // Cho phép gán trực tiếp LocalDate trong code
    public void setDob(LocalDate dob) {
        this.dob = dob;
    }
}
