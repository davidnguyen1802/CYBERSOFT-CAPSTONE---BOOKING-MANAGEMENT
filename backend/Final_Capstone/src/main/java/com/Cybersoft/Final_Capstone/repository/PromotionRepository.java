package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    Optional<Promotion> findByCodeAndStatus_Name(String code, String statusName);
    Optional<Promotion> findByNameAndStatus_Name(String name, String statusName);
}
