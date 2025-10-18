package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.SystemStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SystemStatsRepository extends JpaRepository<SystemStats, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SystemStats s where s.id = 1")
    SystemStats lockOne(); // lấy record duy nhất với khóa ghi

    // Tùy chọn: update atomic cho hiệu năng
    @Modifying
    @Query("update SystemStats s set s.totalUsers = s.totalUsers + 1 where s.id = 1")
    int incUsers();

    @Modifying
    @Query("update SystemStats s set s.totalHosts = s.totalHosts + 1 where s.id = 1")
    int incHosts();

    @Modifying
    @Query("update SystemStats s set s.totalProperties = s.totalProperties + 1 where s.id = 1")
    int incProperties();
}

