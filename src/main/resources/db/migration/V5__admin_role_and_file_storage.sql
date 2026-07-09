-- Add ADMIN role (scoped in original Auth/RBAC session for verification review).
-- Verification documents + agency portfolio metadata for Cloudinary-backed storage.

ALTER TABLE users DROP CONSTRAINT chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (
    role IN ('CUSTOMER', 'CONSTRUCTION_AGENCY', 'DELIVERY_PROVIDER', 'ADMIN')
);

CREATE TABLE verification_documents (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL,
    document_type        VARCHAR(50)  NOT NULL,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    resource_type        VARCHAR(20)  NOT NULL,
    format               VARCHAR(20)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_verification_documents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_verification_documents_user_type UNIQUE (user_id, document_type),
    CONSTRAINT chk_verification_documents_type CHECK (document_type IN (
        'BUSINESS_REGISTRATION', 'GOVERNMENT_ID', 'PROFESSIONAL_LICENSE'
    )),
    CONSTRAINT chk_verification_documents_resource_type CHECK (resource_type IN ('image', 'raw'))
);

CREATE INDEX idx_verification_documents_user_id ON verification_documents (user_id);

CREATE TABLE agency_portfolio_images (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    resource_type        VARCHAR(20)  NOT NULL DEFAULT 'image',
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_agency_portfolio_images_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_agency_portfolio_images_resource_type CHECK (resource_type IN ('image'))
);

CREATE INDEX idx_agency_portfolio_images_user_id ON agency_portfolio_images (user_id);
