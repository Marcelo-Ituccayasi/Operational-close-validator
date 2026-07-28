CREATE FUNCTION ocv.enforce_alert_resolution_result()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    resolution_outcome VARCHAR(15);
    resolution_is_current BOOLEAN;
    resolution_event_id UUID;
    resolution_close_id UUID;
BEGIN
    IF NEW.state <> 'RESOLVED' THEN
        RETURN NEW;
    END IF;

    SELECT
        validation_result.outcome,
        validation_result.is_current,
        validation_result.event_id,
        validation_result.close_id
    INTO
        resolution_outcome,
        resolution_is_current,
        resolution_event_id,
        resolution_close_id
    FROM ocv.validation_result
    WHERE validation_result.id =
          NEW.resolved_by_validation_result_id;

    IF NOT FOUND THEN
        RAISE SQLSTATE '23514';
    END IF;

    IF resolution_outcome <> 'SATISFIED'
       OR resolution_is_current IS NOT TRUE THEN
        RAISE SQLSTATE '23514';
    END IF;

    IF NEW.event_id IS DISTINCT FROM resolution_event_id
       OR NEW.close_id IS DISTINCT FROM resolution_close_id THEN
        RAISE SQLSTATE '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_alert_validate_resolution_result
BEFORE INSERT OR UPDATE
ON ocv.alert
FOR EACH ROW
EXECUTE FUNCTION ocv.enforce_alert_resolution_result();
