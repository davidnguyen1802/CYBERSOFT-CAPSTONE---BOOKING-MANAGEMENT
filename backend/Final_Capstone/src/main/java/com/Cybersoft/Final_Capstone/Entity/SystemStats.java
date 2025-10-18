package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_stats")
public class SystemStats {
    @Id
    private Long id = 1L; // bảng chỉ có 1 row duy nhất

    @Column(nullable = false)
    private long totalUsers = 0;

    @Column(nullable = false)
    private long totalHosts = 0;

    @Column(nullable = false)
    private long totalProperties = 0;

    public void incUser() { this.totalUsers++; }
    public void incHost() { this.totalHosts++; }
    public void incProperty() { this.totalProperties++; }
}
