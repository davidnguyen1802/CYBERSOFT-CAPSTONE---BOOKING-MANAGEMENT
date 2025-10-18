package com.Cybersoft.Final_Capstone.Entity;

import com.Cybersoft.Final_Capstone.Enum.PropertyType;
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
@Table(name = "property")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    private UserAccount host;

    @Column(name = "full_address", nullable = false, columnDefinition = "TEXT")
    private String fullAddress;

    @Column(name = "property_name", nullable = false)
    private String propertyName;

    @Column(name = "price", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "num_rooms")
    private Integer numberOfBedrooms;

    @Column(name = "num_bathrooms")
    private Integer numberOfBathrooms;

    @Column(name ="max_adults")
    private Integer maxAdults;

    @Column(name ="max_children")
    private Integer maxChildren;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "max_infants")
    private Integer maxInfants;

    @Column(name = "max_pets")
    private Integer maxPets;

    @Column(name = "overall_rating")
    private BigDecimal overallRating;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    @Column(name = "create_date")
    @CreationTimestamp
    private LocalDateTime createDate;
    @UpdateTimestamp
    private LocalDateTime updateDate;

    @ManyToOne
    @JoinColumn(name = "id_status", nullable = false)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @OneToMany(mappedBy = "property")
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "property")
    private List<UserReview> reviews = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "property_amenity",
        joinColumns = @JoinColumn(name = "property_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private List<Amenity> amenities = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "property_facility",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "facility_id")
    )
    private List<Facility> facilities = new ArrayList<>();

    @ManyToMany(mappedBy = "favoriteList")
    private List<UserAccount> favoriteBy = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if(favoriteBy.isEmpty()){
            favoriteBy = new ArrayList<>();
        }
    }
}
