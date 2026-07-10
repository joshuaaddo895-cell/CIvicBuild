-- Dev/staging seed data equivalent to frontend mocks.

INSERT INTO categories (id, name, sort_order) VALUES
    ('cement', 'Cement', 1),
    ('blocks', 'Blocks', 2),
    ('gravel', 'Gravel', 3),
    ('steel', 'Steel', 4),
    ('roofing', 'Roofing', 5),
    ('tiles', 'Tiles', 6),
    ('paint', 'Paint', 7),
    ('plumbing', 'Plumbing', 8),
    ('electrical', 'Electrical', 9);

INSERT INTO suppliers (id, name, logo_url, rating, review_count, distance_km, verified, category_id) VALUES
    ('a1000001-0000-4000-8000-000000000001', 'BuildMart Ghana', NULL, 4.7, 128, 2.4, true, 'cement'),
    ('a1000001-0000-4000-8000-000000000002', 'Steel & More Ltd', NULL, 4.5, 86, 5.1, true, 'steel'),
    ('a1000001-0000-4000-8000-000000000003', 'RoofPro Supplies', NULL, 4.3, 54, 3.8, true, 'roofing');

INSERT INTO products (id, name, category, price, unit, image_url, description, supplier_id, stock_quantity, brand, spec, delivery_estimate, active) VALUES
    ('b2000001-0000-4000-8000-000000000001', 'Dangote Cement 50kg', 'cement', 88.00, 'per bag', NULL, 'Premium Portland cement for all construction needs.', 'a1000001-0000-4000-8000-000000000001', 500, 'Dangote', '50kg bag', 'Same day', true),
    ('b2000001-0000-4000-8000-000000000002', 'Iron Rods 12mm', 'steel', 45.00, 'per length', NULL, 'High-tensile reinforcement steel rods.', 'a1000001-0000-4000-8000-000000000002', 200, 'Tema Steel', '12mm x 6m', 'Next day', true),
    ('b2000001-0000-4000-8000-000000000003', 'Aluzinc Roofing Sheet', 'roofing', 120.00, 'per sheet', NULL, 'Corrugated aluzinc roofing sheets.', 'a1000001-0000-4000-8000-000000000003', 150, 'RoofPro', '0.45mm', '2-3 days', true),
    ('b2000001-0000-4000-8000-000000000004', 'Sandcrete Blocks 6"', 'blocks', 5.50, 'per block', NULL, 'Standard 6-inch sandcrete blocks.', 'a1000001-0000-4000-8000-000000000001', 1000, 'Local', '6 inch', 'Same day', true),
    ('b2000001-0000-4000-8000-000000000005', 'Ceramic Floor Tiles', 'tiles', 35.00, 'per sqm', NULL, 'Glazed ceramic floor tiles.', 'a1000001-0000-4000-8000-000000000001', 300, 'TileMax', '40x40cm', '2-3 days', true);
