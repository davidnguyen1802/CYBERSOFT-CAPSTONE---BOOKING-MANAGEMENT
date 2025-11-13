package com.Cybersoft.Final_Capstone.components;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.exception.InvalidParamException;
import com.Cybersoft.Final_Capstone.repository.TokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.Getter;
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
import java.util.UUID;
import java.util.function.Function;

/**
 * NOTE: Unified JWT Service
 * - Access Token: stateless, không lưu DB, chỉ verify signature
 * - Refresh Token: stateful, lưu DB với JTI, có thể revoke
 */
@Component
@RequiredArgsConstructor
@Data
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Getter
    @Value("${jwt.access-expiration-seconds:900}")
    private long accessExpirationSeconds;

    @Getter
    @Value("${jwt.refresh-expiration-seconds:86400}")
    private long refreshExpirationSeconds;

    @Getter
    @Value("${jwt.remember-refresh-expiration-days:30}")
    private long rememberRefreshExpirationDays;

    private final TokenRepository tokenRepository;

    /**
     * Get refresh token expiration in milliseconds (for cookie maxAge)
     */
    public long getRefreshExpirationMs() {
        return refreshExpirationSeconds * 1000;
    }

    /**
     * Get refresh token expiration in milliseconds with rememberMe support
     */
    public long getRefreshExpirationMs(boolean rememberMe) {
        if (rememberMe) {
            return rememberRefreshExpirationDays * 24 * 3600 * 1000;
        }
        return refreshExpirationSeconds * 1000;
    }

    /**
     * NOTE: Generate access token (KHÔNG có JTI, stateless)
     */
    public String generateAccessToken(UserAccount userAccount) {
        return generateToken(userAccount, accessExpirationSeconds * 1000, null, "ACCESS");
    }

    /**
     * NOTE: Generate refresh token với JTI để track rotation
     * rememberMe flag ảnh hưởng đến expiration time
     */
    public String generateRefreshToken(UserAccount userAccount, boolean rememberMe, String rotatedFromJti) {
        String jti = UUID.randomUUID().toString();
        long expirationMs;

        if (rememberMe) {
            expirationMs = rememberRefreshExpirationDays * 24 * 3600 * 1000; // 30 days
        } else {
            expirationMs = refreshExpirationSeconds * 1000; // 1 day
        }

        return generateToken(userAccount, expirationMs, jti, "REFRESH", rotatedFromJti);
    }

    /**
     * NOTE: Overload cho backward compatibility (không có rotatedFromJti)
     */
    public String generateRefreshToken(UserAccount userAccount) {
        return generateRefreshToken(userAccount, false, null);
    }

    /**
     * NOTE: Core token generation method
     * UPDATED: AT payload includes userId, username, role, email
     * RT payload includes only jti for rotation tracking
     */
    private String generateToken(UserAccount userAccount, long expirationMs,
                                 String jti, String type, String... rotatedFromJti) {
        try {
            Map<String, Object> claims = new HashMap<>();

            if (jti != null) {
                // This is a refresh token - only jti needed
                claims.put("jti", jti);
            } else {
                // This is an access token - add userId, username, role, email
                claims.put("userId", userAccount.getId());
                claims.put("username", userAccount.getUsername());
                claims.put("role", userAccount.getRole().getName());
                claims.put("email", userAccount.getEmail());
            }

            Instant now = Instant.now();
            Instant expiration = now.plusMillis(expirationMs);

            String token = Jwts.builder()
                    .claims(claims)
                    .subject(String.valueOf(userAccount.getId()))
                    .issuer(issuer)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .signWith(getSignInKey(), Jwts.SIG.HS256)
                    .compact();

            logger.debug("JWT {} token generated for user ID: {}, jti={}",
                jti != null ? "RT" : "AT", userAccount.getId(), jti);
            return token;
        } catch (Exception e) {
            logger.error("Cannot create jwt token, error: {}", e.getMessage());
            throw new InvalidParamException("Cannot create jwt token, error: " + e.getMessage());
        }
    }

    private String generateToken(UserAccount userAccount, long expirationMs,
                                 String jti, String type) {
        return generateToken(userAccount, expirationMs, jti, type, (String) null);
    }

    private SecretKey getSignInKey() {
        byte[] bytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * NOTE: Extract JTI từ refresh token (cần cho rotation)
     */
    public String getJtiFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get("jti");
    }

    /**
     * NOTE: Get access token expiry time (cho response)
     */
    public Instant getAccessTokenExpiry() {
        return Instant.now().plusSeconds(accessExpirationSeconds);
    }

    /**
     * NOTE: Get refresh token expiry time
     */
    public Instant getRefreshTokenExpiry(boolean rememberMe) {
        if (rememberMe) {
            return Instant.now().plusSeconds(rememberRefreshExpirationDays * 24 * 3600);
        } else {
            return Instant.now().plusSeconds(refreshExpirationSeconds);
        }
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
     * Extract username from access token
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    /**
     * Extract email from access token
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extract role from access token
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    
    /**
     * Extract userId from access token claims
     * Note: userId is also stored in subject, but this method extracts from claims
     */
    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
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
     * Get userId from token subject (since we removed userId claim)
     * Subject now IS the user ID
     */
    public Integer getUserId(String token) {
        try {
            String subject = getSubject(token);
            return subject != null ? Integer.parseInt(subject) : null;
        } catch (NumberFormatException e) {
            logger.error("Invalid subject format - not a valid user ID: {}", e.getMessage());
            return null;
        }
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
     * NOTE: Validate Access Token - STATELESS, KHÔNG check DB
     * Chỉ verify: signature, expiration, subject, user status
     */
    public boolean validateToken(String token, UserAccount userDetails) {
        try {
            String subject = extractClaim(token, Claims::getSubject);

            // Check user status
            if (!userDetails.getStatus().getName().equals("ACTIVE")) {
                logger.warn("User is not active");
                return false;
            }

            // Verify subject matches userId
            boolean subjectMatches = false;
            try {
                Integer subjectAsId = Integer.parseInt(subject);
                subjectMatches = subjectAsId.equals(userDetails.getId());
                if (subjectMatches) {
                    logger.debug("Access token validated (stateless) for userId: {}", subjectAsId);
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Token subject is not a valid userId");
            }

            boolean notExpired = !isTokenExpired(token);

            return subjectMatches && notExpired;

        } catch (Exception e) {
            logger.error("Access token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * NOTE: Validate refresh token - chỉ JWT validation
     * DB check (revoked/expired) được thực hiện ở TokenService
     */
    public boolean validateRefreshToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.error("Refresh token validation error: {}", e.getMessage());
            return false;
        }
    }

}
