package com.Cybersoft.Final_Capstone.controller;

import com.Cybersoft.Final_Capstone.payload.request.PaymentInitRequest;
import com.Cybersoft.Final_Capstone.payload.response.BaseResponse;
import com.Cybersoft.Final_Capstone.payload.response.PaymentInitResponse;
import com.Cybersoft.Final_Capstone.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for payment transaction operations
 */
@RestController
@RequestMapping("/transactions")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * Initialize payment for a confirmed booking
     * Note: Promotion discount is already applied in booking.totalPrice
     *
     * POST /transactions/init
     * {
     *   "bookingId": 456,
     *   "paymentMethod": "PAYOS"
     * }
     * 
     * @param request Payment initialization request
     * @return Payment URL and transaction details
     */
    @PostMapping("/init")
    public ResponseEntity<?> initPayment(@Valid @RequestBody PaymentInitRequest request) {
        log.info("📥 Received payment init request for booking {}", request.getBookingId());

        try {
            PaymentInitResponse paymentData = transactionService.initPayment(request);

            BaseResponse response = new BaseResponse();
            response.setCode(HttpStatus.OK.value());
            response.setMessage("Payment initialized successfully. Redirect user to payment URL.");
            response.setData(paymentData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error initializing payment: {}", e.getMessage(), e);

            BaseResponse response = new BaseResponse();
            response.setCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage("Failed to initialize payment: " + e.getMessage());
            response.setData(null);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}




