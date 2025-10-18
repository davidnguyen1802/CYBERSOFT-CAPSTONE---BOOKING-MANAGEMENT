package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin Service Interface
 * Provides full system access and management capabilities for administrators
 */
public interface AdminService {
    
    // Dashboard & Statistics
    Map<String, Object> getDashboardStatistics();
    Map<String, Object> getStatisticsByDateRange(LocalDate startDate, LocalDate endDate);
    
    // User Management
    Page<UserAccountDTO> getAllUsers(String keyword, String role, String status, Pageable pageable);
    UserAccountDTO getUserById(Integer userId);
    UserAccountDTO updateUser(Integer userId, UpdateUserDTO updateUserDTO);
    UserAccountDTO changeUserRole(Integer userId, String roleName);
    UserAccountDTO changeUserStatus(Integer userId, String statusName);
    void deleteUser(Integer userId);
    
    // Property Management
    Page<PropertyDTO> getAllProperties(String keyword, String status, Pageable pageable);
    PropertyDTO getPropertyById(Integer propertyId);
    PropertyDTO changePropertyStatus(Integer propertyId, String statusName);
    void deleteProperty(Integer propertyId);
    
    // Booking Management
    Page<BookingDTO> getAllBookings(String status, Pageable pageable);
    BookingDTO getBookingById(Integer bookingId);
    BookingDTO changeBookingStatus(Integer bookingId, String statusName);
    void cancelBooking(Integer bookingId);
    
    // Promotion Management
    Page<PromotionDTO> getAllPromotions(String status, Pageable pageable);
    PromotionDTO changePromotionStatus(Integer promotionId, String statusName);
    
    // Review Management
    Page<ReviewDTO> getAllReviews(Integer propertyId, Pageable pageable);
    void deleteReview(Integer reviewId);
    
    // Reports & Analytics
    Map<String, Object> getRevenueReport(LocalDate startDate, LocalDate endDate, String groupBy);
    List<HostStatisticsDTO> getTopHosts(String sortBy, int limit);
    List<PropertyDTO> getTopProperties(String sortBy, int limit);
    Map<String, Object> getUserActivityReport(LocalDate startDate, LocalDate endDate);
}

