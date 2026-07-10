-- Onboarding state, construction agencies, and delivery provider profiles.

CREATE TABLE user_onboarding (
    user_id              UUID         PRIMARY KEY,
    account_type         VARCHAR(30)  NULL,
    onboarding_complete  BOOLEAN      NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_onboarding_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_onboarding_account_type CHECK (
        account_type IS NULL OR account_type IN ('customer', 'construction', 'delivery')
    )
);

CREATE TRIGGER trg_user_onboarding_set_updated_at
    BEFORE UPDATE ON user_onboarding
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE agencies (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id   UUID         NOT NULL,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(50)  NOT NULL,
    logo_url        VARCHAR(512) NULL,
    tagline         VARCHAR(300) NULL,
    description     TEXT         NULL,
    address         VARCHAR(500) NULL,
    phone           VARCHAR(30)  NULL,
    hours           VARCHAR(200) NULL,
    services        TEXT         NULL,
    verified        BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_agencies_owner FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_agencies_owner_user_id UNIQUE (owner_user_id)
);

CREATE INDEX idx_agencies_category ON agencies (category);
CREATE INDEX idx_agencies_verified ON agencies (verified);

CREATE TRIGGER trg_agencies_set_updated_at
    BEFORE UPDATE ON agencies
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TABLE delivery_providers (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID         NOT NULL,
    construction_agency_id  UUID         NULL,
    full_name               VARCHAR(150) NOT NULL,
    vehicle_info            VARCHAR(300) NULL,
    profile_image_url       VARCHAR(512) NULL,
    approval_status         VARCHAR(20)  NOT NULL DEFAULT 'pending',
    submitted_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    handled_at              TIMESTAMPTZ  NULL,
    CONSTRAINT fk_delivery_providers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_providers_agency FOREIGN KEY (construction_agency_id) REFERENCES agencies (id) ON DELETE SET NULL,
    CONSTRAINT uq_delivery_providers_user_id UNIQUE (user_id),
    CONSTRAINT chk_delivery_providers_approval_status CHECK (
        approval_status IN ('pending', 'approved', 'rejected')
    )
);

CREATE INDEX idx_delivery_providers_agency_id ON delivery_providers (construction_agency_id);
CREATE INDEX idx_delivery_providers_approval_status ON delivery_providers (approval_status);
