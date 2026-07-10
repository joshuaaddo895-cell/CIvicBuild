-- Extra suppliers + demo construction agency with delivery personnel.
-- Demo password for all seeded users: Secret123

-- Additional suppliers (V9 seed remains)
INSERT INTO suppliers (id, name, logo_url, rating, review_count, distance_km, verified, category_id) VALUES
    ('a1000001-0000-4000-8000-000000000004', 'Gravel Depot Accra', NULL, 4.4, 62, 4.2, true, 'gravel'),
    ('a1000001-0000-4000-8000-000000000005', 'AquaFlow Plumbing', NULL, 4.6, 41, 3.1, true, 'plumbing'),
    ('a1000001-0000-4000-8000-000000000006', 'PowerLine Electricals', NULL, 4.8, 73, 6.0, true, 'electrical')
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (id, name, category, price, unit, image_url, description, supplier_id, stock_quantity, brand, spec, delivery_estimate, active) VALUES
    ('b2000001-0000-4000-8000-000000000006', 'Quarry Dust', 'gravel', 120.00, 'per ton', NULL, 'Fine quarry dust for screeding.', 'a1000001-0000-4000-8000-000000000004', 80, 'Gravel Depot', '1 ton', 'Same day', true),
    ('b2000001-0000-4000-8000-000000000007', 'PVC Pipe 2 inch', 'plumbing', 18.00, 'per length', NULL, 'Heavy-duty PVC pipe for water lines.', 'a1000001-0000-4000-8000-000000000005', 400, 'AquaFlow', '2 inch x 3m', 'Next day', true),
    ('b2000001-0000-4000-8000-000000000008', 'Twin & Earth Cable 2.5mm', 'electrical', 95.00, 'per roll', NULL, 'House wiring cable roll.', 'a1000001-0000-4000-8000-000000000006', 120, 'PowerLine', '100m roll', '2 days', true)
ON CONFLICT (id) DO NOTHING;

-- Demo construction agency owner
INSERT INTO users (id, full_name, email, password_hash, role, verification_status, is_active) VALUES
    ('c1000001-0000-4000-8000-000000000001', 'BuildStrong Admin', 'agency@demo.civicbuild.test',
     '$2a$12$TPzOOr/P9KAP4t8BMuvdF.4urRArlXwjt1AdtTX0nitd0D2ePXErm', 'CONSTRUCTION_AGENCY', 'VERIFIED', true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_onboarding (user_id, account_type, onboarding_complete) VALUES
    ('c1000001-0000-4000-8000-000000000001', 'construction', true)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO agencies (id, owner_user_id, name, category, tagline, description, address, phone, verified) VALUES
    ('d1000001-0000-4000-8000-000000000001', 'c1000001-0000-4000-8000-000000000001',
     'BuildStrong Ghana', 'general-contracting', 'Quality builds across Accra',
     'Full-service construction agency for residential and commercial projects.',
     '14 Independence Ave, Accra', '+233201234567', true)
ON CONFLICT (id) DO NOTHING;

-- Delivery provider users
INSERT INTO users (id, full_name, email, password_hash, role, verification_status, is_active) VALUES
    ('e1000001-0000-4000-8000-000000000001', 'Kwame Mensah', 'delivery1@demo.civicbuild.test',
     '$2a$12$TPzOOr/P9KAP4t8BMuvdF.4urRArlXwjt1AdtTX0nitd0D2ePXErm', 'DELIVERY_PROVIDER', 'VERIFIED', true),
    ('e1000001-0000-4000-8000-000000000002', 'Ama Osei', 'delivery2@demo.civicbuild.test',
     '$2a$12$TPzOOr/P9KAP4t8BMuvdF.4urRArlXwjt1AdtTX0nitd0D2ePXErm', 'DELIVERY_PROVIDER', 'VERIFIED', true),
    ('e1000001-0000-4000-8000-000000000003', 'Kofi Boateng', 'delivery3@demo.civicbuild.test',
     '$2a$12$TPzOOr/P9KAP4t8BMuvdF.4urRArlXwjt1AdtTX0nitd0D2ePXErm', 'DELIVERY_PROVIDER', 'VERIFIED', true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_onboarding (user_id, account_type, onboarding_complete) VALUES
    ('e1000001-0000-4000-8000-000000000001', 'delivery', true),
    ('e1000001-0000-4000-8000-000000000002', 'delivery', true),
    ('e1000001-0000-4000-8000-000000000003', 'delivery', true)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO delivery_providers (id, user_id, construction_agency_id, full_name, vehicle_info, approval_status, handled_at) VALUES
    ('f1000001-0000-4000-8000-000000000001', 'e1000001-0000-4000-8000-000000000001',
     'd1000001-0000-4000-8000-000000000001', 'Kwame Mensah', 'Toyota Hilux pickup', 'pending', NULL),
    ('f1000001-0000-4000-8000-000000000002', 'e1000001-0000-4000-8000-000000000002',
     'd1000001-0000-4000-8000-000000000001', 'Ama Osei', 'Motorbike delivery', 'approved', now()),
    ('f1000001-0000-4000-8000-000000000003', 'e1000001-0000-4000-8000-000000000003',
     'd1000001-0000-4000-8000-000000000001', 'Kofi Boateng', 'Nissan Urvan van', 'approved', now())
ON CONFLICT (id) DO NOTHING;
