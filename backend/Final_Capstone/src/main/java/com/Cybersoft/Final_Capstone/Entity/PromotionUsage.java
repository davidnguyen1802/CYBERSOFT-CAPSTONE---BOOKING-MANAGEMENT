package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "promotion_usage")
public class PromotionUsage implements Serializable {

    @EmbeddedId
    private PromotionUsageId id;

    // map phần user_promotion_id của composite key
    @MapsId("userPromotionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_promotion_id", nullable = false)
    private UserPromotion userPromotion;

    // map phần booking_id của composite key
    @MapsId("bookingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
