package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.payload.request.BookingRequest;

import java.util.List;

public interface BookingService {
    BookingDTO createBooking(BookingRequest bookingRequest);
    BookingDTO getBookingById(Integer id);
    List<BookingDTO> getBookingsByUserId(Integer userId);
    List<BookingDTO> getBookingsByPropertyId(Integer propertyId);
    List<BookingDTO> getAllBookings();
    BookingDTO updateBookingStatus(Integer id, String status);
    BookingDTO cancelBooking(Integer id);
    void deleteBooking(Integer id);
    boolean isPropertyAvailable(Integer propertyId, String checkIn, String checkOut);
}

