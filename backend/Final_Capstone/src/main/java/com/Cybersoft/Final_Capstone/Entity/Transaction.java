package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @CreationTimestamp
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // Momo Payment specific fields
    @Column(name = "order_id", unique = true)
    private String orderId;

    @Column(name = "request_id", unique = true)
    private String requestId;

    @Column(name = "trans_id")
    private String transId;

    @Column(name = "payment_method")
    private String paymentMethod = "MOMO";

    @Column(name = "payment_status")
    private String paymentStatus = "PENDING"; // PENDING, SUCCESS, FAILED, CANCELLED

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "message")
    private String message;

    @Column(name = "pay_url", length = 1000)
    private String payUrl;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
