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
@Table(name = "booking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDateTime checkOut;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "num_adults", nullable = false)
    private Integer numAdults;

    @Column(name = "num_children", nullable = false)
    private Integer numChildren;

    @Column(name = "num_infant", nullable = false)
    private Integer num_infant;

    @Column(name = "num_pet", nullable = false)
    private Integer numPet;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne
    @JoinColumn(name = "id_status", nullable = false)
    private Status status;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 10)
    private String cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== COMPUTED PROPERTIES ====================

    /**
     * Get payment deadline (confirmed_at + 24h)
     */
    @Transient
    public LocalDateTime getPaymentDeadline() {
        return confirmedAt != null ? confirmedAt.plusHours(24) : null;
    }

    /**
     * Check if payment has expired
     */
    @Transient
    public boolean isPaymentExpired() {
        LocalDateTime deadline = getPaymentDeadline();
        return deadline != null && LocalDateTime.now().isAfter(deadline);
    }

    /**
     * Get applied promotion from promotion_usage (if any)
     * Note: This requires a query to promotion_usage table
     */
    @Transient
    public String getAppliedPromotion() {
        // Will be populated by service layer if needed
        return null;
    }

    /**
     * One-to-one relationship with PromotionUsage
     * A booking can only use one promotion at a time
     */
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private PromotionUsage promotionUsage;
}
