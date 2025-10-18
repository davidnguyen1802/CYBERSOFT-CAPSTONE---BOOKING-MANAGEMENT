package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // Find bookings by user
    List<Booking> findByUserId(Integer userId);

    // Find bookings by user and status
    List<Booking> findByUserIdAndStatus_Name(Integer userId, String statusName);

    // Find bookings by property
    List<Booking> findByPropertyId(Integer propertyId);

    // Find bookings by property and status
    List<Booking> findByPropertyIdAndStatus_Name(Integer propertyId, String statusName);

    // Check if property is available for the given date range
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status.name NOT IN ('Cancelled', 'Rejected') " +
            "AND ((b.checkIn < :checkOut AND b.checkOut > :checkIn))")
    Long countConflictingBookings(@Param("propertyId") Integer propertyId,
                                  @Param("checkIn") LocalDateTime checkIn,
                                  @Param("checkOut") LocalDateTime checkOut);

    // Find all bookings for a specific property within a date range
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.checkIn >= :startDate AND b.checkOut <= :endDate " +
            "AND b.status.name NOT IN ('Cancelled', 'Rejected') " +
            "ORDER BY b.checkIn ASC")
    List<Booking> findBookingsByPropertyAndDateRange(@Param("propertyId") Integer propertyId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    // Find bookings by status
    List<Booking> findByStatus_Name(String statusName);

    // Find bookings by host (property owner)
    @Query("SELECT b FROM Booking b WHERE b.property.host.id = :hostId ORDER BY b.createdAt DESC")
    List<Booking> findByHostId(@Param("hostId") Integer hostId);
}

