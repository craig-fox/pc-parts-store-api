INSERT INTO products (
    id,
    sku,
    name,
    description,
    brand,
    category,
    price,
    stock_quantity,
    weight_kg,
    image_url,
    active,
    created_at,
    updated_at
)
VALUES
-- CPUs
(gen_random_uuid(), 'CPU-AMD-9800X3D', 'AMD Ryzen 7 9800X3D',
    '8-core gaming processor with 3D V-Cache.',
    'AMD', 'CPU', 899.99, 15, 0.12,
    '/images/products/amd-9800x3d.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'CPU-INT-U7-265K', 'Intel Core Ultra 7 265K',
    'High-performance desktop processor.',
    'Intel', 'CPU', 749.99, 10, 0.12,
    '/images/products/intel-ultra7-265k.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Graphics Cards
    (gen_random_uuid(), 'GPU-NV-5070TI', 'NVIDIA GeForce RTX 5070 Ti',
    'High-end graphics card for gaming and AI workloads.',
    'NVIDIA', 'Graphics Card', 1699.99, 6, 1.80,
    '/images/products/rtx5070ti.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'GPU-AMD-9070XT', 'AMD Radeon RX 9070 XT',
    'Flagship AMD graphics card.',
    'AMD', 'Graphics Card', 1399.99, 8, 1.75,
    '/images/products/rx9070xt.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Motherboards
(gen_random_uuid(), 'MB-ASUS-X870E', 'ASUS ROG Strix X870E-E Gaming WiFi',
    'Premium AM5 motherboard.',
    'ASUS', 'Motherboard', 699.99, 12, 1.20,
    '/images/products/asus-x870e.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'MB-MSI-Z890', 'MSI MAG Z890 Tomahawk WiFi',
    'Intel Z890 ATX motherboard.',
    'MSI', 'Motherboard', 529.99, 9, 1.25,
    '/images/products/msi-z890.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Memory
(gen_random_uuid(), 'RAM-COR-32', 'Corsair Vengeance DDR5 32GB',
    '32GB (2x16GB) DDR5-6000 CL30.',
    'Corsair', 'Memory', 269.99, 25, 0.15,
    '/images/products/corsair-ddr5.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'RAM-GSK-64', 'G.Skill Trident Z5 RGB 64GB',
    '64GB DDR5-6400 memory kit.',
    'G.Skill', 'Memory', 489.99, 14, 0.18,
    '/images/products/gskill-z5.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Storage
(gen_random_uuid(), 'SSD-SAM-990-2TB', 'Samsung 990 Pro 2TB',
    'PCIe Gen4 NVMe SSD.',
    'Samsung', 'Storage', 319.99, 30, 0.05,
    '/images/products/samsung-990pro.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'SSD-WD-4TB', 'WD Black SN850X 4TB',
    'High-speed PCIe Gen4 SSD.',
    'Western Digital', 'Storage', 599.99, 11, 0.05,
    '/images/products/sn850x.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Power Supplies
(gen_random_uuid(), 'PSU-COR-850', 'Corsair RM850x',
    '850W 80+ Gold modular power supply.',
    'Corsair', 'Power Supply', 249.99, 18, 2.00,
    '/images/products/rm850x.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'PSU-SEA-1000', 'Seasonic Vertex GX-1000',
    '1000W fully modular PSU.',
    'Seasonic', 'Power Supply', 369.99, 8, 2.20,
    '/images/products/vertex1000.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Cases
(gen_random_uuid(), 'CASE-FRA-N7', 'Fractal Design North',
    'Mid-tower case with walnut front.',
    'Fractal Design', 'Case', 299.99, 13, 8.50,
    '/images/products/fractal-north.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'CASE-LIA-O11', 'Lian Li O11 Dynamic EVO',
    'Popular dual-chamber PC case.',
    'Lian Li', 'Case', 329.99, 9, 9.20,
    '/images/products/o11-evo.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Cooling
(gen_random_uuid(), 'COOL-NOC-D15', 'Noctua NH-D15 G2',
    'Premium dual-tower CPU air cooler.',
    'Noctua', 'Cooling', 249.99, 16, 1.40,
    '/images/products/nhd15g2.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'COOL-ARC-LIQ360', 'Arctic Liquid Freezer III 360',
    '360mm AIO liquid CPU cooler.',
    'Arctic', 'Cooling', 289.99, 7, 2.10,
    '/images/products/liquidfreezer3.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Monitors
(gen_random_uuid(), 'MON-LG-32OLED', 'LG UltraGear 32" OLED',
    '32-inch 4K OLED gaming monitor.',
    'LG', 'Monitor', 1899.99, 5, 7.10,
    '/images/products/lg-oled32.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'MON-DEL-27QHD', 'Dell UltraSharp 27"',
    '27-inch QHD productivity monitor.',
    'Dell', 'Monitor', 649.99, 10, 5.80,
    '/images/products/dell-ultrasharp27.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Peripherals
(gen_random_uuid(), 'KEY-LOG-MX', 'Logitech MX Mechanical',
    'Wireless mechanical keyboard.',
    'Logitech', 'Keyboard', 299.99, 20, 0.90,
    '/images/products/mx-mechanical.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'MOU-LOG-GPX2', 'Logitech G Pro X Superlight 2',
    'Ultra-light wireless gaming mouse.',
    'Logitech', 'Mouse', 279.99, 24, 0.06,
    '/images/products/gprox2.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);