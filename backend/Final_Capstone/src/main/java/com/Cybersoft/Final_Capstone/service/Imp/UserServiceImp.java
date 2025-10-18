package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.dto.*;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.mapper.*;
import com.Cybersoft.Final_Capstone.repository.*;
import com.Cybersoft.Final_Capstone.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User Service Implementation
 * Provides user profile and account management functionality
 */
@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {

    private final UserAccountRepository userAccountRepository;
    private final BookingRepository bookingRepository;

    @Override
    public UserProfileDTO getMyProfile(Integer userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        // Build basic profile
        UserProfileDTO profile = UserProfileDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .dob(user.getDob())
                .roleName(user.getRole().getName())
                .statusName(user.getStatus().getName())
                .createDate(user.getCreateDate())
                .build();

        // Add GUEST-specific statistics
        profile.setTotalBookings(user.getBookings() != null ? user.getBookings().size() : 0);
        profile.setFavoritePropertiesCount(user.getFavoriteList() != null ? user.getFavoriteList().size() : 0);
        
        // Count active promotions
        if (user.getUserPromotions() != null) {
            long activePromotions = user.getUserPromotions().stream()
                    .filter(up -> up.getPromotion() != null && 
                                  up.getPromotion().getStatus() != null &&
                                  "ACTIVE".equals(up.getPromotion().getStatus().getName()))
                    .count();
            profile.setActivePromotionsCount((int) activePromotions);
        } else {
            profile.setActivePromotionsCount(0);
        }
        //count rewviews
        profile.setTotalReviews(user.getReviews() != null ? user.getReviews().size() : 0);

        // Add HOST-specific statistics if user is a HOST
        if ("HOST".equalsIgnoreCase(user.getRole().getName())) {
            List<Property> hostedProperties = user.getHostedProperties();
            profile.setHostedPropertiesCount(hostedProperties != null ? hostedProperties.size() : 0);

            if (hostedProperties != null && !hostedProperties.isEmpty()) {
                // Calculate total earnings from bookings on hosted properties
                BigDecimal totalEarnings = BigDecimal.ZERO;
                int totalPropertyReviews = 0;
                BigDecimal totalRating = BigDecimal.ZERO;
                int propertiesWithRatings = 0;

                for (Property property : hostedProperties) {
                    // Sum up earnings from confirmed bookings
                    List<Booking> propertyBookings = bookingRepository.findByPropertyId(property.getId());
                    if (propertyBookings != null) {
                        BigDecimal propertyEarnings = propertyBookings.stream()
                                .filter(b -> b.getStatus() != null && 
                                           ("CONFIRMED".equals(b.getStatus().getName()) || 
                                            "COMPLETED".equals(b.getStatus().getName())))
                                .map(Booking::getTotalPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        totalEarnings = totalEarnings.add(propertyEarnings);
                    }

                    // Sum up reviews and ratings
                    if (property.getReviewCount() != null) {
                        totalPropertyReviews += property.getReviewCount();
                    }
                    if (property.getOverallRating() != null && property.getOverallRating().compareTo(BigDecimal.ZERO) > 0) {
                        totalRating = totalRating.add(property.getOverallRating());
                        propertiesWithRatings++;
                    }
                }

                profile.setTotalEarnings(totalEarnings.toString());
                profile.setTotalPropertyReviews(totalPropertyReviews);
                
                // Calculate average rating across all properties
                if (propertiesWithRatings > 0) {
                    BigDecimal avgRating = totalRating.divide(
                            BigDecimal.valueOf(propertiesWithRatings), 
                            2, 
                            java.math.RoundingMode.HALF_UP
                    );
                    profile.setAverageRating(avgRating.toString());
                } else {
                    profile.setAverageRating("0.00");
                }
            } else {
                profile.setTotalEarnings("0.00");
                profile.setAverageRating("0.00");
                profile.setTotalPropertyReviews(0);
            }
        }

        return profile;
    }

//    @Override
//    public UserDetailedProfileDTO getMyDetailedProfile(Integer userId, boolean includeDetails) {
//        UserAccount user = userAccountRepository.findById(userId)
//                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));
//
//        UserDetailedProfileDTO detailedProfile = UserDetailedProfileDTO.builder()
//                .userInfo(getMyProfile(userId))
//                .build();
//
//        if (!includeDetails) {
//            return detailedProfile;
//        }
//
//        String roleName = user.getRole().getName();
//
//        // Add GUEST-specific details
//        if ("GUEST".equalsIgnoreCase(roleName) || "HOST".equalsIgnoreCase(roleName)) {
//            // Recent bookings (last 10)
//            List<Booking> bookings = bookingRepository.findByUserId(userId);
//            if (bookings != null && !bookings.isEmpty()) {
//                List<BookingDTO> recentBookings = bookings.stream()
//                        .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
//                        .limit(10)
//                        .map(BookingMapper::toDTO)
//                        .collect(Collectors.toList());
//                detailedProfile.setRecentBookings(recentBookings);
//            }
//
//            // Favorite properties
//            if (user.getFavoriteList() != null && !user.getFavoriteList().isEmpty()) {
//                List<PropertyDTO> favoriteProperties = user.getFavoriteList().stream()
//                        .map(PropertyMapper::toDTO)
//                        .collect(Collectors.toList());
//                detailedProfile.setFavoriteProperties(favoriteProperties);
//            }
//
//            // Active promotions
//            if (user.getUserPromotions() != null && !user.getUserPromotions().isEmpty()) {
//                List<UserPromotionDTO> activePromotions = user.getUserPromotions().stream()
//                        .filter(up -> up.getPromotion() != null &&
//                                      up.getPromotion().getStatus() != null &&
//                                      "ACTIVE".equals(up.getPromotion().getStatus().getName()))
//                        .map(UserPromotionMapper::toDTO)
//                        .collect(Collectors.toList());
//                detailedProfile.setActivePromotions(activePromotions);
//            }
//        }
//
//        // Add HOST-specific details
//        if ("HOST".equalsIgnoreCase(roleName)) {
//            List<Property> hostedProperties = user.getHostedProperties();
//
//            if (hostedProperties != null && !hostedProperties.isEmpty()) {
//                // Hosted properties
//                List<PropertyDTO> propertyDTOs = hostedProperties.stream()
//                        .map(PropertyMapper::toDTO)
//                        .collect(Collectors.toList());
//                detailedProfile.setHostedProperties(propertyDTOs);
//
//                // Recent bookings on hosted properties (last 20)
//                List<BookingDTO> allPropertyBookings = hostedProperties.stream()
//                        .flatMap(property -> {
//                            List<Booking> bookings = bookingRepository.findByPropertyId(property.getId());
//                            return bookings != null ? bookings.stream() : java.util.stream.Stream.empty();
//                        })
//                        .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
//                        .limit(20)
//                        .map(BookingMapper::toDTO)
//                        .collect(Collectors.toList());
//                detailedProfile.setRecentPropertyBookings(allPropertyBookings);
//
//                // Host statistics
//                HostStatisticsDTO stats = buildHostStatistics(user, hostedProperties);
//                detailedProfile.setHostStatistics(stats);
//            }
//        }
//
//        return detailedProfile;
//    }

    @Override
    @Transactional
    public UserProfileDTO updateMyProfile(Integer userId, UpdateUserDTO updateUserDTO) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        // Update allowed fields
        if (updateUserDTO.getFullName() != null && !updateUserDTO.getFullName().trim().isEmpty()) {
            user.setFullName(updateUserDTO.getFullName());
        }
        if (updateUserDTO.getPhoneNumber() != null && !updateUserDTO.getPhoneNumber().trim().isEmpty()) {
            user.setPhone(updateUserDTO.getPhoneNumber());
        }
        if (updateUserDTO.getAddress() != null && !updateUserDTO.getAddress().trim().isEmpty()) {
            user.setAddress(updateUserDTO.getAddress());
        }
        if (updateUserDTO.getDateOfBirth() != null) {
            java.time.LocalDate localDate = updateUserDTO.getDateOfBirth().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            user.setDob(localDate);
        }

        userAccountRepository.save(user);
        return getMyProfile(userId);
    }

    @Override
    public boolean isUserActive(Integer userId) {
        return userAccountRepository.findById(userId)
                .map(user -> "ACTIVE".equalsIgnoreCase(user.getStatus().getName()))
                .orElse(false);
    }

//    /**
//     * Build host statistics from user's hosted properties
//     */
//    private HostStatisticsDTO buildHostStatistics(UserAccount user, List<Property> hostedProperties) {
//        HostStatisticsDTO stats = new HostStatisticsDTO();
//        stats.setTotalProperties(hostedProperties.size());
//
//        BigDecimal totalEarnings = BigDecimal.ZERO;
//        int totalBookings = 0;
//        BigDecimal totalRating = BigDecimal.ZERO;
//        int propertiesWithRatings = 0;
//
//        for (Property property : hostedProperties) {
//            List<Booking> propertyBookings = bookingRepository.findByPropertyId(property.getId());
//            if (propertyBookings != null) {
//                totalBookings += propertyBookings.size();
//
//                BigDecimal propertyEarnings = propertyBookings.stream()
//                        .filter(b -> b.getStatus() != null &&
//                                   ("CONFIRMED".equals(b.getStatus().getName()) ||
//                                    "COMPLETED".equals(b.getStatus().getName())))
//                        .map(Booking::getTotalPrice)
//                        .reduce(BigDecimal.ZERO, BigDecimal::add);
//                totalEarnings = totalEarnings.add(propertyEarnings);
//            }
//
//            if (property.getOverallRating() != null && property.getOverallRating().compareTo(BigDecimal.ZERO) > 0) {
//                totalRating = totalRating.add(property.getOverallRating());
//                propertiesWithRatings++;
//            }
//        }
//
//        stats.setTotalBookings(totalBookings);
//        stats.setTotalRevenue(totalEarnings);
//
//        if (propertiesWithRatings > 0) {
//            BigDecimal avgRating = totalRating.divide(
//                    BigDecimal.valueOf(propertiesWithRatings),
//                    2,
//                    java.math.RoundingMode.HALF_UP
//            );
//            stats.setAverageRating(avgRating);
//        } else {
//            stats.setAverageRating(BigDecimal.ZERO);
//        }
//
//        return stats;
//    }
}
