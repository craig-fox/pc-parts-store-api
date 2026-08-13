ALTER TABLE orders
    ADD COLUMN shipping_address_line1 VARCHAR(255),
    ADD COLUMN shipping_city VARCHAR(100),
    ADD COLUMN shipping_postcode VARCHAR(20),
    ADD COLUMN shipping_country VARCHAR(100);

UPDATE orders
SET
    shipping_address_line1 = '1 Main St',
    shipping_city = 'Auckland',
    shipping_postcode = '1010',
    shipping_country = 'NZ';

ALTER TABLE orders
    ALTER COLUMN shipping_address_line1 SET NOT NULL,
    ALTER COLUMN shipping_city SET NOT NULL,
    ALTER COLUMN shipping_postcode SET NOT NULL,
    ALTER COLUMN shipping_country SET NOT NULL;