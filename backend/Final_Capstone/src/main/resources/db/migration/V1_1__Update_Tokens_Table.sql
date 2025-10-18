-- Migration script to update tokens table for production-grade JWT authentication
-- This script modifies the tokens table to support the new token structure with separate access/refresh tokens

-- Backup existing data (optional, comment out if not needed)
-- CREATE TABLE tokens_backup AS SELECT * FROM tokens;

-- Drop old columns
ALTER TABLE tokens
    DROP COLUMN IF EXISTS refresh_token,
    DROP COLUMN IF EXISTS token_type,
    DROP COLUMN IF EXISTS expiration_date,
    DROP COLUMN IF EXISTS refresh_expiration_date;

-- Modify token column to support longer JWTs
ALTER TABLE tokens
    MODIFY COLUMN token VARCHAR(512) NOT NULL;

-- Add new columns with proper structure
ALTER TABLE tokens
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'ACCESS',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Ensure expired and revoked columns exist with correct defaults
ALTER TABLE tokens
    MODIFY COLUMN expired TINYINT(1) NOT NULL DEFAULT 0,
    MODIFY COLUMN revoked TINYINT(1) NOT NULL DEFAULT 0;

-- Add index for performance on token lookups
CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens(token(255));
CREATE INDEX IF NOT EXISTS idx_tokens_user_type ON tokens(user_id, type);
CREATE INDEX IF NOT EXISTS idx_tokens_expires_at ON tokens(expires_at);

-- Add index for finding valid tokens
CREATE INDEX IF NOT EXISTS idx_tokens_valid ON tokens(revoked, expired, expires_at);

-- Clean up old expired tokens (optional)
-- DELETE FROM tokens WHERE expired = 1 AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

