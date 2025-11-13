-- V3__Add_Promotion_Usage_And_Lock_Support.sql
-- Migration to support promotion usage tracking and concurrent payment handling

-- ==================== 1. Add is_locked to user_promotion ====================
-- Purpose: Prevent race condition where same UserPromotion is used for multiple concurrent bookings
-- Flow:
--   - User claims promotion → is_locked = false
--   - User initiates payment → is_locked = true (locked)
--   - Payment success → status = INACTIVE, is_locked = false (consumed)
--   - Payment fail → is_locked = false (unlocked, can reuse)

ALTER TABLE user_promotion 
ADD COLUMN is_locked BOOLEAN DEFAULT FALSE NOT NULL 
COMMENT 'Prevents concurrent use of same promotion during pending payments';

-- Add index for locked promotions query (optional optimization)
CREATE INDEX idx_user_promotion_locked ON user_promotion(user_account_id, is_locked);

-- ==================== 2. Verify promotion_usage table exists ====================
-- The promotion_usage table should already exist from initial schema
-- This is a safety check to ensure the composite key structure is correct

-- Expected structure:
-- CREATE TABLE promotion_usage (
--     user_promotion_id INT NOT NULL,
--     booking_id INT NOT NULL,
--     discount_amount DECIMAL(10,2) NOT NULL,
--     used_at DATETIME NOT NULL,
--     PRIMARY KEY (user_promotion_id, booking_id),
--     FOREIGN KEY (user_promotion_id) REFERENCES user_promotion(id),
--     FOREIGN KEY (booking_id) REFERENCES booking(id)
-- );

-- If table doesn't exist, create it (idempotent)
CREATE TABLE IF NOT EXISTS promotion_usage (
    user_promotion_id INT NOT NULL,
    booking_id INT NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL COMMENT 'Actual discount amount applied to this booking',
    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the promotion was consumed',
    
    PRIMARY KEY (user_promotion_id, booking_id),
    
    CONSTRAINT fk_promotion_usage_user_promotion 
        FOREIGN KEY (user_promotion_id) REFERENCES user_promotion(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_promotion_usage_booking 
        FOREIGN KEY (booking_id) REFERENCES booking(id) 
        ON DELETE CASCADE,
        
    INDEX idx_promotion_usage_booking (booking_id),
    INDEX idx_promotion_usage_user_promotion (user_promotion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 3. Update existing data ====================
-- Set all existing user_promotions to unlocked state
UPDATE user_promotion SET is_locked = FALSE WHERE is_locked IS NULL;












