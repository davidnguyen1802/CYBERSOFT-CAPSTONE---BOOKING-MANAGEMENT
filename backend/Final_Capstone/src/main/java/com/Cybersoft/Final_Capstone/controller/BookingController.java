package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.payload.request.BookingRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;


    /**
     * Create a new booking with optional payment
     * @param bookingRequest Booking details
     * @param createPayment Optional parameter to create payment immediately (default: true)
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest bookingRequest,
            @RequestParam(required = false, defaultValue = "true") boolean createPayment) {
        
        // Create booking
        BookingDTO booking = bookingService.createBooking(bookingRequest);

        // Combine booking and payment info
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("booking", booking);

        BaseResponse response = new BaseResponse();
        response.setCode(201);
        response.setMessage("Booking created successfully.");
        response.setData(responseData);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get booking by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(@PathVariable Integer id) {
        BookingDTO booking = bookingService.getBookingById(id);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking retrieved successfully");
        response.setData(booking);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all bookings for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUserId(@PathVariable Integer userId) {
        List<BookingDTO> bookings = bookingService.getBookingsByUserId(userId);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User bookings retrieved successfully");
        response.setData(bookings);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all bookings for a specific property
     */
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getBookingsByPropertyId(@PathVariable Integer propertyId) {
        List<BookingDTO> bookings = bookingService.getBookingsByPropertyId(propertyId);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property bookings retrieved successfully");
        response.setData(bookings);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all bookings (admin only)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllBookings() {
        List<BookingDTO> bookings = bookingService.getAllBookings();

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("All bookings retrieved successfully");
        response.setData(bookings);

        return ResponseEntity.ok(response);
    }

    /**
     * Update booking status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable Integer id,
            @RequestParam String status) {
        BookingDTO booking = bookingService.updateBookingStatus(id, status);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking status updated successfully");
        response.setData(booking);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a booking
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Integer id) {
        BookingDTO booking = bookingService.cancelBooking(id);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking cancelled successfully");
        response.setData(booking);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a booking (admin only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Integer id) {
        bookingService.deleteBooking(id);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking deleted successfully");
        response.setData(null);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if property is available for booking
     */
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam Integer propertyId,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {
        boolean isAvailable = bookingService.isPropertyAvailable(propertyId, checkIn, checkOut);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage(isAvailable ? "Property is available" : "Property is not available");
        response.setData(isAvailable);

        return ResponseEntity.ok(response);
    }
}
