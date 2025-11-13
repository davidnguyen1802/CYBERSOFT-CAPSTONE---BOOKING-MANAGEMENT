package com.Cybersoft.Final_Capstone.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for building Pageable objects with validation.
 * Centralizes pagination and sorting logic to ensure consistency across the application.
 */
public class PageableBuilder {

    // Whitelist of allowed sort fields for Property entity
    private static final Set<String> ALLOWED_PROPERTY_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "overallRating", "pricePerNight", "createdAt", "updatedAt",
            "propertyName", "numberOfBedrooms", "numberOfBathrooms"
    ));

    // Whitelist of allowed sort fields for Booking entity
    private static final Set<String> ALLOWED_BOOKING_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "createdAt", "updatedAt", "checkIn", "checkOut", "totalPrice"
    ));

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10; // changed default page size to 10
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "overallRating";
    private static final String DEFAULT_BOOKING_SORT_FIELD = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "DESC";

    /**
     * Build Pageable from individual parameters with validation.
     * @param page Page number (0-based), defaults to 0 if null or negative
     * @param size Page size, defaults to 10, max 100
     * @param sortBy Sort field, defaults to "overallRating", must be in whitelist
     * @param sortDirection Sort direction (ASC/DESC), defaults to DESC
     * @return Validated Pageable object
     */
    public static Pageable buildPropertyPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        // Validate and normalize page
        int validPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;

        // Validate and normalize size
        int validSize = DEFAULT_SIZE;
        if (size != null && size > 0) {
            validSize = Math.min(size, MAX_SIZE);
        }

        // Validate and normalize sort field
        String validSortBy = DEFAULT_SORT_FIELD;
        if (sortBy != null && !sortBy.trim().isEmpty() && ALLOWED_PROPERTY_SORT_FIELDS.contains(sortBy)) {
            validSortBy = sortBy;
        }

        // Validate and normalize sort direction
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.trim().equalsIgnoreCase("ASC")) {
            direction = Sort.Direction.ASC;
        }

        return PageRequest.of(validPage, validSize, Sort.by(direction, validSortBy));
    }

    /**
     * Build Pageable from Spring's "sort" format: "field,direction"
     * Example: "pricePerNight,asc" or "overallRating,desc"
     */
    public static Pageable buildFromSortString(Integer page, Integer size, String sort) {
        String sortBy = DEFAULT_SORT_FIELD;
        String sortDirection = DEFAULT_SORT_DIRECTION;

        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length > 0 && ALLOWED_PROPERTY_SORT_FIELDS.contains(parts[0].trim())) {
                sortBy = parts[0].trim();
            }
            if (parts.length > 1) {
                sortDirection = parts[1].trim();
            }
        }

        return buildPropertyPageable(page, size, sortBy, sortDirection);
    }

    /**
     * Build Pageable for Booking entity with validation.
     * @param page Page number (0-based), defaults to 0 if null or negative
     * @param size Page size, defaults to 10, max 100
     * @param sortBy Sort field, defaults to "createdAt", must be in whitelist
     * @param sortDirection Sort direction (ASC/DESC), defaults to DESC
     * @return Validated Pageable object for Booking
     */
    public static Pageable buildBookingPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        // Validate and normalize page
        int validPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;

        // Validate and normalize size
        int validSize = DEFAULT_SIZE;
        if (size != null && size > 0) {
            validSize = Math.min(size, MAX_SIZE);
        }

        // Validate and normalize sort field
        String validSortBy = DEFAULT_BOOKING_SORT_FIELD;
        if (sortBy != null && !sortBy.trim().isEmpty() && ALLOWED_BOOKING_SORT_FIELDS.contains(sortBy)) {
            validSortBy = sortBy;
        }

        // Validate and normalize sort direction
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.trim().equalsIgnoreCase("ASC")) {
            direction = Sort.Direction.ASC;
        }

        return PageRequest.of(validPage, validSize, Sort.by(direction, validSortBy));
    }
}
