INSERT INTO orders (
    id,
    customer_id,
    status,
    order_date,
    subtotal,
    shipping,
    total
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'PLACED',
    CURRENT_TIMESTAMP,
    100.00,
    15.00,
    115.00
);

INSERT INTO order_items (
    id,
    order_id,
    product_id,
    product_name,
    quantity,
    unit_price,
    line_total
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    '44444444-4444-4444-4444-444444444444',
    'Test Product',
    1,
    100.00,
    100.00
);