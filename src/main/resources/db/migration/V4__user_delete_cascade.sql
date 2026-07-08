-- Ensure deleting a user removes all owned rows across related tables.
-- refresh_tokens and password_reset_tokens already cascade from V1.
-- orders lacked ON DELETE CASCADE; order_items already cascade from orders.

ALTER TABLE orders
    DROP CONSTRAINT fk_orders_user;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- Tie webhook audit rows to orders so they cascade when orders (and users) are removed.
ALTER TABLE payment_events
    ADD COLUMN order_id UUID NULL;

ALTER TABLE payment_events
    ADD CONSTRAINT fk_payment_events_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE;

CREATE INDEX idx_payment_events_order_id ON payment_events (order_id);

UPDATE payment_events pe
SET order_id = o.id
FROM orders o
WHERE pe.order_id IS NULL
  AND o.paystack_reference IS NOT NULL
  AND pe.raw_payload -> 'data' ->> 'reference' = o.paystack_reference;
