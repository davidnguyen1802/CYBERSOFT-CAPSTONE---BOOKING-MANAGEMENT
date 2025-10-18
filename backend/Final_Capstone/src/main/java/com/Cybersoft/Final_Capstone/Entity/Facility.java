package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "facility_name")
    private String facilityName;

    private Integer quantity;

    @Column(name = "icon_url", nullable = false)
    private String iconUrl;
    @ManyToMany(mappedBy = "facilities")
    private List<Property> properties = new ArrayList<>();
}
