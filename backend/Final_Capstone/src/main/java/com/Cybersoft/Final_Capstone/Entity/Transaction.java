package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "request_id", unique = true)
    private String requestId;

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

    // PayOS Payment fields
    @Column(name = "order_id", unique = true)
    private String orderId; // PayOS orderCode as String

    @Column(name = "payment_method")
    private String paymentMethod = "PAYOS";

    @Column(name = "payment_status")
    private String paymentStatus = "PENDING"; // PENDING, SUCCESS, FAILED, CANCELLED

    @Column(name = "pay_url", length = 1000)
    private String payUrl;

    @Column(name = "message")
    private String message;

    // PayOS specific fields from webhook
    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "reference", length = 100)
    private String reference; // Mã tham chiếu giao dịch

    @Column(name = "transaction_datetime")
    private LocalDateTime transactionDateTime; // Thời gian giao dịch thực tế từ PayOS

    @Column(name = "currency", length = 10)
    private String currency = "VND";

    @Column(name = "payment_link_id", length = 100)
    private String paymentLinkId;

    @Column(name = "counter_account_bank_id", length = 50)
    private String counterAccountBankId;

    @Column(name = "counter_account_bank_name", length = 255)
    private String counterAccountBankName;

    @Column(name = "counter_account_name", length = 255)
    private String counterAccountName;

    @Column(name = "counter_account_number", length = 50)
    private String counterAccountNumber;

    @Column(name = "virtual_account_name", length = 255)
    private String virtualAccountName;

    @Column(name = "virtual_account_number", length = 50)
    private String virtualAccountNumber;

    // Refund fields
    @Column(name = "refund_status", length = 20)
    private String refundStatus; // NULL, PENDING, SUCCESS, FAILED

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    // Metadata
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @Column(name = "webhook_signature", length = 255)
    private String webhookSignature; // Lưu signature để verify sau này

    // Promotion tracking fields
    @Column(name = "promotion_code", length = 50)
    private String promotionCode; // Mã promotion đã sử dụng

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Số tiền giảm giá

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
