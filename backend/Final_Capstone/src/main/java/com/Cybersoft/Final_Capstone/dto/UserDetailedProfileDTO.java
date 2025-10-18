package com.Cybersoft.Final_Capstone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Detailed User Profile DTO
 * Includes comprehensive user information with related entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailedProfileDTO {
    @JsonProperty("user_info")
    private UserProfileDTO userInfo;
    
    // For GUEST users
    @JsonProperty("recent_bookings")
    private List<BookingDTO> recentBookings;
    
    @JsonProperty("favorite_properties")
    private List<PropertyDTO> favoriteProperties;
    
    @JsonProperty("active_promotions")
    private List<UserPromotionDTO> activePromotions;
    
    // For HOST users
    @JsonProperty("hosted_properties")
    private List<PropertyDTO> hostedProperties;
    
    @JsonProperty("recent_property_bookings")
    private List<BookingDTO> recentPropertyBookings;
    
    @JsonProperty("host_statistics")
    private HostStatisticsDTO hostStatistics;
}

