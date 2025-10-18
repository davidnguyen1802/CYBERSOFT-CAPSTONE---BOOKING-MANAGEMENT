package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private boolean used = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}

