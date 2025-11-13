package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.*;

public interface HostService {
    
    // ==================== REMOVED DUPLICATE METHODS ====================
    // ❌ getHostProfile() - Use UserService.getMyProfile() + GET /users/me/details instead
    // ❌ getHostProperties() - Use PropertyService.getByHostId() + GET /property/host/{hostId} instead
    // ❌ updateProperty() - Use PropertyService.updateProperty() + PUT /property/{id} instead (now has ownership check)
    // ❌ deleteProperty() - Use PropertyService.deleteProperty() + DELETE /property/{id} instead (now has ownership check)
    
    // ==================== REMOVED REDUNDANT GUEST TRACKING METHODS ====================
    // ❌ getCurrentGuests() - Use BookingController: GET /bookings/host/{hostId}/filter?status=CONFIRMED
    // ❌ getUpcomingGuests() - Use BookingController: GET /bookings/host/{hostId}/filter?status=PENDING,CONFIRMED
    // ❌ getPastGuests() - Use BookingController: GET /bookings/host/{hostId}/filter?status=COMPLETED
    // ❌ getGuestsForProperty() - Use BookingController: GET /bookings/property/{propertyId}
    
    // ==================== Host Statistics & Analytics ====================
    
    /**
     * Get comprehensive statistics for a host
     * Includes: total properties, bookings by status, revenue, average rating
     */
    HostStatisticsDTO getHostStatistics(Integer hostId);
    
    // ❌ REMOVED: getPropertyBookingsInDateRange()
    // ✅ USE INSTEAD: BookingService.getPropertyBookingsInDateRange() + GET /bookings/property/{propertyId}/date-range
}

