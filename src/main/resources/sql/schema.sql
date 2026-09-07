CREATE TABLE IF NOT EXISTS products
(
    product_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id   BIGINT,
    name        VARCHAR(250)                          NOT NULL,
    description VARCHAR(500)                          NOT NULL,
    price       DECIMAL(10, 2)                        NOT NULL,
    popularity  INT                                   NOT NULL,
    image_url   VARCHAR(500),
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  VARCHAR(20)                           NOT NULL,
    updated_at  TIMESTAMP   DEFAULT NULL,
    updated_by  VARCHAR(20) DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS contacts
(
    contact_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)                          NOT NULL,
    email         VARCHAR(100)                          NOT NULL,
    mobile_number VARCHAR(15)                           NOT NULL,
    message       VARCHAR(500)                          NOT NULL,
    status        VARCHAR(50)       NOT NULL,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(20)                           NOT NULL,
    updated_at    TIMESTAMP   DEFAULT NULL,
    updated_by    VARCHAR(20) DEFAULT NULL
    );

CREATE TABLE IF NOT EXISTS customers
(
    customer_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)                          NOT NULL,
    email         VARCHAR(100)                          NOT NULL UNIQUE,
    mobile_number VARCHAR(15)                           NOT NULL,
    password_hash VARCHAR(500)                          NOT NULL,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(20)                           NOT NULL,
    updated_at    TIMESTAMP   DEFAULT NULL,
    updated_by    VARCHAR(20) DEFAULT NULL,
    UNIQUE KEY unique_email (email),
    UNIQUE KEY unique_mobile_number (mobile_number)
    );

CREATE TABLE IF NOT EXISTS address
(
    address_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id       BIGINT NOT NULL UNIQUE,
    street        VARCHAR(150) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100) NOT NULL,
    postal_code   VARCHAR(20)  NOT NULL,
    country       VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by    VARCHAR(20)  NOT NULL,
    updated_at    TIMESTAMP    DEFAULT NULL,
    updated_by    VARCHAR(20)  DEFAULT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS roles (
    role_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP   DEFAULT NULL,
    updated_by VARCHAR(20) DEFAULT NULL,
    UNIQUE KEY unique_name (name)
    );

CREATE TABLE IF NOT EXISTS customer_roles (
   customer_id BIGINT NOT NULL,
   role_id     BIGINT NOT NULL,
   PRIMARY KEY (customer_id, role_id),
   FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
   FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_USER', CURRENT_TIMESTAMP, 'DBA');

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_ADMIN', CURRENT_TIMESTAMP, 'DBA');

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_OPS_ENG', CURRENT_TIMESTAMP, 'DBA');

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_QA_ENG', CURRENT_TIMESTAMP, 'DBA');

INSERT INTO roles (name, created_at, created_by)
VALUES ('ROLE_VENDOR', CURRENT_TIMESTAMP, 'DBA');

CREATE TABLE IF NOT EXISTS vendor_profiles
(
    vendor_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT       NOT NULL UNIQUE,
    store_name       VARCHAR(150) NOT NULL,
    store_description VARCHAR(500),
    logo_url         VARCHAR(500),
    created_at       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR(20)                           NOT NULL,
    updated_at       TIMESTAMP   DEFAULT NULL,
    updated_by       VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
);

ALTER TABLE products
    ADD CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES vendor_profiles (vendor_id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS posts
(
    post_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id   BIGINT       NOT NULL,
    product_id  BIGINT,
    content     VARCHAR(1000) NOT NULL,
    image_url   VARCHAR(500),
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  VARCHAR(20)                           NOT NULL,
    updated_at  TIMESTAMP   DEFAULT NULL,
    updated_by  VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (vendor_id) REFERENCES vendor_profiles (vendor_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS post_likes
(
    like_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id     BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  VARCHAR(20)                           NOT NULL,
    updated_at  TIMESTAMP   DEFAULT NULL,
    updated_by  VARCHAR(20) DEFAULT NULL,
    UNIQUE KEY unique_post_like (post_id, customer_id),
    FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_comments
(
    comment_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id     BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    content     VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  VARCHAR(20)                           NOT NULL,
    updated_at  TIMESTAMP   DEFAULT NULL,
    updated_by  VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS vendor_follows
(
    follow_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id   BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by  VARCHAR(20)                           NOT NULL,
    updated_at  TIMESTAMP   DEFAULT NULL,
    updated_by  VARCHAR(20) DEFAULT NULL,
    UNIQUE KEY unique_vendor_follow (vendor_id, customer_id),
    FOREIGN KEY (vendor_id) REFERENCES vendor_profiles (vendor_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications
(
    notification_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_customer_id BIGINT NOT NULL,
    actor_customer_id     BIGINT,
    type              VARCHAR(30) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    post_id           BIGINT,
    is_read           BOOLEAN     DEFAULT FALSE NOT NULL,
    created_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by        VARCHAR(20)                           NOT NULL,
    updated_at        TIMESTAMP   DEFAULT NULL,
    updated_by        VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (recipient_customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE,
    FOREIGN KEY (actor_customer_id) REFERENCES customers (customer_id) ON DELETE SET NULL,
    FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS orders
(
    order_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL,
    total_price    DECIMAL(10, 2)                        NOT NULL,
    payment_id     VARCHAR(200)                          NOT NULL,
    payment_status VARCHAR(50)                           NOT NULL,
    order_status   VARCHAR(50)                           NOT NULL,
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR(20)                           NOT NULL,
    updated_at     TIMESTAMP   DEFAULT NULL,
    updated_by     VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
    );

CREATE TABLE IF NOT EXISTS order_items
(
    order_item_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    quantity        INT NOT NULL,
    price           DECIMAL(10, 2) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR(20)    NOT NULL,
    updated_at      TIMESTAMP      DEFAULT NULL,
    updated_by      VARCHAR(20)    DEFAULT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    );