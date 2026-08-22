CREATE TABLE shipping_quotes (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    postcode VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    weight_kg NUMERIC(10, 3) NOT NULL,
    shipping_method VARCHAR(50) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_shipping_quotes_order_id
    ON shipping_quotes (order_id);


CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    postcode VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    weight_kg NUMERIC(10, 3) NOT NULL,
    shipping_method VARCHAR(50) NOT NULL,
    shipping_cost NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    tracking_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_shipments_order_id
    ON shipments (order_id);