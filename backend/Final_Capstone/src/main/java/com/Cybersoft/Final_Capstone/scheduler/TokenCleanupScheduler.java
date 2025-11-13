package com.Cybersoft.Final_Capstone.scheduler;

import com.Cybersoft.Final_Capstone.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled job to cleanup old tokens from database
 * Reduces DB load by removing expired/revoked tokens
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);
    private final TokenRepository tokenRepository;

    /**
     * Delete expired tokens every day at 2 AM
     * Tokens are considered expired if expires_at < now
     */
    @Scheduled(cron = "0 0 2 * * ?")  // Run at 2:00 AM every day
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("🧹 Starting cleanup of expired tokens...");

        Instant now = Instant.now();

        // Find and delete all expired tokens (regardless of revoked status)
        int deletedCount = tokenRepository.deleteByExpiresAtBefore(now);

        logger.info("✅ Cleanup completed. Deleted {} expired tokens.", deletedCount);
    }

    /**
     * Delete old revoked tokens (> 7 days) every day at 3 AM
     * Keep revoked tokens for 7 days for audit purposes
     *
     * PRIORITY: Keep tokens from different devices (max 3 devices per user)
     * - If user has tokens from 3 different devices → keep all
     * - If user has multiple tokens from same device → keep only newest
     * - Only delete tokens that are revoked AND older than 7 days
     */
    @Scheduled(cron = "0 0 3 * * ?")  // Run at 3:00 AM every day
    @Transactional
    public void cleanupOldRevokedTokens() {
        logger.info("🧹 Starting cleanup of old revoked tokens...");

        Instant sevenDaysAgo = Instant.now().minusSeconds(7 * 24 * 3600);

        // Delete tokens that were revoked more than 7 days ago
        // This keeps revoked tokens for audit purposes (7 day retention)
        int deletedCount = tokenRepository.deleteByRevokedTrueAndRevokedAtBefore(sevenDaysAgo);

        logger.info("✅ Cleanup completed. Deleted {} old revoked tokens (>7 days).", deletedCount);
    }

    /**
     * Cleanup duplicate tokens from same device (keep only newest per device)
     * Run every day at 4 AM
     *
     * NOTE: This job is now OPTIONAL since the new "revoke-before-insert" logic
     * in addRefreshToken() and rotateRefreshToken() already ensures each device
     * has only 1 active token. This job serves as a SAFETY NET for edge cases.
     *
     * RECOMMENDATION: Enable for first few weeks after deployment, then disable if no duplicates found.
     */
    @Scheduled(cron = "0 0 4 * * ?")  // Run at 4:00 AM every day
    @Transactional
    public void cleanupDuplicateDeviceTokens() {
        logger.info("🧹 [OPTIONAL] Starting cleanup of duplicate device tokens...");

        int deletedCount = tokenRepository.deleteOldDuplicateDeviceTokens();

        if (deletedCount > 0) {
            logger.warn("⚠️ Found {} duplicate device tokens! Investigate revoke-before-insert logic.", deletedCount);
        } else {
            logger.info("✅ No duplicate device tokens found. Revoke-before-insert working correctly.");
        }
    }

    /**
     * Log token statistics every hour for monitoring
     */
    @Scheduled(cron = "0 0 * * * ?")  // Run every hour
    @Transactional(readOnly = true)
    public void logTokenStatistics() {
        long totalTokens = tokenRepository.count();
        long activeTokens = tokenRepository.countByRevokedFalseAndExpiresAtAfter(Instant.now());
        long revokedTokens = tokenRepository.countByRevokedTrue();

        logger.info("📊 Token Statistics - Total: {}, Active: {}, Revoked: {}",
                   totalTokens, activeTokens, revokedTokens);
    }
}
