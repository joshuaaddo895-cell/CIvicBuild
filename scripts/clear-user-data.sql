-- Wipe all accounts, tokens, and user-owned data. Keeps seed catalog (categories, suppliers, products).
-- Refresh tokens and password reset tokens cascade from users.

BEGIN;

DELETE FROM users;

-- Sanity: seed catalog should remain
SELECT 'categories' AS table_name, COUNT(*) AS rows FROM categories
UNION ALL SELECT 'suppliers', COUNT(*) FROM suppliers
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'refresh_tokens', COUNT(*) FROM refresh_tokens;

COMMIT;
