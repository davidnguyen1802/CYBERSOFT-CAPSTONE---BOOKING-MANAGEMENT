package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.*;
import com.Cybersoft.Final_Capstone.payload.request.PropertyRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface HostService {
    
    // Host profile management
    HostDTO getHostProfile(Integer hostId);
    
    // Property management for hosts
    List<PropertyDTO> getHostProperties(Integer hostId);
    PropertyDTO addProperty(Integer hostId, PropertyRequest propertyRequest);
    PropertyDTO updateProperty(Integer hostId, Integer propertyId, PropertyRequest propertyRequest);
    void deleteProperty(Integer hostId, Integer propertyId);
    
    // Booking management for hosts
    List<BookingDTO> getHostBookings(Integer hostId);
    List<BookingDTO> getHostBookingsByStatus(Integer hostId, String statusName);
    List<BookingDTO> getPropertyBookings(Integer hostId, Integer propertyId);
    BookingDTO approveBooking(Integer hostId, Integer bookingId);
    BookingDTO rejectBooking(Integer hostId, Integer bookingId, String reason);
    
    // Guest tracking
    List<GuestInfoDTO> getCurrentGuests(Integer hostId);
    List<GuestInfoDTO> getUpcomingGuests(Integer hostId);
    List<GuestInfoDTO> getPastGuests(Integer hostId);
    List<GuestInfoDTO> getGuestsForProperty(Integer hostId, Integer propertyId);
    
    // Statistics and analytics
    HostStatisticsDTO getHostStatistics(Integer hostId);
    
    // Property availability
    List<BookingDTO> getPropertyBookingsInDateRange(Integer hostId, Integer propertyId, 
                                                     LocalDateTime startDate, LocalDateTime endDate);
}

