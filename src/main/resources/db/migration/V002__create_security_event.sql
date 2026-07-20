CREATE TABLE ocv.security_event (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64),
    username_normalized VARCHAR(100),
    event_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_address INET,
    detail VARCHAR(500),

    CONSTRAINT ck_security_event_user_id
        CHECK (
            user_id IS NULL
            OR user_id = 'responsible-user'
        ),

    CONSTRAINT ck_security_event_username_normalized
        CHECK (
            username_normalized IS NULL
            OR (
                username_normalized <> ''
                AND username_normalized =
                    lower(btrim(username_normalized))
            )
        ),

    CONSTRAINT ck_security_event_type
        CHECK (
            event_type IN (
                'USER_PROVISIONED',
                'CREDENTIAL_ROTATED',
                'LOGIN_SUCCEEDED',
                'LOGIN_FAILED',
                'LOGIN_RATE_LIMITED',
                'SESSION_REPLACED',
                'LOGOUT',
                'SESSION_EXPIRED',
                'CONFIGURATION_FAILED'
            )
        )
);

CREATE INDEX idx_security_event_occurred_at
    ON ocv.security_event (occurred_at);

CREATE INDEX idx_security_event_user_occurred_at
    ON ocv.security_event (user_id, occurred_at);

CREATE INDEX idx_security_event_type_occurred_at
    ON ocv.security_event (event_type, occurred_at);
