package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.dto.ApprovalPreviewDTO;
import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.payload.request.BookingRequest;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {
    Booking createBooking(BookingRequest bookingRequest);
    BookingDTO getBookingById(Integer id);
    // ❌ Removed unused non-paginated methods (replaced by paginated versions):
    // - List<BookingDTO> getBookingsByUserId(Integer userId) → use filterUserBookings()
    // - List<BookingDTO> getBookingsByPropertyId(Integer propertyId) → use getBookingsByPropertyId(pageable)
    // - List<BookingDTO> getAllBookings() → use getAllBookings(pageable)
    BookingDTO updateBookingStatus(Integer id, String status);
    BookingDTO cancelBooking(Integer id, String reason);
    void deleteBooking(Integer id);
    boolean isPropertyAvailable(Integer propertyId, String checkIn, String checkOut);
    
    // Filter and sort bookings by host (replaces getBookingsByHostId)
    PageResponse<BookingDTO> filterHostBookings(Integer hostId, List<String> statusNames, Pageable pageable);
    
    // Filter and sort bookings by user
    PageResponse<BookingDTO> filterUserBookings(Integer userId, List<String> statusNames, Pageable pageable);

    // Booking approval/rejection by host
    ApprovalPreviewDTO previewApproval(Integer hostId, Integer bookingId);
    BookingDTO approveBooking(Integer hostId, Integer bookingId);
    BookingDTO rejectBooking(Integer hostId, Integer bookingId, String reason);
    
    // Get bookings for a specific property within a date range
    List<BookingDTO> getPropertyBookingsInDateRange(Integer hostId, Integer propertyId, 
                                                     String startDate, String endDate);
    
    // Paginated versions
    PageResponse<BookingDTO> getBookingsByPropertyId(Integer propertyId, Pageable pageable);
    PageResponse<BookingDTO> getAllBookings(Pageable pageable);
    PageResponse<BookingDTO> getPropertyBookingsInDateRange(Integer hostId, Integer propertyId, 
                                                            String startDate, String endDate, Pageable pageable);
}

