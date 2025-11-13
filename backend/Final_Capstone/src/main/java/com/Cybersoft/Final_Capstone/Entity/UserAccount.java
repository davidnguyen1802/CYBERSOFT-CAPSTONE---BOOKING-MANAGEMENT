package com.Cybersoft.Final_Capstone.Entity;

import com.Cybersoft.Final_Capstone.Enum.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "user_account")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserAccount implements UserDetails, OAuth2User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String fullName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    private String address;
    
    private String avatar;

    @Column(name = "password_hash", nullable = true)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate dob;

    @ManyToOne
    @JoinColumn(name = "id_role", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "id_status", nullable = false)
    private Status status;

    private Integer priority;

    @Column(name = "create_date")
    private LocalDate createDate;

    @OneToMany(mappedBy = "host")
    private List<Property> hostedProperties = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<UserReview> reviews = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "favourite_list",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "property_id")
    )
    private List<Property> favoriteList = new ArrayList<>();

    @OneToMany(mappedBy = "userAccount")
    private List<UserPromotion> userPromotions;

    @Column(name = "facebook_account_id")
    private String facebookAccountId;

    @Column(name = "google_account_id")
    private String googleAccountId;



    @Override
    public String getUsername() {
        // Return the actual username field for display purposes
        return username;
    }

    @Override
    public <A> A getAttribute(String name) {
        return OAuth2User.super.getAttribute(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        authorityList.add(new SimpleGrantedAuthority("ROLE_"+ getRole().getName().toUpperCase()));
        //authorityList.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return authorityList;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override
    public String getName() {
        return "";
    }
}
