package com.Cybersoft.Final_Capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for previewing what happens when approving a booking
 * Shows which other PENDING bookings will be auto-rejected
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalPreviewDTO {
    
    private BookingDTO bookingToApprove;
    
    private List<ConflictingBookingDTO> willBeAutoRejected;
    
    private Integer totalConflicts;
    
    private String warning;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictingBookingDTO {
        private Integer id;
        private String guestName;
        private String guestEmail;
        private String checkIn;
        private String checkOut;
        private String reason;
    }
}


