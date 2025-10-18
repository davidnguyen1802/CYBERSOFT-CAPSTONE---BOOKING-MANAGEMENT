package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.UserPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Integer> {
    Integer countByUserAccount_IdAndStatus_Name(int userId, String statusName);
    List<UserPromotion> findByUserAccount_IdAndStatus_Name(int userId, String statusName);
}
