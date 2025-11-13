package com.Cybersoft.Final_Capstone.specification;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for filtering Booking entities.
 * Supports filtering by host (properties owned by host) and multiple status values.
 */
public class BookingSpecification {

    /**
     * Filter bookings by host and optional status list.
     * 
     * @param hostId Required - filter bookings for properties owned by this host
     * @param statusNames Optional - list of status names to filter by (OR logic)
     * @return Specification for filtering bookings
     */
    public static Specification<Booking> filterBookings(
            Integer hostId,
            List<String> statusNames
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by host (properties owned by this host)
            if (hostId != null) {
                Join<Object, Object> propertyJoin = root.join("property");
                predicates.add(criteriaBuilder.equal(propertyJoin.get("host").get("id"), hostId));
            }

            // Filter by status (OR logic for multiple status values)
            if (statusNames != null && !statusNames.isEmpty()) {
                List<Predicate> statusPredicates = new ArrayList<>();
                for (String statusName : statusNames) {
                    statusPredicates.add(
                        criteriaBuilder.equal(
                            criteriaBuilder.lower(root.get("status").get("name")),
                            statusName.trim().toLowerCase()
                        )
                    );
                }
                // Combine status predicates with OR
                predicates.add(criteriaBuilder.or(statusPredicates.toArray(new Predicate[0])));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}


