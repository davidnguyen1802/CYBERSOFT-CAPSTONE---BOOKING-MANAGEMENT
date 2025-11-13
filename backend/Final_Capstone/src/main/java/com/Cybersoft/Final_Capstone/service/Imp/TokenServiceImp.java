package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.exception.ExpiredTokenException;
import com.Cybersoft.Final_Capstone.payload.response.RefreshTokenResponse;
import com.Cybersoft.Final_Capstone.repository.TokenRepository;
import com.Cybersoft.Final_Capstone.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * NOTE: TokenService - CHỈ quản lý REFRESH TOKENS
 * Access tokens là stateless JWT, không lưu DB
 *
 * REFACTORED:
 * - Use token_hash (SHA-256) instead of plaintext
 * - Remove expired flag (use expires_at)
 * - Add revoked_at for audit
 * - Remove device_info, ip_address
 *
 * Device Limit: Maximum 3 devices per user
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImp implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImp.class);
    private static final int MAX_DEVICES_PER_USER = 3;
    
    // UUID v4 validation pattern (case-insensitive)
    // Format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx where y is [89ab]
    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE
    );

    private final TokenRepository tokenRepository;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * Add refresh token with device tracking and 3-device limit enforcement
     *
     * NEW ALGORITHM - REVOKE-BEFORE-INSERT PATTERN:
     * 1. Extract deviceId (UUID v4) from X-Device-Id header (or null if missing/invalid)
     * 2. REVOKE ALL active tokens for same (user, deviceId) FIRST
     * 3. Check total active devices, enforce 3-device limit (evict oldest if needed)
     * 4. INSERT new token
     * 5. Cleanup old rotation chains
     *
     * Benefits:
     * - No more NonUniqueResultException (use List instead of Optional)
     * - Guarantees "1 active token per device" (revoke-before-insert)
     * - Thread-safe with PESSIMISTIC_WRITE locks
     *
     * @param user User account
     * @param token Refresh token string (JWT)
     * @param rememberMe Remember me flag (affects token expiry time)
     * @param request HTTP request (X-Device-Id header OPTIONAL, User-Agent fallback)
     * @return Saved token entity
     */
    @Transactional
    @Override
    public Token addRefreshToken(UserAccount user, String token, boolean rememberMe, HttpServletRequest request) {
        String jti = jwtTokenUtil.getJtiFromToken(token);
        String tokenHash = hashToken(token);
        Instant expiresAt = jwtTokenUtil.getRefreshTokenExpiry(rememberMe);
        Instant now = Instant.now();

        // ========== STEP 1: Extract device metadata ==========
        String deviceId = extractDeviceId(request);  // null if missing/invalid UUID v4
        String userAgent = extractUserAgent(request); // Fallback identifier

        String deviceKey = (deviceId != null) ? deviceId : userAgent;
        boolean useDeviceId = (deviceId != null);

        if (!useDeviceId) {
            logger.info("ℹ️ X-Device-Id not provided. Using user-agent fallback for user: {}", user.getId());
        }

        // ========== STEP 2: REVOKE-BEFORE-INSERT - Revoke ALL duplicates for same device FIRST ==========
        List<Token> duplicates;
        String parentJti = null; // For rotation chain tracking

        if (useDeviceId) {
            duplicates = tokenRepository.findAllActiveTokensByUserAndDeviceForUpdate(user, deviceId, now);
        } else {
            duplicates = tokenRepository.findAllActiveTokensByUserAndUserAgentForUpdate(user, userAgent, now);
        }

        if (!duplicates.isEmpty()) {
            // Revoke ALL duplicates (should be 1, but handle edge cases)
            Instant revokedAt = now;
            for (Token dup : duplicates) {
                dup.setRevoked(true);
                dup.setRevokedAt(revokedAt);
                if (parentJti == null) {
                    parentJti = dup.getJti(); // Track most recent parent for rotation chain
                }
            }
            tokenRepository.saveAll(duplicates);
            
            logger.info("🔒 [REVOKE-BEFORE-INSERT] Revoked {} duplicate token(s) for device. User: {}, DeviceKey: {}", 
                       duplicates.size(), user.getId(), deviceKey);
        }

        // ========== STEP 3: Enforce 3-device limit ==========
        List<Token> activeAll = tokenRepository.findActiveTokensByUserForUpdate(user, now);
        
        if (activeAll.size() >= MAX_DEVICES_PER_USER) {
            // At limit → Evict oldest device
            Token victim = activeAll.get(0); // Oldest by created_at ASC
            victim.setRevoked(true);
            victim.setRevokedAt(now);
            tokenRepository.save(victim);

            String victimKey = (victim.getDeviceId() != null) ? victim.getDeviceId() : victim.getUserAgent();
            logger.warn("⚠️ [EVICT] Device limit reached (3). Evicted oldest device. User: {}, VictimDevice: {}", 
                       user.getId(), victimKey);

            // Cleanup evicted token's rotation chain
            if (victim.getRotatedFrom() != null) {
                cleanupOldRotationChains(user, null, victim.getRotatedFrom());
            }
        }

        // ========== STEP 4: INSERT new token ==========
        Token newToken = Token.builder()
                .user(user)
                .tokenHash(tokenHash)
                .jti(jti)
                .revoked(false)
                .rememberMe(rememberMe)
            .rotatedFrom(parentJti) // Link to parent if replacing existing token
            .createdAt(now)
                .expiresAt(expiresAt)
            .deviceId(deviceId) // Can be null (fallback to user-agent)
                .userAgent(userAgent)
                .build();

        Token saved = tokenRepository.save(newToken);

        // ========== STEP 5: Cleanup old rotation chains ==========
        if (parentJti != null) {
            cleanupOldRotationChains(user, saved.getJti(), parentJti);
        }

        logger.info("✅ [INSERT] New token added. User: {}, DeviceKey: {}, JTI: {}, ParentJTI: {}", 
                   user.getId(), deviceKey, jti, parentJti);

        return saved;
    }

    @Transactional
    @Override
    public Token addRefreshToken(UserAccount user, String token) {
        // Fallback method without request → no device tracking
        logger.warn("⚠️ addRefreshToken called without HttpServletRequest for user {}. Device limit not enforced.", user.getId());

        String jti = jwtTokenUtil.getJtiFromToken(token);
        String tokenHash = hashToken(token);
        Instant expiresAt = jwtTokenUtil.getRefreshTokenExpiry(false);

        Token newToken = Token.builder()
                .user(user)
                .tokenHash(tokenHash)
                .jti(jti)
                .revoked(false)
                .rememberMe(false)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        tokenRepository.save(newToken);
        logger.debug("Refresh token added for user: {}", user.getId());
        return newToken;
    }

    /**
     * Rotate refresh token with device metadata update
     *
     * NEW ALGORITHM - REVOKE-BEFORE-INSERT:
     * 1. Validate JWT signature
     * 2. Find token by JTI (with lock)
     * 3. Check revoked status (detect reuse attack)
     * 4. Extract deviceId from old token
     * 5. REVOKE ALL duplicates for same device FIRST (including old token)
     * 6. Generate new JWT
     * 7. INSERT new token
     * 8. Cleanup rotation chains
     *
     * @param refreshTokenString Current refresh token
     * @param request HTTP request for device metadata (optional)
     * @return RefreshTokenResponse containing new JWT and Token entity
     * @throws Exception if token is invalid, expired, or revoked
     */
    @Transactional
    public RefreshTokenResponse rotateRefreshToken(String refreshTokenString, HttpServletRequest request) throws Exception {
        // ========== STEP 1: Validate JWT ==========
        if (!jwtTokenUtil.validateRefreshToken(refreshTokenString)) {
            throw new ExpiredTokenException("Invalid refresh token signature");
        }

        String jti = jwtTokenUtil.getJtiFromToken(refreshTokenString);
        Optional<Token> optionalToken = tokenRepository.findByJti(jti);

        if (optionalToken.isEmpty()) {
            throw new DataNotFoundException("Refresh token not found in database");
        }

        Token existingToken = optionalToken.get();

        // ========== STEP 2: Security checks ==========
        if (existingToken.isRevoked()) {
            logger.warn("⚠️ REUSE ATTACK DETECTED! JTI: {}, User: {}", jti, existingToken.getUser().getId());
            throw new ExpiredTokenException("Refresh token has been revoked. Possible reuse attack!");
        }

        if (existingToken.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(existingToken);
            throw new ExpiredTokenException("Refresh token has expired");
        }

        // ========== STEP 3: Extract metadata from old token ==========
        UserAccount user = existingToken.getUser();
        boolean rememberMe = existingToken.isRememberMe();
        String deviceId = existingToken.getDeviceId(); // Preserve deviceId from old token
        String userAgent = request != null ? extractUserAgent(request) : existingToken.getUserAgent();
        Instant now = Instant.now();

        // ========== STEP 4: REVOKE-BEFORE-INSERT - Revoke ALL duplicates for same device ==========
        List<Token> duplicates;
        
        if (deviceId != null && !deviceId.isEmpty() && !"Unknown".equals(deviceId)) {
            duplicates = tokenRepository.findAllActiveTokensByUserAndDeviceForUpdate(user, deviceId, now);
        } else {
            duplicates = tokenRepository.findAllActiveTokensByUserAndUserAgentForUpdate(user, userAgent, now);
        }

        // Revoke ALL duplicates (including the current token being rotated)
        if (!duplicates.isEmpty()) {
            for (Token dup : duplicates) {
                dup.setRevoked(true);
                dup.setRevokedAt(now);
            }
            tokenRepository.saveAll(duplicates);
            
            logger.info("🔒 [REFRESH] Revoked {} duplicate token(s) for device. User: {}, DeviceId: {}", 
                       duplicates.size(), user.getId(), deviceId);
        }

        // Also ensure the current token is revoked (in case it wasn't in duplicates list)
        if (!existingToken.isRevoked()) {
            existingToken.setRevoked(true);
            existingToken.setRevokedAt(now);
            tokenRepository.save(existingToken);
        }

        // ========== STEP 5: Generate new JWT ==========
        String newRefreshTokenJwt = jwtTokenUtil.generateRefreshToken(user, rememberMe, jti);
        String newJti = jwtTokenUtil.getJtiFromToken(newRefreshTokenJwt);
        String newTokenHash = hashToken(newRefreshTokenJwt);
        Instant expiresAt = jwtTokenUtil.getRefreshTokenExpiry(rememberMe);

        // ========== STEP 6: INSERT new token ==========
        Token newTokenEntity = Token.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .jti(newJti)
                .revoked(false)
                .rememberMe(rememberMe)
                .rotatedFrom(jti) // Link to parent
                .createdAt(now)
                .expiresAt(expiresAt)
                .deviceId(deviceId) // Preserve from old token
                .userAgent(userAgent)
                .build();

        Token saved = tokenRepository.save(newTokenEntity);

        // ========== STEP 7: Cleanup rotation chains ==========
        cleanupOldRotationChains(user, saved.getJti(), jti);

        logger.info("✅ [REFRESH] Token rotated. User: {}, OldJTI: {}, NewJTI: {}, DeviceId: {}",
                   user.getId(), jti, newJti, deviceId);

        // Return both JWT string and entity
        return RefreshTokenResponse.builder()
                .jwtToken(newRefreshTokenJwt)
                .tokenEntity(saved)
                .build();
    }

    @Transactional
    @Override
    public RefreshTokenResponse refreshToken(String refreshToken, HttpServletRequest request) throws Exception {
        return rotateRefreshToken(refreshToken, request);
    }

    @Transactional
    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) throws Exception {
        return rotateRefreshToken(refreshToken, null);
    }

    @Transactional
    @Override
    public void revokeToken(String token) {
        String tokenHash = hashToken(token);
        Optional<Token> optionalToken = tokenRepository.findByTokenHash(tokenHash);

        if (optionalToken.isPresent()) {
            Token existingToken = optionalToken.get();
            existingToken.setRevoked(true);
            existingToken.setRevokedAt(Instant.now());
            tokenRepository.save(existingToken);
            logger.info("🔒 Refresh token revoked. User: {}, DeviceId: {}",
                       existingToken.getUser().getId(), existingToken.getDeviceId());
        }
    }

    @Transactional
    public void revokeTokenByJti(String jti) {
        Optional<Token> optionalToken = tokenRepository.findByJti(jti);
        if (optionalToken.isPresent()) {
            Token token = optionalToken.get();
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            tokenRepository.save(token);
            logger.info("🔒 Refresh token revoked by JTI: {}", jti);
        }
    }

    @Transactional
    @Override
    public void revokeAllUserTokens(UserAccount user) {
        List<Token> userTokens = tokenRepository.findByUser(user);
        if (!userTokens.isEmpty()) {
            Instant now = Instant.now();
            userTokens.forEach(token -> {
                token.setRevoked(true);
                token.setRevokedAt(now);
            });
            tokenRepository.saveAll(userTokens);
            logger.info("🔒 All refresh tokens revoked for user: {} (logout-all). Total: {}",
                       user.getId(), userTokens.size());
        }
    }

    @Transactional
    @Override
    public void revokeAllUserRefreshTokens(UserAccount user) {
        List<Token> refreshTokens = tokenRepository.findValidRefreshTokensByUser(user, Instant.now());
        if (!refreshTokens.isEmpty()) {
            Instant now = Instant.now();
            refreshTokens.forEach(token -> {
                token.setRevoked(true);
                token.setRevokedAt(now);
            });
            tokenRepository.saveAll(refreshTokens);
            logger.info("🔒 All active refresh tokens revoked for user: {}. Total: {}",
                       user.getId(), refreshTokens.size());
        }
    }

    /**
     * Hash token using SHA-256
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Extract and validate device ID from X-Device-Id header
     * 
     * SIMPLIFIED LOGIC (Option 2 - FE Ownership):
     * - Frontend MUST generate and send UUID v4 in X-Device-Id header
     * - Returns null if header is missing/empty/invalid
     * - Validates UUID v4 format (strict)
     * - Normalizes to lowercase
     * - Logs warning if format is invalid
     * 
     * REMOVED:
     * - ❌ No more generateDeviceId() fallback
     * - ❌ No more request.getAttribute("X-Device-Id-Generated")
     * - ❌ Controller now rejects (428) instead of generating UUID
     * 
     * @param request HTTP request
     * @return Valid UUID v4 string (lowercase) or null
     */
    private String extractDeviceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        String deviceId = request.getHeader("X-Device-Id");
        
        // Missing or empty header → return null (Controller will reject with 428)
        if (deviceId == null || deviceId.trim().isEmpty() || "Unknown".equalsIgnoreCase(deviceId)) {
            return null;
        }
        
        String trimmed = deviceId.trim().toLowerCase();
        
        // Validate UUID v4 format (strict)
        if (!UUID_V4_PATTERN.matcher(trimmed).matches()) {
            logger.warn("⚠️ Invalid X-Device-Id format (expected UUID v4): '{}'. Treating as missing.", deviceId);
            return null;
        }
        
        // Valid UUID v4
        return trimmed;
    }

    /**
     * Extract User-Agent header (fallback identifier when deviceId is null)
     * Truncates to 512 chars max (DB column limit)
     * 
     * @param request HTTP request
     * @return User-Agent string (max 512 chars) or "Unknown"
     */
    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "Unknown";
        }
        
        // Truncate to DB column limit (512 chars)
        return userAgent.substring(0, Math.min(userAgent.length(), 512));
    }

    /**
     * Cleanup old rotation chains
     * Strategy: Keep only 1 most recent rotation (the immediate parent)
     * Delete older revoked tokens that are 2+ rotations back
     *
     * @param user User account
     * @param currentJti Current active token JTI (to protect)
     * @param parentJti Immediate parent JTI (1 rotation back, to protect)
     */
    private void cleanupOldRotationChains(UserAccount user, String currentJti, String parentJti) {
        try {
            // Build list of protected JTIs (tokens we must NOT delete)
            List<String> protectedJtis = new java.util.ArrayList<>();
            if (currentJti != null) protectedJtis.add(currentJti);
            if (parentJti != null) protectedJtis.add(parentJti);

            // If no protected JTIs, don't run cleanup (avoid deleting everything)
            if (protectedJtis.isEmpty()) {
                return;
            }

            // Find old rotation chain tokens (revoked tokens NOT in protected list)
            List<Token> oldTokens = tokenRepository.findOldRotationChainTokensForUpdate(user, protectedJtis);

            if (!oldTokens.isEmpty()) {
                tokenRepository.deleteAll(oldTokens);
                logger.info("🧹 [CLEANUP] Deleted {} old rotation chain tokens for user: {}", oldTokens.size(), user.getId());
            }
        } catch (Exception e) {
            // Don't fail the main operation if cleanup fails
            logger.error("❌ [CLEANUP] Failed to cleanup old rotation chains for user: {}. Error: {}", user.getId(), e.getMessage());
        }
    }
}
