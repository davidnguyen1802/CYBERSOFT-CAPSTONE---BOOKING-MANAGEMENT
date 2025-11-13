package com.Cybersoft.Final_Capstone.scheduler;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.Status;
import com.Cybersoft.Final_Capstone.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to auto-complete PAID bookings where check-out date has passed
 * Runs every hour
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCompletionJob {

    private final BookingRepository bookingRepository;

    /**
     * Auto-complete PAID bookings where check_out < now
     * Cron: Every hour at :00
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void completeFinishedBookings() {
        log.info("🕐 Running booking completion job...");

        // Find PAID bookings where check_out < now
        LocalDateTime now = LocalDateTime.now();
        List<Booking> finishedBookings = bookingRepository.findToComplete(now);

        if (finishedBookings.isEmpty()) {
            log.info("✅ No bookings to complete");
            return;
        }

        log.info("📋 Found {} bookings to mark as COMPLETED", finishedBookings.size());

        for (Booking booking : finishedBookings) {
            try {
                booking.setStatus(new Status(9)); // COMPLETED

                bookingRepository.save(booking);

                log.info("✅ Completed booking {} (guest: {}, property: {}, check-out: {})",
                        booking.getId(),
                        booking.getUser().getEmail(),
                        booking.getProperty().getPropertyName(),
                        booking.getCheckOut());

                // TODO: Send notification to guest (ask for review)
                // TODO: Update property availability

            } catch (Exception e) {
                log.error("Failed to complete booking {}: {}", booking.getId(), e.getMessage(), e);
            }
        }

        log.info("✅ Booking completion job finished. Completed {} bookings", finishedBookings.size());
    }
}


