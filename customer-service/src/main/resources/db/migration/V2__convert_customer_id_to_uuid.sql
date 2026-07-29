-- Remove the existing primary key column
ALTER TABLE customers
DROP COLUMN id;

-- Add a UUID primary key
ALTER TABLE customers
ADD COLUMN id UUID PRIMARY KEY;