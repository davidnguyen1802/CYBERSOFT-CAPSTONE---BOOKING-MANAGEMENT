package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.PasswordResetToken;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    List<PasswordResetToken> findByUserAccount(UserAccount userAccount);
    void deleteByExpiryDateBefore(LocalDateTime now);
    void deleteByUserAccount(UserAccount userAccount);
}

