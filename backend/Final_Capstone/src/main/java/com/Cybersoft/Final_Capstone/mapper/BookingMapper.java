package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.dto.BookingDTO;
import com.Cybersoft.Final_Capstone.payload.response.BookingResponse;
import lombok.Data;

@Data
public class BookingMapper {

    public static BookingDTO toDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setUserName(booking.getUser().getFullName());
        dto.setPropertyId(booking.getProperty().getId());
        dto.setPropertyName(booking.getProperty().getPropertyName());
        dto.setCheckIn(booking.getCheckIn());
        dto.setCheckOut(booking.getCheckOut());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setNumAdults(booking.getNumAdults());
        dto.setNumChildren(booking.getNumChildren());
        dto.setNum_teenager(booking.getNum_teenager());
        dto.setNum_infant(booking.getNum_infant());
        dto.setNotes(booking.getNotes());
        dto.setStatus(booking.getStatus().getName());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }

    public static BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUser().getId());
        response.setPropertyId(booking.getProperty().getId());
        response.setPropertyName(booking.getProperty().getPropertyName());
        response.setCheckIn(booking.getCheckIn());
        response.setCheckOut(booking.getCheckOut());
        response.setTotalPrice(booking.getTotalPrice());
        response.setNumAdults(booking.getNumAdults());
        response.setNumChildren(booking.getNumChildren());
        response.setNotes(booking.getNotes());
        response.setStatus(booking.getStatus().getName());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }
}


