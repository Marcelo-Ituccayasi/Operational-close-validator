package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;

/**
 * Persistence contract for Operational Close consolidations.
 *
 * <p>Mutation use cases must lock the owning Operational Close before
 * changing the current consolidation.</p>
 */
public interface ConsolidationRepository {

    void saveNew(
            Consolidation consolidation);

    Optional<Consolidation> findById(
            ConsolidationId consolidationId);

    Optional<Consolidation> findCurrentByCloseId(
            OperationalCloseId closeId);

    List<Consolidation> findAllByCloseIdOrderByCompletedAt(
            OperationalCloseId closeId);

    void saveInvalidation(
            Consolidation consolidation);

}