CREATE TABLE ocv.validation_result (
    id UUID PRIMARY KEY,
    rule_code VARCHAR(10) NOT NULL,
    rule_version INTEGER NOT NULL,
    event_id UUID,
    close_id UUID,
    outcome VARCHAR(15) NOT NULL,
    detail TEXT NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    evaluated_by_user_id VARCHAR(64) NOT NULL,
    evaluated_by_username VARCHAR(100) NOT NULL,
    event_data_revision BIGINT,
    consolidation_id UUID,
    is_current BOOLEAN NOT NULL,
    invalidated_at TIMESTAMP WITH TIME ZONE,
    invalidation_reason TEXT,

    CONSTRAINT fk_validation_result_rule
        FOREIGN KEY (
            rule_code,
            rule_version
        )
        REFERENCES ocv.validation_rule (
            rule_code,
            rule_version
        ),

    CONSTRAINT fk_validation_result_event
        FOREIGN KEY (event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT fk_validation_result_close
        FOREIGN KEY (close_id)
        REFERENCES ocv.operational_close (id),

    CONSTRAINT ck_validation_result_rule_version
        CHECK (
            rule_version >= 1
        ),

    CONSTRAINT ck_validation_result_target
        CHECK (
            (
                event_id IS NOT NULL
                AND close_id IS NULL
            )
            OR
            (
                event_id IS NULL
                AND close_id IS NOT NULL
            )
        ),

    CONSTRAINT ck_validation_result_outcome
        CHECK (
            outcome IN (
                'SATISFIED',
                'FAILED'
            )
        ),

    CONSTRAINT ck_validation_result_detail
        CHECK (
            btrim(detail) <> ''
        ),

    CONSTRAINT ck_validation_result_evaluated_by_user
        CHECK (
            evaluated_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_validation_result_evaluated_by_username
        CHECK (
            btrim(evaluated_by_username) <> ''
        ),

    CONSTRAINT ck_validation_result_event_revision
        CHECK (
            (
                event_id IS NOT NULL
                AND event_data_revision IS NOT NULL
                AND event_data_revision >= 1
            )
            OR
            (
                event_id IS NULL
                AND event_data_revision IS NULL
            )
        ),

    CONSTRAINT ck_validation_result_consolidation
        CHECK (
            (
                rule_code = 'VR-008'
                AND outcome = 'SATISFIED'
                AND consolidation_id IS NOT NULL
            )
            OR
            (
                rule_code = 'VR-008'
                AND outcome = 'FAILED'
            )
            OR
            (
                rule_code <> 'VR-008'
                AND consolidation_id IS NULL
            )
        ),

    CONSTRAINT ck_validation_result_validity
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
                AND invalidated_at >= evaluated_at
            )
        )
);

CREATE UNIQUE INDEX uq_validation_result_current_event_rule
    ON ocv.validation_result (
        event_id,
        rule_code
    )
    WHERE
        is_current = TRUE
        AND event_id IS NOT NULL;

CREATE UNIQUE INDEX uq_validation_result_current_close_rule
    ON ocv.validation_result (
        close_id,
        rule_code
    )
    WHERE
        is_current = TRUE
        AND close_id IS NOT NULL;

CREATE INDEX idx_validation_result_event_current
    ON ocv.validation_result (
        event_id,
        is_current
    );

CREATE INDEX idx_validation_result_close_current
    ON ocv.validation_result (
        close_id,
        is_current
    );

CREATE INDEX idx_validation_result_rule_outcome
    ON ocv.validation_result (
        rule_code,
        outcome
    );

CREATE INDEX idx_validation_result_evaluated_at
    ON ocv.validation_result (
        evaluated_at
    );

CREATE FUNCTION ocv.enforce_validation_result_immutability()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
       OR NEW.rule_code IS DISTINCT FROM OLD.rule_code
       OR NEW.rule_version IS DISTINCT FROM OLD.rule_version
       OR NEW.event_id IS DISTINCT FROM OLD.event_id
       OR NEW.close_id IS DISTINCT FROM OLD.close_id
       OR NEW.outcome IS DISTINCT FROM OLD.outcome
       OR NEW.detail IS DISTINCT FROM OLD.detail
       OR NEW.evaluated_at IS DISTINCT FROM OLD.evaluated_at
       OR NEW.evaluated_by_user_id
            IS DISTINCT FROM OLD.evaluated_by_user_id
       OR NEW.evaluated_by_username
            IS DISTINCT FROM OLD.evaluated_by_username
       OR NEW.event_data_revision
            IS DISTINCT FROM OLD.event_data_revision
       OR NEW.consolidation_id
            IS DISTINCT FROM OLD.consolidation_id THEN

        RAISE EXCEPTION
            'validation result evaluation content is immutable'
            USING ERRCODE = '23514';
    END IF;

    IF OLD.is_current = FALSE
       AND NEW IS DISTINCT FROM OLD THEN

        RAISE EXCEPTION
            'invalidated validation result cannot be modified'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validation_result_immutability
BEFORE UPDATE
ON ocv.validation_result
FOR EACH ROW
EXECUTE FUNCTION ocv.enforce_validation_result_immutability();