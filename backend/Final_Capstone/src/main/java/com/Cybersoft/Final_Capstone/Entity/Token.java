package com.Cybersoft.Final_Capstone.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * NOTE: Token entity - CHỈ lưu REFRESH TOKENS
 * Access tokens là stateless JWT, không lưu DB
 *
 * REFACTORED:
 * - token → token_hash (SHA-256, 64 chars)
 * - expired → removed (use expires_at)
 * - device_info → removed (duplicate of user_agent)
 * - ip_address → removed (not needed for audit)
 * - revoked_at → added (audit timestamp)
 */
@Entity
@Table(name = "tokens", indexes = {
    @Index(name = "idx_jti", columnList = "jti"),
    @Index(name = "idx_token_hash", columnList = "token_hash"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_user_active", columnList = "user_id, revoked, expires_at"),
    @Index(name = "idx_device_limit", columnList = "user_id, device_id, revoked, expires_at"),
    @Index(name = "idx_cleanup", columnList = "expires_at, revoked")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // NOTE: SHA-256 hash của JWT (64 chars) - for integrity check
    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    // NOTE: JWT ID để track RT rotation và detect reuse attack
    @Column(name = "jti", length = 100, unique = true, nullable = false)
    private String jti;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    // NOTE: Audit timestamp - when was this token revoked
    @Column(name = "revoked_at")
    private Instant revokedAt;

    // NOTE: Track RT rotation chain để detect reuse
    @Column(name = "rotated_from", length = 100)
    private String rotatedFrom;

    // NOTE: Remember me flag - affects cookie persistence
    @Column(name = "remember_me", nullable = false)
    @Builder.Default
    private boolean rememberMe = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // NOTE: Device tracking for 3-device limit
    @Column(name = "device_id", length = 128)
    private String deviceId;

    // NOTE: User agent for session management and audit
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Helper method kiểm tra token còn hạn
     * NOTE: Không dùng expired flag nữa, tính từ expires_at
     */
    public boolean isValid() {
        return !this.revoked && Instant.now().isBefore(this.expiresAt);
    }
}
