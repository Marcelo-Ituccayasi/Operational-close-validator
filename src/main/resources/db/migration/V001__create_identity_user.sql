CREATE SCHEMA IF NOT EXISTS ocv;

CREATE TABLE ocv.identity_user (
    user_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    username_normalized VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN NOT NULL,
    credential_version BIGINT NOT NULL,
    provisioned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT ck_identity_user_id
        CHECK (user_id = 'responsible-user'),

    CONSTRAINT ck_identity_user_username_not_blank
        CHECK (btrim(username) <> ''),

    CONSTRAINT ck_identity_user_username_normalized
        CHECK (
            username_normalized <> ''
            AND username_normalized = lower(btrim(username_normalized))
        ),

    CONSTRAINT ck_identity_user_password_hash_not_blank
        CHECK (btrim(password_hash) <> ''),

    CONSTRAINT ck_identity_user_credential_version
        CHECK (credential_version >= 1)
);

CREATE UNIQUE INDEX uq_identity_user_username_normalized
    ON ocv.identity_user (username_normalized);
