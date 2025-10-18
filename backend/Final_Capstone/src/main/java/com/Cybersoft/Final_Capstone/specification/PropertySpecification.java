package com.Cybersoft.Final_Capstone.specification;

import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Enum.PropertyType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PropertySpecification {

    public static Specification<Property> filterProperties(
            Integer type,
            String city,
            String location,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer bedrooms,
            Integer bathrooms,
            Integer maxAdults,
            Integer maxChildren,
            Integer maxInfants,
            Integer maxPets,
            List<Integer> amenities,
            List<Integer> facilities,
            String name // Add name search parameter
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by AVAILABLE status
            predicates.add(criteriaBuilder.equal(root.get("status").get("name"), "AVAILABLE"));

            // Filter by property type
            if (type != null) {
                try {
                    PropertyType propertyType = PropertyType.fromValue(type);
                    predicates.add(criteriaBuilder.equal(root.get("propertyType"), propertyType));
                } catch (IllegalArgumentException e) {
                    // Invalid type - skip this filter
                }
            }

            // Filter by city
            if (city != null && !city.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("location").get("city").get("cityName"), city));
            }

            // Filter by location
            if (location != null && !location.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("location").get("locationName"), location));
            }

            // Filter by price range
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("pricePerNight"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("pricePerNight"), maxPrice));
            }

            // Filter by bedrooms
            if (bedrooms != null) {
                predicates.add(criteriaBuilder.equal(root.get("numberOfBedrooms"), bedrooms));
            }

            // Filter by bathrooms
            if (bathrooms != null) {
                predicates.add(criteriaBuilder.equal(root.get("numberOfBathrooms"), bathrooms));
            }

            // Filter by max adults
            if (maxAdults != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("maxAdults"), maxAdults));
            }

            // Filter by max children
            if (maxChildren != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("maxChildren"), maxChildren));
            }

            // Filter by max infants
            if (maxInfants != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("maxInfants"), maxInfants));
            }

            // Filter by max pets
            if (maxPets != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("maxPets"), maxPets));
            }

            // Filter by amenities (property must have all specified amenities)
            if (amenities != null && !amenities.isEmpty()) {
                Join<Object, Object> amenitiesJoin = root.join("amenities", JoinType.LEFT);
                predicates.add(amenitiesJoin.get("id").in(amenities));
            }

            // Filter by facilities (property must have all specified facilities)
            if (facilities != null && !facilities.isEmpty()) {
                Join<Object, Object> facilitiesJoin = root.join("facilities", JoinType.LEFT);
                predicates.add(facilitiesJoin.get("id").in(facilities));
            }

            // Filter by property name (partial match, case-insensitive)
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("propertyName")),
                        "%" + name.trim().toLowerCase() + "%"
                ));
            }

            // Make query distinct to avoid duplicates from joins
            if (query != null) {
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
