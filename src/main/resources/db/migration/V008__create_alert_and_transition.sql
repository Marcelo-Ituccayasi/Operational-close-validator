CREATE TABLE ocv.alert (
    id UUID PRIMARY KEY,
    event_id UUID,
    close_id UUID,
    source_validation_result_id UUID,
    cause_code VARCHAR(40) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    is_blocking BOOLEAN NOT NULL,
    state VARCHAR(20) NOT NULL,
    detail TEXT NOT NULL,
    resolved_by_validation_result_id UUID,
    discard_justification TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_by_username VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_alert_event
        FOREIGN KEY (event_id)
        REFERENCES ocv.operational_event (id),

    CONSTRAINT fk_alert_close
        FOREIGN KEY (close_id)
        REFERENCES ocv.operational_close (id),

    CONSTRAINT fk_alert_source_validation_result
        FOREIGN KEY (source_validation_result_id)
        REFERENCES ocv.validation_result (id),

    CONSTRAINT fk_alert_resolution_validation_result
        FOREIGN KEY (resolved_by_validation_result_id)
        REFERENCES ocv.validation_result (id),

    CONSTRAINT ck_alert_target
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

    CONSTRAINT ck_alert_cause_code
        CHECK (
            btrim(cause_code) <> ''
        ),

    CONSTRAINT ck_alert_severity
        CHECK (
            severity IN (
                'CRITICAL',
                'HIGH'
            )
        ),

    CONSTRAINT ck_alert_state
        CHECK (
            state IN (
                'ACTIVE',
                'ACKNOWLEDGED',
                'UNDER_REVIEW',
                'RESOLVED',
                'DISCARDED'
            )
        ),

    CONSTRAINT ck_alert_detail
        CHECK (
            btrim(detail) <> ''
        ),

    CONSTRAINT ck_alert_created_by_user
        CHECK (
            created_by_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_alert_created_by_username
        CHECK (
            btrim(created_by_username) <> ''
        ),

    CONSTRAINT ck_alert_update_instant
        CHECK (
            updated_at >= created_at
        ),

    CONSTRAINT ck_alert_close_instant
        CHECK (
            closed_at IS NULL
            OR
            (
                closed_at >= created_at
                AND closed_at <= updated_at
            )
        ),

    CONSTRAINT ck_alert_terminal_metadata
        CHECK (
            (
                state = 'RESOLVED'
                AND resolved_by_validation_result_id IS NOT NULL
                AND discard_justification IS NULL
                AND closed_at IS NOT NULL
            )
            OR
            (
                state = 'DISCARDED'
                AND resolved_by_validation_result_id IS NULL
                AND discard_justification IS NOT NULL
                AND btrim(discard_justification) <> ''
                AND closed_at IS NOT NULL
            )
            OR
            (
                state IN (
                    'ACTIVE',
                    'ACKNOWLEDGED',
                    'UNDER_REVIEW'
                )
                AND resolved_by_validation_result_id IS NULL
                AND discard_justification IS NULL
                AND closed_at IS NULL
            )
        )
);

CREATE INDEX idx_alert_event_state
    ON ocv.alert (
        event_id,
        state
    );

CREATE INDEX idx_alert_close_state
    ON ocv.alert (
        close_id,
        state
    );

CREATE INDEX idx_alert_blocking_open_event
    ON ocv.alert (
        event_id
    )
    WHERE
        is_blocking = TRUE
        AND event_id IS NOT NULL
        AND state NOT IN (
            'RESOLVED',
            'DISCARDED'
        );

CREATE INDEX idx_alert_blocking_open_close
    ON ocv.alert (
        close_id
    )
    WHERE
        is_blocking = TRUE
        AND close_id IS NOT NULL
        AND state NOT IN (
            'RESOLVED',
            'DISCARDED'
        );

CREATE INDEX idx_alert_source_validation_result
    ON ocv.alert (
        source_validation_result_id
    );

CREATE TABLE ocv.alert_transition (
    id UUID PRIMARY KEY,
    alert_id UUID NOT NULL,
    from_state VARCHAR(20),
    to_state VARCHAR(20) NOT NULL,
    action_code VARCHAR(40) NOT NULL,
    detail TEXT,
    justification TEXT,
    validation_result_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,

    CONSTRAINT fk_alert_transition_alert
        FOREIGN KEY (alert_id)
        REFERENCES ocv.alert (id),

    CONSTRAINT fk_alert_transition_validation_result
        FOREIGN KEY (validation_result_id)
        REFERENCES ocv.validation_result (id),

    CONSTRAINT ck_alert_transition_from_state
        CHECK (
            from_state IS NULL
            OR from_state IN (
                'ACTIVE',
                'ACKNOWLEDGED',
                'UNDER_REVIEW',
                'RESOLVED',
                'DISCARDED'
            )
        ),

    CONSTRAINT ck_alert_transition_to_state
        CHECK (
            to_state IN (
                'ACTIVE',
                'ACKNOWLEDGED',
                'UNDER_REVIEW',
                'RESOLVED',
                'DISCARDED'
            )
        ),

    CONSTRAINT ck_alert_transition_state_change
        CHECK (
            from_state IS NULL
            OR from_state <> to_state
        ),

    CONSTRAINT ck_alert_transition_action_code
        CHECK (
            btrim(action_code) <> ''
        ),

    CONSTRAINT ck_alert_transition_terminal_metadata
        CHECK (
            (
                to_state = 'RESOLVED'
                AND validation_result_id IS NOT NULL
                AND justification IS NULL
            )
            OR
            (
                to_state = 'DISCARDED'
                AND validation_result_id IS NULL
                AND justification IS NOT NULL
                AND btrim(justification) <> ''
            )
            OR
            (
                to_state IN (
                    'ACTIVE',
                    'ACKNOWLEDGED',
                    'UNDER_REVIEW'
                )
                AND validation_result_id IS NULL
                AND justification IS NULL
            )
        ),

    CONSTRAINT ck_alert_transition_actor_user
        CHECK (
            actor_user_id = 'responsible-user'
        ),

    CONSTRAINT ck_alert_transition_actor_username
        CHECK (
            btrim(actor_username) <> ''
        )
);

CREATE INDEX idx_alert_transition_alert_occurred_at
    ON ocv.alert_transition (
        alert_id,
        occurred_at
    );

CREATE INDEX idx_alert_transition_to_state
    ON ocv.alert_transition (
        to_state
    );