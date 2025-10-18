package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "amenities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Amenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "amenity_name", nullable = false, unique = true)
    private String amenityName;

    @Column(name = "icon_url", nullable = false)
    private String iconUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToMany(mappedBy = "amenities")
    private List<Property> properties = new ArrayList<>();


}
