-- Support Google Sign-In accounts that have no manual password.
-- profile_picture_url is populated from Google's verified ID token on first sign-in.

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN profile_picture_url VARCHAR(512) NULL;
