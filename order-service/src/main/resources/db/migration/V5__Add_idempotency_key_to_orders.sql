ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(255),
    ADD COLUMN idempotency_request_hash VARCHAR(64);

UPDATE orders
SET
    idempotency_key = 'legacy-order-22222222-2222-2222-2222-222222222222',
    idempotency_request_hash = 'legacy';

ALTER TABLE orders
    ALTER COLUMN idempotency_key SET NOT NULL,
    ALTER COLUMN idempotency_request_hash SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT uk_orders_customer_idempotency_key
    UNIQUE (customer_id, idempotency_key);