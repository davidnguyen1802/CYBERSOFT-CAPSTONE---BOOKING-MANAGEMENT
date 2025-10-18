package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find transaction by order ID
    Optional<Transaction> findByOrderId(String orderId);

    // Find transaction by request ID
    Optional<Transaction> findByRequestId(String requestId);

    // Find transactions by booking ID
    List<Transaction> findByBookingId(Integer bookingId);

    // Find transactions by user ID
    List<Transaction> findByUserId(Integer userId);

    // Find transactions by payment status
    List<Transaction> findByPaymentStatus(String paymentStatus);

    // Find transactions by user and payment status
    List<Transaction> findByUserIdAndPaymentStatus(Integer userId, String paymentStatus);
}

