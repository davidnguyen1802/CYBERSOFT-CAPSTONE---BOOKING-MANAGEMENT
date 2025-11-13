package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.Enum.DiscountType;
import com.Cybersoft.Final_Capstone.dto.ApprovalPreviewDTO;
import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.InvalidException;
import com.Cybersoft.Final_Capstone.mapper.BookingMapper;
import com.Cybersoft.Final_Capstone.payload.request.BookingRequest;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.repository.*;
import com.Cybersoft.Final_Capstone.service.BookingService;
import com.Cybersoft.Final_Capstone.specification.BookingSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingServiceImp implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private UserPromotionRepository userPromotionRepository;

    @Autowired
    private PromotionUsageRepository promotionUsageRepository;

    @Transactional
    @Override
    public Booking createBooking(BookingRequest bookingRequest) {
        // Validate check-in and check-out dates
        validateBookingDates(bookingRequest.getCheckIn(), bookingRequest.getCheckOut());

        UserAccount user = userAccountRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + bookingRequest.getUserId()));

        // Get property
        Property property = propertyRepository.findById(bookingRequest.getPropertyId())
                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + bookingRequest.getPropertyId()));

        // Validate property is active and available
        String statusName = property.getStatus().getName();
        if (!statusName.equals("AVAILABLE")) {
            throw new InvalidException("Property is not available for booking. Current status: " + statusName);
        }

        // Check if property capacity is sufficient
        validatePropertyCapacity(property, 
                               bookingRequest.getNumAdults(), 
                               bookingRequest.getNumChildren() != null ? bookingRequest.getNumChildren() : 0,
                               bookingRequest.getNum_infant() != null ? bookingRequest.getNum_infant() : 0);

        // ✅ NEW: Check conflicts với CONFIRMED/PAID bookings only
        // PENDING bookings được phép trùng dates (host sẽ chọn sau)
        Long conflictCount = bookingRepository.countConflictingConfirmedBookings(
                bookingRequest.getPropertyId(),
                bookingRequest.getCheckIn(),
                bookingRequest.getCheckOut()
        );

        if (conflictCount > 0) {
            throw new InvalidException("Property is already booked for the selected dates");
        }

        // Calculate BASE total price (BEFORE any promotion)
        BigDecimal originalPrice = calculateTotalPrice(
                property.getPricePerNight(),
                bookingRequest.getCheckIn(),
                bookingRequest.getCheckOut()
        );
        
        BigDecimal finalPrice = originalPrice; // Default = no discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        UserPromotion appliedUserPromotion = null; // Track if promotion was applied
        
        // ========== APPLY PROMOTION IMMEDIATELY IF PROVIDED ==========
        if (bookingRequest.getPromotionCode() != null && !bookingRequest.getPromotionCode().isEmpty()) {
            if (bookingRequest.getOriginalAmount() == null) {
                throw new InvalidException("Original amount is required when using promotion code");
            }
            
            // Verify FE calculation matches BE calculation (prevent price manipulation)
            if (bookingRequest.getOriginalAmount().compareTo(originalPrice) != 0) {
                log.warn("⚠️ Price mismatch: FE sent {} but BE calculated {}", 
                         bookingRequest.getOriginalAmount(), originalPrice);
                throw new InvalidException(String.format(
                    "Price mismatch detected. Expected: %s, Got: %s", 
                    originalPrice, bookingRequest.getOriginalAmount()
                ));
            }
            
            // Validate and lock promotion NOW
            appliedUserPromotion = validateAndLockPromotion(
                user, 
                bookingRequest.getPromotionCode(), 
                property.getId(),
                bookingRequest.getCheckIn(), 
                bookingRequest.getCheckOut()
            );
            
            // Calculate discount
            Promotion promotion = appliedUserPromotion.getPromotion();
            if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = originalPrice
                    .multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else { // FIXED_AMOUNT
                discountAmount = promotion.getDiscountValue();
            }
            
            // Apply max discount constraint
            if (promotion.getMaxDiscountAmount() != null && 
                discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discountAmount = promotion.getMaxDiscountAmount();
            }
            
            finalPrice = originalPrice.subtract(discountAmount);
            
            log.info("✅ Promotion '{}' applied: {} - {} = {}", 
                     bookingRequest.getPromotionCode(), 
                     originalPrice, discountAmount, finalPrice);
        }

        // Create booking with DISCOUNTED price
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setProperty(property);
        booking.setCheckIn(bookingRequest.getCheckIn());
        booking.setCheckOut(bookingRequest.getCheckOut());
        booking.setTotalPrice(finalPrice); // Save DISCOUNTED price
        booking.setNumAdults(bookingRequest.getNumAdults());
        booking.setNumChildren(bookingRequest.getNumChildren() != null ? bookingRequest.getNumChildren() : 0);
        booking.setNum_infant(bookingRequest.getNum_infant() != null ? bookingRequest.getNum_infant() : 0);
        booking.setNumPet(bookingRequest.getNum_pet() != null ? bookingRequest.getNum_pet() : 0);
        booking.setNotes(bookingRequest.getNotes());

        // ✅ Set status to PENDING (id = 6)
        booking.setStatus(new Status(6));

        Booking savedBooking = bookingRepository.save(booking);
        
        // ========== SAVE BOOKING-PROMOTION LINK IF PROMOTION WAS APPLIED ==========
        if (appliedUserPromotion != null) {
            PromotionUsageId usageId = new PromotionUsageId(
                appliedUserPromotion.getId(), 
                savedBooking.getId()
            );
            
            PromotionUsage usage = new PromotionUsage();
            usage.setId(usageId);
            usage.setUserPromotion(appliedUserPromotion);
            usage.setBooking(savedBooking);
            usage.setDiscountAmount(discountAmount);
            usage.setUsedAt(LocalDateTime.now());
            
            promotionUsageRepository.save(usage);
            
            // Mark UserPromotion as INACTIVE (consumed) and LOCKED
            appliedUserPromotion.setStatus(new Status(2)); // INACTIVE
            appliedUserPromotion.setIsLocked(true); // Lock to prevent reuse
            userPromotionRepository.save(appliedUserPromotion);
            
            // Increment promotion usage count
            Promotion promotion = appliedUserPromotion.getPromotion();
            int newTimesUsed = promotion.getTimesUsed() + 1;
            promotion.setTimesUsed(newTimesUsed);

            // ✅ CHECK IF THIS IS THE LAST USAGE (usage_limit reached)
            // usage_limit = -1 means UNLIMITED, null also means UNLIMITED
            if (promotion.getUsageLimit() != null &&
                promotion.getUsageLimit() != -1 &&
                newTimesUsed >= promotion.getUsageLimit()) {

                log.warn("🚨 Promotion '{}' reached usage limit ({}/{})",
                    promotion.getCode(), newTimesUsed, promotion.getUsageLimit());

                // 1. Set Promotion status to INACTIVE (no longer available)
                promotion.setStatus(new Status(2)); // INACTIVE

                log.info("❌ Promotion '{}' status set to INACTIVE (limit reached)", promotion.getCode());

                // 2. Deactivate ALL UserPromotions with this promotion that are still ACTIVE (unused)
                List<UserPromotion> activeUserPromotions = userPromotionRepository
                    .findByPromotionAndStatus(promotion, new Status(1)); // ACTIVE

                if (!activeUserPromotions.isEmpty()) {
                    for (UserPromotion up : activeUserPromotions) {
                        up.setStatus(new Status(2)); // INACTIVE
                        up.setIsLocked(true); // Also lock them
                    }
                    userPromotionRepository.saveAll(activeUserPromotions);

                    log.info("❌ Deactivated {} unused UserPromotions for promotion '{}'",
                        activeUserPromotions.size(), promotion.getCode());
                }
            }

            promotionRepository.save(promotion);
            
            log.info("💰 Promotion consumed: UserPromotion #{} → Booking #{}, Discount: {}, Usage: {}/{}",
                     appliedUserPromotion.getId(), savedBooking.getId(), discountAmount,
                     newTimesUsed,
                     promotion.getUsageLimit() != null && promotion.getUsageLimit() != -1
                         ? promotion.getUsageLimit() : "∞");
        }

        return savedBooking;
    }

    @Override
    public BookingDTO getBookingById(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + id));
        return BookingMapper.toDTO(booking);
    }

    @Transactional
    @Override
    public BookingDTO updateBookingStatus(Integer id, String statusName) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + id));

        // Map status name to ID
        Status status;
        switch (statusName.toUpperCase()) {
            case "ACTIVE":
                status = new Status(1);
                break;
            case "INACTIVE":
                status = new Status(2);
                break;
            case "DELETED":
                status = new Status(3);
                break;
            case "AVAILABLE":
                status = new Status(4);
                break;
            case "UNAVAILABLE":
                status = new Status(5);
                break;
            case "PENDING":
                status = new Status(6);
                break;
            case "CONFIRMED":
                status = new Status(7);
                break;
            case "PAID":
                status = new Status(8);
                break;
            case "COMPLETED":
                status = new Status(9);
                break;
            case "CANCELLED":
                status = new Status(10);
                break;
            case "REJECTED":
                status = new Status(11);
                break;
            case "USED":
                status = new Status(12);
                break;
            default:
                throw new DataNotFoundException("Status not found with name: " + statusName);
        }

        booking.setStatus(status);
        Booking updatedBooking = bookingRepository.save(booking);

        return BookingMapper.toDTO(updatedBooking);
    }

    @Override
    @Transactional
    public BookingDTO cancelBooking(Integer id, String cancelReason) {
        log.info("Guest cancelling booking {}", id);

        // 1. Find booking
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Booking not found"));

        // 2. Verify status (can cancel PENDING, CONFIRMED, or PAID)
        String status = booking.getStatus().getName();
        if (!List.of("PENDING", "CONFIRMED", "PAID").contains(status)) {
            throw new InvalidException("Cannot cancel booking with status: " + status);
        }

        // 3. Warning if already PAID (no refund)
        if ("PAID".equals(status)) {
            log.warn("⚠️ Cancelling PAID booking {}. No refund will be issued", id);
        }

        // 4. REFUND PROMOTION (if used and not yet paid)
        //    - If PENDING/CONFIRMED: Refund promotion (user can reuse)
        //    - If PAID: DO NOT refund (payment already completed)
        if (!status.equals("PAID")) {
            refundPromotionIfUsed(booking);
        }

        // 5. Update booking to CANCELLED
        booking.setStatus(new Status(10)); // CANCELLED
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy("guest");
        booking.setCancelReason(cancelReason != null ? cancelReason : "Cancelled by guest");

        Booking saved = bookingRepository.save(booking);

        log.info("✅ Booking {} cancelled by guest. Reason: {}", id, cancelReason);

        // TODO: Send notification to host
        // TODO: If PAID status, note in transaction that no refund is issued

        return BookingMapper.toDTO(saved);
    }

    /**
     * Refund promotion if it was used for this booking
     * Called when booking is cancelled (PENDING or CONFIRMED status only)
     * 
     * Logic:
     * 1. Find PromotionUsage for this booking
     * 2. Check if promotion is USED (payment successful) - DO NOT REFUND
     * 3. If INACTIVE (not yet paid):
     *    - Set UserPromotion back to ACTIVE
     *    - Decrement Promotion.timesUsed
     *    - Delete PromotionUsage record
     * 4. User can now reuse the same promotion
     */
    private void refundPromotionIfUsed(Booking booking) {
        try {
            // Find PromotionUsage for this booking
            Optional<PromotionUsage> usageOpt = promotionUsageRepository.findByBookingId(booking.getId());
            
            if (usageOpt.isEmpty()) {
                log.info("ℹ️ No promotion used for booking {}. Nothing to refund.", booking.getId());
                return;
            }
            
            PromotionUsage usage = usageOpt.get();
            UserPromotion userPromotion = usage.getUserPromotion();
            Promotion promotion = userPromotion.getPromotion();
            
            // ✅ NEW: Check if promotion is USED (payment was successful)
            // USED status means payment was completed, promotion cannot be refunded
            if ("USED".equals(userPromotion.getStatus().getName())) {
                log.warn("⚠️ Cannot refund USED promotion for booking {}. Payment was already completed.",
                    booking.getId());
                log.info("💡 Promotion {} (code: {}) remains USED and locked permanently.",
                    userPromotion.getId(), promotion.getCode());
                return; // Do NOT refund
            }

            // ✅ Only refund if status = INACTIVE (not yet paid)
            log.info("🔄 Refunding INACTIVE promotion {} for booking {}",
                promotion.getCode(), booking.getId());
            
            // 1. Set UserPromotion back to ACTIVE
            userPromotion.setStatus(new Status(1)); // ACTIVE
            userPromotion.setIsLocked(false); // Unlock if locked
            userPromotionRepository.save(userPromotion);
            
            log.info("✅ UserPromotion {} reactivated (status = ACTIVE)", userPromotion.getId());
            
            // 2. Decrement Promotion.timesUsed (undo the increment from createBooking)
            int oldTimesUsed = promotion.getTimesUsed();
            if (oldTimesUsed > 0) {
                int newTimesUsed = oldTimesUsed - 1;
                promotion.setTimesUsed(newTimesUsed);

                log.info("✅ Promotion {} times_used decremented: {} → {}",
                    promotion.getCode(), oldTimesUsed, newTimesUsed);

                // ✅ NEW: Check if Promotion was INACTIVE due to reaching limit
                // If so, reactivate Promotion AND all INACTIVE UserPromotions
                if ("INACTIVE".equals(promotion.getStatus().getName()) &&
                    promotion.getUsageLimit() != null &&
                    promotion.getUsageLimit() != -1) {

                    // Check if promotion was inactive because it reached limit
                    // If oldTimesUsed >= limit, it means we deactivated it
                    if (oldTimesUsed >= promotion.getUsageLimit()) {
                        log.warn("🔓 Promotion '{}' was INACTIVE (limit reached: {}/{}). Reactivating...",
                            promotion.getCode(), oldTimesUsed, promotion.getUsageLimit());

                        // 1. Reactivate Promotion itself
                        promotion.setStatus(new Status(1)); // ACTIVE

                        log.info("✅ Promotion '{}' status set back to ACTIVE", promotion.getCode());

                        // 2. Reactivate ALL INACTIVE UserPromotions that were auto-deactivated
                        // (Only those with isLocked = true, meaning they were deactivated by system, not expired)
                        List<UserPromotion> inactiveUserPromotions = userPromotionRepository
                            .findByPromotionAndStatus(promotion, new Status(2)); // INACTIVE

                        // Filter: only reactivate those that are locked (auto-deactivated by system)
                        // Don't reactivate expired or manually cancelled ones
                        List<UserPromotion> toReactivate = inactiveUserPromotions.stream()
                            .filter(up -> Boolean.TRUE.equals(up.getIsLocked()))
                            .filter(up -> up.getExpiresAt() == null || up.getExpiresAt().isAfter(LocalDateTime.now()))
                            .toList();

                        if (!toReactivate.isEmpty()) {
                            for (UserPromotion up : toReactivate) {
                                up.setStatus(new Status(1)); // ACTIVE
                                up.setIsLocked(false);
                            }
                            userPromotionRepository.saveAll(toReactivate);

                            log.info("✅ Reactivated {} UserPromotions that were auto-deactivated when limit was reached",
                                toReactivate.size());
                        }
                    }
                }

                promotionRepository.save(promotion);
            }
            
            // 3. Delete PromotionUsage record
            promotionUsageRepository.delete(usage);
            log.info("✅ PromotionUsage record deleted for booking {}", booking.getId());
            
            log.info("🎉 Promotion refund complete. User can reuse promotion.");
            
        } catch (Exception e) {
            log.error("❌ Error refunding promotion for booking {}: {}", 
                booking.getId(), e.getMessage(), e);
            // Don't throw - cancellation should still proceed even if refund fails
        }
    }

    @Transactional
    @Override
    public void deleteBooking(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + id));
        bookingRepository.delete(booking);
    }

    @Override
    public boolean isPropertyAvailable(Integer propertyId, String checkIn, String checkOut) {
        LocalDateTime checkInDate = LocalDateTime.parse(checkIn);
        LocalDateTime checkOutDate = LocalDateTime.parse(checkOut);

        Long conflictCount = bookingRepository.countConflictingBookings(
                propertyId,
                checkInDate,
                checkOutDate
        );

        return conflictCount == 0;
    }

    @Override
    public PageResponse<BookingDTO> filterHostBookings(Integer hostId, List<String> statusNames, Pageable pageable) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));

        // Build specification for filtering
        Specification<Booking> spec = BookingSpecification.filterBookings(hostId, statusNames);

        // Execute query with pagination
        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);

        // Use helper method to build response
        return buildBookingPageResponse(bookingPage);
    }

    @Override
    public PageResponse<BookingDTO> filterUserBookings(Integer userId, List<String> statusNames, Pageable pageable) {
        // Verify user exists
        userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        // Build specification for filtering by userId and optional status
        Specification<Booking> spec = (root, query, criteriaBuilder) -> {
            // Filter by userId
            var userPredicate = criteriaBuilder.equal(root.get("user").get("id"), userId);

            // Filter by status (if provided)
            if (statusNames != null && !statusNames.isEmpty()) {
                var statusPredicate = root.get("status").get("name").in(statusNames);
                return criteriaBuilder.and(userPredicate, statusPredicate);
            }

            return userPredicate;
        };

        // Execute query with pagination
        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);

        // Use helper method to build response with promotion info
        return buildBookingPageResponseWithPromotion(bookingPage);
    }

    // Helper methods

    private void validateBookingDates(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidException("Check-in and check-out dates are required");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidException("Check-out date must be after check-in date");
        }


        LocalDateTime now = LocalDateTime.now();
        if (checkIn.isBefore(now)) {
            throw new InvalidException("Check-in date cannot be in the past");
        }

        // Minimum booking duration (at least 1 night)
        long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        if (nights < 1) {
            throw new InvalidException("Minimum booking duration is 1 night");
        }
    }

    private void validatePropertyCapacity(Property property, Integer numAdults, Integer numChildren, Integer numInfant) {
        if (numAdults > property.getMaxAdults()) {
            throw new InvalidException("Number of adults exceeds property capacity. Maximum: " + property.getMaxAdults());
        }

        if (numChildren > property.getMaxChildren()) {
            throw new InvalidException("Number of children exceeds property capacity. Maximum: " + property.getMaxChildren());
        }
    }

    private BigDecimal calculateTotalPrice(BigDecimal pricePerNight, LocalDateTime checkIn, LocalDateTime checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }


    // ==================== PREVIEW APPROVAL ====================

    @Override
    public ApprovalPreviewDTO previewApproval(Integer hostId, Integer bookingId) {
        log.info("Previewing approval for booking {} by host {}", bookingId, hostId);

        // 1. Find booking with details
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new DataNotFoundException("Booking not found"));

        // 2. Verify host ownership
        if (!booking.getProperty().getHost().getId().equals(hostId)) {
            throw new InvalidException("You don't own this property");
        }

        // 3. Verify status
        if (!"PENDING".equals(booking.getStatus().getName())) {
            throw new InvalidException("Only PENDING bookings can be approved. Current status: " + booking.getStatus().getName());
        }

        // 4. Find conflicting PENDING bookings
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                booking.getProperty().getId(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                bookingId
        );

        // 5. Build preview DTO
        List<ApprovalPreviewDTO.ConflictingBookingDTO> conflictDTOs = conflicts.stream()
                .map(b -> ApprovalPreviewDTO.ConflictingBookingDTO.builder()
                        .id(b.getId())
                        .guestName(b.getUser().getFullName())
                        .guestEmail(b.getUser().getEmail())
                        .checkIn(b.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .checkOut(b.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .reason("Dates overlap with approved booking #" + bookingId)
                        .build())
                .collect(Collectors.toList());

        String warning = conflicts.isEmpty()
                ? null
                : String.format("Approving this booking will auto-reject %d other pending bookings", conflicts.size());

        return ApprovalPreviewDTO.builder()
                .bookingToApprove(BookingMapper.toDTO(booking))
                .willBeAutoRejected(conflictDTOs)
                .totalConflicts(conflicts.size())
                .warning(warning)
                .build();
    }

    // ==================== APPROVE BOOKING ====================

    @Override
    @Transactional
    public BookingDTO approveBooking(Integer hostId, Integer bookingId) {
        log.info("Host {} approving booking {}", hostId, bookingId);

        // 1. Lock booking FOR UPDATE (pessimistic lock)
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new DataNotFoundException("Booking not found"));

        // 2. Verify host ownership
        if (!booking.getProperty().getHost().getId().equals(hostId)) {
            throw new InvalidException("You don't own this property");
        }

        // 3. Verify status (must be PENDING)
        if (!"PENDING".equals(booking.getStatus().getName())) {
            throw new InvalidException("Only PENDING bookings can be approved. Current status: " + booking.getStatus().getName());
        }

        // 4. Update booking to CONFIRMED
        booking.setStatus(new Status(7)); // CONFIRMED
        booking.setConfirmedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        // 5. Auto-reject conflicting PENDING bookings
        int rejectedCount = bookingRepository.autoRejectConflicts(
                booking.getProperty().getId(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                bookingId,
                "Automatically rejected: Another booking was approved for overlapping dates"
        );

        log.info("✅ Booking {} approved. Auto-rejected {} conflicting bookings", bookingId, rejectedCount);

        // TODO: Send notification to guest (booking confirmed)
        // TODO: Send notifications to guests of rejected bookings

        return BookingMapper.toDTO(saved);
    }

    // ==================== REJECT BOOKING ====================

    @Override
    @Transactional
    public BookingDTO rejectBooking(Integer hostId, Integer bookingId, String reason) {
        log.info("Host {} rejecting booking {}", hostId, bookingId);

        // 1. Find booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new DataNotFoundException("Booking not found"));

        // 2. Verify host ownership
        if (!booking.getProperty().getHost().getId().equals(hostId)) {
            throw new InvalidException("You don't own this property");
        }

        // 3. Verify status (only PENDING can be rejected by host)
        if (!"PENDING".equals(booking.getStatus().getName())) {
            throw new InvalidException("Only PENDING bookings can be rejected. Current status: " + booking.getStatus().getName());
        }

        // 4. REFUND PROMOTION (if used)
        refundPromotionIfUsed(booking);

        // 5. Update booking to REJECTED
        booking.setStatus(new Status(11)); // REJECTED
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy("host");
        booking.setCancelReason(reason != null ? reason : "Host rejected the booking request");

        Booking saved = bookingRepository.save(booking);

        log.info("✅ Booking {} rejected by host. Reason: {}", bookingId, reason);

        // TODO: Send notification to guest

        return BookingMapper.toDTO(saved);
    }

    @Override
    public List<BookingDTO> getPropertyBookingsInDateRange(Integer hostId, Integer propertyId, 
                                                           String startDate, String endDate) {
        // Verify host exists
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        // Verify property exists and belongs to host
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
        
        if (!property.getHost().getId().equals(hostId)) {
            throw new InvalidException("This property does not belong to the specified host");
        }
        
        // Parse dates
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        
        // Get bookings
        List<Booking> bookings = bookingRepository.findBookingsByPropertyAndDateRange(
                propertyId, start, end);
        
        return bookings.stream()
                .map(BookingMapper::toDTO)
                .toList();
    }

    // ==================== Paginated versions ====================


    @Override
    public PageResponse<BookingDTO> getBookingsByPropertyId(Integer propertyId, Pageable pageable) {
        Page<Booking> bookingPage = bookingRepository.findAll(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("property").get("id"), propertyId),
            pageable
        );
        
        return buildBookingPageResponse(bookingPage);
    }

    @Override
    public PageResponse<BookingDTO> getAllBookings(Pageable pageable) {
        Page<Booking> bookingPage = bookingRepository.findAll(pageable);
        return buildBookingPageResponse(bookingPage);
    }

    @Override
    public PageResponse<BookingDTO> getPropertyBookingsInDateRange(Integer hostId, Integer propertyId,
                                                                   String startDate, String endDate,
                                                                   Pageable pageable) {
        // Validate host owns property using helper method
        validateHostOwnsProperty(hostId, propertyId);

        // Parse dates
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        // Build specification for date range filtering
        Specification<Booking> spec = (root, query, criteriaBuilder) ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("property").get("id"), propertyId),
                criteriaBuilder.greaterThanOrEqualTo(root.get("checkIn"), start),
                criteriaBuilder.lessThanOrEqualTo(root.get("checkOut"), end),
                criteriaBuilder.not(root.get("status").get("name").in("Cancelled", "Rejected"))
            );

        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);

        return buildBookingPageResponse(bookingPage);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Build PageResponse from Page<Booking> with standard mapper
     * Reduces duplicate code in paginated methods
     */
    private PageResponse<BookingDTO> buildBookingPageResponse(Page<Booking> bookingPage) {
        List<BookingDTO> bookingDTOs = bookingPage.getContent().stream()
                .map(BookingMapper::toDTO)
                .toList();

        return PageResponse.<BookingDTO>builder()
                .content(bookingDTOs)
                .currentPage(bookingPage.getNumber())
                .pageSize(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .first(bookingPage.isFirst())
                .last(bookingPage.isLast())
                .empty(bookingPage.isEmpty())
                .build();
    }

    /**
     * Build PageResponse from Page<Booking> with promotion info
     * Used for user bookings that need promotion details
     */
    private PageResponse<BookingDTO> buildBookingPageResponseWithPromotion(Page<Booking> bookingPage) {
        List<BookingDTO> bookingDTOs = bookingPage.getContent().stream()
                .map(booking -> BookingMapper.toDTOWithPromotion(booking, promotionUsageRepository))
                .toList();

        return PageResponse.<BookingDTO>builder()
                .content(bookingDTOs)
                .currentPage(bookingPage.getNumber())
                .pageSize(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .first(bookingPage.isFirst())
                .last(bookingPage.isLast())
                .empty(bookingPage.isEmpty())
                .build();
    }

    /**
     * Validate that host owns the property
     * Reduces duplicate validation code
     *
     * @return Property if validation passes
     */
    private Property validateHostOwnsProperty(Integer hostId, Integer propertyId) {
        userAccountRepository.findById(hostId)
                .orElseThrow(() -> new DataNotFoundException("Host not found with id: " + hostId));
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
        
        if (!property.getHost().getId().equals(hostId)) {
            throw new InvalidException("This property does not belong to the specified host");
        }
        
        return property;
    }

    /**
     * Validate promotion and lock UserPromotion to prevent concurrent use
     * Called during booking creation when promotion code is provided
     */
    private UserPromotion validateAndLockPromotion(UserAccount user, String promotionCode, 
                                                    Integer propertyId, LocalDateTime checkIn, 
                                                    LocalDateTime checkOut) {
        // Find promotion by code
        Promotion promotion = promotionRepository.findByCode(promotionCode)
                .orElseThrow(() -> new DataNotFoundException("Promotion not found with code: " + promotionCode));
        
        // Find UserPromotion for this user
        UserPromotion userPromotion = userPromotionRepository
                .findByUserIdAndPromotionId(user.getId(), promotion.getId())
                .orElseThrow(() -> new InvalidException("You have not claimed this promotion yet"));
        
        // Validate UserPromotion is ACTIVE
        if (!userPromotion.getStatus().getName().equals("ACTIVE")) {
            throw new InvalidException("This promotion is not active. Status: " + userPromotion.getStatus().getName());
        }
        
        // Check if already locked by another transaction
        if (userPromotion.getIsLocked()) {
            throw new InvalidException("This promotion is currently being used in another booking");
        }
        
        // Check expiration
        if (userPromotion.getExpiresAt() != null && 
            LocalDateTime.now().isAfter(userPromotion.getExpiresAt())) {
            throw new InvalidException("This promotion has expired on " + 
                userPromotion.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        // Validate promotion itself
        if (!promotion.getStatus().getName().equals("ACTIVE")) {
            throw new InvalidException("Promotion is not active");
        }
        
        if (promotion.getStartDate() != null && LocalDateTime.now().isBefore(promotion.getStartDate())) {
            throw new InvalidException("Promotion has not started yet");
        }
        
        if (promotion.getEndDate() != null && LocalDateTime.now().isAfter(promotion.getEndDate())) {
            throw new InvalidException("Promotion has ended");
        }
        
        // Check usage limit (skip if usageLimit = -1 which means unlimited)
        if (promotion.getUsageLimit() != null &&
            promotion.getUsageLimit() != -1 &&
            promotion.getTimesUsed() >= promotion.getUsageLimit()) {
            throw new InvalidException("Promotion usage limit has been reached");
        }
        
        // Validate minimum purchase limit
        BigDecimal bookingAmount = calculateTotalPrice(
            propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found"))
                .getPricePerNight(),
            checkIn, 
            checkOut
        );
        
        if (promotion.getMinPurchaseLimit() != null && 
            bookingAmount.compareTo(promotion.getMinPurchaseLimit()) < 0) {
            throw new InvalidException(String.format(
                "Minimum purchase amount is %s but booking amount is %s",
                promotion.getMinPurchaseLimit(), bookingAmount
            ));
        }
        
        // Lock the UserPromotion to prevent concurrent use
        userPromotion.setIsLocked(true);
        userPromotionRepository.save(userPromotion);
        
        log.info("🔒 Locked UserPromotion #{} for user #{}", userPromotion.getId(), user.getId());
        
        return userPromotion;
    }
}
