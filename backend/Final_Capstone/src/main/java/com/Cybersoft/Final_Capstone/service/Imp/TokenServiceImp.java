package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.Enum.TokenType;
import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.ExpiredTokenException;
import com.Cybersoft.Final_Capstone.repository.TokenRepository;
import com.Cybersoft.Final_Capstone.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenServiceImp implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImp.class);
    private static final int MAX_ACCESS_TOKENS = 3;

    private final TokenRepository tokenRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    @Override
    public Token addAccessToken(UserAccount user, String token, boolean isMobileDevice) {
        List<Token> userAccessTokens = tokenRepository.findValidTokensByUserAndType(user, TokenType.ACCESS);
        int tokenCount = userAccessTokens.size();

        // Remove old tokens if exceeding limit
        if (tokenCount >= MAX_ACCESS_TOKENS) {
            boolean hasNonMobileToken = userAccessTokens.stream().anyMatch(t -> !t.isMobile());
            Token tokenToDelete;
            if (hasNonMobileToken) {
                tokenToDelete = userAccessTokens.stream()
                        .filter(userToken -> !userToken.isMobile())
                        .findFirst()
                        .orElse(userAccessTokens.get(0));
            } else {
                tokenToDelete = userAccessTokens.get(0);
            }
            tokenRepository.delete(tokenToDelete);
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtTokenUtil.getAccessExpirationMs());

        Token newToken = Token.builder()
                .user(user)
                .token(token)
                .type(TokenType.ACCESS)
                .revoked(false)
                .expired(false)
                .createdAt(now)
                .expiresAt(expiresAt)
                .isMobile(isMobileDevice)
                .build();

        tokenRepository.save(newToken);
        logger.debug("Access token added for user: {}", user.getId());
        return newToken;
    }

    @Transactional
    @Override
    public Token addRefreshToken(UserAccount user, String token) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtTokenUtil.getRefreshExpirationMs());

        Token newToken = Token.builder()
                .user(user)
                .token(token)
                .type(TokenType.REFRESH)
                .revoked(false)
                .expired(false)
                .createdAt(now)
                .expiresAt(expiresAt)
                .isMobile(false)
                .build();

        tokenRepository.save(newToken);
        logger.debug("Refresh token added for user: {}", user.getId());
        return newToken;
    }

    @Transactional
    @Override
    public Token refreshToken(String refreshToken) throws Exception {
        Token existingToken = tokenRepository.findByToken(refreshToken);

        if (existingToken == null) {
            throw new DataNotFoundException("Refresh token does not exist");
        }

        if (existingToken.getType() != TokenType.REFRESH) {
            throw new DataNotFoundException("Token is not a refresh token");
        }

        if (existingToken.isRevoked()) {
            throw new ExpiredTokenException("Refresh token is revoked");
        }

        if (existingToken.isExpired() || existingToken.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(existingToken);
            throw new ExpiredTokenException("Refresh token is expired");
        }

        // Validate JWT signature and expiration
        if (!jwtTokenUtil.validateRefreshToken(refreshToken)) {
            throw new ExpiredTokenException("Refresh token validation failed");
        }

        UserAccount user = existingToken.getUser();

        // Generate new access token
        String newAccessToken = jwtTokenUtil.generateAccessToken(user);

        // Generate new refresh token (rotation)
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(user);

        // Revoke old refresh token
        existingToken.setRevoked(true);
        existingToken.setExpired(true);
        tokenRepository.save(existingToken);

        // Save new refresh token
        Token newRefreshTokenEntity = addRefreshToken(user, newRefreshToken);

        logger.info("Refresh token rotated for user: {}", user.getId());

        // Return the new refresh token entity (access token will be added separately)
        return newRefreshTokenEntity;
    }

    @Transactional
    @Override
    public void revokeToken(String token) {
        Token existingToken = tokenRepository.findByToken(token);
        if (existingToken != null) {
            existingToken.setRevoked(true);
            existingToken.setExpired(true);
            tokenRepository.save(existingToken);
            logger.debug("Token revoked: type={}", existingToken.getType());
        }
    }

    @Transactional
    @Override
    public void revokeAllUserTokens(UserAccount user) {
        List<Token> userTokens = tokenRepository.findByUser(user);
        if (!userTokens.isEmpty()) {
            userTokens.forEach(token -> {
                token.setRevoked(true);
                token.setExpired(true);
            });
            tokenRepository.saveAll(userTokens);
            logger.info("All tokens revoked for user: {}", user.getId());
        }
    }

    @Transactional
    @Override
    public void revokeAllUserRefreshTokens(UserAccount user) {
        List<Token> refreshTokens = tokenRepository.findValidRefreshTokensByUser(user);
        if (!refreshTokens.isEmpty()) {
            refreshTokens.forEach(token -> {
                token.setRevoked(true);
                token.setExpired(true);
            });
            tokenRepository.saveAll(refreshTokens);
            logger.info("All refresh tokens revoked for user: {}", user.getId());
        }
    }
}
