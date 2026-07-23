CREATE TABLE ocv.supporting_evidence (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    evidence_type VARCHAR(40) NOT NULL,
    content_reference VARCHAR(500) NOT NULL,
    supported_amount NUMERIC(19,4),
    evidence_date DATE NOT NULL,
    legibility_status VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    revision BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_by_username VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id VARCHAR(64) NOT NULL,
    updated_by_username VARCHAR(100) NOT NULL,
    deactivated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_supporting_evidence_event
        FOREIGN KEY (event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT ck_supporting_evidence_evidence_type
        CHECK (
            btrim(evidence_type) <> ''
        ),

    CONSTRAINT ck_supporting_evidence_content_reference
        CHECK (
            btrim(content_reference) <> ''
        ),

    CONSTRAINT ck_supporting_evidence_supported_amount
        CHECK (
            supported_amount IS NULL
            OR supported_amount > 0
        ),

    CONSTRAINT ck_supporting_evidence_legibility_status
        CHECK (
            legibility_status IN (
                'UNVERIFIED',
                'LEGIBLE',
                'ILLEGIBLE'
            )
        ),

    CONSTRAINT ck_supporting_evidence_revision
        CHECK (
            revision >= 1
        ),

    CONSTRAINT ck_supporting_evidence_activity
        CHECK (
            (
                is_active = TRUE
                AND deactivated_at IS NULL
            )
            OR
            (
                is_active = FALSE
                AND deactivated_at IS NOT NULL
            )
        ),

    CONSTRAINT ck_supporting_evidence_created_by_user
        CHECK (
            created_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_supporting_evidence_created_by_username
        CHECK (
            btrim(created_by_username) <> ''
        ),

    CONSTRAINT ck_supporting_evidence_updated_by_user
        CHECK (
            updated_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_supporting_evidence_updated_by_username
        CHECK (
            btrim(updated_by_username) <> ''
        )
);

CREATE INDEX idx_supporting_evidence_event_active
    ON ocv.supporting_evidence (
        event_id,
        is_active
    );

CREATE INDEX idx_supporting_evidence_event_date
    ON ocv.supporting_evidence (
        event_id,
        evidence_date
    );

CREATE TABLE ocv.event_authorization (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    authorized_by_name VARCHAR(200) NOT NULL,
    reason TEXT NOT NULL,
    authorized_at TIMESTAMP WITH TIME ZONE NOT NULL,
    formal_reference VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL,
    revision BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_by_username VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id VARCHAR(64) NOT NULL,
    updated_by_username VARCHAR(100) NOT NULL,
    deactivated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_event_authorization_event
        FOREIGN KEY (event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT ck_event_authorization_authorized_by_name
        CHECK (
            btrim(authorized_by_name) <> ''
        ),

    CONSTRAINT ck_event_authorization_reason
        CHECK (
            btrim(reason) <> ''
        ),

    CONSTRAINT ck_event_authorization_formal_reference
        CHECK (
            btrim(formal_reference) <> ''
        ),

    CONSTRAINT ck_event_authorization_revision
        CHECK (
            revision >= 1
        ),

    CONSTRAINT ck_event_authorization_activity
        CHECK (
            (
                is_active = TRUE
                AND deactivated_at IS NULL
            )
            OR
            (
                is_active = FALSE
                AND deactivated_at IS NOT NULL
            )
        ),

    CONSTRAINT ck_event_authorization_created_by_user
        CHECK (
            created_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_event_authorization_created_by_username
        CHECK (
            btrim(created_by_username) <> ''
        ),

    CONSTRAINT ck_event_authorization_updated_by_user
        CHECK (
            updated_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_event_authorization_updated_by_username
        CHECK (
            btrim(updated_by_username) <> ''
        )
);

CREATE INDEX idx_event_authorization_event_active
    ON ocv.event_authorization (
        event_id,
        is_active
    );

CREATE INDEX idx_event_authorization_authorized_at
    ON ocv.event_authorization (
        authorized_at
    );