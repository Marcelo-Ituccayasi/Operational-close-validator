CREATE TABLE ocv.operational_close (
    id UUID PRIMARY KEY,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    currency_code CHAR(3) NOT NULL,
    initial_balance NUMERIC(19,4) NOT NULL,
    state VARCHAR(30) NOT NULL,
    state_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_by_username VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by_user_id VARCHAR(64) NOT NULL,
    updated_by_username VARCHAR(100) NOT NULL,

    CONSTRAINT ck_operational_close_period
        CHECK (period_end >= period_start),

    CONSTRAINT ck_operational_close_currency_code
        CHECK (currency_code ~ '^[A-Z]{3}$'),

    CONSTRAINT ck_operational_close_initial_balance
        CHECK (initial_balance >= 0),

    CONSTRAINT ck_operational_close_state
        CHECK (
            state IN (
                'PREPARATION',
                'BLOCKED',
                'VALIDATED',
                'SENT_TO_ACCOUNTING'
            )
        ),

    CONSTRAINT ck_operational_close_created_by_user
        CHECK (
            created_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_operational_close_created_by_username
        CHECK (
            btrim(created_by_username) <> ''
        ),

    CONSTRAINT ck_operational_close_updated_by_user
        CHECK (
            updated_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_operational_close_updated_by_username
        CHECK (
            btrim(updated_by_username) <> ''
        )
);

CREATE UNIQUE INDEX uq_operational_close_period
    ON ocv.operational_close (
        period_start,
        period_end
    );

CREATE INDEX idx_operational_close_state
    ON ocv.operational_close (state);

CREATE INDEX idx_operational_close_period_end
    ON ocv.operational_close (period_end);

CREATE TABLE ocv.close_state_transition (
    id UUID PRIMARY KEY,
    close_id UUID NOT NULL,
    from_state VARCHAR(30),
    to_state VARCHAR(30) NOT NULL,
    cause_code VARCHAR(40) NOT NULL,
    detail TEXT,
    validation_result_id UUID,
    consolidation_id UUID,
    submission_attempt_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,

    CONSTRAINT fk_close_state_transition_close
        FOREIGN KEY (close_id)
        REFERENCES ocv.operational_close (id),

    CONSTRAINT ck_close_state_transition_from_state
        CHECK (
            from_state IS NULL
            OR from_state IN (
                'PREPARATION',
                'BLOCKED',
                'VALIDATED',
                'SENT_TO_ACCOUNTING'
            )
        ),

    CONSTRAINT ck_close_state_transition_to_state
        CHECK (
            to_state IN (
                'PREPARATION',
                'BLOCKED',
                'VALIDATED',
                'SENT_TO_ACCOUNTING'
            )
        ),

    CONSTRAINT ck_close_state_transition_state_change
        CHECK (
            from_state IS NULL
            OR from_state <> to_state
        ),

    CONSTRAINT ck_close_state_transition_cause_code
        CHECK (
            btrim(cause_code) <> ''
        ),

    CONSTRAINT ck_close_state_transition_actor_user
        CHECK (
            actor_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_close_state_transition_actor_username
        CHECK (
            btrim(actor_username) <> ''
        )
);

CREATE INDEX idx_close_transition_close_occurred_at
    ON ocv.close_state_transition (
        close_id,
        occurred_at
    );

CREATE INDEX idx_close_transition_to_state
    ON ocv.close_state_transition (to_state);
