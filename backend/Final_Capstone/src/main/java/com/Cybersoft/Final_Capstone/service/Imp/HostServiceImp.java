package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.dto.*;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.repository.*;
import com.Cybersoft.Final_Capstone.service.HostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class HostServiceImp implements HostService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // ==================== REMOVED DUPLICATE METHODS ====================
    // ❌ getHostProfile() - Use UserService.getMyProfile() + GET /users/me/details instead
    // ❌ getHostProperties() - Use PropertyService.getByHostId() + GET /property/host/{hostId} instead
    // ❌ updateProperty() - Use PropertyService.updateProperty() + PUT /property/{id} instead (now has ownership check)
    // ❌ deleteProperty() - Use PropertyService.deleteProperty() + DELETE /property/{id} instead (now has ownership check)
    
    // ==================== REMOVED REDUNDANT GUEST TRACKING METHODS ====================
    // ❌ getCurrentGuests() - Use BookingController: GET /bookings/host/{hostId}/filter?status=CONFIRMED (filter by date on frontend)
    // ❌ getUpcomingGuests() - Use BookingController: GET /bookings/host/{hostId}/filter?status=PENDING,CONFIRMED (filter by date on frontend)
    // ❌ getPastGuests() - Use BookingController: GET /bookings/host/{hostId}/filter?status=COMPLETED (filter by date on frontend)
    // ❌ getGuestsForProperty() - Use BookingController: GET /bookings/property/{propertyId} (already exists!)
    
    // ✅ Advantages of using filter endpoint:
    // - Has pagination support (better performance)
    // - More flexible filtering and sorting
    // - Single unified endpoint instead of 4 separate ones
    // - Frontend has full control over date filtering logic

    // ==================== Host Statistics & Analytics ====================
    
    @Override
    public HostStatisticsDTO getHostStatistics(Integer hostId) {
        // Verify host exists
        UserAccount host = userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        List<Booking> allBookings = bookingRepository.findByHostId(hostId);
        List<Property> properties = propertyRepository.findByHostId(hostId);
        
        HostStatisticsDTO stats = new HostStatisticsDTO();
        stats.setHostId(hostId);
        stats.setHostName(host.getFullName());
        stats.setTotalProperties(properties.size());
        stats.setTotalBookings(allBookings.size());
        
        // Count bookings by status
        long pendingCount = allBookings.stream()
                .filter(b -> "Pending".equals(b.getStatus().getName()))
                .count();
        stats.setPendingBookings((int) pendingCount);
        
        long confirmedCount = allBookings.stream()
                .filter(b -> "Confirmed".equals(b.getStatus().getName()))
                .count();
        stats.setConfirmedBookings((int) confirmedCount);
        
        long completedCount = allBookings.stream()
                .filter(b -> "Completed".equals(b.getStatus().getName()))
                .count();
        stats.setCompletedBookings((int) completedCount);
        
        long cancelledCount = allBookings.stream()
                .filter(b -> "Cancelled".equals(b.getStatus().getName()) || 
                            "Rejected".equals(b.getStatus().getName()))
                .count();
        stats.setCancelledBookings((int) cancelledCount);
        
        // Calculate total revenue from completed bookings
        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> "Completed".equals(b.getStatus().getName()))
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);
        
        // Calculate average rating across all properties
        BigDecimal totalRating = properties.stream()
                .map(Property::getOverallRating)
                .filter(rating -> rating != null && rating.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long propertiesWithRating = properties.stream()
                .filter(p -> p.getOverallRating() != null && p.getOverallRating().compareTo(BigDecimal.ZERO) > 0)
                .count();
        
        if (propertiesWithRating > 0) {
            stats.setAverageRating(totalRating.divide(
                    BigDecimal.valueOf(propertiesWithRating), 2, RoundingMode.HALF_UP));
        } else {
            stats.setAverageRating(BigDecimal.ZERO);
        }
        
        return stats;
    }
}

