//package com.Cybersoft.Final_Capstone.util;
//
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.JwtException;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.SecretKey;
//import java.util.Date;
//
//@Component
//public class JWTHelper {
//
//    private static final Logger logger = LoggerFactory.getLogger(JWTHelper.class);
//
//    @Value("${jwt.secret-key}")
//    private String secretKey;
//
//    @Value("${jwt.expiration:3600000}") // Default 1 hour (3600000 ms)
//    private long timeExpiration;
//
//    /**
//    @Value("${jwt.refresh-expiration}")
//     * @param userId The user ID to encode in the token
//     * @return JWT token string
//     */
//    public String generateToken(String userId) {
//        try {
//            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
//            Date currentDate = new Date();
//            Date expirationDate = new Date(currentDate.getTime() + timeExpiration);
//
//            String token = Jwts.builder()
//                    .subject(userId)
//                    .issuedAt(currentDate)
//                    .expiration(expirationDate)
//                    .signWith(key)
//                    .compact();
//
//            logger.info("JWT token generated successfully for user ID: {}", userId);
//            return token;
//        } catch (Exception e) {
//            logger.error("Error generating JWT token for user ID {}: {}", userId, e.getMessage());
//            throw new RuntimeException("Failed to generate JWT token", e);
//        }
//    }
//
//    /**
//     * Decode JWT token and extract user ID
//     * @param token JWT token string
//     * @return User ID from token subject, or null if invalid
//     */
//    public String decodeToken(String token) {
//        try {
//            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
//            String userId = Jwts.parser()
//                    .verifyWith(key)
//                    .build()
//                    .parseSignedClaims(token)
//                    .getPayload()
//                    .getSubject();
//
//            logger.debug("JWT token decoded successfully. User ID: {}", userId);
//            return userId;
//        } catch (ExpiredJwtException e) {
//            logger.warn("JWT token has expired: {}", e.getMessage());
//            return null;
//        } catch (JwtException e) {
//            logger.warn("Invalid JWT token: {}", e.getMessage());
//            return null;
//        } catch (Exception e) {
//            logger.error("Error decoding JWT token: {}", e.getMessage());
//            return null;
//        }
//    }
//}
