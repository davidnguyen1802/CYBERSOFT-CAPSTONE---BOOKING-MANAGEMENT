package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer>, JpaSpecificationExecutor<Booking> {

    // Find bookings by user
    List<Booking> findByUserId(Integer userId);

    // Find bookings by user and status
    List<Booking> findByUserIdAndStatus_Name(Integer userId, String statusName);

    // Find bookings by property
    List<Booking> findByPropertyId(Integer propertyId);

    // Find bookings by property and status
    List<Booking> findByPropertyIdAndStatus_Name(Integer propertyId, String statusName);

    // Check if property is available for the given date range
    // Only count CONFIRMED and PAID bookings (PENDING doesn't block availability)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status.name IN ('Confirmed', 'Paid') " +
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

    // Find booking by ID with all relationships eagerly loaded
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.property p " +
            "LEFT JOIN FETCH p.host " +
            "LEFT JOIN FETCH b.status " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Integer id);

    // ==================== NEW: BOOKING APPROVAL FLOW ====================

    /**
     * Count CONFIRMED or PAID bookings that overlap with the given dates
     * Used to check conflicts when creating new bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status.name IN ('CONFIRMED', 'PAID') " +
            "AND ((b.checkIn < :checkOut AND b.checkOut > :checkIn))")
    Long countConflictingConfirmedBookings(@Param("propertyId") Integer propertyId,
                                           @Param("checkIn") LocalDateTime checkIn,
                                           @Param("checkOut") LocalDateTime checkOut);

    /**
     * Find PENDING bookings that overlap with the given dates (same property)
     * Used to preview which bookings will be auto-rejected when approving
     */
    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status.name = 'PENDING' " +
            "AND b.id != :excludeBookingId " +
            "AND ((b.checkIn < :checkOut AND b.checkOut > :checkIn))")
    List<Booking> findConflictingBookings(@Param("propertyId") Integer propertyId,
                                          @Param("checkIn") LocalDateTime checkIn,
                                          @Param("checkOut") LocalDateTime checkOut,
                                          @Param("excludeBookingId") Integer excludeBookingId);

    /**
     * Bulk update: Auto-reject conflicting PENDING bookings
     * Called after approving a booking
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE booking b " +
            "JOIN status s ON b.id_status = s.id " +
            "SET b.id_status = 11, " +
            "    b.cancelled_at = NOW(), " +
            "    b.cancelled_by = 'system', " +
            "    b.cancel_reason = :reason " +
            "WHERE b.property_id = :propertyId " +
            "AND s.name = 'PENDING' " +
            "AND b.id != :excludeBookingId " +
            "AND ((b.check_in < :checkOut AND b.check_out > :checkIn))",
            nativeQuery = true)
    int autoRejectConflicts(@Param("propertyId") Integer propertyId,
                            @Param("checkIn") LocalDateTime checkIn,
                            @Param("checkOut") LocalDateTime checkOut,
                            @Param("excludeBookingId") Integer excludeBookingId,
                            @Param("reason") String reason);

    /**
     * Find booking by ID with FOR UPDATE lock (pessimistic locking)
     * Used when approving booking to prevent race conditions
     */
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findByIdForUpdate(@Param("id") Integer id);

    /**
     * Find CONFIRMED bookings where payment deadline has passed
     * Used by scheduled job to auto-cancel expired bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.status.name = 'CONFIRMED' " +
            "AND b.confirmedAt IS NOT NULL " +
            "AND b.confirmedAt < :deadline")
    List<Booking> findExpiredUnpaid(@Param("deadline") LocalDateTime deadline);

    /**
     * Find PAID bookings where check-out date has passed
     * Used by scheduled job to mark bookings as COMPLETED
     */
    @Query("SELECT b FROM Booking b WHERE b.status.name = 'PAID' " +
            "AND b.checkOut < :now")
    List<Booking> findToComplete(@Param("now") LocalDateTime now);
}
