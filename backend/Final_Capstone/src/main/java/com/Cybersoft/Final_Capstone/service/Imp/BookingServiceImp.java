package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.Enum.DiscountType;
import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.InvalidException;
import com.Cybersoft.Final_Capstone.mapper.BookingMapper;
import com.Cybersoft.Final_Capstone.payload.request.BookingRequest;
import com.Cybersoft.Final_Capstone.repository.*;
import com.Cybersoft.Final_Capstone.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
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

    @Transactional
    @Override
    public BookingDTO createBooking(BookingRequest bookingRequest) {
        // Validate check-in and check-out dates
        validateBookingDates(bookingRequest.getCheckIn(), bookingRequest.getCheckOut());

        // Get user (In production, get from SecurityContext)
        /* Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); */
        int userId = 3; // Temporary hardcoded user ID
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        // Get property
        Property property = propertyRepository.findById(bookingRequest.getPropertyId())
                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + bookingRequest.getPropertyId()));

        // Validate property is active and available
        String statusName = property.getStatus().getName();
        if (!statusName.equals("AVAILABLE")) {
            throw new InvalidException("Property is not available for booking. Current status: " + statusName);
        }

        // Check if property capacity is sufficient
        validatePropertyCapacity(property, bookingRequest.getNumAdults(), 
                               bookingRequest.getNumChildren() != null ? bookingRequest.getNumChildren() : 0);

        // Check if property is available for the requested dates
        Long conflictCount = bookingRepository.countConflictingBookings(
                bookingRequest.getPropertyId(),
                bookingRequest.getCheckIn(),
                bookingRequest.getCheckOut()
        );

        if (conflictCount > 0) {
            throw new InvalidException("Property is not available for the selected dates");
        }

        // Calculate total price
        BigDecimal totalPrice = calculateTotalPrice(
                property.getPricePerNight(),
                bookingRequest.getCheckIn(),
                bookingRequest.getCheckOut()
        );

        // Apply promotion if provided
        if (bookingRequest.getPromotionCode() != null && !bookingRequest.getPromotionCode().isEmpty()) {
            totalPrice = applyPromotion(bookingRequest.getPromotionCode(), totalPrice);
        }

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setProperty(property);
        booking.setCheckIn(bookingRequest.getCheckIn());
        booking.setCheckOut(bookingRequest.getCheckOut());
        booking.setTotalPrice(totalPrice);
        booking.setNumAdults(bookingRequest.getNumAdults());
        booking.setNumChildren(bookingRequest.getNumChildren() != null ? bookingRequest.getNumChildren() : 0);
        booking.setNum_teenager(bookingRequest.getNum_teenager() != null ? bookingRequest.getNum_teenager() : 0);
        booking.setNum_infant(bookingRequest.getNum_infant() != null ? bookingRequest.getNum_infant() : 0);
        booking.setNotes(bookingRequest.getNotes());

        // Set status to "Pending"
        Status status = statusRepository.findByName("Pending")
                .orElseThrow(() -> new DataNotFoundException("Pending status not found in system"));
        booking.setStatus(status);

        // Save booking
        Booking savedBooking = bookingRepository.save(booking);

        return BookingMapper.toDTO(savedBooking);
    }

    @Override
    public BookingDTO getBookingById(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + id));
        return BookingMapper.toDTO(booking);
    }

    @Override
    public List<BookingDTO> getBookingsByUserId(Integer userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
                .map(BookingMapper::toDTO)
                .toList();
    }

    @Override
    public List<BookingDTO> getBookingsByPropertyId(Integer propertyId) {
        List<Booking> bookings = bookingRepository.findByPropertyId(propertyId);
        return bookings.stream()
                .map(BookingMapper::toDTO)
                .toList();
    }

    @Override
    public List<BookingDTO> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return bookings.stream()
                .map(BookingMapper::toDTO)
                .toList();
    }

    @Transactional
    @Override
    public BookingDTO updateBookingStatus(Integer id, String statusName) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + id));

        Status status = statusRepository.findByName(statusName)
                .orElseThrow(() -> new DataNotFoundException("Status not found with name: " + statusName));

        booking.setStatus(status);
        Booking updatedBooking = bookingRepository.save(booking);

        return BookingMapper.toDTO(updatedBooking);
    }

    @Transactional
    @Override
    public BookingDTO cancelBooking(Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + id));

        // Check if booking can be cancelled (not already completed or cancelled)
        if (booking.getStatus().getName().equals("Completed") ||
                booking.getStatus().getName().equals("Cancelled")) {
            throw new InvalidException("Cannot cancel booking with status: " + booking.getStatus().getName());
        }

        // Check if check-in date is in the past
        if (booking.getCheckIn().isBefore(LocalDateTime.now())) {
            throw new InvalidException("Cannot cancel booking that has already started");
        }

        Status cancelledStatus = statusRepository.findByName("Cancelled")
                .orElseThrow(() -> new DataNotFoundException("Cancelled status not found"));

        booking.setStatus(cancelledStatus);
        Booking cancelledBooking = bookingRepository.save(booking);

        return BookingMapper.toDTO(cancelledBooking);
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

    // Helper methods

    private void validateBookingDates(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidException("Check-in and check-out dates are required");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidException("Check-out date must be after check-in date");
        }

        // Allow 1 hour grace period for same-day bookings
        LocalDateTime now = LocalDateTime.now();
        if (checkIn.isBefore(now.minusHours(1))) {
            throw new InvalidException("Check-in date cannot be in the past");
        }

        // Minimum booking duration (at least 1 night)
        long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        if (nights < 1) {
            throw new InvalidException("Minimum booking duration is 1 night");
        }
    }

    private void validatePropertyCapacity(Property property, Integer numAdults, Integer numChildren) {
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

    private BigDecimal applyPromotion(String promotionCode, BigDecimal totalPrice) {
        Promotion promotion = promotionRepository.findByCodeAndStatus_Name(promotionCode, "Active")
                .orElseThrow(() -> new DataNotFoundException("Promotion not found or is not active with code: " + promotionCode));

        // Validate promotion dates
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate().isAfter(now) || promotion.getEndDate().isBefore(now)) {
            throw new InvalidException("Promotion is not valid for current date");
        }

        // Validate minimum purchase amount
        if (promotion.getMinPurchaseLimit() != null && 
            totalPrice.compareTo(promotion.getMinPurchaseLimit()) < 0) {
            throw new InvalidException("Total price does not meet minimum purchase requirement: " 
                + promotion.getMinPurchaseLimit());
        }

        // Validate usage limit (-1 means unlimited)
        if (promotion.getUsageLimit() != -1 && promotion.getTimesUsed() >= promotion.getUsageLimit()) {
            throw new InvalidException("Promotion usage limit reached");
        }

        // Apply discount
        BigDecimal discount;
        if (promotion.getDiscountType() == DiscountType.PERCENT) {
            // Use RoundingMode to avoid ArithmeticException for non-terminating decimals
            discount = totalPrice.multiply(
                promotion.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            );

            // Apply max discount amount if set
            if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();
            }
        } else {
            discount = promotion.getDiscountValue();
        }

        BigDecimal finalPrice = totalPrice.subtract(discount);

        // Ensure price doesn't go negative
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }

        // Increment usage count
        promotion.setTimesUsed(promotion.getTimesUsed() + 1);
        promotionRepository.save(promotion);

        return finalPrice;
    }
}

