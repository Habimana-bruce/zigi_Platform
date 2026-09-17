

CREATE DATABASE IF NOT EXISTS zigi_db;
USE zigi_db;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NULL,
    password VARCHAR(255) NULL,
    main_balance DECIMAL(12,2) DEFAULT 0.00,
    airtime_balance DECIMAL(12,2) DEFAULT 0.00,
    data_balance_mb INT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE customers ADD COLUMN IF NOT EXISTS email VARCHAR(150) NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS password VARCHAR(255) NULL;

CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    group_name VARCHAR(20) NOT NULL,
    label VARCHAR(150) NOT NULL,
    icon VARCHAR(10),
    response_text TEXT,
    action_type VARCHAR(20) DEFAULT 'TEXT',
    sort_order INT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES menu_items(id)
);

DELETE FROM menu_items;

INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, action_type, sort_order) VALUES
(1, NULL, 'MAIN', 'My Balance', '📜', NULL, 'BALANCE', 1),
(2, NULL, 'MAIN', 'Play Services', '🎮', NULL, 'TEXT', 2),
(3, NULL, 'MAIN', 'Hot Deals', '😎', NULL, 'TEXT', 3),
(4, NULL, 'MAIN', 'Zigi Daily Freebies', '🎁', NULL, 'TEXT', 4);

INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(5, 2, 'MAIN', 'Daily Quiz', '🧠', 'Today''s Daily Quiz - coming soon. Play daily to win airtime and data!', 1);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(6, 3, 'MAIN', 'Night Offers', '🌙', NULL, 1),
(7, 3, 'MAIN', 'CALL', '📞', NULL, 2),
(8, 3, 'MAIN', 'Internet', '🌐', NULL, 3);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(9,  6, 'MAIN', '500Frw=3GB', '💾', 'You have selected the Night Offer: 500Frw = 3GB.', 1),
(10, 6, 'MAIN', '1000Frw=7GB', '💾', 'You have selected the Night Offer: 1000Frw = 7GB.', 2),
(11, 6, 'MAIN', '10,000Frw=30720MB', '💾', 'You have selected the Night Offer: 10,000Frw = 30720MB.', 3);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(12, 7, 'MAIN', '200Frw=250mins', '📞', 'You have selected: 200Frw = 250 minutes.', 1),
(13, 7, 'MAIN', '500Frw=800mins', '📞', 'You have selected: 500Frw = 800 minutes.', 2);

INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(14, 8, 'MAIN', '500Frw=3GB', '🌐', 'You have selected: 500Frw = 3GB (Internet).', 1),
(15, 8, 'MAIN', '1000Frw=7GB', '🌐', 'You have selected: 1000Frw = 7GB (Internet).', 2),
(16, 8, 'MAIN', '10,000Frw=30720MB', '🌐', 'You have selected: 10,000Frw = 30720MB (Internet).', 3);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(17, 4, 'MAIN', '500Frw=3GB', '🎁', 'Freebie claimed: 500Frw = 3GB.', 1),
(18, 4, 'MAIN', '1000Frw=7GB', '🎁', 'Freebie claimed: 1000Frw = 7GB.', 2),
(19, 4, 'MAIN', '10,000Frw=30720MB', '🎁', 'Freebie claimed: 10,000Frw = 30720MB.', 3);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(20, NULL, 'HELP', 'Tariff Plans', '📞', NULL, 1),
(21, NULL, 'HELP', 'Account History', '🔍', 'Please wait a moment while we retrieve your account history...', 2),
(22, NULL, 'HELP', 'Pay Bill', '🧑‍💰', NULL, 3),
(23, NULL, 'HELP', 'NIN Services', '🆔', NULL, 4),
(24, NULL, 'HELP', 'Track Complaint', '📋', NULL, 5),
(25, NULL, 'HELP', 'Get My PUK', '🔓', 'Please visit the nearest service center to retrieve your PUK code.', 6);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(26, 20, 'HELP', 'View Available Plans', '📄', 'Here are the available tariff plans - coming soon.', 1),
(27, 20, 'HELP', 'Check My Current Plan', '📄', 'Your current plan - coming soon.', 2);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(28, 22, 'HELP', 'Electricity', '💡', 'Electricity bill payment - coming soon.', 1),
(29, 22, 'HELP', 'Water', '💧', 'Water bill payment - coming soon.', 2),
(30, 22, 'HELP', 'Internet (Canal box)', '📡', 'Canal box internet bill payment - coming soon.', 3),
(31, 22, 'HELP', 'TV Subscription', '📺', NULL, 4);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(32, 31, 'HELP', 'DSTV', '📺', 'DSTV subscription payment - coming soon.', 1),
(33, 31, 'HELP', 'AZAMTV', '📺', 'AZAMTV subscription payment - coming soon.', 2),
(34, 31, 'HELP', 'Canal+', '📺', 'Canal+ subscription payment - coming soon.', 3);

INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(35, 23, 'HELP', 'Check Dependents Numbers', '🆔', 'Your dependents'' numbers - coming soon.', 1),
(36, 23, 'HELP', 'Self Deregistration', '🆔', 'NIN self deregistration - coming soon.', 2);

INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(37, 24, 'HELP', 'Add a Complaint', '📋', 'Complaint submission - coming soon.', 1),
(38, 24, 'HELP', 'View My Recent Complaints', '📋', 'Your recent complaints - coming soon.', 2);

-- ===== DATA button (top-level group, shown when "Data" is clicked) =====
INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(39, NULL, 'DATA', '500Frw=3GB', '💳', 'You have purchased: 500Frw = 3GB.', 1),
(40, NULL, 'DATA', '1000Frw=7GB', '💳', 'You have purchased: 1000Frw = 7GB.', 2),
(41, NULL, 'DATA', '10,000Frw=30720MB', '💳', 'You have purchased: 10,000Frw = 30720MB.', 3);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(42, NULL, 'AIRTIME', 'Buy Airtime', '💡', NULL, 1),
(43, NULL, 'AIRTIME', 'Check Airtime Balance', '💡', 'Please use My Balance from the main menu to check your airtime balance.', 2),
(44, NULL, 'AIRTIME', 'Transfer Airtime', '💡', 'Airtime transfer - coming soon.', 3),
(45, NULL, 'AIRTIME', 'Airtime History', '💡', 'Your airtime history - coming soon.', 4);

-- Buy Airtime pricing tiers
INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(46, 42, 'AIRTIME', '500Frw=3GB', '💡', 'You have purchased airtime: 500Frw = 3GB.', 1),
(47, 42, 'AIRTIME', '1000Frw=7GB', '💡', 'You have purchased airtime: 1000Frw = 7GB.', 2),
(48, 42, 'AIRTIME', '10,000Frw=30720MB', '💡', 'You have purchased airtime: 10,000Frw = 30720MB.', 3);


INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, sort_order) VALUES
(49, NULL, 'ASSIST', 'Assist', '', 'Just call 100 or visit the nearest service center.', 1);


ALTER TABLE customers ADD COLUMN IF NOT EXISTS minutes_balance INT DEFAULT 0;

-- Extra menu_item columns for payable items
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) NULL;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS credit_type VARCHAR(20) NULL;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS credit_amount DECIMAL(10,2) NULL;

-- One row per payment request. Created PENDING, updated to SUCCESS/FAILED by the callback.
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    menu_item_id BIGINT,
    description VARCHAR(200) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'MTN',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_ref VARCHAR(100),
    channel VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS payment_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    provider VARCHAR(20) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attempt_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);



CREATE TABLE IF NOT EXISTS otp_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    delivery_method VARCHAR(10) NOT NULL DEFAULT 'PHONE', 
    code VARCHAR(6) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    attempts INT NOT NULL DEFAULT 0,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_otp_phone ON otp_codes(phone_number);

CREATE TABLE IF NOT EXISTS client_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME NULL
);
CREATE INDEX idx_session_token ON client_sessions(token);
CREATE INDEX idx_session_phone ON client_sessions(phone_number);

UPDATE menu_items SET action_type='PAYMENT', price=500,    credit_type='DATA_MB', credit_amount=3072  WHERE id=9;
UPDATE menu_items SET action_type='PAYMENT', price=1000,   credit_type='DATA_MB', credit_amount=7168  WHERE id=10;
UPDATE menu_items SET action_type='PAYMENT', price=10000,  credit_type='DATA_MB', credit_amount=30720 WHERE id=11;

UPDATE menu_items SET action_type='PAYMENT', price=200,  credit_type='MINUTES', credit_amount=250 WHERE id=12;
UPDATE menu_items SET action_type='PAYMENT', price=500,  credit_type='MINUTES', credit_amount=800 WHERE id=13;

UPDATE menu_items SET action_type='PAYMENT', price=500,    credit_type='DATA_MB', credit_amount=3072  WHERE id=14;
UPDATE menu_items SET action_type='PAYMENT', price=1000,   credit_type='DATA_MB', credit_amount=7168  WHERE id=15;
UPDATE menu_items SET action_type='PAYMENT', price=10000,  credit_type='DATA_MB', credit_amount=30720 WHERE id=16;

UPDATE menu_items SET action_type='PAYMENT', price=500,    credit_type='DATA_MB', credit_amount=3072  WHERE id=17;
UPDATE menu_items SET action_type='PAYMENT', price=1000,   credit_type='DATA_MB', credit_amount=7168  WHERE id=18;
UPDATE menu_items SET action_type='PAYMENT', price=10000,  credit_type='DATA_MB', credit_amount=30720 WHERE id=19;

UPDATE menu_items SET action_type='PAYMENT', price=500,    credit_type='DATA_MB', credit_amount=3072  WHERE id=39;
UPDATE menu_items SET action_type='PAYMENT', price=1000,   credit_type='DATA_MB', credit_amount=7168  WHERE id=40;
UPDATE menu_items SET action_type='PAYMENT', price=10000,  credit_type='DATA_MB', credit_amount=30720 WHERE id=41;

UPDATE menu_items SET action_type='PAYMENT', price=500,    credit_type='AIRTIME', credit_amount=500   WHERE id=46;
UPDATE menu_items SET action_type='PAYMENT', price=1000,   credit_type='AIRTIME', credit_amount=1000  WHERE id=47;
UPDATE menu_items SET action_type='PAYMENT', price=10000,  credit_type='AIRTIME', credit_amount=10000 WHERE id=48;

ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS balance_type VARCHAR(20) NULL;

UPDATE menu_items SET action_type = 'TEXT', balance_type = NULL WHERE id = 1;

INSERT INTO menu_items (id, parent_id, group_name, label, icon, response_text, action_type, balance_type, sort_order) VALUES
(50, 1, 'MAIN', 'Airtime Balance', '💰', NULL, 'BALANCE', 'AIRTIME', 1),
(51, 1, 'MAIN', 'Data Balance', '📶', NULL, 'BALANCE', 'DATA', 2)
ON DUPLICATE KEY UPDATE label = VALUES(label);

ALTER TABLE menu_items MODIFY id BIGINT AUTO_INCREMENT;

ALTER TABLE menu_items AUTO_INCREMENT = 100;

UPDATE menu_items SET response_text = REPLACE(response_text, ' (demo) - coming soon.', ' - coming soon.');
UPDATE menu_items SET response_text = REPLACE(response_text, '(demo) - coming soon.', '- coming soon.');
UPDATE menu_items SET response_text = REPLACE(response_text, ' (demo)', '');
UPDATE menu_items SET response_text = REPLACE(response_text, '(demo)', '');

CREATE TABLE IF NOT EXISTS ussd_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    phone_number VARCHAR(20),
    accumulated_text TEXT,
    expires_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE ussd_sessions ADD COLUMN IF NOT EXISTS expires_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS ussd_xml_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    phone_number VARCHAR(20),
    accumulated_text TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
