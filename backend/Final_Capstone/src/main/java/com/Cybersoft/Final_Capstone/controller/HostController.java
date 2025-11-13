package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.components.SecurityUtil;
import com.Cybersoft.Final_Capstone.dto.*;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.service.HostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
     * ❌ REMOVED: getHostProfile()
     * 
     * ✅ USE INSTEAD: GET /users/me/details
     * This endpoint provides unified profile information for all users (GUEST, HOST, ADMIN)
     * and includes role-specific statistics automatically.
     */
    
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
     * ❌ REMOVED: getHostProperties()
     * 
     * ✅ USE INSTEAD: GET /property/host/{hostId}
     * Returns all properties owned by a specific host.
     */
    
    /**
     * Add a new property
     * POST /host/{hostId}/properties
     * 
     * ⚠️ SECURITY: User can only add properties to their own account
     */
//    @PostMapping("/{hostId}/properties")
//    public ResponseEntity<?> addProperty(
//            @PathVariable Integer hostId,
//            @ModelAttribute PropertyRequest propertyRequest) {
//        // Authorization check: User can only add properties to their own account
//        ResponseEntity<?> authCheck = checkHostAuthorization(hostId);
//        if (authCheck != null) return authCheck;
//
//        PropertyDTO property = hostService.addProperty(hostId, propertyRequest);
//        BaseResponse response = new BaseResponse();
//        response.setCode(201);
//        response.setMessage("Property added successfully");
//        response.setData(property);
//        return ResponseEntity.ok(response);
//    }

    /**
     * ❌ REMOVED: updateProperty()
     * 
     * ✅ USE INSTEAD: PUT /property/{id}
     * The PropertyController endpoint now includes automatic ownership verification.
     * Only the owner (or ADMIN) can update a property.
     */
    
    /**
     * ❌ REMOVED: deleteProperty()
     * 
     * ✅ USE INSTEAD: DELETE /property/{id}
     * The PropertyController endpoint now includes automatic ownership verification.
     * Only the owner (or ADMIN) can delete a property.
     */

    // ==================== Booking Management ====================
    
    /**
     * ⚠️ DEPRECATED: Use BookingController filter endpoint instead
     * GET /bookings/host/{hostId}/filter?status=PENDING,CONFIRMED&sortBy=createdAt&sortDirection=DESC
     * 
     * The new endpoint provides:
     * - Multiple status filtering with OR logic
     * - Sorting by createdAt, checkIn, checkOut, totalPrice
     * - Pagination support
     */

    /**
     * ❌ REMOVED: getPropertyBookingsInDateRange()
     * 
     * ✅ USE INSTEAD: GET /bookings/property/{propertyId}/date-range?hostId={hostId}&startDate={startDate}&endDate={endDate}
     * The new endpoint in BookingController provides:
     * - Same functionality
     * - Automatic ownership verification
     * - Cleaner URL structure
     * - Consistent with other booking endpoints
     */

    // ==================== REMOVED: Guest Tracking Endpoints ====================
    
    /**
     * ❌ REMOVED: getCurrentGuests()
     * 
     * ✅ USE INSTEAD: GET /bookings/host/{hostId}/filter?status=CONFIRMED
     * Then filter on frontend: bookings.filter(b => checkIn < now && checkOut > now)
     * 
     * Benefits:
     * - Pagination support (better performance)
     * - More flexible filtering
     * - Consistent API design
     */
    
    /**
     * ❌ REMOVED: getUpcomingGuests()
     * 
     * ✅ USE INSTEAD: GET /bookings/host/{hostId}/filter?status=PENDING,CONFIRMED
     * Then filter on frontend: bookings.filter(b => checkIn > now)
     * 
     * Benefits:
     * - Pagination support (better performance)
     * - More flexible filtering
     * - Consistent API design
     */
    
    /**
     * ❌ REMOVED: getPastGuests()
     * 
     * ✅ USE INSTEAD: GET /bookings/host/{hostId}/filter?status=COMPLETED
     * Then filter on frontend: bookings.filter(b => checkOut < now)
     * 
     * Benefits:
     * - Pagination support (better performance)
     * - More flexible filtering
     * - Consistent API design
     */
    
    /**
     * ❌ REMOVED: getGuestsForProperty()
     * 
     * ✅ USE INSTEAD: GET /bookings/property/{propertyId}
     * This endpoint already exists in BookingController!
     * 
     * Benefits:
     * - Simpler URL (no redundant hostId)
     * - Already implements ownership checks
     * - Consistent with other booking endpoints
     */
}
