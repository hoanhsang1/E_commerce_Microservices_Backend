CREATE DATABASE IF NOT EXISTS user_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE user_db;
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT UNSIGNED NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

INSERT INTO users
(username, email, password, full_name, phone, role)
VALUES
('sang', 'sang@gmail.com', '123456', 'Ho Anh Sang', '0900000001', 'ADMIN'),
('nguyenan', 'an@gmail.com', '123456', 'Nguyen Van An', '0900000002', 'USER');

CREATE DATABASE IF NOT EXISTS product_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE product_db;

CREATE TABLE products (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(15,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    category VARCHAR(100),
    status ENUM('AVAILABLE', 'OUT_OF_STOCK', 'INACTIVE')
        NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT chk_product_quantity CHECK (quantity >= 0)
);

INSERT INTO products
(name, description, price, quantity, category)
VALUES
('iPhone 15', 'Apple smartphone', 20000000, 10, 'Phone'),
('MacBook Air M3', 'Apple laptop', 28000000, 5, 'Laptop'),
('Logitech K380', 'Wireless keyboard', 800000, 20, 'Keyboard');

CREATE DATABASE IF NOT EXISTS order_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE order_db;

CREATE TABLE orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT UNSIGNED NOT NULL,

    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,

    status ENUM(
        'PENDING',
        'CONFIRMED',
        'PAID',
        'SHIPPING',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',

    shipping_address VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_order_total CHECK (total_amount >= 0)
);

CREATE TABLE order_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    order_id BIGINT UNSIGNED NOT NULL,

    product_id BIGINT UNSIGNED NOT NULL,

    quantity INT NOT NULL,

    price DECIMAL(15,2) NOT NULL,

    subtotal DECIMAL(15,2) NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_item_price
        CHECK (price >= 0),

    CONSTRAINT chk_order_item_subtotal
        CHECK (subtotal >= 0)
);

CREATE DATABASE IF NOT EXISTS payment_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE payment_db;

CREATE TABLE payments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    order_id BIGINT UNSIGNED NOT NULL,

    user_id BIGINT UNSIGNED NOT NULL,

    amount DECIMAL(15,2) NOT NULL,

    method ENUM(
        'CASH',
        'BANKING',
        'MOMO',
        'VNPAY'
    ) NOT NULL,

    status ENUM(
        'PENDING',
        'SUCCESS',
        'FAILED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'PENDING',

    transaction_code VARCHAR(100) UNIQUE,

    paid_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

INSERT INTO payments
(order_id, user_id, amount, method, status, transaction_code, paid_at)
VALUES
(1, 1, 20800000, 'BANKING', 'SUCCESS', 'TXN001', NOW());

