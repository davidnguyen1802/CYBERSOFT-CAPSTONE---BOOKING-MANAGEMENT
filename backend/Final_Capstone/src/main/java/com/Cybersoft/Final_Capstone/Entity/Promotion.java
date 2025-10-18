package com.Cybersoft.Final_Capstone.Entity;

import com.Cybersoft.Final_Capstone.Enum.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String code;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal minPurchaseLimit;
    private BigDecimal maxDiscountAmount;

    private LocalDateTime startDate;
    private Integer remainingDays;
    private LocalDateTime endDate;

    private Integer usageLimit;
    private Integer timesUsed;

    @ManyToOne
    @JoinColumn(name = "id_status", nullable = false)
    private Status status;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "promotion")
    private List<UserPromotion> userPromotions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (startDate == null) startDate = LocalDateTime.now();
        timesUsed = 0;
        if(endDate == null){
            if(remainingDays == null || remainingDays <= 0){
                throw new IllegalArgumentException("remainingDays must be greater than 0");
            } else {
                endDate = startDate.plusDays(remainingDays);
            }
        }
        if(usageLimit == null){
            usageLimit = -1; // -1 means unlimited
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (endDate == null) {
            if (remainingDays == null || remainingDays <= 0) {
                throw new IllegalArgumentException("remainingDays must be greater than 0");
            } else {
                endDate = startDate.plusDays(remainingDays);
            }
        }
    }
}
