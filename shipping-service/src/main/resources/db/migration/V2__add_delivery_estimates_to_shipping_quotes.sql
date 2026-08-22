ALTER TABLE shipping_quotes
    ADD COLUMN estimated_delivery_min INTEGER,
    ADD COLUMN estimated_delivery_max INTEGER;

UPDATE shipping_quotes
SET estimated_delivery_min = 2,
    estimated_delivery_max = 5
WHERE estimated_delivery_min IS NULL;

ALTER TABLE shipping_quotes
    ALTER COLUMN estimated_delivery_min SET NOT NULL,
    ALTER COLUMN estimated_delivery_max SET NOT NULL;