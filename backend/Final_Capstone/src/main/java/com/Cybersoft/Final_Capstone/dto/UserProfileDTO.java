package com.Cybersoft.Final_Capstone.dto;

import com.Cybersoft.Final_Capstone.Enum.Gender;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

/**
 * User Profile DTO
 * Contains user's basic information and role-specific data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
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
    
    @JsonProperty("role")
    private String roleName;
    
    @JsonProperty("status")
    private String statusName;
    
    @JsonProperty("create_date")
    private LocalDate createDate;
    
    // GUEST specific fields
    @JsonProperty("total_bookings")
    private Integer totalBookings;
    
    @JsonProperty("total_reviews")
    private Integer totalReviews;
    
    @JsonProperty("favorite_properties_count")
    private Integer favoritePropertiesCount;
    
    @JsonProperty("active_promotions_count")
    private Integer activePromotionsCount;
    
    // HOST specific fields (only included if user is a HOST)
    @JsonProperty("hosted_properties_count")
    private Integer hostedPropertiesCount;
    
    @JsonProperty("total_earnings")
    private String totalEarnings;
    
    @JsonProperty("average_rating")
    private String averageRating;
    
    @JsonProperty("total_property_reviews")
    private Integer totalPropertyReviews;
}

