package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite persistence identifier of a consolidation Event snapshot.
 */
public final class ConsolidationEventSnapshotEntityId
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID consolidationId;

    private UUID eventId;

    public ConsolidationEventSnapshotEntityId() {
        // Required by JPA.
    }

    public ConsolidationEventSnapshotEntityId(
            UUID consolidationId,
            UUID eventId) {

        this.consolidationId =
                Objects.requireNonNull(
                        consolidationId);

        this.eventId =
                Objects.requireNonNull(
                        eventId);
    }

    @Override
    public boolean equals(
            Object other) {

        if (this == other) {
            return true;
        }

        if (!(other
                instanceof ConsolidationEventSnapshotEntityId otherId)) {

            return false;
        }

        return Objects.equals(
                consolidationId,
                otherId.consolidationId)
                && Objects.equals(
                        eventId,
                        otherId.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                consolidationId,
                eventId);
    }

}