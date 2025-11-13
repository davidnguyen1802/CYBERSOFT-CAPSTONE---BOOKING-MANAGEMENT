package com.Cybersoft.Final_Capstone.scheduler;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.Promotion;
import com.Cybersoft.Final_Capstone.Entity.PromotionUsage;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.Entity.UserPromotion;
import com.Cybersoft.Final_Capstone.repository.BookingRepository;
import com.Cybersoft.Final_Capstone.repository.PromotionRepository;
import com.Cybersoft.Final_Capstone.repository.PromotionUsageRepository;
import com.Cybersoft.Final_Capstone.repository.StatusRepository;
import com.Cybersoft.Final_Capstone.repository.UserPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled job to auto-cancel CONFIRMED bookings where payment deadline has passed
 * Runs every 15 minutes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutJob {

    private final BookingRepository bookingRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final UserPromotionRepository userPromotionRepository;
    private final PromotionRepository promotionRepository;

    /**
     * Auto-cancel CONFIRMED bookings where (confirmed_at + 24h) < now
     * Cron: Every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *")  // Runs every 15 minutes at :00, :15, :30, :45
    @Transactional
    public void cancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        log.info("🕐 ========== PAYMENT TIMEOUT JOB STARTED ==========");
        log.info("🕐 Current time: {}", now);

        // Find CONFIRMED bookings where confirmed_at < (now - 24 hours)
        LocalDateTime deadline = now.minusHours(24);
        log.info("⏰ Deadline (24h ago): {}", deadline);
        log.info("📋 Looking for CONFIRMED bookings with confirmed_at < {}", deadline);

        List<Booking> expiredBookings = bookingRepository.findExpiredUnpaid(deadline);

        log.info("🔍 Found {} bookings to check", expiredBookings.size());

        if (expiredBookings.isEmpty()) {
            log.info("✅ No expired bookings found. All good!");
            log.info("🕐 ========== PAYMENT TIMEOUT JOB COMPLETED ==========\n");
            return;
        }

        log.warn("⚠️ Found {} expired bookings to cancel:", expiredBookings.size());

        // Log details of each booking to cancel
        for (Booking b : expiredBookings) {
            log.info("   📌 Booking ID: {}, Confirmed at: {}, User: {}, Property: {}",
                b.getId(),
                b.getConfirmedAt(),
                b.getUser().getEmail(),
                b.getProperty().getPropertyName());
        }

        int cancelledCount = 0;
        int failedCount = 0;

        for (Booking booking : expiredBookings) {
            try {
                // Process each booking in separate transaction to prevent rollback cascade
                cancelSingleBooking(booking);
                cancelledCount++;
            } catch (Exception e) {
                failedCount++;
                log.error("❌ Failed to cancel booking {}: {}", booking.getId(), e.getMessage(), e);
                // Continue with next booking - don't let one failure stop the whole job
            }
        }

        log.info("🎯 ========== PAYMENT TIMEOUT JOB COMPLETED ==========");
        log.info("✅ Successfully cancelled {}/{} bookings", cancelledCount, expiredBookings.size());
        if (failedCount > 0) {
            log.warn("⚠️ Failed to cancel {} bookings - check errors above", failedCount);
        }
        log.info("🕐 Job finished at: {}\n", LocalDateTime.now());
    }

    /**
     * Cancel a single booking in isolated transaction
     * REQUIRES_NEW ensures one booking failure doesn't rollback others
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void cancelSingleBooking(Booking booking) {
        // ✅ REFUND PROMOTION before cancelling
        refundPromotionIfUsed(booking);

        booking.setStatus(new Status(10)); // CANCELLED
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy("system");
        booking.setCancelReason("Payment deadline expired (24 hours after confirmation)");

        bookingRepository.save(booking);

        log.info("❌ Cancelled booking {} (guest: {}, property: {})",
                booking.getId(),
                booking.getUser().getEmail(),
                booking.getProperty().getPropertyName());

        // TODO: Send notification to guest
        // TODO: Send notification to host
    }

    /**
     * Refund promotion if it was used for this booking
     * Called when booking is auto-cancelled due to payment timeout
     *
     * Logic:
     * 1. Find PromotionUsage for this booking
     * 2. Check if promotion is USED (payment successful) - DO NOT REFUND
     * 3. If INACTIVE (not yet paid):
     *    - Set UserPromotion back to ACTIVE
     *    - Unlock UserPromotion
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
            // USED status means payment was completed before timeout, promotion cannot be refunded
            if ("USED".equals(userPromotion.getStatus().getName())) {
                log.warn("⚠️ Cannot refund USED promotion for booking {}. Payment was already completed before timeout.",
                    booking.getId());
                log.info("💡 Promotion {} (code: {}) remains USED and locked permanently.",
                    userPromotion.getId(), promotion.getCode());
                return; // Do NOT refund
            }

            // ✅ Only refund if status = INACTIVE (not yet paid)
            log.info("🔄 Refunding INACTIVE promotion {} for booking {} (payment timeout)",
                promotion.getCode(), booking.getId());

            // 1. CRITICAL: Remove bidirectional references BEFORE deleting to avoid ObjectDeletedException
            // Clear references from both sides to prevent Hibernate from trying to merge deleted entity
            if (usage.getBooking() != null) {
                usage.setBooking(null);
            }
            if (usage.getUserPromotion() != null) {
                usage.setUserPromotion(null);
            }

            // 2. Delete PromotionUsage record first and flush to DB
            promotionUsageRepository.delete(usage);
            promotionUsageRepository.flush(); // Force DELETE to execute NOW before any merge operations
            log.info("✅ PromotionUsage record deleted and flushed for booking {}", booking.getId());

            // 3. Set UserPromotion back to ACTIVE (now safe to save)
            Status activeStatus = new Status(1); // ACTIVE = 1
            userPromotion.setStatus(activeStatus);
            userPromotion.setIsLocked(false); // Unlock
            userPromotionRepository.save(userPromotion);

            log.info("✅ UserPromotion {} reactivated (status = ACTIVE)", userPromotion.getId());

            // 4. Decrement Promotion.timesUsed
            if (promotion.getTimesUsed() > 0) {
                promotion.setTimesUsed(promotion.getTimesUsed() - 1);
                promotionRepository.save(promotion);
                log.info("✅ Promotion {} times_used decremented: {} → {}",
                    promotion.getCode(),
                    promotion.getTimesUsed() + 1,
                    promotion.getTimesUsed());
            }

            log.info("🎉 Promotion refund complete. User can reuse promotion.");

        } catch (Exception e) {
            log.error("❌ Error refunding promotion for booking {}: {}",
                booking.getId(), e.getMessage(), e);
            // Don't throw - cancellation should still proceed even if refund fails
        }
    }
}


