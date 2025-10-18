package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.dto.*;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller - Full System Access
 * 
 * ⚠️ SECURITY: ALL endpoints require ROLE_ADMIN
 * Admins have complete access to all system resources and operations
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ==================== Dashboard & Statistics ====================
    
    /**
     * Get comprehensive system statistics
     * GET /admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStatistics() {
        Map<String, Object> stats = adminService.getDashboardStatistics();
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Dashboard statistics retrieved successfully");
        response.setData(stats);
        return ResponseEntity.ok(response);
    }

    /**
     * Get statistics for specific date range
     * GET /admin/statistics/date-range
     */
    @GetMapping("/statistics/date-range")
    public ResponseEntity<?> getStatisticsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> stats = adminService.getStatisticsByDateRange(startDate, endDate);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Statistics for date range retrieved successfully");
        response.setData(stats);
        return ResponseEntity.ok(response);
    }

    // ==================== User Management ====================
    
    /**
     * Get all users with pagination and filtering
     * GET /admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserAccountDTO> users = adminService.getAllUsers(keyword, role, status, pageable);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Users retrieved successfully");
        response.setData(users);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     * GET /admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Integer userId) {
        UserAccountDTO user = adminService.getUserById(userId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User retrieved successfully");
        response.setData(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user information
     * PUT /admin/users/{userId}
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Integer userId,
            @RequestBody UpdateUserDTO updateUserDTO) {
        UserAccountDTO user = adminService.updateUser(userId, updateUserDTO);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User updated successfully");
        response.setData(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Change user role
     * PATCH /admin/users/{userId}/role
     */
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<?> changeUserRole(
            @PathVariable Integer userId,
            @RequestParam String roleName) {
        UserAccountDTO user = adminService.changeUserRole(userId, roleName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User role changed successfully");
        response.setData(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Change user status (ACTIVE/INACTIVE/DELETED)
     * PATCH /admin/users/{userId}/status
     */
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<?> changeUserStatus(
            @PathVariable Integer userId,
            @RequestParam String statusName) {
        UserAccountDTO user = adminService.changeUserStatus(userId, statusName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User status changed successfully");
        response.setData(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete user (soft delete - sets status to DELETED)
     * DELETE /admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer userId) {
        adminService.deleteUser(userId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User deleted successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    // ==================== Property Management ====================
    
    /**
     * Get all properties with pagination
     * GET /admin/properties
     */
    @GetMapping("/properties")
    public ResponseEntity<?> getAllProperties(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<PropertyDTO> properties = adminService.getAllProperties(keyword, status, pageable);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Properties retrieved successfully");
        response.setData(properties);
        return ResponseEntity.ok(response);
    }

    /**
     * Get property by ID
     * GET /admin/properties/{propertyId}
     */
    @GetMapping("/properties/{propertyId}")
    public ResponseEntity<?> getPropertyById(@PathVariable Integer propertyId) {
        PropertyDTO property = adminService.getPropertyById(propertyId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property retrieved successfully");
        response.setData(property);
        return ResponseEntity.ok(response);
    }

    /**
     * Change property status
     * PATCH /admin/properties/{propertyId}/status
     */
    @PatchMapping("/properties/{propertyId}/status")
    public ResponseEntity<?> changePropertyStatus(
            @PathVariable Integer propertyId,
            @RequestParam String statusName) {
        PropertyDTO property = adminService.changePropertyStatus(propertyId, statusName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property status changed successfully");
        response.setData(property);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete property
     * DELETE /admin/properties/{propertyId}
     */
    @DeleteMapping("/properties/{propertyId}")
    public ResponseEntity<?> deleteProperty(@PathVariable Integer propertyId) {
        adminService.deleteProperty(propertyId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property deleted successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    // ==================== Booking Management ====================
    
    /**
     * Get all bookings with pagination
     * GET /admin/bookings
     */
    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<BookingDTO> bookings = adminService.getAllBookings(status, pageable);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Bookings retrieved successfully");
        response.setData(bookings);
        return ResponseEntity.ok(response);
    }

    /**
     * Get booking by ID
     * GET /admin/bookings/{bookingId}
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable Integer bookingId) {
        BookingDTO booking = adminService.getBookingById(bookingId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking retrieved successfully");
        response.setData(booking);
        return ResponseEntity.ok(response);
    }

    /**
     * Change booking status
     * PATCH /admin/bookings/{bookingId}/status
     */
    @PatchMapping("/bookings/{bookingId}/status")
    public ResponseEntity<?> changeBookingStatus(
            @PathVariable Integer bookingId,
            @RequestParam String statusName) {
        BookingDTO booking = adminService.changeBookingStatus(bookingId, statusName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking status changed successfully");
        response.setData(booking);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel booking
     * DELETE /admin/bookings/{bookingId}
     */
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Integer bookingId) {
        adminService.cancelBooking(bookingId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking cancelled successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    // ==================== Promotion Management ====================
    
    /**
     * Get all promotions
     * GET /admin/promotions
     */
    @GetMapping("/promotions")
    public ResponseEntity<?> getAllPromotions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<PromotionDTO> promotions = adminService.getAllPromotions(status, pageable);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Promotions retrieved successfully");
        response.setData(promotions);
        return ResponseEntity.ok(response);
    }

    /**
     * Change promotion status
     * PATCH /admin/promotions/{promotionId}/status
     */
    @PatchMapping("/promotions/{promotionId}/status")
    public ResponseEntity<?> changePromotionStatus(
            @PathVariable Integer promotionId,
            @RequestParam String statusName) {
        PromotionDTO promotion = adminService.changePromotionStatus(promotionId, statusName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Promotion status changed successfully");
        response.setData(promotion);
        return ResponseEntity.ok(response);
    }

    // ==================== Review Management ====================
    
    /**
     * Get all reviews
     * GET /admin/reviews
     */
    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews(
            @RequestParam(required = false) Integer propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ReviewDTO> reviews = adminService.getAllReviews(propertyId, pageable);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Reviews retrieved successfully");
        response.setData(reviews);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete review
     * DELETE /admin/reviews/{reviewId}
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Integer reviewId) {
        adminService.deleteReview(reviewId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Review deleted successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    // ==================== Reports & Analytics ====================
    
    /**
     * Get revenue report
     * GET /admin/reports/revenue
     */
    @GetMapping("/reports/revenue")
    public ResponseEntity<?> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String groupBy) {
        
        Map<String, Object> report = adminService.getRevenueReport(startDate, endDate, groupBy);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Revenue report generated successfully");
        response.setData(report);
        return ResponseEntity.ok(response);
    }

    /**
     * Get top hosts by revenue/bookings
     * GET /admin/reports/top-hosts
     */
    @GetMapping("/reports/top-hosts")
    public ResponseEntity<?> getTopHosts(
            @RequestParam(defaultValue = "revenue") String sortBy,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<HostStatisticsDTO> topHosts = adminService.getTopHosts(sortBy, limit);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Top hosts retrieved successfully");
        response.setData(topHosts);
        return ResponseEntity.ok(response);
    }

    /**
     * Get top properties by bookings/ratings
     * GET /admin/reports/top-properties
     */
    @GetMapping("/reports/top-properties")
    public ResponseEntity<?> getTopProperties(
            @RequestParam(defaultValue = "bookings") String sortBy,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<PropertyDTO> topProperties = adminService.getTopProperties(sortBy, limit);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Top properties retrieved successfully");
        response.setData(topProperties);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user activity report
     * GET /admin/reports/user-activity
     */
    @GetMapping("/reports/user-activity")
    public ResponseEntity<?> getUserActivityReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> report = adminService.getUserActivityReport(startDate, endDate);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User activity report generated successfully");
        response.setData(report);
        return ResponseEntity.ok(response);
    }
}
