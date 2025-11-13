-- ============================================================
-- Migration: Add Booking Payment Flow Support
-- Description: Thêm các columns và statuses cần thiết cho luồng
--              approve/reject/cancel booking và promotion tracking
-- ============================================================

-- ============================================================
-- 1. ADD NEW COLUMNS TO BOOKING TABLE
-- ============================================================

ALTER TABLE booking
    ADD COLUMN confirmed_at DATETIME NULL COMMENT 'Timestamp khi host approve booking',
    ADD COLUMN cancelled_at DATETIME NULL COMMENT 'Timestamp khi booking bị cancel',
    ADD COLUMN cancelled_by ENUM('guest', 'system') NULL COMMENT 'Ai đã cancel booking (guest hoặc system)',
    ADD COLUMN cancel_reason TEXT NULL COMMENT 'Lý do cancel booking';

-- Add check constraint
ALTER TABLE booking
    ADD CONSTRAINT chk_cancelled_by CHECK (cancelled_by IN ('guest', 'system') OR cancelled_by IS NULL);

-- ============================================================
-- 2. ADD NEW COLUMNS TO TRANSACTION TABLE
-- ============================================================

ALTER TABLE transaction
    ADD COLUMN promotion_code VARCHAR(50) NULL COMMENT 'Mã promotion đã sử dụng',
    ADD COLUMN discount_amount DECIMAL(10,2) NULL DEFAULT 0.00 COMMENT 'Số tiền giảm giá';

-- ============================================================
-- 3. ADD VERSION COLUMN TO PROMOTION (OPTIMISTIC LOCKING)
-- ============================================================

ALTER TABLE promotion
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Version cho optimistic locking';

-- ============================================================
-- 4. ADD STATUS TO USER_PROMOTION
-- ============================================================

ALTER TABLE user_promotion
    ADD COLUMN id_status INT NOT NULL DEFAULT 1 COMMENT 'FK to status table (1=ACTIVE, 2=INACTIVE)',
    ADD CONSTRAINT fk_user_promotion_status FOREIGN KEY (id_status) REFERENCES status(id);

-- ============================================================
-- 5. INSERT NEW STATUSES FOR BOOKING FLOW
-- ============================================================

-- Check và insert statuses nếu chưa tồn tại
INSERT IGNORE INTO status (id, name) VALUES
    (6, 'PENDING'),      -- Booking chờ host approve
    (7, 'CONFIRMED'),    -- Host đã approve, chờ payment
    (8, 'PAID'),         -- Đã thanh toán
    (9, 'COMPLETED'),    -- Hoàn thành (sau check-out)
    (10, 'CANCELLED'),   -- Đã hủy
    (11, 'REJECTED');    -- Host từ chối

-- ============================================================
-- 6. CREATE INDEXES FOR PERFORMANCE
-- ============================================================

-- Index cho conflict check (tìm bookings trùng dates)
CREATE INDEX idx_booking_property_dates_status 
    ON booking(property_id, check_in, check_out, id_status);

-- Index cho auto-reject conflicting bookings
CREATE INDEX idx_booking_status_property 
    ON booking(id_status, property_id);

-- Index cho payment timeout job
CREATE INDEX idx_booking_confirmed_at 
    ON booking(confirmed_at, id_status);

-- Index cho booking completion job  
CREATE INDEX idx_booking_checkout_status 
    ON booking(check_out, id_status);

-- Index cho user promotion lookup
CREATE INDEX idx_user_promotion_user_code_status 
    ON user_promotion(id_user_account, id_status);

-- Index cho transaction lookup by promotion
CREATE INDEX idx_transaction_promotion_code 
    ON transaction(promotion_code);

-- Index cho promotion usage tracking
CREATE INDEX idx_promotion_usage_user_promotion 
    ON promotion_usage(id_user_promotion);

-- ============================================================
-- 7. UPDATE EXISTING DATA (IF ANY)
-- ============================================================

-- Set default status cho existing user_promotion records
UPDATE user_promotion 
SET id_status = 1 
WHERE id_status IS NULL OR id_status = 0;

-- ============================================================
-- END OF MIGRATION
-- ============================================================


