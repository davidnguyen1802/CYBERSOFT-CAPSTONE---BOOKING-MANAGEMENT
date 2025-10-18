package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.dto.*;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.InvalidException;
import com.Cybersoft.Final_Capstone.mapper.BookingMapper;
import com.Cybersoft.Final_Capstone.mapper.HostMapper;
import com.Cybersoft.Final_Capstone.payload.request.PropertyRequest;
import com.Cybersoft.Final_Capstone.repository.*;
import com.Cybersoft.Final_Capstone.service.HostService;
import com.Cybersoft.Final_Capstone.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HostServiceImp implements HostService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PropertyService propertyService;

    @Override
    public HostDTO getHostProfile(Integer hostId) {
        UserAccount host = userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        return HostMapper.toDTO(host);
    }

    @Override
    public List<PropertyDTO> getHostProperties(Integer hostId) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        return propertyService.getByHostId(hostId);
    }

    @Transactional
    @Override
    public PropertyDTO addProperty(Integer hostId, PropertyRequest propertyRequest) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        // Set the host ID in the request
        propertyRequest.setHostId(hostId);
        
        return propertyService.insertProperty(propertyRequest);
    }

    @Transactional
    @Override
    public PropertyDTO updateProperty(Integer hostId, Integer propertyId, PropertyRequest propertyRequest) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        // Verify property belongs to this host
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
        
        if (!property.getHost().getId().equals(hostId)) {
            throw new InvalidException("This property does not belong to the specified host");
        }
        
        return propertyService.updateProperty(propertyId, propertyRequest);
    }

    @Transactional
    @Override
    public void deleteProperty(Integer hostId, Integer propertyId) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        // Verify property belongs to this host
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
        
        if (!property.getHost().getId().equals(hostId)) {
            throw new InvalidException("This property does not belong to the specified host");
        }
        
        propertyService.deleteProperty(propertyId);
    }

    @Override
    public List<BookingDTO> getHostBookings(Integer hostId) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        List<Booking> bookings = bookingRepository.findByHostId(hostId);
        return bookings.stream()
                .map(BookingMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDTO> getHostBookingsByStatus(Integer hostId, String statusName) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        List<Booking> bookings = bookingRepository.findByHostId(hostId);
        return bookings.stream()
                .filter(booking -> booking.getStatus().getName().equalsIgnoreCase(statusName))
                .map(BookingMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDTO> getPropertyBookings(Integer hostId, Integer propertyId) {
        // Verify host exists and owns the property
        verifyHostOwnsProperty(hostId, propertyId);
        
        List<Booking> bookings = bookingRepository.findByPropertyId(propertyId);
        return bookings.stream()
                .map(BookingMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public BookingDTO approveBooking(Integer hostId, Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + bookingId));
        
        // Verify the property belongs to this host
        if (!booking.getProperty().getHost().getId().equals(hostId)) {
            throw new InvalidException("This booking is not for a property you own");
        }
        
        // Check if booking is in a state that can be approved
        if (!booking.getStatus().getName().equals("Pending")) {
            throw new InvalidException("Only pending bookings can be approved. Current status: " + booking.getStatus().getName());
        }
        
        Status confirmedStatus = statusRepository.findByName("Confirmed")
                .orElseThrow(() -> new DataNotFoundException("Confirmed status not found"));
        
        booking.setStatus(confirmedStatus);
        Booking updatedBooking = bookingRepository.save(booking);
        
        return BookingMapper.toDTO(updatedBooking);
    }

    @Transactional
    @Override
    public BookingDTO rejectBooking(Integer hostId, Integer bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + bookingId));
        
        // Verify the property belongs to this host
        if (!booking.getProperty().getHost().getId().equals(hostId)) {
            throw new InvalidException("This booking is not for a property you own");
        }
        
        // Check if booking is in a state that can be rejected
        if (booking.getStatus().getName().equals("Completed") || 
            booking.getStatus().getName().equals("Cancelled") ||
            booking.getStatus().getName().equals("Rejected")) {
            throw new InvalidException("Cannot reject a " + booking.getStatus().getName() + " booking");
        }
        
        Status rejectedStatus = statusRepository.findByName("Rejected")
                .orElseThrow(() -> new DataNotFoundException("Rejected status not found"));
        
        booking.setStatus(rejectedStatus);
        if (reason != null && !reason.isEmpty()) {
            String currentNotes = booking.getNotes() != null ? booking.getNotes() : "";
            booking.setNotes(currentNotes + "\nRejection reason: " + reason);
        }
        
        Booking updatedBooking = bookingRepository.save(booking);
        
        return BookingMapper.toDTO(updatedBooking);
    }

    @Override
    public List<GuestInfoDTO> getCurrentGuests(Integer hostId) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByHostId(hostId);
        
        return bookings.stream()
                .filter(booking -> booking.getCheckIn().isBefore(now) && 
                                 booking.getCheckOut().isAfter(now) &&
                                 booking.getStatus().getName().equals("Confirmed"))
                .map(HostMapper::toGuestInfoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GuestInfoDTO> getUpcomingGuests(Integer hostId) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByHostId(hostId);
        
        return bookings.stream()
                .filter(booking -> booking.getCheckIn().isAfter(now) &&
                                 (booking.getStatus().getName().equals("Confirmed") ||
                                  booking.getStatus().getName().equals("Pending")))
                .map(HostMapper::toGuestInfoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GuestInfoDTO> getPastGuests(Integer hostId) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByHostId(hostId);
        
        return bookings.stream()
                .filter(booking -> booking.getCheckOut().isBefore(now) &&
                                 booking.getStatus().getName().equals("Completed"))
                .map(HostMapper::toGuestInfoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GuestInfoDTO> getGuestsForProperty(Integer hostId, Integer propertyId) {
        // Verify host exists and owns the property
        verifyHostOwnsProperty(hostId, propertyId);
        
        List<Booking> bookings = bookingRepository.findByPropertyId(propertyId);
        return bookings.stream()
                .map(HostMapper::toGuestInfoDTO)
                .collect(Collectors.toList());
    }

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
        stats.setPendingBookings((int) allBookings.stream()
                .filter(b -> b.getStatus().getName().equals("Pending")).count());
        stats.setConfirmedBookings((int) allBookings.stream()
                .filter(b -> b.getStatus().getName().equals("Confirmed")).count());
        stats.setCompletedBookings((int) allBookings.stream()
                .filter(b -> b.getStatus().getName().equals("Completed")).count());
        stats.setCancelledBookings((int) allBookings.stream()
                .filter(b -> b.getStatus().getName().equals("Cancelled") ||
                           b.getStatus().getName().equals("Rejected")).count());
        
        // Calculate total revenue from completed bookings
        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> b.getStatus().getName().equals("Completed"))
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

    @Override
    public List<BookingDTO> getPropertyBookingsInDateRange(Integer hostId, Integer propertyId, 
                                                           LocalDateTime startDate, LocalDateTime endDate) {
        // Verify host exists and owns the property
        verifyHostOwnsProperty(hostId, propertyId);
        
        List<Booking> bookings = bookingRepository.findBookingsByPropertyAndDateRange(
                propertyId, startDate, endDate);
        
        return bookings.stream()
                .map(BookingMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Helper method
    private void verifyHostOwnsProperty(Integer hostId, Integer propertyId) {
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
        
        if (!property.getHost().getId().equals(hostId)) {
            throw new InvalidException("This property does not belong to the specified host");
        }
    }
}

