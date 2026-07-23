package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEventId;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidence;
import com.marceloituccayasi.ocv.operationalclose.domain.SupportingEvidenceId;

/**
 * Persistence contract required by Supporting Evidence use cases.
 *
 * <p>Mutation use cases must lock the owning Operational Close and
 * Operational Event before locking Supporting Evidence.
 */
public interface SupportingEvidenceRepository {

    void saveNew(
            SupportingEvidence supportingEvidence);

    Optional<SupportingEvidence> findById(
            SupportingEvidenceId evidenceId);

    List<SupportingEvidence>
            findAllByEventIdOrderByEvidenceDateDescending(
                    OperationalEventId eventId);

    Optional<SupportingEvidence> findByIdForUpdate(
            OperationalCloseId closeId,
            OperationalEventId eventId,
            SupportingEvidenceId evidenceId);

    void saveRevision(
            SupportingEvidence supportingEvidence);

}