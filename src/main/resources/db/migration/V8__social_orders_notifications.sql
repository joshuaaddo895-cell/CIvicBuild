-- Saved items, reviews, messaging, notifications, order fulfillment, delivery jobs.

CREATE TABLE saved_items (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    subject_id    UUID         NOT NULL,
    subject_type  VARCHAR(20)  NOT NULL,
    saved_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_saved_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_saved_items_user_subject UNIQUE (user_id, subject_type, subject_id),
    CONSTRAINT chk_saved_items_subject_type CHECK (subject_type IN ('product', 'supplier', 'agency'))
);

CREATE INDEX idx_saved_items_user_id ON saved_items (user_id);

CREATE TABLE reviews (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_type       VARCHAR(20)  NOT NULL,
    subject_id         UUID         NOT NULL,
    user_id            UUID         NOT NULL,
    reviewer_name      VARCHAR(150) NOT NULL,
    rating             INT          NOT NULL,
    text               TEXT         NULL,
    verified_purchase  BOOLEAN      NOT NULL DEFAULT false,
    order_number       VARCHAR(100) NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_subject_type CHECK (subject_type IN ('product', 'supplier')),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_subject ON reviews (subject_type, subject_id);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);

CREATE TRIGGER trg_reviews_set_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE message_threads (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_user_id  UUID         NOT NULL,
    agency_id         UUID         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_message_threads_customer FOREIGN KEY (customer_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_threads_agency FOREIGN KEY (agency_id) REFERENCES agencies (id) ON DELETE CASCADE,
    CONSTRAINT uq_message_threads_customer_agency UNIQUE (customer_user_id, agency_id)
);

CREATE INDEX idx_message_threads_customer ON message_threads (customer_user_id);
CREATE INDEX idx_message_threads_agency ON message_threads (agency_id);

CREATE TRIGGER trg_message_threads_set_updated_at
    BEFORE UPDATE ON message_threads
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE messages (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id       UUID         NOT NULL,
    sender_user_id  UUID         NOT NULL,
    text            TEXT         NOT NULL,
    sent_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_messages_thread FOREIGN KEY (thread_id) REFERENCES message_threads (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_thread_id ON messages (thread_id);

CREATE TABLE thread_read_states (
    thread_id     UUID         NOT NULL,
    user_id       UUID         NOT NULL,
    last_read_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (thread_id, user_id),
    CONSTRAINT fk_thread_read_states_thread FOREIGN KEY (thread_id) REFERENCES message_threads (id) ON DELETE CASCADE,
    CONSTRAINT fk_thread_read_states_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT         NOT NULL,
    read        BOOLEAN      NOT NULL DEFAULT false,
    data        JSONB        NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_type CHECK (type IN ('order', 'verification', 'personnel', 'message'))
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, read);

-- Order fulfillment fields for agency + delivery workflows.
ALTER TABLE orders ADD COLUMN fulfillment_status VARCHAR(20) NOT NULL DEFAULT 'pending';
ALTER TABLE orders ADD CONSTRAINT chk_orders_fulfillment_status CHECK (
    fulfillment_status IN ('pending', 'processing', 'delivered', 'cancelled')
);

ALTER TABLE order_items ADD COLUMN product_id UUID NULL;
ALTER TABLE order_items ADD COLUMN agency_id UUID NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_agency FOREIGN KEY (agency_id) REFERENCES agencies (id) ON DELETE SET NULL;

CREATE INDEX idx_order_items_agency_id ON order_items (agency_id);

CREATE TABLE delivery_jobs (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id              UUID         NOT NULL,
    delivery_provider_id  UUID         NOT NULL,
    pickup_address        VARCHAR(500) NULL,
    delivery_address      VARCHAR(500) NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'assigned',
    assigned_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_delivery_jobs_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_jobs_provider FOREIGN KEY (delivery_provider_id) REFERENCES delivery_providers (id) ON DELETE CASCADE,
    CONSTRAINT chk_delivery_jobs_status CHECK (status IN ('assigned', 'in_transit', 'delivered'))
);

CREATE INDEX idx_delivery_jobs_provider_id ON delivery_jobs (delivery_provider_id);
CREATE INDEX idx_delivery_jobs_order_id ON delivery_jobs (order_id);

CREATE TRIGGER trg_delivery_jobs_set_updated_at
    BEFORE UPDATE ON delivery_jobs
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
