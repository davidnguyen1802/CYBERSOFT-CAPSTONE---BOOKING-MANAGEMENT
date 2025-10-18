package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.Enum.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findByUser(UserAccount user);

    Token findByToken(String token);

    @Query("SELECT t FROM Token t WHERE t.token = :token AND t.revoked = false AND t.expired = false")
    Optional<Token> findValidToken(@Param("token") String token);

    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.type = :type AND t.revoked = false AND t.expired = false")
    List<Token> findValidTokensByUserAndType(@Param("user") UserAccount user, @Param("type") TokenType type);

    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.type = 'REFRESH' AND t.revoked = false AND t.expired = false")
    List<Token> findValidRefreshTokensByUser(@Param("user") UserAccount user);
}
