package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PromotionUsageId implements Serializable {
    @Column(name = "user_promotion_id")
    private Integer userPromotionId;

    @Column(name = "booking_id")
    private Integer bookingId;
}
