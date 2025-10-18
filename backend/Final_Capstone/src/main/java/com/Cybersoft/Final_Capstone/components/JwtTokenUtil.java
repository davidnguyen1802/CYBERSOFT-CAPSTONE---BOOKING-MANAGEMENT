package com.Cybersoft.Final_Capstone.components;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.Enum.TokenType;
import com.Cybersoft.Final_Capstone.exception.InvalidParamException;
import com.Cybersoft.Final_Capstone.repository.TokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final TokenRepository tokenRepository;

    /**
     * Generate access token for user
     */
    public String generateAccessToken(UserAccount userAccount) {
        return generateToken(userAccount, accessExpirationMs);
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(UserAccount userAccount) {
        return generateToken(userAccount, refreshExpirationMs);
    }

    /**
     * Generate JWT token with specified expiration
     */
    private String generateToken(UserAccount userAccount, long expirationMs) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userAccount.getId());

            Instant now = Instant.now();
            Instant expiration = now.plusMillis(expirationMs);

            String token = Jwts.builder()
                    .claims(claims)
                    .subject(getSubject(userAccount))
                    .issuer(issuer)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .signWith(getSignInKey(), Jwts.SIG.HS256)
                    .compact();

            logger.debug("JWT token generated for user: {}", userAccount.getId());
            return token;
        } catch (Exception e) {
            logger.error("Cannot create jwt token, error: {}", e.getMessage());
            throw new InvalidParamException("Cannot create jwt token, error: " + e.getMessage());
        }
    }

    /**
     * Get authentication subject (phone or email) from user
     */
    private String getSubject(UserAccount user) {
        return user.getAuthenticationSubject();
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSignInKey() {
        byte[] bytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Extract all claims from token using parser API
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Check if token is expired based on JWT expiration claim
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = extractClaim(token, Claims::getExpiration);
            return expirationDate.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Get subject from token
     */
    public String getSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Get userId from token claims
     */
    public Integer getUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    /**
     * Get issued at time
     */
    public Instant getIssuedAt(String token) {
        Date issuedAt = extractClaim(token, Claims::getIssuedAt);
        return issuedAt.toInstant();
    }

    /**
     * Get expiration time
     */
    public Instant getExpiration(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.toInstant();
    }

    /**
     * Validate token with signature, expiration, and database state checks
     */
    public boolean validateToken(String token, UserAccount userDetails) {
        try {
            // Parse and validate signature + expiration
            String subject = extractClaim(token, Claims::getSubject);

            // Check database token state
            Token existingToken = tokenRepository.findByToken(token);
            if (existingToken == null) {
                logger.warn("Token not found in database");
                return false;
            }

            if (existingToken.isRevoked()) {
                logger.warn("Token is revoked");
                return false;
            }

            if (existingToken.isExpired()) {
                logger.warn("Token is marked as expired in database");
                return false;
            }

            // Check user status
            if (!userDetails.getStatus().getName().equals("ACTIVE")) {
                logger.warn("User is not active");
                return false;
            }

            // Verify subject matches
            boolean subjectMatches = subject.equals(userDetails.getAuthenticationSubject());
            boolean notExpired = !isTokenExpired(token);

            return subjectMatches && notExpired;

        } catch (ExpiredJwtException e) {
            logger.debug("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate refresh token
     */
    public boolean validateRefreshToken(String token) {
        try {
            // Parse and validate signature + expiration
            extractAllClaims(token);

            // Check database token state
            Token existingToken = tokenRepository.findByToken(token);
            if (existingToken == null) {
                logger.warn("Refresh token not found in database");
                return false;
            }

            if (existingToken.getType() != TokenType.REFRESH) {
                logger.warn("Token is not a refresh token");
                return false;
            }

            if (existingToken.isRevoked()) {
                logger.warn("Refresh token is revoked");
                return false;
            }

            if (existingToken.isExpired()) {
                logger.warn("Refresh token is marked as expired in database");
                return false;
            }

            // Check expiration time
            return !isTokenExpired(token);

        } catch (ExpiredJwtException e) {
            logger.debug("Refresh token is expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Refresh token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get access token expiration in milliseconds
     */
    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    /**
     * Get refresh token expiration in milliseconds
     */
    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}
