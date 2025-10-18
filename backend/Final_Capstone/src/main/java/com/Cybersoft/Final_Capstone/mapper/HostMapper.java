package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.dto.GuestInfoDTO;
import com.Cybersoft.Final_Capstone.dto.HostDTO;
import lombok.Data;

@Data
public class HostMapper {

    public static HostDTO toDTO(UserAccount userAccount) {
        if (userAccount == null) {
            return null;
        }

        HostDTO dto = new HostDTO();
        dto.setId(userAccount.getId());
        dto.setFullName(userAccount.getFullName());
        dto.setUsername(userAccount.getUsername());
        dto.setEmail(userAccount.getEmail());
        dto.setPhone(userAccount.getPhone());
        dto.setAddress(userAccount.getAddress());
        dto.setGender(userAccount.getGender());
        dto.setDob(userAccount.getDob());
        dto.setJoinDate(userAccount.getCreateDate());
        dto.setTotalProperties(userAccount.getHostedProperties() != null ? userAccount.getHostedProperties().size() : 0);
        dto.setTotalBookings(userAccount.getBookings() != null ? userAccount.getBookings().size() : 0);

        return dto;
    }

    public static GuestInfoDTO toGuestInfoDTO(Booking booking) {
        if (booking == null) {
            return null;
        }

        GuestInfoDTO dto = new GuestInfoDTO();
        dto.setBookingId(booking.getId());
        dto.setGuestId(booking.getUser().getId());
        dto.setGuestName(booking.getUser().getFullName());
        dto.setGuestEmail(booking.getUser().getEmail());
        dto.setGuestPhone(booking.getUser().getPhone());
        dto.setPropertyId(booking.getProperty().getId());
        dto.setPropertyName(booking.getProperty().getPropertyName());
        dto.setCheckIn(booking.getCheckIn());
        dto.setCheckOut(booking.getCheckOut());
        dto.setNumAdults(booking.getNumAdults());
        dto.setNumChildren(booking.getNumChildren());
        dto.setBookingStatus(booking.getStatus().getName());
        dto.setNotes(booking.getNotes());

        return dto;
    }
}

