package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "status")
    private List<UserPromotion> userPromotions;

    @OneToMany(mappedBy = "status")
    private List<Promotion> promotions;

    @OneToMany(mappedBy = "status")
    private List<Property> properties;

    @OneToMany(mappedBy = "status")
    private List<Booking> bookings;

    @OneToMany(mappedBy = "status")
    private List<UserAccount> userAccounts;

    /**
     * Constructor với ID - dùng để tạo Status object mà không cần query DB
     * QUAN TRỌNG: Phải verify IDs từ database thực tế trước khi sử dụng
     */
    public Status(int id) {
        this.id = id;
        switch (id) {
            case 1:
                this.name = "ACTIVE";
                break;
            case 2:
                this.name = "INACTIVE";
                break;
            case 3:
                this.name = "DELETED";
                break;
            case 4:
                this.name = "AVAILABLE";
                break;
            case 5:
                this.name = "UNAVAILABLE";
                break;
            case 6:
                this.name = "PENDING"; // Booking status
                break;
            case 7:
                this.name = "CONFIRMED"; // Booking status
                break;
            case 8:
                this.name = "PAID"; // Booking status
                break;
            case 9:
                this.name = "COMPLETED"; // Booking status
                break;
            case 10:
                this.name = "CANCELLED"; // Booking status
                break;
            case 11:
                this.name = "REJECTED"; // Booking status
                break;
            case 12:
                this.name = "USED"; // UserPromotion status (after payment success)
                break;
            default:
                this.name = "Unknown";
                break;
        }
    }


}
