package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.Entity.Booking;
import com.Cybersoft.Final_Capstone.payload.response.PayOSPaymentData;

public interface PayOSService {
    PayOSPaymentData createPaymentForBooking(Booking booking);

    // Overloaded method for retry payments with custom orderCode
    PayOSPaymentData createPaymentForBooking(Booking booking, String orderCode);
}
