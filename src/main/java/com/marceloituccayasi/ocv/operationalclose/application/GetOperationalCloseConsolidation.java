package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.port.TransactionRunner;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalEventRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalEvent;

/**
 * Loads the current read model required by the consolidation form.
 */
public final class GetOperationalCloseConsolidation {

    private final OperationalCloseRepository
            closeRepository;

    private final OperationalEventRepository
            eventRepository;

    private final CloseConsolidationReadinessEvaluator
            readinessEvaluator;

    private final TransactionRunner transactionRunner;

    public GetOperationalCloseConsolidation(
            OperationalCloseRepository closeRepository,
            OperationalEventRepository eventRepository,
            CloseConsolidationReadinessEvaluator readinessEvaluator,
            TransactionRunner transactionRunner) {

        this.closeRepository =
                Objects.requireNonNull(
                        closeRepository);

        this.eventRepository =
                Objects.requireNonNull(
                        eventRepository);

        this.readinessEvaluator =
                Objects.requireNonNull(
                        readinessEvaluator);

        this.transactionRunner =
                Objects.requireNonNull(
                        transactionRunner);
    }

    public GetOperationalCloseConsolidationResult execute(
            UUID closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return transactionRunner.execute(
                () -> executeInsideTransaction(
                        new OperationalCloseId(
                                closeId)));
    }

    private GetOperationalCloseConsolidationResult
            executeInsideTransaction(
                    OperationalCloseId closeId) {

        Optional<OperationalClose> persistedClose =
                closeRepository.findById(
                        closeId);

        if (persistedClose.isEmpty()) {
            return GetOperationalCloseConsolidationResult
                    .notFound();
        }

        OperationalClose operationalClose =
                persistedClose.orElseThrow();

        if (operationalClose.state()
                != OperationalCloseState.PREPARATION
                && operationalClose.state()
                        != OperationalCloseState.BLOCKED) {

            return GetOperationalCloseConsolidationResult
                    .notAvailable();
        }

        List<OperationalEvent> events =
                Objects.requireNonNull(
                        eventRepository
                                .findAllByCloseIdOrderByOccurredAtDescending(
                                        closeId),
                        "operational events must not be null");

        CloseConsolidationReadiness readiness =
                Objects.requireNonNull(
                        readinessEvaluator.evaluate(
                                closeId,
                                events),
                        "consolidation readiness must not be null");

        return GetOperationalCloseConsolidationResult
                .found(
                        CloseConsolidationPreview.fromDomain(
                                operationalClose,
                                events,
                                readiness));
    }

}