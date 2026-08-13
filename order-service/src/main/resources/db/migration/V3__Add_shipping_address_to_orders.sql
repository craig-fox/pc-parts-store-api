ALTER TABLE orders
    ADD COLUMN address_line1 VARCHAR(255),
    ADD COLUMN city VARCHAR(100),
    ADD COLUMN postcode VARCHAR(20),
    ADD COLUMN country VARCHAR(100);

UPDATE orders
SET
    address_line1 = '1 Main St',
    city = 'Auckland',
    postcode = '1010',
    country = 'NZ';

ALTER TABLE orders
    ALTER COLUMN address_line1 SET NOT NULL,
    ALTER COLUMN city SET NOT NULL,
    ALTER COLUMN postcode SET NOT NULL,
    ALTER COLUMN country SET NOT NULL;