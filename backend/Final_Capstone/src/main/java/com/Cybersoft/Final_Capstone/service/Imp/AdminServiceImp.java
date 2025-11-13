//package com.Cybersoft.Final_Capstone.service.Imp;
//
//import com.Cybersoft.Final_Capstone.Entity.*;
//import com.Cybersoft.Final_Capstone.dto.*;
//import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
//import com.Cybersoft.Final_Capstone.mapper.BookingMapper;
//import com.Cybersoft.Final_Capstone.mapper.PropertyMapper;
//import com.Cybersoft.Final_Capstone.mapper.UserAccountMapper;
//import com.Cybersoft.Final_Capstone.repository.*;
//import com.Cybersoft.Final_Capstone.service.AdminService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class AdminServiceImp implements AdminService {
//
//    @Autowired
//    private UserAccountRepository userAccountRepository;
//
//    @Autowired
//    private PropertyRepository propertyRepository;
//
//    @Autowired
//    private BookingRepository bookingRepository;
//
//    @Autowired
//    private PromotionRepository promotionRepository;
//
//    @Autowired
//    private UserReviewRepository userReviewRepository;
//
//    @Autowired
//    private RoleRepository roleRepository;
//
//    @Autowired
//    private StatusRepository statusRepository;
//
//    // ==================== Dashboard & Statistics ====================
//
//    @Override
//    public Map<String, Object> getDashboardStatistics() {
//        Map<String, Object> stats = new HashMap<>();
//
//        // User statistics
//        long totalUsers = userAccountRepository.count();
//        long activeUsers = userAccountRepository.findAll().stream()
//                .filter(u -> "ACTIVE".equals(u.getStatus().getName()))
//                .count();
//        long hostsCount = userAccountRepository.findAll().stream()
//                .filter(u -> "HOST".equals(u.getRole().getName()))
//                .count();
//        long guestsCount = userAccountRepository.findAll().stream()
//                .filter(u -> "GUEST".equals(u.getRole().getName()))
//                .count();
//
//        // Property statistics
//        long totalProperties = propertyRepository.count();
//        long activeProperties = propertyRepository.findAll().stream()
//                .filter(p -> "ACTIVE".equals(p.getStatus().getName()))
//                .count();
//
//        // Booking statistics
//        long totalBookings = bookingRepository.count();
//        long pendingBookings = bookingRepository.findAll().stream()
//                .filter(b -> "Pending".equals(b.getStatus().getName()))
//                .count();
//        long confirmedBookings = bookingRepository.findAll().stream()
//                .filter(b -> "Confirmed".equals(b.getStatus().getName()))
//                .count();
//        long completedBookings = bookingRepository.findAll().stream()
//                .filter(b -> "Completed".equals(b.getStatus().getName()))
//                .count();
//
//        // Revenue statistics
//        BigDecimal totalRevenue = bookingRepository.findAll().stream()
//                .filter(b -> "Completed".equals(b.getStatus().getName()))
//                .map(Booking::getTotalPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal monthlyRevenue = bookingRepository.findAll().stream()
//                .filter(b -> "Completed".equals(b.getStatus().getName())
//                        && b.getCreatedAt().getMonth() == LocalDate.now().getMonth()
//                        && b.getCreatedAt().getYear() == LocalDate.now().getYear())
//                .map(Booking::getTotalPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // Promotion statistics
//        long totalPromotions = promotionRepository.count();
//        long activePromotions = promotionRepository.findAll().stream()
//                .filter(p -> "ACTIVE".equals(p.getStatus().getName()))
//                .count();
//
//        // Build response
//        stats.put("users", Map.of(
//                "total", totalUsers,
//                "active", activeUsers,
//                "hosts", hostsCount,
//                "guests", guestsCount
//        ));
//
//        stats.put("properties", Map.of(
//                "total", totalProperties,
//                "active", activeProperties
//        ));
//
//        stats.put("bookings", Map.of(
//                "total", totalBookings,
//                "pending", pendingBookings,
//                "confirmed", confirmedBookings,
//                "completed", completedBookings
//        ));
//
//        stats.put("revenue", Map.of(
//                "total", totalRevenue,
//                "monthly", monthlyRevenue
//        ));
//
//        stats.put("promotions", Map.of(
//                "total", totalPromotions,
//                "active", activePromotions
//        ));
//
//        return stats;
//    }
//
//    @Override
//    public Map<String, Object> getStatisticsByDateRange(LocalDate startDate, LocalDate endDate) {
//        Map<String, Object> stats = new HashMap<>();
//
//        // Bookings in date range
//        List<Booking> bookingsInRange = bookingRepository.findAll().stream()
//                .filter(b -> !b.getCreatedAt().toLocalDate().isBefore(startDate)
//                        && !b.getCreatedAt().toLocalDate().isAfter(endDate))
//                .collect(Collectors.toList());
//
//        long totalBookings = bookingsInRange.size();
//        BigDecimal totalRevenue = bookingsInRange.stream()
//                .filter(b -> "Completed".equals(b.getStatus().getName()))
//                .map(Booking::getTotalPrice)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // New users in date range
//        long newUsers = userAccountRepository.findAll().stream()
//                .filter(u -> u.getCreateDate() != null
//                        && !u.getCreateDate().isBefore(startDate)
//                        && !u.getCreateDate().isAfter(endDate))
//                .count();
//
//        stats.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));
//        stats.put("bookings", totalBookings);
//        stats.put("revenue", totalRevenue);
//        stats.put("newUsers", newUsers);
//
//        return stats;
//    }
//
//    // ==================== User Management ====================
//
//    @Override
//    public Page<UserAccountDTO> getAllUsers(String keyword, String role, String status, Pageable pageable) {
//        Page<UserAccount> users;
//
//        if (role != null && !role.isEmpty()) {
//            users = userAccountRepository.findAll(pageable);
//            // Filter by role in memory (since repository doesn't have this method yet)
//            List<UserAccount> filteredUsers = users.getContent().stream()
//                    .filter(u -> role.equalsIgnoreCase(u.getRole().getName()))
//                    .collect(Collectors.toList());
//            return new org.springframework.data.domain.PageImpl<>(
//                    filteredUsers.stream().map(UserAccountMapper::toDTO).collect(Collectors.toList()),
//                    pageable,
//                    filteredUsers.size()
//            );
//        } else {
//            users = userAccountRepository.findAll(keyword, pageable);
//        }
//
//        return users.map(UserAccountMapper::toDTO);
//    }
//
//    @Override
//    public UserAccountDTO getUserById(Integer userId) {
//        UserAccount user = userAccountRepository.findById(userId)
//                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));
//        return UserAccountMapper.toDTO(user);
//    }
//
//    @Override
//    @Transactional
//    public UserAccountDTO updateUser(Integer userId, UpdateUserDTO updateUserDTO) {
//        UserAccount user = userAccountRepository.findById(userId)
//                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));
//
//        // Update fields
//        if (updateUserDTO.getFullName() != null) {
//            user.setFullName(updateUserDTO.getFullName());
//        }
//        if (updateUserDTO.getPhoneNumber() != null) {
//            user.setPhone(updateUserDTO.getPhoneNumber());
//        }
//        if (updateUserDTO.getAddress() != null) {
//            user.setAddress(updateUserDTO.getAddress());
//        }
//        if (updateUserDTO.getDateOfBirth() != null) {
//            // Convert Date to LocalDate
//            java.time.LocalDate localDate = updateUserDTO.getDateOfBirth().toInstant()
//                    .atZone(java.time.ZoneId.systemDefault())
//                    .toLocalDate();
//            user.setDob(localDate);
//        }
//
//        UserAccount savedUser = userAccountRepository.save(user);
//        return UserAccountMapper.toDTO(savedUser);
//    }
//
//    @Override
//    @Transactional
//    public UserAccountDTO changeUserRole(Integer userId, String roleName) {
//        UserAccount user = userAccountRepository.findById(userId)
//                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));
//
//        Role role = roleRepository.findByName(roleName)
//                .orElseThrow(() -> new DataNotFoundException("Role not found: " + roleName));
//
//        user.setRole(role);
//        UserAccount savedUser = userAccountRepository.save(user);
//        return UserAccountMapper.toDTO(savedUser);
//    }
//
//    @Override
//    @Transactional
//    public UserAccountDTO changeUserStatus(Integer userId, String statusName) {
//        // Map status name to ID
//        Status status;
//        switch (statusName.toUpperCase()) {
//            case "ACTIVE":
//                status = new Status(1);
//                break;
//            case "INACTIVE":
//                status = new Status(2);
//                break;
//            case "DELETED":
//                status = new Status(3);
//                break;
//            default:
//                throw new DataNotFoundException("Status not found: " + statusName);
//        }
//                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));
//
//        // Soft delete - set status to DELETED
//        Status deletedStatus = statusRepository.findByName("DELETED")
//                .orElse(statusRepository.findByName("INACTIVE")
//                        .orElseThrow(() -> new DataNotFoundException("Status not found")));
//
//        user.setStatus(deletedStatus);
//        userAccountRepository.save(user);
//    }
//
//    // ==================== Property Management ====================
//
//        user.setStatus(new Status(3)); // DELETED
//    public Page<PropertyDTO> getAllProperties(String keyword, String status, Pageable pageable) {
//        Page<Property> properties = propertyRepository.findAll(pageable);
//        return properties.map(PropertyMapper::toDTO);
//    }
//
//    @Override
//    public PropertyDTO getPropertyById(Integer propertyId) {
//        Property property = propertyRepository.findById(propertyId)
//                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
//        return PropertyMapper.toDTO(property);
//    }
//
//    @Override
//    @Transactional
//    public PropertyDTO changePropertyStatus(Integer propertyId, String statusName) {
//        Property property = propertyRepository.findById(propertyId)
//                .orElseThrow(() -> new DataNotFoundException("Property not found with id: " + propertyId));
//
//        Status status = statusRepository.findByName(statusName)
//                .orElseThrow(() -> new DataNotFoundException("Status not found: " + statusName));
//
//        property.setStatus(status);
//        Property savedProperty = propertyRepository.save(property);
//        return PropertyMapper.toDTO(savedProperty);
//        // Map status name to ID
//        Status status;
//        switch (statusName.toUpperCase()) {
//            case "AVAILABLE":
//                status = new Status(4);
//                break;
//            case "UNAVAILABLE":
//                status = new Status(5);
//                break;
//            case "DELETED":
//                status = new Status(3);
//                break;
//            default:
//                throw new DataNotFoundException("Status not found: " + statusName);
//        }
//    }
//
//    // ==================== Booking Management ====================
//
//    @Override
//    public Page<BookingDTO> getAllBookings(String status, Pageable pageable) {
//        Page<Booking> bookings = bookingRepository.findAll(pageable);
//        return bookings.map(BookingMapper::toDTO);
//    }
//
//    @Override
//    public BookingDTO getBookingById(Integer bookingId) {
//        Booking booking = bookingRepository.findById(bookingId)
//        property.setStatus(new Status(3)); // DELETED
//        return BookingMapper.toDTO(booking);
//    }
//
//    @Override
//    @Transactional
//    public BookingDTO changeBookingStatus(Integer bookingId, String statusName) {
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + bookingId));
//
//        Status status = statusRepository.findByName(statusName)
//                .orElseThrow(() -> new DataNotFoundException("Status not found: " + statusName));
//
//        booking.setStatus(status);
//        Booking savedBooking = bookingRepository.save(booking);
//        return BookingMapper.toDTO(savedBooking);
//    }
//
//    @Override
//    @Transactional
//    public void cancelBooking(Integer bookingId) {
//        Booking booking = bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new DataNotFoundException("Booking not found with id: " + bookingId));
//
//        Status cancelledStatus = statusRepository.findByName("Cancelled")
//        // Map status name to ID
//        Status status;
//        switch (statusName.toUpperCase()) {
//            case "PENDING":
//                status = new Status(6);
//                break;
//            case "CONFIRMED":
//                status = new Status(7);
//                break;
//            case "PAID":
//                status = new Status(8);
//                break;
//            case "COMPLETED":
//                status = new Status(9);
//                break;
//            case "CANCELLED":
//                status = new Status(10);
//                break;
//            case "REJECTED":
//                status = new Status(11);
//                break;
//            default:
//                throw new DataNotFoundException("Status not found: " + statusName);
//        }
//        Promotion savedPromotion = promotionRepository.save(promotion);
//        return toPromotionDTO(savedPromotion);
//    }
//
//    // ==================== Review Management ====================
//
//    @Override
//    public Page<ReviewDTO> getAllReviews(Integer propertyId, Pageable pageable) {
//        Page<UserReview> reviews;
//        if (propertyId != null) {
//            reviews = userReviewRepository.findAll(pageable).map(review -> {
//                if (review.getProperty().getId().equals(propertyId)) {
//        booking.setStatus(new Status(10)); // CANCELLED
//                }
//                return null;
//            });
//        } else {
//            reviews = userReviewRepository.findAll(pageable);
//        }
//        return reviews.map(this::toReviewDTO);
//    }
//
//    @Override
//    @Transactional
//    public void deleteReview(Integer reviewId) {
//        UserReview review = userReviewRepository.findById(reviewId)
//                .orElseThrow(() -> new DataNotFoundException("Review not found with id: " + reviewId));
//        userReviewRepository.delete(review);
//    }
//
//        // Map status name to ID (using Status constructor instead of DB query)
//        int statusId;
//        switch (statusName.toUpperCase()) {
//            case "ACTIVE":
//                statusId = 1;
//                break;
//            case "INACTIVE":
//                statusId = 2;
//                break;
//            case "DELETED":
//                statusId = 3;
//                break;
//            default:
//                throw new DataNotFoundException("Invalid status name: " + statusName + ". Allowed: ACTIVE, INACTIVE, DELETED");
//        }
//
//        Status status = new Status(statusId);
//        report.put("endDate", endDate);
//        report.put("totalRevenue", totalRevenue);
//        report.put("totalBookings", bookings.size());
//        report.put("averageBookingValue", bookings.isEmpty() ? BigDecimal.ZERO
//                : totalRevenue.divide(BigDecimal.valueOf(bookings.size()), 2, java.math.RoundingMode.HALF_UP));
//
//        return report;
//    }
//
//    @Override
//    public List<HostStatisticsDTO> getTopHosts(String sortBy, int limit) {
//        List<UserAccount> hosts = userAccountRepository.findAll().stream()
//                .filter(u -> "HOST".equals(u.getRole().getName()))
//                .limit(limit)
//                .collect(Collectors.toList());
//
//        // Create statistics for each host
//        return hosts.stream()
//                .map(host -> {
//                    HostStatisticsDTO stats = new HostStatisticsDTO();
//                    stats.setHostId(host.getId());
//                    stats.setHostName(host.getFullName());
//                    stats.setTotalProperties(host.getHostedProperties().size());
//                    // Add more statistics as needed
//                    return stats;
//                })
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<PropertyDTO> getTopProperties(String sortBy, int limit) {
//        List<Property> properties = propertyRepository.findAll().stream()
//                .limit(limit)
//                .collect(Collectors.toList());
//        return properties.stream()
//                .map(PropertyMapper::toDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public Map<String, Object> getUserActivityReport(LocalDate startDate, LocalDate endDate) {
//        Map<String, Object> report = new HashMap<>();
//
//        long newUsers = userAccountRepository.findAll().stream()
//                .filter(u -> u.getCreateDate() != null
//                        && !u.getCreateDate().isBefore(startDate)
//                        && !u.getCreateDate().isAfter(endDate))
//                .count();
//
//        report.put("startDate", startDate);
//        report.put("endDate", endDate);
//        report.put("newUsers", newUsers);
//
//        return report;
//    }
//
//    // ==================== Helper Methods ====================
//
//    private PromotionDTO toPromotionDTO(Promotion promotion) {
//        PromotionDTO dto = new PromotionDTO();
//        dto.setId(promotion.getId());
//        dto.setCode(promotion.getCode());
//        dto.setName(promotion.getName());
//        dto.setDescription(promotion.getDescription());
//        dto.setDiscountValue(promotion.getDiscountValue() != null ? promotion.getDiscountValue().toString() : "0");
//        dto.setDiscountType(promotion.getDiscountType() != null ? promotion.getDiscountType().name() : "");
//        dto.setActive("ACTIVE".equals(promotion.getStatus().getName()));
//        return dto;
//    }
//
//    private ReviewDTO toReviewDTO(UserReview review) {
//        ReviewDTO dto = new ReviewDTO();
//        dto.setReviewId(review.getId());
//        dto.setRating(review.getRating());
//        dto.setComment(review.getComment());
//        dto.setPropertyId(review.getProperty().getId());
//        dto.setReviewDate(review.getReviewDate());
//        return dto;
//    }
//}
//
