package com.Cybersoft.Final_Capstone.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for property search and filtering.
 *
 * Note: "Search" functionality is intentionally limited to property name only (the
 * 'name' field below). All other fields are considered filters (type, location, price range,
 * amenities, etc.). This keeps the semantics clear: use 'name' for text search and the
 * other parameters to narrow results. Both GET (query params) and POST (request body)
 * search endpoints should map to this DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertySearchRequest {

    // Property type filter (1=VILLA, 2=APARTMENT, etc.)
    private Integer type;

    // Location filters
    private String city;
    private String location;

    // Price range filters
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Room/guest capacity filters
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxInfants;
    private Integer maxPets;

    // Feature filters (list of IDs)
    private List<Integer> amenities;
    private List<Integer> facilities;

    // Search by property name ONLY (partial match, case-insensitive)
    // This field is the only free-text search parameter. Do not put other search
    // semantics here — use the explicit filter fields above.
    private String name;

    // Pagination & sorting
    // NOTE: page is 1-based for frontend convenience; we'll convert to 0-based internally.
    private Integer page = 1;
    private Integer size = 10;
    private String sortBy = "overallRating"; // field name: overallRating, pricePerNight, createdAt, etc.
    private String sortDirection = "DESC"; // ASC or DESC
}
