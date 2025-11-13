package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.payload.request.PaymentInitRequest;
import com.Cybersoft.Final_Capstone.payload.response.PaymentInitResponse;

/**
 * Service for handling payment transactions
 */
public interface TransactionService {
    /**
     * Initialize payment for a confirmed booking
     * Validates booking status, checks 24h deadline, calls payment gateway,
     * creates Transaction record, and returns payment URL
     *
     * Note: Promotion handling (validation, discount calculation, locking)
     * is done during booking creation, not here
     *
     * @param request Payment initialization request
     * @return PaymentInitResponse with payment URL and transaction details
     */
    PaymentInitResponse initPayment(PaymentInitRequest request);
}




