package com.Cybersoft.Final_Capstone.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for booking filtering and sorting.
 * 
 * Filter bookings by host (properties owned by host) and optional status filter.
 * Supports multiple status values with OR logic (e.g., status=PENDING,CONFIRMED).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingFilterRequest {

    // Filter by host (required - must filter bookings for properties owned by this host)
    private Integer hostId;

    // Filter by status (optional, multiple values with OR logic)
    // Example: ["PENDING", "CONFIRMED"] means booking.status = "PENDING" OR "CONFIRMED"
    private List<String> status;

    // Pagination & sorting
    // NOTE: page is 0-based for backend consistency
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "createdAt"; // field name: createdAt, checkIn, checkOut, totalPrice, etc.
    private String sortDirection = "DESC"; // ASC or DESC
}


