CREATE TABLE orders (
    id UUID,
    customer_id UUID,
    status VARCHAR(20),
    order_date TIMESTAMP,
    subtotal NUMERIC(10,2),
    shipping NUMERIC(10,2),
    total NUMERIC(10,2)
);

CREATE TABLE order_items (
    id UUID,
    order_id UUID,
    product_id UUID,
    product_name VARCHAR(255),
    quantity INTEGER,
    unit_price NUMERIC(10,2),
    line_total NUMERIC(10,2)
)