package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * NOTE: TokenRepository - CHỈ quản lý REFRESH TOKENS
 * Access tokens không lưu DB (stateless)
 *
 * REFACTORED: All queries now use expires_at instead of expired flag
 * OPTIMIZED: Added pessimistic locking for 3-device limit enforcement
 */
public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findByUser(UserAccount user);

    // NOTE: Find by token hash for integrity check (optional use)
    Optional<Token> findByTokenHash(String tokenHash);

    // NOTE: Tìm token theo JTI (primary lookup method)
    Optional<Token> findByJti(String jti);

    // NOTE: Find valid token by hash
    @Query("SELECT t FROM Token t WHERE t.tokenHash = :tokenHash AND t.revoked = false AND t.expiresAt > :now")
    Optional<Token> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    // NOTE: Find all valid tokens for user
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.revoked = false AND t.expiresAt > :now")
    List<Token> findValidTokensByUser(@Param("user") UserAccount user, @Param("now") Instant now);

    // NOTE: Find valid refresh tokens (alias for clarity)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.revoked = false AND t.expiresAt > :now")
    List<Token> findValidRefreshTokensByUser(@Param("user") UserAccount user, @Param("now") Instant now);

    // NOTE: Find expired tokens for cleanup (scheduled job)
    @Query("SELECT t FROM Token t WHERE t.expiresAt < :now AND t.revoked = false")
    List<Token> findExpiredTokens(@Param("now") Instant now);

    // NOTE: Check if JTI exists and is valid
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Token t WHERE t.jti = :jti AND t.revoked = false AND t.expiresAt > :now")
    boolean existsByJtiAndValid(@Param("jti") String jti, @Param("now") Instant now);

    // ==================== PESSIMISTIC LOCKING QUERIES FOR 3-DEVICE LIMIT ====================

    /**
     * Find all active tokens for user with PESSIMISTIC WRITE lock
     * Used for device limit enforcement to prevent race conditions
     * ORDER BY created_at ASC to easily identify oldest token for eviction
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.revoked = false AND t.expiresAt > :now ORDER BY t.createdAt ASC")
    List<Token> findActiveTokensByUserForUpdate(@Param("user") UserAccount user, @Param("now") Instant now);

    /**
     * Find active token for specific device with PESSIMISTIC WRITE lock
     * Returns newest token if multiple exist (should not happen after optimization)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.deviceId = :deviceId AND t.revoked = false AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    Optional<Token> findActiveTokenByUserAndDeviceForUpdate(@Param("user") UserAccount user, @Param("deviceId") String deviceId, @Param("now") Instant now);

    /**
     * Find ANY revoked token for specific device with PESSIMISTIC WRITE lock
     * Used for token recycling - prioritize oldest revoked token
     * This allows reusing DB row instead of inserting new one
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.deviceId = :deviceId AND t.revoked = true ORDER BY t.revokedAt ASC")
    Optional<Token> findRevokedTokenByUserAndDeviceForUpdate(@Param("user") UserAccount user, @Param("deviceId") String deviceId);

    /**
     * Find all active tokens by user and user_agent with PESSIMISTIC WRITE lock
     * Fallback when device_id is not available
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.userAgent = :userAgent AND t.revoked = false AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<Token> findActiveTokensByUserAndUserAgentForUpdate(@Param("user") UserAccount user, @Param("userAgent") String userAgent, @Param("now") Instant now);

    /**
     * Find revoked token by user and user_agent with PESSIMISTIC WRITE lock
     * For recycling when device_id is not available
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.userAgent = :userAgent AND t.revoked = true ORDER BY t.revokedAt ASC")
    Optional<Token> findRevokedTokenByUserAndUserAgentForUpdate(@Param("user") UserAccount user, @Param("userAgent") String userAgent);

    /**
     * Find old rotation chain tokens (revoked tokens that are 2+ rotations old)
     * Keep only 1 most recent rotation (rotated_from of current active token)
     * Delete older revoked tokens to reduce DB bloat
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.revoked = true AND t.jti NOT IN :protectedJtis ORDER BY t.revokedAt ASC")
    List<Token> findOldRotationChainTokensForUpdate(@Param("user") UserAccount user, @Param("protectedJtis") List<String> protectedJtis);

    /**
     * NEW: Find ALL active tokens for specific device (return List to avoid NonUniqueResultException)
     * Used to revoke ALL duplicates before inserting new token (revoke-before-insert pattern)
     * ORDER BY created_at DESC to get newest first
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.deviceId = :deviceId " +
           "AND t.revoked = false AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<Token> findAllActiveTokensByUserAndDeviceForUpdate(
        @Param("user") UserAccount user, 
        @Param("deviceId") String deviceId, 
        @Param("now") Instant now
    );

    /**
     * NEW: Find ALL active tokens for specific user_agent (fallback when no deviceId)
     * Returns List to handle multiple duplicates (revoke-before-insert pattern)
     * ORDER BY created_at DESC to get newest first
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.userAgent = :userAgent " +
           "AND (t.deviceId IS NULL OR t.deviceId = 'Unknown') " +
           "AND t.revoked = false AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<Token> findAllActiveTokensByUserAndUserAgentForUpdate(
        @Param("user") UserAccount user, 
        @Param("userAgent") String userAgent, 
        @Param("now") Instant now
    );

    // ==================== LEGACY QUERIES (kept for backward compatibility) ====================

    // NOTE: Device-based queries for 3-device limit feature (NON-LOCKING - use ForUpdate variants above)
    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.revoked = false AND t.expiresAt > :now ORDER BY t.createdAt ASC")
    List<Token> findActiveTokensByUserOrderByCreatedAsc(@Param("user") UserAccount user, @Param("now") Instant now);

    @Query("SELECT t FROM Token t WHERE t.user = :user AND t.deviceId = :deviceId AND t.revoked = false AND t.expiresAt > :now")
    Optional<Token> findActiveTokenByUserAndDevice(@Param("user") UserAccount user, @Param("deviceId") String deviceId, @Param("now") Instant now);

    // NOTE: Cleanup methods for scheduled jobs (reduce DB load)

    /**
     * Delete all expired tokens (expires_at < now)
     * @return number of deleted tokens
     */
    int deleteByExpiresAtBefore(Instant expiresAt);

    /**
     * Delete old revoked tokens (for audit retention - keep 7 days)
     * @return number of deleted tokens
     */
    @Query("DELETE FROM Token t WHERE t.revoked = true AND t.revokedAt < :revokedAt")
    int deleteByRevokedTrueAndRevokedAtBefore(@Param("revokedAt") Instant revokedAt);

    /**
     * Delete old duplicate tokens from same device (keep only newest per device)
     * Priority: Keep tokens from different devices
     *
     * For each user+device combination, keep only the newest token
     * This ensures:
     * - Each device has max 1 active token
     * - Different devices are preserved (up to 3 devices per user)
     * - Reduces DB load from duplicate logins on same device
     *
     * @return number of deleted tokens
     */
    @Query(value = """
        DELETE FROM tokens 
        WHERE id IN (
            SELECT t1.id 
            FROM tokens t1
            WHERE t1.revoked = 0 
              AND t1.device_id IS NOT NULL 
              AND t1.device_id != 'Unknown'
              AND EXISTS (
                SELECT 1 
                FROM tokens t2 
                WHERE t2.user_id = t1.user_id 
                  AND t2.device_id = t1.device_id
                  AND t2.revoked = 0
                  AND t2.created_at > t1.created_at
              )
        )
        """, nativeQuery = true)
    int deleteOldDuplicateDeviceTokens();

    /**
     * Count active tokens (not revoked and not expired)
     */
    long countByRevokedFalseAndExpiresAtAfter(Instant now);

    /**
     * Count revoked tokens
     */
    long countByRevokedTrue();
}
