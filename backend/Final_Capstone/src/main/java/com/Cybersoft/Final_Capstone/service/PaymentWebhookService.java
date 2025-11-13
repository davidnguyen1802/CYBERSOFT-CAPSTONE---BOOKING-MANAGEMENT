package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.payload.request.PayOSWebhookRequest;

import java.util.Map;

public interface PaymentWebhookService {
    void processWebhook(PayOSWebhookRequest webhookRequest);
    Map<String, Object> getPaymentStatus(Integer bookingId);
}

