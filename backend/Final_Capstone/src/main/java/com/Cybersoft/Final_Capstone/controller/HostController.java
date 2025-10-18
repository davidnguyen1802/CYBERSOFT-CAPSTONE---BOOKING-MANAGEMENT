package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import com.Cybersoft.Final_Capstone.dto.*;
import com.Cybersoft.Final_Capstone.payload.request.PropertyRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.HostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for HOST-specific operations
 * Hosts can manage properties, track bookings, and view guest information
 * 
 * ⚠️ SECURITY: ALL endpoints in this controller require ROLE_HOST
 */
@RestController
@RequestMapping("/host")
@PreAuthorize("hasRole('HOST')")  // Apply to all methods in this controller
public class HostController {

    @Autowired
    private HostService hostService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * Private helper method to check if the current user can access the requested hostId
     * @param hostId The host ID from the URL path
     * @return ResponseEntity with 403 error if unauthorized, null if authorized
     */
    private ResponseEntity<?> checkHostAuthorization(Integer hostId) {
        if (!securityUtil.isAuthorizedToAccessUser(hostId)) {
            BaseResponse response = new BaseResponse();
            response.setCode(403);
            response.setMessage("Access denied. You can only access your own data.");
            response.setData(null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        return null; // Authorized
    }

    // ==================== Host Profile ====================
    
    /**
     * Get host profile information
     * GET /host/{hostId}/profile
     * 
     * ⚠️ SECURITY: User can only access their own profile
     */
    @GetMapping("/{hostId}/profile")
    public ResponseEntity<?> getHostProfile(@PathVariable Integer hostId) {
        // Authorization check: User can only access their own profile
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        HostDTO hostDTO = hostService.getHostProfile(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get host profile successfully");
        response.setData(hostDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Get host statistics (total properties, bookings, revenue, ratings)
     * GET /host/{hostId}/statistics
     * 
     * ⚠️ SECURITY: User can only access their own statistics
     */
    @GetMapping("/{hostId}/statistics")
    public ResponseEntity<?> getHostStatistics(@PathVariable Integer hostId) {
        // Authorization check: User can only access their own statistics
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        HostStatisticsDTO stats = hostService.getHostStatistics(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get host statistics successfully");
        response.setData(stats);
        return ResponseEntity.ok(response);
    }

    // ==================== Property Management ====================
    
    /**
     * Get all properties owned by the host
     * GET /host/{hostId}/properties
     * 
     * ⚠️ SECURITY: User can only access their own properties
     */
    @GetMapping("/{hostId}/properties")
    public ResponseEntity<?> getHostProperties(@PathVariable Integer hostId) {
        // Authorization check: User can only access their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<PropertyDTO> properties = hostService.getHostProperties(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get host properties successfully");
        response.setData(properties);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a new property
     * POST /host/{hostId}/properties
     * 
     * ⚠️ SECURITY: User can only add properties to their own account
     */
    @PostMapping("/{hostId}/properties")
    public ResponseEntity<?> addProperty(
            @PathVariable Integer hostId,
            @ModelAttribute PropertyRequest propertyRequest) {
        // Authorization check: User can only add properties to their own account
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        PropertyDTO property = hostService.addProperty(hostId, propertyRequest);
        BaseResponse response = new BaseResponse();
        response.setCode(201);
        response.setMessage("Property added successfully");
        response.setData(property);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a property
     * PUT /host/{hostId}/properties/{propertyId}
     * 
     * ⚠️ SECURITY: User can only update their own properties
     */
    @PutMapping("/{hostId}/properties/{propertyId}")
    public ResponseEntity<?> updateProperty(
            @PathVariable Integer hostId,
            @PathVariable Integer propertyId,
            @ModelAttribute PropertyRequest propertyRequest) {
        // Authorization check: User can only update their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        PropertyDTO property = hostService.updateProperty(hostId, propertyId, propertyRequest);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property updated successfully");
        response.setData(property);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a property
     * DELETE /host/{hostId}/properties/{propertyId}
     * 
     * ⚠️ SECURITY: User can only delete their own properties
     */
    @DeleteMapping("/{hostId}/properties/{propertyId}")
    public ResponseEntity<?> deleteProperty(
            @PathVariable Integer hostId,
            @PathVariable Integer propertyId) {
        // Authorization check: User can only delete their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        hostService.deleteProperty(hostId, propertyId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Property deleted successfully");
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    // ==================== Booking Management ====================
    
    /**
     * Get all bookings for host's properties
     * GET /host/{hostId}/bookings
     * 
     * ⚠️ SECURITY: User can only access their own bookings
     */
    @GetMapping("/{hostId}/bookings")
    public ResponseEntity<?> getHostBookings(@PathVariable Integer hostId) {
        // Authorization check: User can only access their own bookings
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<BookingDTO> bookings = hostService.getHostBookings(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get host bookings successfully");
        response.setData(bookings);
        return ResponseEntity.ok(response);
    }

    /**
     * Get bookings by status (Pending, Confirmed, Completed, Cancelled, etc.)
     * GET /host/{hostId}/bookings/status/{statusName}
     * 
     * ⚠️ SECURITY: User can only access their own bookings
     */
    @GetMapping("/{hostId}/bookings/status/{statusName}")
    public ResponseEntity<?> getHostBookingsByStatus(
            @PathVariable Integer hostId,
            @PathVariable String statusName) {
        // Authorization check: User can only access their own bookings
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<BookingDTO> bookings = hostService.getHostBookingsByStatus(hostId, statusName);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get bookings by status successfully");
        response.setData(bookings);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all bookings for a specific property
     * GET /host/{hostId}/properties/{propertyId}/bookings
     * 
     * ⚠️ SECURITY: User can only access bookings for their own properties
     */
    @GetMapping("/{hostId}/properties/{propertyId}/bookings")
    public ResponseEntity<?> getPropertyBookings(
            @PathVariable Integer hostId,
            @PathVariable Integer propertyId) {
        // Authorization check: User can only access bookings for their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<BookingDTO> bookings = hostService.getPropertyBookings(hostId, propertyId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get property bookings successfully");
        response.setData(bookings);
        return ResponseEntity.ok(response);
    }

    /**
     * Get bookings for a property within a date range
     * GET /host/{hostId}/properties/{propertyId}/bookings/date-range
     * Params: startDate, endDate (format: yyyy-MM-dd'T'HH:mm:ss)
     * 
     * ⚠️ SECURITY: User can only access bookings for their own properties
     */
    @GetMapping("/{hostId}/properties/{propertyId}/bookings/date-range")
    public ResponseEntity<?> getPropertyBookingsInDateRange(
            @PathVariable Integer hostId,
            @PathVariable Integer propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Authorization check: User can only access bookings for their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<BookingDTO> bookings = hostService.getPropertyBookingsInDateRange(
                hostId, propertyId, startDate, endDate);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get property bookings in date range successfully");
        response.setData(bookings);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a booking
     * PUT /host/{hostId}/bookings/{bookingId}/approve
     * 
     * ⚠️ SECURITY: User can only approve bookings for their own properties
     */
    @PutMapping("/{hostId}/bookings/{bookingId}/approve")
    public ResponseEntity<?> approveBooking(
            @PathVariable Integer hostId,
            @PathVariable Integer bookingId) {
        // Authorization check: User can only approve bookings for their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        BookingDTO booking = hostService.approveBooking(hostId, bookingId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking approved successfully");
        response.setData(booking);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a booking
     * PUT /host/{hostId}/bookings/{bookingId}/reject
     * 
     * ⚠️ SECURITY: User can only reject bookings for their own properties
     */
    @PutMapping("/{hostId}/bookings/{bookingId}/reject")
    public ResponseEntity<?> rejectBooking(
            @PathVariable Integer hostId,
            @PathVariable Integer bookingId,
            @RequestParam(required = false) String reason) {
        // Authorization check: User can only reject bookings for their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        BookingDTO booking = hostService.rejectBooking(hostId, bookingId, reason);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Booking rejected successfully");
        response.setData(booking);
        return ResponseEntity.ok(response);
    }

    // ==================== Guest Tracking ====================
    
    /**
     * Get current guests (checked-in)
     * GET /host/{hostId}/guests/current
     * 
     * ⚠️ SECURITY: User can only access their own guest information
     */
    @GetMapping("/{hostId}/guests/current")
    public ResponseEntity<?> getCurrentGuests(@PathVariable Integer hostId) {
        // Authorization check: User can only access their own guest information
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<GuestInfoDTO> guests = hostService.getCurrentGuests(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get current guests successfully");
        response.setData(guests);
        return ResponseEntity.ok(response);
    }

    /**
     * Get upcoming guests (confirmed but not yet checked-in)
     * GET /host/{hostId}/guests/upcoming
     * 
     * ⚠️ SECURITY: User can only access their own guest information
     */
    @GetMapping("/{hostId}/guests/upcoming")
    public ResponseEntity<?> getUpcomingGuests(@PathVariable Integer hostId) {
        // Authorization check: User can only access their own guest information
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<GuestInfoDTO> guests = hostService.getUpcomingGuests(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get upcoming guests successfully");
        response.setData(guests);
        return ResponseEntity.ok(response);
    }

    /**
     * Get past guests (checked-out)
     * GET /host/{hostId}/guests/past
     * 
     * ⚠️ SECURITY: User can only access their own guest information
     */
    @GetMapping("/{hostId}/guests/past")
    public ResponseEntity<?> getPastGuests(@PathVariable Integer hostId) {
        // Authorization check: User can only access their own guest information
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<GuestInfoDTO> guests = hostService.getPastGuests(hostId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get past guests successfully");
        response.setData(guests);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all guests for a specific property
     * GET /host/{hostId}/properties/{propertyId}/guests
     * 
     * ⚠️ SECURITY: User can only access guests for their own properties
     */
    @GetMapping("/{hostId}/properties/{propertyId}/guests")
    public ResponseEntity<?> getGuestsForProperty(
            @PathVariable Integer hostId,
            @PathVariable Integer propertyId) {
        // Authorization check: User can only access guests for their own properties
        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
        if (authCheck != null) return authCheck;

        List<GuestInfoDTO> guests = hostService.getGuestsForProperty(hostId, propertyId);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Get guests for property successfully");
        response.setData(guests);
        return ResponseEntity.ok(response);
    }
}
