package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_promotion",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_promotion_slot", columnNames = {"promotion_id", "user_account_id"})
        },
        indexes = {
                @Index(name = "idx_user_promotion_user", columnList = "user_account_id,id_status")
        })
@Data
public class UserPromotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean isLocked = false; // Prevents concurrent use during pending payments

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "id_status", nullable = false)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @OneToMany(mappedBy = "userPromotion")
    private List<PromotionUsage> usages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        assignedAt = LocalDateTime.now();
        if(promotion != null && promotion.getRemainingDays() != null) {
            expiresAt = assignedAt.plusDays(promotion.getRemainingDays());
        }
    }
}
