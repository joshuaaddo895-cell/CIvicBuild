-- Order + Payment schema for Paystack checkout flow.
-- Money is stored as DECIMAL(10,2); never use floating-point types for currency.

CREATE TABLE orders (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID           NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    subtotal            DECIMAL(10, 2) NOT NULL,
    total               DECIMAL(10, 2) NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'GHS',
    delivery_address    VARCHAR(500)   NOT NULL,
    delivery_city       VARCHAR(100)   NOT NULL,
    delivery_region     VARCHAR(100)   NOT NULL,
    phone_number        VARCHAR(30)    NOT NULL,
    paystack_reference  VARCHAR(100)   NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_orders_paystack_reference UNIQUE (paystack_reference),
    CONSTRAINT chk_orders_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'REFUNDED')
    ),
    CONSTRAINT chk_orders_subtotal_non_negative CHECK (subtotal >= 0),
    CONSTRAINT chk_orders_total_non_negative CHECK (total >= 0)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_paystack_reference ON orders (paystack_reference);

CREATE TABLE order_items (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID           NOT NULL,
    product_name    VARCHAR(255)   NOT NULL,
    supplier_name   VARCHAR(255)   NOT NULL,
    unit_price      DECIMAL(10, 2) NOT NULL,
    quantity        DECIMAL(10, 2) NOT NULL,
    unit            VARCHAR(20)    NOT NULL,
    line_total      DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_items_line_total_non_negative CHECK (line_total >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE payment_events (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_key       VARCHAR(255) NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    raw_payload     JSONB        NOT NULL,
    processed_at    TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_events_event_key UNIQUE (event_key)
);

CREATE INDEX idx_payment_events_event_key ON payment_events (event_key);
CREATE INDEX idx_payment_events_event_type ON payment_events (event_type);

CREATE TRIGGER trg_orders_set_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
