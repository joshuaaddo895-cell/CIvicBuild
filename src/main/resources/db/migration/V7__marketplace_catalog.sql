-- Marketplace catalog: categories, suppliers, products, agency posts.

CREATE TABLE categories (
    id          VARCHAR(50)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0
);

CREATE TABLE suppliers (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(200)  NOT NULL,
    logo_url      VARCHAR(512)  NULL,
    rating        DECIMAL(2, 1) NOT NULL DEFAULT 0,
    review_count  INT           NOT NULL DEFAULT 0,
    distance_km   DECIMAL(5, 1) NULL,
    verified      BOOLEAN       NOT NULL DEFAULT false,
    category_id   VARCHAR(50)   NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_suppliers_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_suppliers_category_id ON suppliers (category_id);
CREATE INDEX idx_suppliers_verified ON suppliers (verified);

CREATE TRIGGER trg_suppliers_set_updated_at
    BEFORE UPDATE ON suppliers
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE products (
    id                 UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255)   NOT NULL,
    category           VARCHAR(50)    NOT NULL,
    price              DECIMAL(10, 2) NOT NULL,
    unit               VARCHAR(50)    NOT NULL,
    image_url          VARCHAR(512)   NULL,
    description        TEXT           NULL,
    supplier_id        UUID           NULL,
    agency_id          UUID           NULL,
    stock_quantity     INT            NOT NULL DEFAULT 0,
    brand              VARCHAR(100)   NULL,
    spec               VARCHAR(255)   NULL,
    delivery_estimate  VARCHAR(100)   NULL,
    active             BOOLEAN        NOT NULL DEFAULT true,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE SET NULL,
    CONSTRAINT fk_products_agency FOREIGN KEY (agency_id) REFERENCES agencies (id) ON DELETE CASCADE,
    CONSTRAINT chk_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_products_stock_non_negative CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_supplier_id ON products (supplier_id);
CREATE INDEX idx_products_agency_id ON products (agency_id);
CREATE INDEX idx_products_active ON products (active);

CREATE TRIGGER trg_products_set_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE agency_posts (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id    UUID         NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT         NULL,
    image_url    VARCHAR(512) NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_agency_posts_agency FOREIGN KEY (agency_id) REFERENCES agencies (id) ON DELETE CASCADE,
    CONSTRAINT chk_agency_posts_type CHECK (type IN ('service', 'material', 'general'))
);

CREATE INDEX idx_agency_posts_agency_id ON agency_posts (agency_id);

CREATE TRIGGER trg_agency_posts_set_updated_at
    BEFORE UPDATE ON agency_posts
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
