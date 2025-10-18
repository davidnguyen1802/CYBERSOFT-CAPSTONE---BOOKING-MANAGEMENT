package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "user_social_accounts")
public class UserSocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String provider; // google, facebook, ...

    @Column(name = "provider_id", nullable = false, length = 50)
    private String providerId;

    @Column(name = "name",length = 150)
    private String name;

    @Column(name = "email", length = 150)
    private String email;

}
