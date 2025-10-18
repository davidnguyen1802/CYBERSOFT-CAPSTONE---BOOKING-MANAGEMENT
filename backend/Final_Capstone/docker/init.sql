-- =================================================================
-- SCRIPT TẠO DATABASE VÀ DỮ LIỆU MẪU - HOTELS MANAGEMENT
-- Phiên bản đã được tối ưu để chạy tuần tự
-- =================================================================

-- PHẦN 1: KHỞI TẠO DATABASE VÀ CẤU HÌNH
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET collation_connection = 'utf8mb4_unicode_ci';

CREATE DATABASE IF NOT EXISTS hotels_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hotels_management;

-- PHẦN 2: TẠO CÁC BẢNG (ĐÃ SẮP XẾP LẠI THỨ TỰ)

-- 2.1: Các bảng Core (Parent)
CREATE TABLE role(
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE status(
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- 2.2: Các bảng chính khác
CREATE TABLE system_stats (
    id BIGINT PRIMARY KEY,
    total_users BIGINT NOT NULL DEFAULT 0,
    total_hosts BIGINT NOT NULL DEFAULT 0,
    total_properties BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE cities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    city_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE locations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    location_name VARCHAR(100) NOT NULL,
    city_id INT NOT NULL
);

CREATE TABLE property (
    id INT PRIMARY KEY AUTO_INCREMENT,
    host_id INT NOT NULL,
    full_address TEXT NOT NULL,
    property_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    num_rooms INT NOT NULL DEFAULT 1 CHECK (num_rooms > 0),
    num_bathrooms INT NOT NULL DEFAULT 1 CHECK (num_bathrooms >= 0),
    description TEXT,
    property_type ENUM('APARTMENT','HOUSE','VILLA','HOTEL') NOT NULL,
    overall_rating DECIMAL(2, 1) DEFAULT 0.0 CHECK (overall_rating >= 0 AND overall_rating <= 5),
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    is_guest_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    id_status INT NOT NULL,
    priority INT DEFAULT 0,
    location_id INT NOT NULL,
    max_adults   INT NOT NULL DEFAULT 1 CHECK (max_adults   >= 0),
    max_children INT NOT NULL DEFAULT 0 CHECK (max_children >= 0),
    max_infants  INT NOT NULL DEFAULT 0 CHECK (max_infants  >= 0),
    max_pets     INT NOT NULL DEFAULT 0 CHECK (max_pets     >= 0),
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE amenities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    amenity_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    status_id INT NOT NULL DEFAULT 1
);

CREATE TABLE facilities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    facility_name VARCHAR(100),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity >= 0),
    price DECIMAL(10, 2) DEFAULT 0.00 CHECK (price >= 0),
    status_id INT NOT NULL DEFAULT 1,
    is_broken BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE property_amenity (
    property_id INT,
    amenity_id INT,
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE property_facility (
    property_id INT,
    facility_id INT,
    PRIMARY KEY (property_id, facility_id)
);

CREATE TABLE images (
    id INT PRIMARY KEY AUTO_INCREMENT,
    property_id INT NOT NULL,
    image_path TEXT NOT NULL,
    image_description TEXT,
    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id_status INT NOT NULL
);

CREATE TABLE user_account (
    id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    password_hash VARCHAR(255) NOT NULL,
    gender ENUM('MALE','FEMALE') NOT NULL,
    dob DATE,
    id_role INT NOT NULL,
    id_status INT NOT NULL,
    priority INT DEFAULT 0,
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_social_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    provider VARCHAR(50),
    social_id VARCHAR(255) UNIQUE,
    access_token VARCHAR(255),
    refresh_token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255),
    refresh_token VARCHAR(255),
    token_type VARCHAR(50),
    expiration_date DATETIME,
    refresh_expiration_date DATETIME,
    is_mobile TINYINT(1) DEFAULT 0,
    revoked BOOLEAN DEFAULT FALSE,
    expired BOOLEAN DEFAULT FALSE,
    user_id INT NOT NULL
);

CREATE TABLE favourite_list(
    property_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (property_id, user_id)
);

CREATE TABLE booking (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    property_id INT NOT NULL,
    check_in TIMESTAMP NOT NULL,
    check_out TIMESTAMP NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL CHECK (total_price > 0),
    num_adults INT NOT NULL CHECK (num_adults >= 1),
    num_children INT NOT NULL CHECK (num_children >= 0),
    notes TEXT,
    id_status INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE booking
  ADD CONSTRAINT chk_booking_time CHECK (check_out > check_in),
  ADD CONSTRAINT chk_booking_adults   CHECK (num_adults >= 1),
  ADD CONSTRAINT chk_booking_children CHECK (num_children >= 0),
  ADD CONSTRAINT chk_booking_price    CHECK (total_price > 0);

-- Promotion
CREATE TABLE promotion (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_value DECIMAL(10,2) NOT NULL,
    discount_type ENUM('PERCENT','AMOUNT') NOT NULL,
    min_purchase_limit DECIMAL(10, 2) DEFAULT 0 CHECK (min_purchase_limit >= 0),
    max_discount_amount DECIMAL(10, 2) DEFAULT 0 CHECK (max_discount_amount >= 0),
    start_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remaining_days INT NOT NULL DEFAULT 0 CHECK (remaining_days >= 0),
    end_date TIMESTAMP NOT NULL,
    usage_limit INT DEFAULT -1 CHECK (usage_limit >= -1),
    times_used INT NOT NULL DEFAULT 0,
    id_status INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_discount_logic CHECK (
        (discount_type = 'PERCENT' AND discount_value >= 0 AND discount_value <= 100) OR
        (discount_type = 'AMOUNT'  AND discount_value >= 0)
    )
);

CREATE TABLE user_promotion (
    id INT NOT NULL AUTO_INCREMENT,
    promotion_id INT NOT NULL,
    user_account_id INT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id_status INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_promotion_slot (promotion_id, user_account_id),
    KEY idx_user_promotion_user (user_account_id, id_status)
);

CREATE TABLE promotion_usage (
    user_promotion_id INT NOT NULL,
    booking_id INT NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL CHECK (discount_amount >= 0),
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_promotion_id, booking_id)
);

CREATE TABLE `transaction` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    booking_id INT NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(12,2) NOT NULL CHECK (total_amount >= 0)
);

CREATE TABLE user_review (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    property_id INT NOT NULL,
    comment TEXT,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- PHẦN 3: THÊM TẤT CẢ KHÓA NGOẠI (ĐÃ GỘP LỆNH)

ALTER TABLE `user_account`
  ADD CONSTRAINT `fk_user_account_role` FOREIGN KEY (`id_role`) REFERENCES `role`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_user_account_status` FOREIGN KEY (`id_status`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `user_social_accounts`
  ADD CONSTRAINT `fk_user_social_accounts_user_account` FOREIGN KEY (`user_id`) REFERENCES `user_account`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `tokens`
  ADD CONSTRAINT `fk_tokens_user_account` FOREIGN KEY (`user_id`) REFERENCES `user_account`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `locations`
  ADD CONSTRAINT `fk_locations_city` FOREIGN KEY (`city_id`) REFERENCES `cities`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `amenities`
  ADD CONSTRAINT `fk_amenities_status` FOREIGN KEY (`status_id`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `facilities`
  ADD CONSTRAINT `fk_facilities_status` FOREIGN KEY (`status_id`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `property`
  ADD CONSTRAINT `fk_property_location` FOREIGN KEY (`location_id`) REFERENCES `locations`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_property_host` FOREIGN KEY (`host_id`) REFERENCES `user_account`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_property_status` FOREIGN KEY (`id_status`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `images`
  ADD CONSTRAINT `fk_images_property` FOREIGN KEY (`property_id`) REFERENCES `property`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_images_status` FOREIGN KEY (`id_status`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `property_amenity`
  ADD CONSTRAINT `fk_property_amenity_property` FOREIGN KEY (`property_id`) REFERENCES `property`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_property_amenity_amenity` FOREIGN KEY (`amenity_id`) REFERENCES `amenities`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `property_facility`
  ADD CONSTRAINT `fk_property_facility_property` FOREIGN KEY (`property_id`) REFERENCES `property`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_property_facility_facility` FOREIGN KEY (`facility_id`) REFERENCES `facilities`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `favourite_list`
  ADD CONSTRAINT `fk_favourite_list_user` FOREIGN KEY (`user_id`) REFERENCES `user_account`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_favourite_list_property` FOREIGN KEY (`property_id`) REFERENCES `property`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `booking`
  ADD CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user_account`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_booking_property` FOREIGN KEY (`property_id`) REFERENCES `property`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_booking_status` FOREIGN KEY (`id_status`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `transaction`
  ADD CONSTRAINT `fk_transaction_user` FOREIGN KEY (`user_id`) REFERENCES `user_account`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_transaction_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `user_review`
  ADD CONSTRAINT `fk_user_review_user` FOREIGN KEY (`user_id`) REFERENCES `user_account`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_user_review_property` FOREIGN KEY (`property_id`) REFERENCES `property`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `promotion`
  ADD CONSTRAINT `fk_promotion_status` FOREIGN KEY (`id_status`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `user_promotion`
  ADD CONSTRAINT `fk_user_promotion_user` FOREIGN KEY (`user_account_id`) REFERENCES `user_account`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_user_promotion_promotion` FOREIGN KEY (`promotion_id`) REFERENCES `promotion`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_user_promotion_status` FOREIGN KEY (`id_status`) REFERENCES `status`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE `promotion_usage`
  ADD CONSTRAINT `fk_promo_usage_user_promo` FOREIGN KEY (`user_promotion_id`) REFERENCES `user_promotion`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_promo_usage_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;


-- PHẦN 4: THÊM CÁC RÀNG BUỘC UNIQUE VÀ CHỈNH SỬA CỘT
ALTER TABLE facilities
  MODIFY facility_name VARCHAR(100) NOT NULL,
  ADD UNIQUE KEY uq_facility_name (facility_name);

ALTER TABLE user_social_accounts
  ADD CONSTRAINT uq_user_provider UNIQUE (user_id, provider);


-- PHẦN 5: THÊM DỮ LIỆU MẪU (SAMPLE DATA)
-- Thứ tự INSERT đã được đảm bảo đúng (Parent -> Child)

INSERT IGNORE INTO role(name) VALUES ('ADMIN'),('HOST'),('GUEST');
INSERT IGNORE INTO status(name) VALUES ('ACTIVE'),('INACTIVE'),('DELETED');

INSERT INTO system_stats (id, total_users, total_hosts, total_properties) VALUES (1, 0, 0, 0);

INSERT IGNORE INTO status(name) VALUES
('ACTIVE'),('INACTIVE'),('DELETED'),
('Pending'),('Confirmed'),('Completed'),('Cancelled'),('Rejected');

/* 1) CITIES — đủ 63 tỉnh/thành Việt Nam */
INSERT IGNORE INTO cities(city_name) VALUES
('Hà Nội'),('TP Hồ Chí Minh'),('Hải Phòng'),('Đà Nẵng'),('Cần Thơ'),('An Giang'),('Bà Rịa - Vũng Tàu'),('Bạc Liêu'),('Bắc Giang'),('Bắc Kạn'),('Bắc Ninh'),('Bến Tre'),('Bình Dương'),('Bình Định'),('Bình Phước'),('Bình Thuận'),('Cà Mau'),('Cao Bằng'),('Đắk Lắk'),('Đắk Nông'),('Điện Biên'),('Đồng Nai'),('Đồng Tháp'),('Gia Lai'),('Hà Giang'),('Hà Nam'),('Hà Tĩnh'),('Hải Dương'),('Hậu Giang'),('Hòa Bình'),('Hưng Yên'),('Khánh Hòa'),('Kiên Giang'),('Kon Tum'),('Lai Châu'),('Lâm Đồng'),('Lạng Sơn'),('Lào Cai'),('Long An'),('Nam Định'),('Nghệ An'),('Ninh Bình'),('Ninh Thuận'),('Phú Thọ'),('Phú Yên'),('Quảng Bình'),('Quảng Nam'),('Quảng Ngãi'),('Quảng Ninh'),('Quảng Trị'),('Sóc Trăng'),('Sơn La'),('Tây Ninh'),('Thái Bình'),('Thái Nguyên'),('Thanh Hóa'),('Thừa Thiên Huế'),('Tiền Giang'),('Trà Vinh'),('Tuyên Quang'),('Vĩnh Long'),('Vĩnh Phúc'),('Yên Bái');

INSERT INTO locations(location_name, city_id) VALUES
('Hồ Gươm (Hoàn Kiếm)',(SELECT id FROM cities WHERE city_name='Hà Nội')),('Văn Miếu - Quốc Tử Giám',(SELECT id FROM cities WHERE city_name='Hà Nội')),('Địa đạo Củ Chi',(SELECT id FROM cities WHERE city_name='TP Hồ Chí Minh')),('Nhà thờ Đức Bà Sài Gòn',(SELECT id FROM cities WHERE city_name='TP Hồ Chí Minh')),('Bà Nà Hills',(SELECT id FROM cities WHERE city_name='Đà Nẵng')),('Cầu Rồng',(SELECT id FROM cities WHERE city_name='Đà Nẵng')),('Vịnh Hạ Long',(SELECT id FROM cities WHERE city_name='Quảng Ninh')),('Phố cổ Hội An',(SELECT id FROM cities WHERE city_name='Quảng Nam')),('Thánh địa Mỹ Sơn',(SELECT id FROM cities WHERE city_name='Quảng Nam')),('Kinh thành Huế',(SELECT id FROM cities WHERE city_name='Thừa Thiên Huế')),('Phong Nha - Kẻ Bàng',(SELECT id FROM cities WHERE city_name='Quảng Bình')),('Tràng An',(SELECT id FROM cities WHERE city_name='Ninh Bình')),('Tam Cốc - Bích Động',(SELECT id FROM cities WHERE city_name='Ninh Bình')),('Sa Pa - Fansipan',(SELECT id FROM cities WHERE city_name='Lào Cai')),('Mộc Châu',(SELECT id FROM cities WHERE city_name='Sơn La')),('Đèo Mã Pí Lèng',(SELECT id FROM cities WHERE city_name='Hà Giang')),('Thác Bản Giốc',(SELECT id FROM cities WHERE city_name='Cao Bằng')),('Hồ Ba Bể',(SELECT id FROM cities WHERE city_name='Bắc Kạn')),('Tam Đảo',(SELECT id FROM cities WHERE city_name='Vĩnh Phúc')),('Chùa Tam Chúc',(SELECT id FROM cities WHERE city_name='Hà Nam')),('Đền Hùng',(SELECT id FROM cities WHERE city_name='Phú Thọ')),('Cát Bà',(SELECT id FROM cities WHERE city_name='Hải Phòng')),('Bãi Sau Vũng Tàu',(SELECT id FROM cities WHERE city_name='Bà Rịa - Vũng Tàu')),('Đồi cát Mũi Né',(SELECT id FROM cities WHERE city_name='Bình Thuận')),('Gành Đá Đĩa',(SELECT id FROM cities WHERE city_name='Phú Yên')),('Eo Gió',(SELECT id FROM cities WHERE city_name='Bình Định')),('VinWonders Nha Trang',(SELECT id FROM cities WHERE city_name='Khánh Hòa')),('Hồ Xuân Hương - Đà Lạt',(SELECT id FROM cities WHERE city_name='Lâm Đồng')),('Thung lũng Tình Yêu',(SELECT id FROM cities WHERE city_name='Lâm Đồng')),('Vịnh Vĩnh Hy',(SELECT id FROM cities WHERE city_name='Ninh Thuận')),('Lý Sơn',(SELECT id FROM cities WHERE city_name='Quảng Ngãi')),('Biển Cửa Lò',(SELECT id FROM cities WHERE city_name='Nghệ An')),('Thiên Cầm',(SELECT id FROM cities WHERE city_name='Hà Tĩnh')),('Đảo Phú Quốc - Bãi Sao',(SELECT id FROM cities WHERE city_name='Kiên Giang')),('Chợ nổi Cái Răng',(SELECT id FROM cities WHERE city_name='Cần Thơ')),('Rừng tràm Trà Sư',(SELECT id FROM cities WHERE city_name='An Giang')),('Núi Bà Đen',(SELECT id FROM cities WHERE city_name='Tây Ninh')),('Làng nổi Tân Lập',(SELECT id FROM cities WHERE city_name='Long An')),('VQG Cát Tiên',(SELECT id FROM cities WHERE city_name='Đồng Nai')),('Bù Gia Mập',(SELECT id FROM cities WHERE city_name='Bình Phước'));

INSERT IGNORE INTO amenities(amenity_name, description, status_id) VALUES
('Wi-Fi','Internet không dây tốc độ cao',(SELECT id FROM status WHERE name='ACTIVE')),('Air Conditioning','Điều hòa nhiệt độ',(SELECT id FROM status WHERE name='ACTIVE')),('Heating','Sưởi ấm',(SELECT id FROM status WHERE name='ACTIVE')),('Kitchen','Bếp đầy đủ dụng cụ',(SELECT id FROM status WHERE name='ACTIVE')),('Free Parking','Đỗ xe miễn phí',(SELECT id FROM status WHERE name='ACTIVE')),('Washer','Máy giặt',(SELECT id FROM status WHERE name='ACTIVE')),('Dryer','Máy sấy',(SELECT id FROM status WHERE name='ACTIVE')),('Refrigerator','Tủ lạnh',(SELECT id FROM status WHERE name='ACTIVE')),('Microwave','Lò vi sóng',(SELECT id FROM status WHERE name='ACTIVE')),('Coffee Maker','Máy pha cà phê',(SELECT id FROM status WHERE name='ACTIVE')),('Hair Dryer','Máy sấy tóc',(SELECT id FROM status WHERE name='ACTIVE')),('Iron','Bàn ủi',(SELECT id FROM status WHERE name='ACTIVE')),('Smoke Alarm','Báo khói',(SELECT id FROM status WHERE name='ACTIVE')),('CO Alarm','Báo khí CO',(SELECT id FROM status WHERE name='ACTIVE')),('First Aid Kit','Túi sơ cứu',(SELECT id FROM status WHERE name='ACTIVE')),('Fire Extinguisher','Bình chữa cháy',(SELECT id FROM status WHERE name='ACTIVE')),('Workspace','Bàn làm việc',(SELECT id FROM status WHERE name='ACTIVE')),('Balcony','Ban công',(SELECT id FROM status WHERE name='ACTIVE')),('Pool Access','Hồ bơi',(SELECT id FROM status WHERE name='ACTIVE')),('Gym Access','Phòng gym',(SELECT id FROM status WHERE name='ACTIVE'));

INSERT IGNORE INTO facilities(facility_name, quantity, price, status_id, is_broken) VALUES
('Giường ngủ (Queen)',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Sofa',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Bàn ăn',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Ghế ăn',4,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Tủ quần áo',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Đèn bàn',2,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Tivi',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Điều hòa',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Quạt điện',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Bình nước nóng',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Bếp từ',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Lò nướng',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Máy giặt',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Máy sấy',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Máy rửa chén',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Lò vi sóng',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Ấm đun nước',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Bàn làm việc',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Gương toàn thân',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0),('Thùng rác',1,0,(SELECT id FROM status WHERE name='ACTIVE'),0);

INSERT INTO user_account (full_name, username, email, phone, address, password_hash, gender, dob, id_role, id_status, priority) VALUES
('System Admin','admin','admin@demo.local','0900000000','Hà Nội','$2a$10$y8f1b7demoHashForAdminxxxxxxx','FEMALE','1995-01-01',(SELECT id FROM role WHERE name='ADMIN'),(SELECT id FROM status WHERE name='ACTIVE'),100),('Bảo Host','host_bao','host_bao@example.com','0901111111','TP.HCM','$2a$10$y8f1b7demoHashForHost1xxxxxx','MALE','1990-02-02',(SELECT id FROM role WHERE name='HOST'),(SELECT id FROM status WHERE name='ACTIVE'),10),('Chi Host','host_chi','host_chi@example.com','0902222222','Đà Nẵng','$2a$10$y8f1b7demoHashForHost2xxxxxx','FEMALE','1992-03-03',(SELECT id FROM role WHERE name='HOST'),(SELECT id FROM status WHERE name='ACTIVE'),10),('Dũng Host','host_dung','host_dung@example.com','0903333333','Hà Nội','$2a$10$y8f1b7demoHashForHost3xxxxxx','MALE','1989-04-04',(SELECT id FROM role WHERE name='HOST'),(SELECT id FROM status WHERE name='ACTIVE'),10),('Elin Host','host_elin','host_elin@example.com','0904444444','Nha Trang','$2a$10$y8f1b7demoHashForHost4xxxxxx','FEMALE','1993-05-05',(SELECT id FROM role WHERE name='HOST'),(SELECT id FROM status WHERE name='ACTIVE'),10),('Hải Host','host_hai','host_hai@example.com','0905555555','Đà Lạt','$2a$10$y8f1b7demoHashForHost5xxxxxx','MALE','1988-06-06',(SELECT id FROM role WHERE name='HOST'),(SELECT id FROM status WHERE name='ACTIVE'),10),('Linh Guest','guest_linh','guest_linh@example.com','0910000001','Hà Nội','$2a$10$y8f1b7demoHashForGuestxxxxxx','FEMALE','2000-01-10',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Minh Guest','guest_minh','guest_minh@example.com','0910000002','TP.HCM','$2a$10$y8f1b7demoHashForGuestxxxxxx','MALE','1999-02-11',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Hoa Guest','guest_hoa','guest_hoa@example.com','0910000003','Đà Nẵng','$2a$10$y8f1b7demoHashForGuestxxxxxx','FEMALE','1998-03-12',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Thanh Guest','guest_thanh','guest_thanh@example.com','0910000004','Hải Phòng','$2a$10$y8f1b7demoHashForGuestxxxxxx','MALE','1997-04-13',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Anh Guest','guest_anh','guest_anh@example.com','0910000005','Huế','$2a$10$y8f1b7demoHashForGuestxxxxxx','FEMALE','1996-05-14',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Huy Guest','guest_huy','guest_huy@example.com','0910000006','Nha Trang','$2a$10$y8f1b7demoHashForGuestxxxxxx','MALE','1995-06-15',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Mi Guest','guest_mi','guest_mi@example.com','0910000007','Đà Lạt','$2a$10$y8f1b7demoHashForGuestxxxxxx','FEMALE','1994-07-16',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Nam Guest','guest_nam','guest_nam@example.com','0910000008','Quảng Ninh','$2a$10$y8f1b7demoHashForGuestxxxxxx','MALE','1993-08-17',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Lê Guest','guest_le','guest_le@example.com','0910000009','Ninh Bình','$2a$10$y8f1b7demoHashForGuestxxxxxx','FEMALE','1992-09-18',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Thủy Guest','guest_thuy','guest_thuy@example.com','0910000010','Phú Quốc','$2a$10$y8f1b7demoHashForGuestxxxxxx','FEMALE','1991-10-19',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Hân Guest','guest_han','guest_han@example.com','0910000011','Hội An','$2a$10$y8f1b7demoHashForGuestxxxxxx','FEMALE','1990-11-20',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Khánh Guest','guest_khanh','guest_khanh@example.com','0910000012','Quy Nhơn','$2a$10$y8f1b7demoHashForGuestxxxxxx','MALE','1989-12-21',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0),('Tuấn Guest','guest_tuan','guest_tuan@example.com','0910000013','Cần Thơ','$2a$10$y8f1b7demoHashForGuestxxxxxx','MALE','1998-01-22',(SELECT id FROM role WHERE name='GUEST'),(SELECT id FROM status WHERE name='ACTIVE'),0);

INSERT INTO user_social_accounts(user_id, provider, social_id, access_token, refresh_token) VALUES
((SELECT id FROM user_account WHERE username='admin'),'google','gg_admin_001','tokA','refA'),((SELECT id FROM user_account WHERE username='host_bao'),'google','gg_host_bao','tokB','refB'),((SELECT id FROM user_account WHERE username='host_chi'),'facebook','fb_host_chi','tokC','refC'),((SELECT id FROM user_account WHERE username='guest_linh'),'google','gg_guest_linh','tokD','refD'),((SELECT id FROM user_account WHERE username='guest_minh'),'facebook','fb_guest_minh','tokE','refE'),((SELECT id FROM user_account WHERE username='guest_thanh'),'google','gg_guest_thanh','tokF','refF');

INSERT INTO property (host_id, full_address, property_name, price, num_rooms, num_bathrooms, description, property_type, overall_rating, is_guest_favorite, id_status, priority, location_id, max_adults, max_children, max_infants, max_pets) VALUES
((SELECT id FROM user_account WHERE username='host_bao'),'Quận 1, TP.HCM','Saigon Central Apartment',950000,2,1,'Căn hộ trung tâm gần Nhà thờ Đức Bà','APARTMENT',4.7, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),10,(SELECT id FROM locations WHERE location_name='Nhà thờ Đức Bà Sài Gòn'),4,1,1,0),((SELECT id FROM user_account WHERE username='host_chi'),'Sơn Trà, Đà Nẵng','Da Nang Sea View House',1200000,3,2,'Nhà ven biển gần Cầu Rồng','HOUSE',4.6, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),9,(SELECT id FROM locations WHERE location_name='Cầu Rồng'),6,2,1,1),((SELECT id FROM user_account WHERE username='host_dung'),'Hoàn Kiếm, Hà Nội','Old Quarter Studio Hanoi',700000,1,1,'Studio gần Hồ Gươm','APARTMENT',4.8, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),9,(SELECT id FROM locations WHERE location_name='Hồ Gươm (Hoàn Kiếm)'),2,1,1,0),((SELECT id FROM user_account WHERE username='host_elin'),'Hội An, Quảng Nam','Hoi An Riverside Villa',1800000,4,3,'Villa view sông Thu Bồn','VILLA',4.9, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),8,(SELECT id FROM locations WHERE location_name='Phố cổ Hội An'),8,3,2,1),((SELECT id FROM user_account WHERE username='host_hai'),'Đà Lạt, Lâm Đồng','Da Lat Pine Hills Villa',2000000,4,3,'Villa giữa rừng thông','VILLA',4.7, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),8,(SELECT id FROM locations WHERE location_name='Hồ Xuân Hương - Đà Lạt'),8,3,2,0),((SELECT id FROM user_account WHERE username='host_elin'),'Hạ Long, Quảng Ninh','Ha Long Bay View Hotel',1500000,10,10,'Khách sạn view vịnh','HOTEL',4.5, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),7,(SELECT id FROM locations WHERE location_name='Vịnh Hạ Long'),20,10,5,0),((SELECT id FROM user_account WHERE username='host_chi'),'Mỹ Khê, Đà Nẵng','Ba Na Hills Retreat',1300000,2,2,'Căn hộ nghỉ dưỡng Bà Nà','APARTMENT',4.4, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),6,(SELECT id FROM locations WHERE location_name='Bà Nà Hills'),4,2,1,0),((SELECT id FROM user_account WHERE username='host_bao'),'Quận 3, TP.HCM','Saigon Postal Loft',880000,1,1,'Loft gần Bưu điện TP','APARTMENT',4.3, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),6,(SELECT id FROM locations WHERE location_name='Nhà thờ Đức Bà Sài Gòn'),2,1,1,0),((SELECT id FROM user_account WHERE username='host_dung'),'Ninh Bình','Trang An Homestay',650000,2,1,'Homestay gần Tràng An','HOUSE',4.6, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),6,(SELECT id FROM locations WHERE location_name='Tràng An'),4,1,1,0),((SELECT id FROM user_account WHERE username='host_elin'),'Sa Pa, Lào Cai','Sapa Mountain Lodge',1200000,3,2,'Lodge ấm cúng gần Fansipan','HOUSE',4.7, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),6,(SELECT id FROM locations WHERE location_name='Sa Pa - Fansipan'),6,2,1,0),((SELECT id FROM user_account WHERE username='host_hai'),'Phú Quốc, Kiên Giang','Phu Quoc Beach Villa',2500000,5,4,'Villa sát biển Bãi Sao','VILLA',4.9, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),9,(SELECT id FROM locations WHERE location_name='Đảo Phú Quốc - Bãi Sao'),10,4,2,1),((SELECT id FROM user_account WHERE username='host_bao'),'Vũng Tàu','Vung Tau Coastal Hotel',1100000,8,8,'Khách sạn gần Bãi Sau','HOTEL',4.2, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),5,(SELECT id FROM locations WHERE location_name='Bãi Sau Vũng Tàu'),16,8,4,0),((SELECT id FROM user_account WHERE username='host_chi'),'Nha Trang','Nha Trang Oceanfront Apt',900000,2,1,'Căn hộ sát biển','APARTMENT',4.4, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),5,(SELECT id FROM locations WHERE location_name='VinWonders Nha Trang'),4,1,1,0),((SELECT id FROM user_account WHERE username='host_dung'),'Quy Nhơn, Bình Định','Quy Nhon Eo Gio Bungalow',950000,2,1,'Bungalow gần Eo Gió','HOUSE',4.5, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),5,(SELECT id FROM locations WHERE location_name='Eo Gió'),4,1,1,0),((SELECT id FROM user_account WHERE username='host_elin'),'Phú Yên','Phu Yen Ganh Da Dia House',800000,2,1,'Nhà nghỉ cạnh Gành Đá Đĩa','HOUSE',4.3, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),4,(SELECT id FROM locations WHERE location_name='Gành Đá Đĩa'),4,1,1,0),((SELECT id FROM user_account WHERE username='host_hai'),'Quảng Bình','Phong Nha Cave Lodge',1000000,3,2,'Lodge gần PNKB','HOUSE',4.6, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),7,(SELECT id FROM locations WHERE location_name='Phong Nha - Kẻ Bàng'),6,2,1,0),((SELECT id FROM user_account WHERE username='host_bao'),'Cần Thơ','Can Tho Floating Homestay',700000,2,1,'Homestay gần chợ nổi','HOUSE',4.2, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),4,(SELECT id FROM locations WHERE location_name='Chợ nổi Cái Răng'),4,1,1,0),((SELECT id FROM user_account WHERE username='host_chi'),'Tây Ninh','Tay Ninh Ba Den Retreat',750000,2,1,'Nhà nghỉ gần Núi Bà Đen','HOUSE',4.1, FALSE,(SELECT id FROM status WHERE name='ACTIVE'),4,(SELECT id FROM locations WHERE location_name='Núi Bà Đen'),4,1,0,0),((SELECT id FROM user_account WHERE username='host_dung'),'Hội An','Riverside Garden House',900000,3,2,'Nhà vườn ven sông','HOUSE',4.6, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),6,(SELECT id FROM locations WHERE location_name='Phố cổ Hội An'),6,2,1,0),((SELECT id FROM user_account WHERE username='host_elin'),'Đà Lạt','Dalat Valley Apartment',850000,2,1,'Căn hộ nhìn thung lũng','APARTMENT',4.5, TRUE,(SELECT id FROM status WHERE name='ACTIVE'),6,(SELECT id FROM locations WHERE location_name='Thung lũng Tình Yêu'),4,1,1,0);

INSERT INTO images(property_id, image_path, image_description, id_status) VALUES
((SELECT id FROM property WHERE property_name='Saigon Central Apartment'),'/images/sg_central_1.jpg','Phòng khách sáng', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Saigon Central Apartment'),'/images/sg_central_2.jpg','Phòng ngủ', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Da Nang Sea View House'),'/images/dn_sea_1.jpg','View biển', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),'/images/hn_studio_1.jpg','Gần Hồ Gươm', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),'/images/ha_villa_1.jpg','Hồ bơi riêng', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),'/images/dl_pine_1.jpg','Rừng thông', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),'/images/hl_hotel_1.jpg','View vịnh', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Ba Na Hills Retreat'),'/images/dn_bana_1.jpg','Ban công', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Saigon Postal Loft'),'/images/sg_postal_1.jpg','Loft', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Trang An Homestay'),'/images/nb_trangan_1.jpg','Cạnh Tràng An', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Sapa Mountain Lodge'),'/images/sp_lodge_1.jpg','Núi Fansipan', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),'/images/pq_villa_1.jpg','Biển', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Vung Tau Coastal Hotel'),'/images/vt_hotel_1.jpg','Bãi Sau', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Nha Trang Oceanfront Apt'),'/images/nt_apt_1.jpg','Sát biển', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Quy Nhon Eo Gio Bungalow'),'/images/qn_bungalow_1.jpg','Eo Gió', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Phu Yen Ganh Da Dia House'),'/images/py_house_1.jpg','Gành Đá Đĩa', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Phong Nha Cave Lodge'),'/images/qb_lodge_1.jpg','Phong Nha', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Can Tho Floating Homestay'),'/images/ct_homestay_1.jpg','Chợ nổi', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Tay Ninh Ba Den Retreat'),'/images/tn_retreat_1.jpg','Núi Bà Đen', (SELECT id FROM status WHERE name='ACTIVE')),((SELECT id FROM property WHERE property_name='Dalat Valley Apartment'),'/images/dl_valley_1.jpg','Thung lũng', (SELECT id FROM status WHERE name='ACTIVE'));

INSERT INTO property_amenity(property_id, amenity_id) VALUES
((SELECT id FROM property WHERE property_name='Saigon Central Apartment'),(SELECT id FROM amenities WHERE amenity_name='Wi-Fi')),((SELECT id FROM property WHERE property_name='Saigon Central Apartment'),(SELECT id FROM amenities WHERE amenity_name='Air Conditioning')),((SELECT id FROM property WHERE property_name='Da Nang Sea View House'),(SELECT id FROM amenities WHERE amenity_name='Kitchen')),((SELECT id FROM property WHERE property_name='Da Nang Sea View House'),(SELECT id FROM amenities WHERE amenity_name='Free Parking')),((SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),(SELECT id FROM amenities WHERE amenity_name='Wi-Fi')),((SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),(SELECT id FROM amenities WHERE amenity_name='Heating')),((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),(SELECT id FROM amenities WHERE amenity_name='Pool Access')),((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),(SELECT id FROM amenities WHERE amenity_name='First Aid Kit')),((SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),(SELECT id FROM amenities WHERE amenity_name='Workspace')),((SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),(SELECT id FROM amenities WHERE amenity_name='Balcony')),((SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),(SELECT id FROM amenities WHERE amenity_name='Gym Access')),((SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),(SELECT id FROM amenities WHERE amenity_name='Smoke Alarm')),((SELECT id FROM property WHERE property_name='Ba Na Hills Retreat'),(SELECT id FROM amenities WHERE amenity_name='Coffee Maker')),((SELECT id FROM property WHERE property_name='Saigon Postal Loft'),(SELECT id FROM amenities WHERE amenity_name='Wi-Fi')),((SELECT id FROM property WHERE property_name='Trang An Homestay'),(SELECT id FROM amenities WHERE amenity_name='Kitchen')),((SELECT id FROM property WHERE property_name='Sapa Mountain Lodge'),(SELECT id FROM amenities WHERE amenity_name='Heating')),((SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),(SELECT id FROM amenities WHERE amenity_name='Pool Access')),((SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),(SELECT id FROM amenities WHERE amenity_name='CO Alarm')),((SELECT id FROM property WHERE property_name='Vung Tau Coastal Hotel'),(SELECT id FROM amenities WHERE amenity_name='Wi-Fi')),((SELECT id FROM property WHERE property_name='Nha Trang Oceanfront Apt'),(SELECT id FROM amenities WHERE amenity_name='Refrigerator')),((SELECT id FROM property WHERE property_name='Quy Nhon Eo Gio Bungalow'),(SELECT id FROM amenities WHERE amenity_name='Free Parking')),((SELECT id FROM property WHERE property_name='Phu Yen Ganh Da Dia House'),(SELECT id FROM amenities WHERE amenity_name='Kitchen')),((SELECT id FROM property WHERE property_name='Phong Nha Cave Lodge'),(SELECT id FROM amenities WHERE amenity_name='First Aid Kit')),((SELECT id FROM property WHERE property_name='Dalat Valley Apartment'),(SELECT id FROM amenities WHERE amenity_name='Balcony'));

INSERT INTO property_facility(property_id, facility_id) VALUES
((SELECT id FROM property WHERE property_name='Saigon Central Apartment'),(SELECT id FROM facilities WHERE facility_name='Giường ngủ (Queen)')),((SELECT id FROM property WHERE property_name='Saigon Central Apartment'),(SELECT id FROM facilities WHERE facility_name='Tivi')),((SELECT id FROM property WHERE property_name='Da Nang Sea View House'),(SELECT id FROM facilities WHERE facility_name='Sofa')),((SELECT id FROM property WHERE property_name='Da Nang Sea View House'),(SELECT id FROM facilities WHERE facility_name='Bàn ăn')),((SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),(SELECT id FROM facilities WHERE facility_name='Bàn làm việc')),((SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),(SELECT id FROM facilities WHERE facility_name='Đèn bàn')),((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),(SELECT id FROM facilities WHERE facility_name='Máy rửa chén')),((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),(SELECT id FROM facilities WHERE facility_name='Máy giặt')),((SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),(SELECT id FROM facilities WHERE facility_name='Lò nướng')),((SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),(SELECT id FROM facilities WHERE facility_name='Bếp từ')),((SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),(SELECT id FROM facilities WHERE facility_name='Tủ quần áo')),((SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),(SELECT id FROM facilities WHERE facility_name='Bình nước nóng')),((SELECT id FROM property WHERE property_name='Ba Na Hills Retreat'),(SELECT id FROM facilities WHERE facility_name='Sofa')),((SELECT id FROM property WHERE property_name='Saigon Postal Loft'),(SELECT id FROM facilities WHERE facility_name='Gương toàn thân')),((SELECT id FROM property WHERE property_name='Trang An Homestay'),(SELECT id FROM facilities WHERE facility_name='Bàn ăn')),((SELECT id FROM property WHERE property_name='Sapa Mountain Lodge'),(SELECT id FROM facilities WHERE facility_name='Quạt điện')),((SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),(SELECT id FROM facilities WHERE facility_name='Giường ngủ (Queen)')),((SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),(SELECT id FROM facilities WHERE facility_name='Sofa')),((SELECT id FROM property WHERE property_name='Vung Tau Coastal Hotel'),(SELECT id FROM facilities WHERE facility_name='Tivi')),((SELECT id FROM property WHERE property_name='Nha Trang Oceanfront Apt'),(SELECT id FROM facilities WHERE facility_name='Ấm đun nước')),((SELECT id FROM property WHERE property_name='Quy Nhon Eo Gio Bungalow'),(SELECT id FROM facilities WHERE facility_name='Bàn làm việc')),((SELECT id FROM property WHERE property_name='Phu Yen Ganh Da Dia House'),(SELECT id FROM facilities WHERE facility_name='Đèn bàn')),((SELECT id FROM property WHERE property_name='Phong Nha Cave Lodge'),(SELECT id FROM facilities WHERE facility_name='Máy giặt')),((SELECT id FROM property WHERE property_name='Can Tho Floating Homestay'),(SELECT id FROM facilities WHERE facility_name='Bàn ăn')),((SELECT id FROM property WHERE property_name='Tay Ninh Ba Den Retreat'),(SELECT id FROM facilities WHERE facility_name='Giường ngủ (Queen)')),((SELECT id FROM property WHERE property_name='Dalat Valley Apartment'),(SELECT id FROM facilities WHERE facility_name='Bàn làm việc')),((SELECT id FROM property WHERE property_name='Dalat Valley Apartment'),(SELECT id FROM facilities WHERE facility_name='Tủ quần áo')),((SELECT id FROM property WHERE property_name='Sapa Mountain Lodge'),(SELECT id FROM facilities WHERE facility_name='Bình nước nóng')),((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),(SELECT id FROM facilities WHERE facility_name='Lò vi sóng')),((SELECT id FROM property WHERE property_name='Nha Trang Oceanfront Apt'),(SELECT id FROM facilities WHERE facility_name='Máy sấy'));

INSERT INTO favourite_list(property_id, user_id) VALUES
((SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),
 (SELECT id FROM user_account WHERE username='guest_linh')),
((SELECT id FROM property WHERE property_name='Saigon Central Apartment'),
 (SELECT id FROM user_account WHERE username='guest_minh')),
((SELECT id FROM property WHERE property_name='Da Nang Sea View House'),
 (SELECT id FROM user_account WHERE username='guest_hoa')),
((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),
 (SELECT id FROM user_account WHERE username='guest_thanh')),
((SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),
 (SELECT id FROM user_account WHERE username='guest_anh')),
((SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),
 (SELECT id FROM user_account WHERE username='guest_huy')),
((SELECT id FROM property WHERE property_name='Ba Na Hills Retreat'),
 (SELECT id FROM user_account WHERE username='guest_mi')),
((SELECT id FROM property WHERE property_name='Saigon Postal Loft'),
 (SELECT id FROM user_account WHERE username='guest_nam')),
((SELECT id FROM property WHERE property_name='Trang An Homestay'),
 (SELECT id FROM user_account WHERE username='guest_le')),
((SELECT id FROM property WHERE property_name='Sapa Mountain Lodge'),
 (SELECT id FROM user_account WHERE username='guest_thuy')),
((SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),
 (SELECT id FROM user_account WHERE username='guest_han')),
((SELECT id FROM property WHERE property_name='Vung Tau Coastal Hotel'),
 (SELECT id FROM user_account WHERE username='guest_khanh')),
((SELECT id FROM property WHERE property_name='Nha Trang Oceanfront Apt'),
 (SELECT id FROM user_account WHERE username='guest_tuan')),
((SELECT id FROM property WHERE property_name='Quy Nhon Eo Gio Bungalow'),
 (SELECT id FROM user_account WHERE username='guest_linh')),
((SELECT id FROM property WHERE property_name='Phu Yen Ganh Da Dia House'),
 (SELECT id FROM user_account WHERE username='guest_minh')),
((SELECT id FROM property WHERE property_name='Phong Nha Cave Lodge'),
 (SELECT id FROM user_account WHERE username='guest_hoa')),
((SELECT id FROM property WHERE property_name='Can Tho Floating Homestay'),
 (SELECT id FROM user_account WHERE username='guest_thanh')),
((SELECT id FROM property WHERE property_name='Tay Ninh Ba Den Retreat'),
 (SELECT id FROM user_account WHERE username='guest_anh')),
((SELECT id FROM property WHERE property_name='Dalat Valley Apartment'),
 (SELECT id FROM user_account WHERE username='guest_huy')),
((SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),
 (SELECT id FROM user_account WHERE username='guest_mi'));

/* 12) BOOKING (~20) — đảm bảo check_out > check_in, status ACTIVE */
INSERT INTO booking
(user_id, property_id, check_in, check_out, total_price, num_adults, num_children, notes, id_status)
VALUES
((SELECT id FROM user_account WHERE username='guest_linh'),
 (SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),
 '2025-10-24 14:00:00','2025-10-26 12:00:00',1400000,2,0,'Kỷ niệm 25/10', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_minh'),
 (SELECT id FROM property WHERE property_name='Saigon Central Apartment'),
 '2025-11-05 15:00:00','2025-11-08 11:00:00',2850000,2,1,'Công tác', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_hoa'),
 (SELECT id FROM property WHERE property_name='Da Nang Sea View House'),
 '2025-12-20 14:00:00','2025-12-23 12:00:00',3600000,3,1,'Du lịch Noel', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_thanh'),
 (SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),
 '2025-11-10 14:00:00','2025-11-13 12:00:00',5400000,4,1,'Gia đình', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_anh'),
 (SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),
 '2025-12-01 14:00:00','2025-12-04 12:00:00',6000000,3,1,'Team nhỏ', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_huy'),
 (SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),
 '2025-10-15 14:00:00','2025-10-17 12:00:00',3000000,2,0,'Công tác', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_mi'),
 (SELECT id FROM property WHERE property_name='Ba Na Hills Retreat'),
 '2025-11-20 14:00:00','2025-11-22 12:00:00',2600000,2,0,'Nghỉ dưỡng', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_nam'),
 (SELECT id FROM property WHERE property_name='Saigon Postal Loft'),
 '2025-10-05 14:00:00','2025-10-07 12:00:00',1760000,2,0,'Thăm bạn', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_le'),
 (SELECT id FROM property WHERE property_name='Trang An Homestay'),
 '2025-10-18 14:00:00','2025-10-19 12:00:00',650000,2,0,'Thăm Ninh Bình', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_thuy'),
 (SELECT id FROM property WHERE property_name='Sapa Mountain Lodge'),
 '2025-12-28 14:00:00','2026-01-01 12:00:00',4800000,2,1,'Tết Dương lịch', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_han'),
 (SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),
 '2025-12-10 14:00:00','2025-12-13 12:00:00',7500000,5,1,'Tiệc bạn bè', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_khanh'),
 (SELECT id FROM property WHERE property_name='Vung Tau Coastal Hotel'),
 '2025-10-21 14:00:00','2025-10-23 12:00:00',2200000,2,0,'Nghỉ biển', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_tuan'),
 (SELECT id FROM property WHERE property_name='Nha Trang Oceanfront Apt'),
 '2025-11-02 14:00:00','2025-11-05 12:00:00',2700000,2,1,'Lặn biển', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_linh'),
 (SELECT id FROM property WHERE property_name='Quy Nhon Eo Gio Bungalow'),
 '2025-10-28 14:00:00','2025-10-30 12:00:00',1900000,2,0,'Check-in Eo Gió', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_minh'),
 (SELECT id FROM property WHERE property_name='Phu Yen Ganh Da Dia House'),
 '2025-11-15 14:00:00','2025-11-17 12:00:00',1600000,2,0,'Phượt Phú Yên', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_hoa'),
 (SELECT id FROM property WHERE property_name='Phong Nha Cave Lodge'),
 '2025-10-09 14:00:00','2025-10-11 12:00:00',2000000,2,0,'Khám phá hang động', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_thanh'),
 (SELECT id FROM property WHERE property_name='Can Tho Floating Homestay'),
 '2025-10-12 14:00:00','2025-10-13 12:00:00',700000,2,0,'Chợ nổi sáng sớm', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_anh'),
 (SELECT id FROM property WHERE property_name='Tay Ninh Ba Den Retreat'),
 '2025-11-25 14:00:00','2025-11-26 12:00:00',750000,2,0,'Leo núi', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_huy'),
 (SELECT id FROM property WHERE property_name='Dalat Valley Apartment'),
 '2025-12-05 14:00:00','2025-12-07 12:00:00',1700000,2,0,'Săn mây', (SELECT id FROM status WHERE name='ACTIVE')),

((SELECT id FROM user_account WHERE username='guest_mi'),
 (SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),
 '2025-12-18 14:00:00','2025-12-20 12:00:00',3600000,2,1,'Chụp ảnh Hội An', (SELECT id FROM status WHERE name='ACTIVE'));

/* 13) `TRANSACTION` (~20) — dùng backtick vì từ khóa */
INSERT INTO `transaction`(user_id, booking_id, total_amount)
SELECT b.user_id, b.id, b.total_price FROM booking b;

INSERT INTO user_review(user_id, property_id, comment, rating) VALUES
((SELECT id FROM user_account WHERE username='guest_linh'),(SELECT id FROM property WHERE property_name='Old Quarter Studio Hanoi'),'Rất gần Hồ Gươm, tiện đi bộ',5),((SELECT id FROM user_account WHERE username='guest_minh'),(SELECT id FROM property WHERE property_name='Saigon Central Apartment'),'Sạch sẽ, chủ nhiệt tình',5),((SELECT id FROM user_account WHERE username='guest_hoa'),(SELECT id FROM property WHERE property_name='Da Nang Sea View House'),'View biển đẹp',4),((SELECT id FROM user_account WHERE username='guest_thanh'),(SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),'Không gian yên tĩnh',5),((SELECT id FROM user_account WHERE username='guest_anh'),(SELECT id FROM property WHERE property_name='Da Lat Pine Hills Villa'),'Không khí tuyệt',5),((SELECT id FROM user_account WHERE username='guest_huy'),(SELECT id FROM property WHERE property_name='Ha Long Bay View Hotel'),'Gần bến tàu',4),((SELECT id FROM user_account WHERE username='guest_mi'),(SELECT id FROM property WHERE property_name='Ba Na Hills Retreat'),'Phòng ấm cúng',4),((SELECT id FROM user_account WHERE username='guest_nam'),(SELECT id FROM property WHERE property_name='Saigon Postal Loft'),'Loft đẹp',4),((SELECT id FROM user_account WHERE username='guest_le'),(SELECT id FROM property WHERE property_name='Trang An Homestay'),'Chủ thân thiện',5),((SELECT id FROM user_account WHERE username='guest_thuy'),(SELECT id FROM property WHERE property_name='Sapa Mountain Lodge'),'View núi xịn',5),((SELECT id FROM user_account WHERE username='guest_han'),(SELECT id FROM property WHERE property_name='Phu Quoc Beach Villa'),'Bãi biển sạch',5),((SELECT id FROM user_account WHERE username='guest_khanh'),(SELECT id FROM property WHERE property_name='Vung Tau Coastal Hotel'),'Vị trí tốt',4),((SELECT id FROM user_account WHERE username='guest_tuan'),(SELECT id FROM property WHERE property_name='Nha Trang Oceanfront Apt'),'Gần biển',4),((SELECT id FROM user_account WHERE username='guest_linh'),(SELECT id FROM property WHERE property_name='Quy Nhon Eo Gio Bungalow'),'Gần Eo Gió',5),((SELECT id FROM user_account WHERE username='guest_minh'),(SELECT id FROM property WHERE property_name='Phu Yen Ganh Da Dia House'),'Yên bình',4),((SELECT id FROM user_account WHERE username='guest_hoa'),(SELECT id FROM property WHERE property_name='Phong Nha Cave Lodge'),'Gần hang động',5),((SELECT id FROM user_account WHERE username='guest_thanh'),(SELECT id FROM property WHERE property_name='Can Tho Floating Homestay'),'Sáng sớm rất vui',4),((SELECT id FROM user_account WHERE username='guest_anh'),(SELECT id FROM property WHERE property_name='Tay Ninh Ba Den Retreat'),'Thuận tiện leo núi',4),((SELECT id FROM user_account WHERE username='guest_huy'),(SELECT id FROM property WHERE property_name='Dalat Valley Apartment'),'Không khí mát',5),((SELECT id FROM user_account WHERE username='guest_mi'),(SELECT id FROM property WHERE property_name='Hoi An Riverside Villa'),'Riverside chill',5);

INSERT INTO promotion (code, name, description, discount_value, discount_type, min_purchase_limit, max_discount_amount, start_date, remaining_days, usage_limit, times_used, id_status) VALUES
('SAVE10','Giảm 10%','Mã giảm 10%',10,'PERCENT',0,200000,'2025-10-01 00:00:00',60,-1,0,(SELECT id FROM status WHERE name='ACTIVE')),('WELCOME5','Giảm 50k','Khách mới giảm 50k',50000,'AMOUNT',0,50000,'2025-10-01 00:00:00',90,-1,0,(SELECT id FROM status WHERE name='ACTIVE')),('OCT15','Tháng 10 -15%','Áp dụng cuối tuần',15,'PERCENT',500000,300000,'2025-10-01 00:00:00',31,100,0,(SELECT id FROM status WHERE name='ACTIVE')),('WEEKEND8','Giảm 80k','Cuối tuần',80000,'AMOUNT',400000,80000,'2025-10-01 00:00:00',45,500,0,(SELECT id FROM status WHERE name='ACTIVE')),('LONGSTAY12','12%','Ở >=3 đêm',12,'PERCENT',1000000,600000,'2025-10-01 00:00:00',120,200,0,(SELECT id FROM status WHERE name='ACTIVE')),('HONEY200','Giảm 200k','Cặp đôi',200000,'AMOUNT',800000,200000,'2025-10-01 00:00:00',120,300,0,(SELECT id FROM status WHERE name='ACTIVE')),('VIP20','20%','Khách thân thiết',20,'PERCENT',2000000,800000,'2025-10-01 00:00:00',180,100,0,(SELECT id FROM status WHERE name='ACTIVE')),('FLASH9','9%','Flash sale',9,'PERCENT',0,300000,'2025-10-05 00:00:00',15,1000,0,(SELECT id FROM status WHERE name='ACTIVE')),('BIRTHDAY25','25%','Sinh nhật',25,'PERCENT',500000,700000,'2025-10-01 00:00:00',60,300,0,(SELECT id FROM status WHERE name='ACTIVE')),('HALONG100','100k','Chỉ Hạ Long',100000,'AMOUNT',600000,100000,'2025-10-01 00:00:00',60,200,0,(SELECT id FROM status WHERE name='ACTIVE'));

INSERT INTO user_promotion(promotion_id, user_account_id, expires_at, id_status) VALUES
((SELECT id FROM promotion WHERE code='SAVE10'), (SELECT id FROM user_account WHERE username='guest_linh'), '2025-12-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='SAVE10'), (SELECT id FROM user_account WHERE username='guest_minh'), '2025-12-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='WELCOME5'),(SELECT id FROM user_account WHERE username='guest_hoa'), '2026-01-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='OCT15'), (SELECT id FROM user_account WHERE username='guest_thanh'), '2025-10-31', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='WEEKEND8'),(SELECT id FROM user_account WHERE username='guest_anh'), '2025-11-30', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='LONGSTAY12'),(SELECT id FROM user_account WHERE username='guest_huy'), '2026-03-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='HONEY200'),(SELECT id FROM user_account WHERE username='guest_mi'), '2026-02-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='VIP20'), (SELECT id FROM user_account WHERE username='guest_nam'), '2026-04-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='FLASH9'), (SELECT id FROM user_account WHERE username='guest_le'), '2025-10-20', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='BIRTHDAY25'),(SELECT id FROM user_account WHERE username='guest_thuy'), '2025-12-31', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='SAVE10'), (SELECT id FROM user_account WHERE username='guest_han'), '2025-12-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='SAVE10'), (SELECT id FROM user_account WHERE username='guest_khanh'), '2025-12-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='WELCOME5'),(SELECT id FROM user_account WHERE username='guest_tuan'), '2026-01-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='HALONG100'),(SELECT id FROM user_account WHERE username='guest_huy'), '2025-12-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='OCT15'), (SELECT id FROM user_account WHERE username='guest_linh'), '2025-10-31', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='HONEY200'),(SELECT id FROM user_account WHERE username='guest_anh'), '2026-02-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='VIP20'), (SELECT id FROM user_account WHERE username='guest_minh'), '2026-04-01', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='FLASH9'), (SELECT id FROM user_account WHERE username='guest_mi'), '2025-10-20', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='BIRTHDAY25'),(SELECT id FROM user_account WHERE username='guest_thanh'), '2025-12-31', (SELECT id FROM status WHERE name='ACTIVE')),
((SELECT id FROM promotion WHERE code='WELCOME5'),(SELECT id FROM user_account WHERE username='guest_le'), '2026-01-01', (SELECT id FROM status WHERE name='ACTIVE'));

START TRANSACTION;
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2025-12-31', s.id FROM promotion p JOIN user_account u ON u.username='guest_linh' JOIN status s ON s.name='ACTIVE' WHERE p.code='BIRTHDAY25';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2025-12-31', s.id FROM promotion p JOIN user_account u ON u.username='guest_minh' JOIN status s ON s.name='ACTIVE' WHERE p.code='SAVE10';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2025-10-31', s.id FROM promotion p JOIN user_account u ON u.username='guest_thanh' JOIN status s ON s.name='ACTIVE' WHERE p.code='OCT15';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2026-01-01', s.id FROM promotion p JOIN user_account u ON u.username='guest_hoa' JOIN status s ON s.name='ACTIVE' WHERE p.code='WELCOME5';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2026-04-01', s.id FROM promotion p JOIN user_account u ON u.username='guest_nam' JOIN status s ON s.name='ACTIVE' WHERE p.code='VIP20';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2025-12-01', s.id FROM promotion p JOIN user_account u ON u.username='guest_huy' JOIN status s ON s.name='ACTIVE' WHERE p.code='HALONG100';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2025-10-20', s.id FROM promotion p JOIN user_account u ON u.username='guest_le' JOIN status s ON s.name='ACTIVE' WHERE p.code='FLASH9';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2026-02-01', s.id FROM promotion p JOIN user_account u ON u.username='guest_mi' JOIN status s ON s.name='ACTIVE' WHERE p.code='HONEY200';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2025-12-01', s.id FROM promotion p JOIN user_account u ON u.username='guest_khanh' JOIN status s ON s.name='ACTIVE' WHERE p.code='SAVE10';
INSERT IGNORE INTO user_promotion (promotion_id, user_account_id, expires_at, id_status) SELECT p.id, u.id, '2026-01-01', s.id FROM promotion p JOIN user_account u ON u.username='guest_tuan' JOIN status s ON s.name='ACTIVE' WHERE p.code='WELCOME5';
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 350000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='BIRTHDAY25' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_linh' JOIN booking b ON b.user_id=u.id JOIN property pr ON pr.id=b.property_id AND pr.property_name='Old Quarter Studio Hanoi' WHERE b.check_in='2025-10-24 14:00:00';
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 285000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='SAVE10' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_minh' JOIN booking b ON b.user_id=u.id LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 540000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='OCT15' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_thanh' JOIN booking b ON b.user_id=u.id LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 50000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='WELCOME5' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_hoa' JOIN booking b ON b.user_id=u.id LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 352000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='VIP20' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_nam' JOIN booking b ON b.user_id=u.id LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 100000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='HALONG100' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_huy' JOIN booking b ON b.user_id=u.id JOIN property pr ON pr.id=b.property_id AND pr.property_name='Ha Long Bay View Hotel' LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 58500 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='FLASH9' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_le' JOIN booking b ON b.user_id=u.id LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 200000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='HONEY200' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_mi' JOIN booking b ON b.user_id=u.id LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 220000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='SAVE10' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_khanh' JOIN booking b ON b.user_id=u.id LIMIT 1;
INSERT IGNORE INTO promotion_usage(user_promotion_id, booking_id, discount_amount) SELECT up.id, b.id, 50000 FROM user_promotion up JOIN promotion p ON p.id=up.promotion_id AND p.code='WELCOME5' JOIN user_account u ON u.id=up.user_account_id AND u.username='guest_tuan' JOIN booking b ON b.user_id=u.id LIMIT 1;
COMMIT;

-- PHẦN 6: CẬP NHẬT STATUS (TÙY CHỌN)
INSERT IGNORE INTO status(name) VALUES ('AVAILABLE'), ('UNAVAILABLE');

UPDATE property
SET id_status = (SELECT id FROM status WHERE name='AVAILABLE')
WHERE id_status = (SELECT id FROM status WHERE name='ACTIVE');

UPDATE property
SET id_status = (SELECT id FROM status WHERE name='UNAVAILABLE')
WHERE property_name IN (
  'Ha Long Bay View Hotel',
  'Ba Na Hills Retreat'
);

-- 4) Update booking statuses from generic ACTIVE to specific booking statuses
--    Bookings use: Pending (awaiting approval), Confirmed (approved), Completed (finished)
UPDATE booking
SET id_status = (SELECT id FROM status WHERE name='Pending')
WHERE id_status = (SELECT id FROM status WHERE name='ACTIVE') AND check_in > NOW();

-- Some bookings start as Confirmed (host pre-approved)
UPDATE booking b
INNER JOIN property p ON b.property_id = p.id
SET b.id_status = (SELECT id FROM status WHERE name='Confirmed')
WHERE b.id_status = (SELECT id FROM status WHERE name='Pending')
  AND p.host_id = 2  -- Host Bảo's properties
  AND b.id IN (
    SELECT id FROM (
      SELECT id FROM booking 
      WHERE property_id IN (SELECT id FROM property WHERE host_id = 2)
      ORDER BY RAND() 
      LIMIT 2
    ) AS temp
  );

--
--
---- Checck khóa ngoại
--/*SELECT table_name, column_name
--FROM information_schema.KEY_COLUMN_USAGE
--WHERE table_schema = DATABASE()
--  AND referenced_table_name = 'status'
--ORDER BY table_name, column_name;
--
--SELECT c.table_name, c.column_name
--FROM information_schema.COLUMNS c
--LEFT JOIN information_schema.KEY_COLUMN_USAGE k
--  ON  k.table_schema = c.table_schema
--  AND k.table_name   = c.table_name
--  AND k.column_name  = c.column_name
--  AND k.referenced_table_name IS NOT NULL
--WHERE c.table_schema = DATABASE()
--  AND c.column_name IN ('id_status','status_id')
--  AND k.referenced_table_name IS NULL
--ORDER BY c.table_name, c.column_name;*/
--
--
--
--
---- Xóa tất cả dữ liệu mẫu
--/*-- Tắt kiểm tra khóa ngoại để tránh lỗi ràng buộc
--SET FOREIGN_KEY_CHECKS = 0;
--
---- Bảng phụ thuộc (chi tiết / N-N / log)
--TRUNCATE TABLE promotion_usage;
--TRUNCATE TABLE user_promotion;
--TRUNCATE TABLE property_amenity;
--TRUNCATE TABLE property_facility;
--TRUNCATE TABLE favourite_list;
--TRUNCATE TABLE images;
--TRUNCATE TABLE booking;
--TRUNCATE TABLE `transaction`;
--TRUNCATE TABLE user_review;
--
---- Bảng chính
--TRUNCATE TABLE property;
--TRUNCATE TABLE amenities;
--TRUNCATE TABLE facilities;
--TRUNCATE TABLE locations;
--TRUNCATE TABLE cities;
--TRUNCATE TABLE user_social_accounts;
--TRUNCATE TABLE user_account;
--TRUNCATE TABLE promotion;
--TRUNCATE TABLE role;
--TRUNCATE TABLE status;
--TRUNCATE TABLE system_stats;
--
---- Bật lại kiểm tra khóa ngoại
--SET FOREIGN_KEY_CHECKS = 1;*/
--
---- Xóa tất cả khóa ngoại
--/*DELIMITER $$
--
--DROP PROCEDURE IF EXISTS drop_all_foreign_keys$$
--CREATE PROCEDURE drop_all_foreign_keys()
--BEGIN
--  -- ===== DECLARE: luôn đứng đầu block =====
--  DECLARE done INT DEFAULT 0;
--  DECLARE v_table  VARCHAR(128);
--  DECLARE v_fkname VARCHAR(128);
--  DECLARE v_dropped INT DEFAULT 0;
--
--  DECLARE fk_cur CURSOR FOR
--    SELECT rc.TABLE_NAME, rc.CONSTRAINT_NAME
--    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
--    WHERE rc.CONSTRAINT_SCHEMA = DATABASE();
--
--  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
--
--  -- ===== logic =====
--  SET @old_fk_checks := @@FOREIGN_KEY_CHECKS;
--  SET FOREIGN_KEY_CHECKS = 0;
--
--  OPEN fk_cur;
--  read_loop: LOOP
--    FETCH fk_cur INTO v_table, v_fkname;
--    IF done THEN
--      LEAVE read_loop;
--    END IF;
--
--    SET @sql := CONCAT('ALTER TABLE `', v_table, '` DROP FOREIGN KEY `', v_fkname, '`');
--    PREPARE stmt FROM @sql;
--    EXECUTE stmt;
--    DEALLOCATE PREPARE stmt;
--
--    SET v_dropped = v_dropped + 1;
--  END LOOP;
--  CLOSE fk_cur;
--
--  SET FOREIGN_KEY_CHECKS = @old_fk_checks;
--
--  -- Trả về số FK đã drop
--  SELECT v_dropped AS dropped_foreign_keys;
--END$$
--DELIMITER ;
--
--CALL drop_all_foreign_keys();
--DROP PROCEDURE drop_all_foreign_keys;*/

-- PHẦN 7: ĐỊNH NGHĨA TRIGGERS (ĐẶT Ở CUỐI CÙNG)
-- Di chuyển xuống cuối để đảm bảo tất cả bảng đã tồn tại và tránh lỗi client
DELIMITER $$

CREATE TRIGGER trg_promotion_set_end_date_before_insert
BEFORE INSERT ON promotion
FOR EACH ROW
BEGIN
  SET NEW.end_date = NEW.start_date + INTERVAL NEW.remaining_days DAY;
END$$

CREATE TRIGGER trg_promotion_set_end_date_before_update
BEFORE UPDATE ON promotion
FOR EACH ROW
BEGIN
  SET NEW.end_date = NEW.start_date + INTERVAL NEW.remaining_days DAY;
END$$

DELIMITER ;

