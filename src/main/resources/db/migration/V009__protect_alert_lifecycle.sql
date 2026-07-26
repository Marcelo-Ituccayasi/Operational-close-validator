CREATE FUNCTION ocv.enforce_alert_lifecycle()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'alerts cannot be physically deleted'
            USING ERRCODE = '23514';
    END IF;

    IF OLD.state IN (
        'RESOLVED',
        'DISCARDED'
    ) THEN
        RAISE EXCEPTION
            'terminal alerts cannot be modified'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_alert_protect_lifecycle
BEFORE UPDATE OR DELETE
ON ocv.alert
FOR EACH ROW
EXECUTE FUNCTION ocv.enforce_alert_lifecycle();

CREATE FUNCTION ocv.enforce_alert_transition_append_only()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'alert transitions are append-only'
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER trg_alert_transition_append_only
BEFORE UPDATE OR DELETE
ON ocv.alert_transition
FOR EACH ROW
EXECUTE FUNCTION ocv.enforce_alert_transition_append_only();