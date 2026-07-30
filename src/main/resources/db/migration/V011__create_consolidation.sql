CREATE TABLE ocv.consolidation (
    id UUID NOT NULL,
    close_id UUID NOT NULL,
    currency_code CHAR(3) NOT NULL,
    event_count INTEGER NOT NULL,
    total_income NUMERIC(19,4) NOT NULL,
    total_expense NUMERIC(19,4) NOT NULL,
    total_discount NUMERIC(19,4) NOT NULL,
    total_cancellation NUMERIC(19,4) NOT NULL,
    initial_balance NUMERIC(19,4) NOT NULL,
    expected_balance NUMERIC(19,4) NOT NULL,
    actual_balance NUMERIC(19,4) NOT NULL,
    difference NUMERIC(19,4) NOT NULL,
    is_current BOOLEAN NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_by_user_id VARCHAR(64) NOT NULL,
    completed_by_username VARCHAR(100) NOT NULL,
    invalidated_at TIMESTAMP WITH TIME ZONE,
    invalidation_reason TEXT,

    CONSTRAINT pk_consolidation
        PRIMARY KEY (id),

    CONSTRAINT fk_consolidation_close
        FOREIGN KEY (close_id)
        REFERENCES ocv.operational_close (id),

    CONSTRAINT ck_consolidation_currency_code
        CHECK (
            currency_code ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT ck_consolidation_event_count
        CHECK (
            event_count >= 1
        ),

    CONSTRAINT ck_consolidation_totals
        CHECK (
            total_income >= 0
            AND total_expense >= 0
            AND total_discount >= 0
            AND total_cancellation >= 0
        ),

    CONSTRAINT ck_consolidation_initial_balance
        CHECK (
            initial_balance >= 0
        ),

    CONSTRAINT ck_consolidation_actual_balance
        CHECK (
            actual_balance >= 0
        ),

    CONSTRAINT ck_consolidation_difference
        CHECK (
            difference = actual_balance - expected_balance
        ),

    CONSTRAINT ck_consolidation_completed_by_user
        CHECK (
            completed_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_consolidation_completed_by_username
        CHECK (
            btrim(completed_by_username) <> ''
        ),

    CONSTRAINT ck_consolidation_validity
        CHECK (
            (
                is_current = TRUE
                AND invalidated_at IS NULL
                AND invalidation_reason IS NULL
            )
            OR
            (
                is_current = FALSE
                AND invalidated_at IS NOT NULL
                AND invalidation_reason IS NOT NULL
                AND btrim(invalidation_reason) <> ''
                AND invalidated_at >= completed_at
            )
        )
);

CREATE UNIQUE INDEX uq_consolidation_current_close
    ON ocv.consolidation (
        close_id
    )
    WHERE is_current = TRUE;

CREATE INDEX idx_consolidation_close_completed_at
    ON ocv.consolidation (
        close_id,
        completed_at
    );

CREATE TABLE ocv.consolidation_event_snapshot (
    consolidation_id UUID NOT NULL,
    event_id UUID NOT NULL,
    event_data_revision BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    balance_effect NUMERIC(19,4) NOT NULL,
    reversed_event_id UUID,
    event_state VARCHAR(30) NOT NULL,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_consolidation_event_snapshot
        PRIMARY KEY (
            consolidation_id,
            event_id
        ),

    CONSTRAINT fk_consolidation_snapshot_consolidation
        FOREIGN KEY (consolidation_id)
        REFERENCES ocv.consolidation (id),

    CONSTRAINT fk_consolidation_snapshot_event
        FOREIGN KEY (event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT fk_consolidation_snapshot_reversed_event
        FOREIGN KEY (reversed_event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT ck_consolidation_snapshot_revision
        CHECK (
            event_data_revision >= 1
        ),

    CONSTRAINT ck_consolidation_snapshot_type
        CHECK (
            event_type IN (
                'INCOME',
                'EXPENSE',
                'DISCOUNT',
                'CANCELLATION'
            )
        ),

    CONSTRAINT ck_consolidation_snapshot_amount
        CHECK (
            amount > 0
        ),

    CONSTRAINT ck_consolidation_snapshot_balance_effect
        CHECK (
            abs(balance_effect) = amount
        ),

    CONSTRAINT ck_consolidation_snapshot_reversed_reference
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

    CONSTRAINT ck_consolidation_snapshot_state
        CHECK (
            event_state = 'VALIDATED'
        )
);

CREATE INDEX idx_consolidation_snapshot_event
    ON ocv.consolidation_event_snapshot (
        event_id
    );

CREATE INDEX idx_consolidation_snapshot_reversed_event
    ON ocv.consolidation_event_snapshot (
        reversed_event_id
    )
    WHERE reversed_event_id IS NOT NULL;

ALTER TABLE ocv.validation_result
    ADD CONSTRAINT fk_validation_result_consolidation
        FOREIGN KEY (consolidation_id)
        REFERENCES ocv.consolidation (id);

ALTER TABLE ocv.close_state_transition
    ADD CONSTRAINT fk_close_state_transition_validation_result
        FOREIGN KEY (validation_result_id)
        REFERENCES ocv.validation_result (id);

ALTER TABLE ocv.close_state_transition
    ADD CONSTRAINT fk_close_state_transition_consolidation
        FOREIGN KEY (consolidation_id)
        REFERENCES ocv.consolidation (id);

CREATE FUNCTION ocv.enforce_consolidation_immutability()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
       OR NEW.close_id IS DISTINCT FROM OLD.close_id
       OR NEW.currency_code IS DISTINCT FROM OLD.currency_code
       OR NEW.event_count IS DISTINCT FROM OLD.event_count
       OR NEW.total_income IS DISTINCT FROM OLD.total_income
       OR NEW.total_expense IS DISTINCT FROM OLD.total_expense
       OR NEW.total_discount IS DISTINCT FROM OLD.total_discount
       OR NEW.total_cancellation
            IS DISTINCT FROM OLD.total_cancellation
       OR NEW.initial_balance IS DISTINCT FROM OLD.initial_balance
       OR NEW.expected_balance IS DISTINCT FROM OLD.expected_balance
       OR NEW.actual_balance IS DISTINCT FROM OLD.actual_balance
       OR NEW.difference IS DISTINCT FROM OLD.difference
       OR NEW.completed_at IS DISTINCT FROM OLD.completed_at
       OR NEW.completed_by_user_id
            IS DISTINCT FROM OLD.completed_by_user_id
       OR NEW.completed_by_username
            IS DISTINCT FROM OLD.completed_by_username THEN

        RAISE EXCEPTION
            'consolidation calculation content is immutable'
            USING ERRCODE = '23514';
    END IF;

    IF OLD.is_current = FALSE
       AND NEW IS DISTINCT FROM OLD THEN

        RAISE EXCEPTION
            'invalidated consolidation cannot be modified'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_consolidation_immutability
BEFORE UPDATE
ON ocv.consolidation
FOR EACH ROW
EXECUTE FUNCTION ocv.enforce_consolidation_immutability();

CREATE FUNCTION ocv.reject_consolidation_snapshot_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'consolidation event snapshot is append-only'
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER trg_consolidation_snapshot_append_only
BEFORE UPDATE OR DELETE
ON ocv.consolidation_event_snapshot
FOR EACH ROW
EXECUTE FUNCTION ocv.reject_consolidation_snapshot_mutation();