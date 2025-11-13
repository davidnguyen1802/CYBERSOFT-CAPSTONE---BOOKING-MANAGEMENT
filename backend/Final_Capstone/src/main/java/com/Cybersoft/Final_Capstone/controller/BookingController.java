package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.mapper.BookingMapper;
import com.Cybersoft.Final_Capstone.payload.request.BookingRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.service.BookingService;
import com.Cybersoft.Final_Capstone.util.PageableBuilder;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * Create a new booking request (PENDING status, waiting for host approval)
     * @param bookingRequest Booking details
     * @return BookingDTO with PENDING status
     */
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequest bookingRequest) {
        // Create booking with PENDING status
        Booking booking = bookingService.createBooking(bookingRequest);
        BookingDTO bookingDTO = BookingMapper.toDTO(booking);

        BaseResponse response = new BaseResponse();
        response.setCode(HttpStatus.CREATED.value());
        response.setMessage("Booking request submitted successfully. Waiting for host approval.");
        response.setData(bookingDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get booking by ID
     */
    @PreAuthorize("hasRole('ADMIN')")
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
     * Get all bookings for a specific user (paginated, filterable by status)
     * Always sorted by createdAt DESC
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUserId(
            @PathVariable Integer userId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 9;

        // Always sort by createdAt DESC
        Pageable pageable = PageableBuilder.buildBookingPageable(p, s, "createdAt", "DESC");

        PageResponse<BookingDTO> pageResponse = bookingService.filterUserBookings(userId, status, pageable);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("User bookings retrieved successfully");
        response.setData(pageResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all bookings for a specific property (paginated)
     */
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getBookingsByPropertyId(
            @PathVariable Integer propertyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 9;
        String sb = sortBy != null ? sortBy : "createdAt";
        String sd = sortDirection != null ? sortDirection : "DESC";
        Pageable pageable = PageableBuilder.buildBookingPageable(p, s, sb, sd);

        PageResponse<BookingDTO> pageResponse = bookingService.getBookingsByPropertyId(propertyId, pageable);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property bookings retrieved successfully");
        response.setData(pageResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all bookings (admin only) (paginated)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllBookings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 9;
        String sb = sortBy != null ? sortBy : "createdAt";
        String sd = sortDirection != null ? sortDirection : "DESC";
        Pageable pageable = PageableBuilder.buildBookingPageable(p, s, sb, sd);

        PageResponse<BookingDTO> pageResponse = bookingService.getAllBookings(pageable);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("All bookings retrieved successfully");
        response.setData(pageResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Update booking status --> For Host
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
     * Cancel a booking --> For Guest
     * PUT /bookings/{id}/cancel?reason={reason}
     * 
     * Guest can cancel booking at any stage (PENDING/CONFIRMED/PAID)
     * Note: No refund if already PAID
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason) {
        
        BookingDTO booking = bookingService.cancelBooking(id, reason);

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

    /**
     * Filter and sort bookings for a host (properties owned by host)
     * GET /bookings/host/{hostId}/filter?status=PENDING,CONFIRMED&page=0&size=10&sortBy=createdAt&sortDirection=DESC
     * 
     * Supports:
     * - Multiple status filter with OR logic (e.g., status=PENDING,CONFIRMED)
     * - Sorting by: createdAt, updatedAt, checkIn, checkOut, totalPrice
     * - Pagination with 0-based page indexing
     */
    @GetMapping("/host/{hostId}/filter")
    public ResponseEntity<?> filterHostBookings(
            @PathVariable Integer hostId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        // Build pageable with validation
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 10;
        String sb = sortBy != null ? sortBy : "createdAt";
        String sd = sortDirection != null ? sortDirection : "DESC";
        Pageable pageable = PageableBuilder.buildBookingPageable(p, s, sb, sd);

        // Filter bookings
        PageResponse<BookingDTO> pageResponse = bookingService.filterHostBookings(hostId, status, pageable);

        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Filter host bookings successfully");
        response.setData(pageResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Preview approval - see which bookings will be auto-rejected
     * GET /bookings/{bookingId}/approve/preview?hostId={hostId}
     */
    @GetMapping("/{bookingId}/approve/preview")
    public ResponseEntity<?> previewApproval(
            @PathVariable Integer bookingId,
            @RequestParam Integer hostId) {
        
        var previewData = bookingService.previewApproval(hostId, bookingId);
        
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Preview approval conflicts");
        response.setData(previewData);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a booking (host only)
     * PUT /bookings/{bookingId}/approve?hostId={hostId}
     * 
     * Host approves a pending booking for their property
     */
    @PutMapping("/{bookingId}/approve")
    public ResponseEntity<?> approveBooking(
            @PathVariable Integer bookingId,
            @RequestParam Integer hostId) {
        
        BookingDTO booking = bookingService.approveBooking(hostId, bookingId);
        
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking approved successfully");
        response.setData(booking);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a booking (host only)
     * PUT /bookings/{bookingId}/reject?hostId={hostId}&reason={reason}
     * 
     * Host rejects a pending booking for their property
     */
    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<?> rejectBooking(
            @PathVariable Integer bookingId,
            @RequestParam Integer hostId,
            @RequestParam(required = false) String reason) {
        
        BookingDTO booking = bookingService.rejectBooking(hostId, bookingId, reason);
        
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking rejected successfully");
        response.setData(booking);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get bookings for a specific property within a date range (paginated)
     * GET /bookings/property/{propertyId}/date-range?hostId={hostId}&startDate={startDate}&endDate={endDate}
     * 
     * Retrieves all active bookings for a property within the specified date range
     */
    @GetMapping("/property/{propertyId}/date-range")
    public ResponseEntity<?> getPropertyBookingsInDateRange(
            @PathVariable Integer propertyId,
            @RequestParam Integer hostId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 9;
        String sb = sortBy != null ? sortBy : "checkIn";
        String sd = sortDirection != null ? sortDirection : "ASC";
        Pageable pageable = PageableBuilder.buildBookingPageable(p, s, sb, sd);

        PageResponse<BookingDTO> pageResponse = bookingService.getPropertyBookingsInDateRange(
                hostId, propertyId, startDate, endDate, pageable);
        
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property bookings in date range retrieved successfully");
        response.setData(pageResponse);
        
        return ResponseEntity.ok(response);
    }
}
