CREATE TABLE inventory (
    product_id UUID PRIMARY KEY,
    quantity_on_hand INTEGER NOT NULL CHECK (quantity_on_hand >= 0),
    quantity_reserved INTEGER NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL
);