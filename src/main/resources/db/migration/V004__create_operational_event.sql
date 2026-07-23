CREATE TABLE ocv.operational_event (
    id UUID PRIMARY KEY,
    close_id UUID NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    balance_effect NUMERIC(19,4) NOT NULL,
    reversed_event_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responsible_name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    state VARCHAR(30) NOT NULL,
    evidence_required BOOLEAN NOT NULL,
    authorization_required BOOLEAN NOT NULL,
    data_revision BIGINT NOT NULL,
    state_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_by_username VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id VARCHAR(64) NOT NULL,
    updated_by_username VARCHAR(100) NOT NULL,

    CONSTRAINT fk_operational_event_close
        FOREIGN KEY (close_id)
        REFERENCES ocv.operational_close (id),

    CONSTRAINT fk_operational_event_reversed_event
        FOREIGN KEY (reversed_event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT ck_operational_event_type
        CHECK (
            event_type IN (
                'INCOME',
                'EXPENSE',
                'DISCOUNT',
                'CANCELLATION'
            )
        ),

    CONSTRAINT ck_operational_event_amount
        CHECK (amount > 0),

    CONSTRAINT ck_operational_event_balance_effect
        CHECK (abs(balance_effect) = amount),

    CONSTRAINT ck_operational_event_reversed_reference
        CHECK (
            (
                event_type = 'CANCELLATION'
                AND reversed_event_id IS NOT NULL
            )
            OR
            (
                event_type <> 'CANCELLATION'
                AND reversed_event_id IS NULL
            )
        ),

    CONSTRAINT ck_operational_event_not_self_reversed
        CHECK (
            reversed_event_id IS NULL
            OR reversed_event_id <> id
        ),

    CONSTRAINT ck_operational_event_responsible_name
        CHECK (
            btrim(responsible_name) <> ''
        ),

    CONSTRAINT ck_operational_event_description
        CHECK (
            btrim(description) <> ''
        ),

    CONSTRAINT ck_operational_event_state
        CHECK (
            state IN (
                'REGISTERED',
                'PENDING_SUPPORT',
                'PENDING_AUTHORIZATION',
                'OBSERVED',
                'VALIDATED'
            )
        ),

    CONSTRAINT ck_operational_event_data_revision
        CHECK (data_revision >= 1),

    CONSTRAINT ck_operational_event_created_by_user
        CHECK (
            created_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_operational_event_created_by_username
        CHECK (
            btrim(created_by_username) <> ''
        ),

    CONSTRAINT ck_operational_event_updated_by_user
        CHECK (
            updated_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_operational_event_updated_by_username
        CHECK (
            btrim(updated_by_username) <> ''
        )
);

CREATE INDEX idx_operational_event_close_state
    ON ocv.operational_event (
        close_id,
        state
    );

CREATE INDEX idx_operational_event_close_type
    ON ocv.operational_event (
        close_id,
        event_type
    );

CREATE INDEX idx_operational_event_close_occurred_at
    ON ocv.operational_event (
        close_id,
        occurred_at
    );

CREATE UNIQUE INDEX uq_operational_event_reversed_event
    ON ocv.operational_event (
        reversed_event_id
    )
    WHERE event_type = 'CANCELLATION';

CREATE TABLE ocv.event_state_transition (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    from_state VARCHAR(30),
    to_state VARCHAR(30) NOT NULL,
    cause_code VARCHAR(40) NOT NULL,
    detail TEXT,
    validation_result_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,

    CONSTRAINT fk_event_state_transition_event
        FOREIGN KEY (event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT ck_event_transition_from_state
        CHECK (
            from_state IS NULL
            OR from_state IN (
                'REGISTERED',
                'PENDING_SUPPORT',
                'PENDING_AUTHORIZATION',
                'OBSERVED',
                'VALIDATED'
            )
        ),

    CONSTRAINT ck_event_transition_to_state
        CHECK (
            to_state IN (
                'REGISTERED',
                'PENDING_SUPPORT',
                'PENDING_AUTHORIZATION',
                'OBSERVED',
                'VALIDATED'
            )
        ),

    CONSTRAINT ck_event_transition_state_change
        CHECK (
            from_state IS NULL
            OR from_state <> to_state
        ),

    CONSTRAINT ck_event_transition_cause_code
        CHECK (
            btrim(cause_code) <> ''
        ),

    CONSTRAINT ck_event_transition_actor_user
        CHECK (
            actor_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_event_transition_actor_username
        CHECK (
            btrim(actor_username) <> ''
        )
);

CREATE INDEX idx_event_transition_event_occurred_at
    ON ocv.event_state_transition (
        event_id,
        occurred_at
    );

CREATE INDEX idx_event_transition_to_state
    ON ocv.event_state_transition (
        to_state
    );