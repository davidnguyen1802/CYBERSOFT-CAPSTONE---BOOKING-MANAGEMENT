package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "role")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "role")
    private List<UserAccount> users = new ArrayList<>();

    public static String ADMIN = "ADMIN";
    public static String HOST = "HOST";
    public static String GUEST = "GUEST";

    public Role(int id){
        this.id = id;
        switch (id){
            case 1:
                this.name = ADMIN;
                break;
            case 2:
                this.name = HOST;
                break;
            default:
                this.name = GUEST;
        }
    }

}
