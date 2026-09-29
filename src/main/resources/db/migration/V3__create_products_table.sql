CREATE TABLE products (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    description    TEXT,
    price          DECIMAL(10,2) NOT NULL,
    stock_quantity INT           NOT NULL DEFAULT 0,
    category_id    BIGINT        NOT NULL,
    seller_id      BIGINT        NOT NULL,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_seller   FOREIGN KEY (seller_id)   REFERENCES users (id)
);