CREATE TABLE ocv.accounting_submission_attempt (
    id UUID NOT NULL,
    close_id UUID NOT NULL,
    vr008_result_id UUID NOT NULL,
    consolidation_id UUID,
    outcome VARCHAR(15) NOT NULL,
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempted_by_user_id VARCHAR(64) NOT NULL,
    attempted_by_username VARCHAR(100) NOT NULL,
    summary TEXT,

    CONSTRAINT pk_accounting_submission_attempt
        PRIMARY KEY (id),

    CONSTRAINT fk_submission_attempt_close
        FOREIGN KEY (close_id)
        REFERENCES ocv.operational_close (id),

    CONSTRAINT fk_submission_attempt_vr008_result
        FOREIGN KEY (vr008_result_id)
        REFERENCES ocv.validation_result (id),

    CONSTRAINT fk_submission_attempt_consolidation
        FOREIGN KEY (consolidation_id)
        REFERENCES ocv.consolidation (id),

    CONSTRAINT ck_submission_attempt_outcome
        CHECK (
            outcome IN (
                'SUCCEEDED',
                'REJECTED'
            )
        ),

    CONSTRAINT ck_submission_attempt_consolidation
        CHECK (
            (
                outcome = 'SUCCEEDED'
                AND consolidation_id IS NOT NULL
            )
            OR
            outcome = 'REJECTED'
        ),

    CONSTRAINT ck_submission_attempt_attempted_by_user
        CHECK (
            attempted_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_submission_attempt_attempted_by_username
        CHECK (
            btrim(attempted_by_username) <> ''
        )
);

CREATE UNIQUE INDEX uq_submission_success_close
    ON ocv.accounting_submission_attempt (
        close_id
    )
    WHERE outcome = 'SUCCEEDED';

CREATE UNIQUE INDEX uq_submission_vr008_result
    ON ocv.accounting_submission_attempt (
        vr008_result_id
    );

CREATE INDEX idx_submission_close_attempted_at
    ON ocv.accounting_submission_attempt (
        close_id,
        attempted_at
    );

CREATE INDEX idx_submission_outcome_attempted_at
    ON ocv.accounting_submission_attempt (
        outcome,
        attempted_at
    );

CREATE TABLE ocv.submission_attempt_issue (
    id UUID NOT NULL,
    submission_attempt_id UUID NOT NULL,
    issue_type VARCHAR(40) NOT NULL,
    event_id UUID,
    alert_id UUID,
    validation_result_id UUID,
    consolidation_id UUID,
    detail TEXT NOT NULL,

    CONSTRAINT pk_submission_attempt_issue
        PRIMARY KEY (id),

    CONSTRAINT fk_submission_issue_attempt
        FOREIGN KEY (submission_attempt_id)
        REFERENCES ocv.accounting_submission_attempt (id),

    CONSTRAINT fk_submission_issue_event
        FOREIGN KEY (event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT fk_submission_issue_alert
        FOREIGN KEY (alert_id)
        REFERENCES ocv.alert (id),

    CONSTRAINT fk_submission_issue_validation_result
        FOREIGN KEY (validation_result_id)
        REFERENCES ocv.validation_result (id),

    CONSTRAINT fk_submission_issue_consolidation
        FOREIGN KEY (consolidation_id)
        REFERENCES ocv.consolidation (id),

    CONSTRAINT ck_submission_issue_type
        CHECK (
            issue_type IN (
                'EVENT_NOT_VALIDATED',
                'BLOCKING_ALERT',
                'VALIDATION_RESULT_FAILED',
                'VALIDATION_RESULT_STALE',
                'CONSOLIDATION_MISSING',
                'CONSOLIDATION_STALE',
                'OTHER_CRITICAL_INCONSISTENCY'
            )
        ),

    CONSTRAINT ck_submission_issue_detail
        CHECK (
            btrim(detail) <> ''
        )
);

CREATE INDEX idx_submission_issue_attempt
    ON ocv.submission_attempt_issue (
        submission_attempt_id
    );

CREATE INDEX idx_submission_issue_event
    ON ocv.submission_attempt_issue (
        event_id
    )
    WHERE event_id IS NOT NULL;

CREATE INDEX idx_submission_issue_alert
    ON ocv.submission_attempt_issue (
        alert_id
    )
    WHERE alert_id IS NOT NULL;

CREATE INDEX idx_submission_issue_validation_result
    ON ocv.submission_attempt_issue (
        validation_result_id
    )
    WHERE validation_result_id IS NOT NULL;

CREATE INDEX idx_submission_issue_consolidation
    ON ocv.submission_attempt_issue (
        consolidation_id
    )
    WHERE consolidation_id IS NOT NULL;

ALTER TABLE ocv.close_state_transition
    ADD CONSTRAINT fk_close_state_transition_submission_attempt
        FOREIGN KEY (submission_attempt_id)
        REFERENCES ocv.accounting_submission_attempt (id);

CREATE FUNCTION ocv.reject_accounting_submission_attempt_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'accounting submission attempt is append-only'
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER trg_accounting_submission_attempt_append_only
BEFORE UPDATE OR DELETE
ON ocv.accounting_submission_attempt
FOR EACH ROW
EXECUTE FUNCTION
    ocv.reject_accounting_submission_attempt_mutation();

CREATE FUNCTION ocv.reject_submission_attempt_issue_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'submission attempt issue is append-only'
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER trg_submission_attempt_issue_append_only
BEFORE UPDATE OR DELETE
ON ocv.submission_attempt_issue
FOR EACH ROW
EXECUTE FUNCTION
    ocv.reject_submission_attempt_issue_mutation();