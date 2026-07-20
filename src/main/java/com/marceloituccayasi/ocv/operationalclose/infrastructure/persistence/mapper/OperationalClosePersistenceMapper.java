package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.operationalclose.domain.AuditActor;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CurrencyCode;
import com.marceloituccayasi.ocv.operationalclose.domain.InitialBalance;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseState;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.CloseStateTransitionEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.OperationalCloseEntity;

/**
 * Explicit mapping between Operational Close domain objects and JPA entities.
 */
@Component
public final class OperationalClosePersistenceMapper {

    public OperationalCloseEntity toEntity(
            OperationalClose operationalClose) {

        Objects.requireNonNull(
                operationalClose,
                "operationalClose must not be null");

        return OperationalCloseEntity.create(
                operationalClose.id().value(),
                operationalClose.period().startDate(),
                operationalClose.period().endDate(),
                operationalClose.currencyCode().value(),
                operationalClose.initialBalance().value(),
                operationalClose.state().name(),
                operationalClose.stateChangedAt(),
                operationalClose.createdAt(),
                operationalClose.createdBy().userId(),
                operationalClose.createdBy().username(),
                operationalClose.updatedAt(),
                operationalClose.updatedBy().userId(),
                operationalClose.updatedBy().username());
    }

    public OperationalClose toDomain(
            OperationalCloseEntity entity) {

        Objects.requireNonNull(
                entity,
                "entity must not be null");

        return new OperationalClose(
                new OperationalCloseId(
                        entity.id()),
                new OperationalPeriod(
                        entity.periodStart(),
                        entity.periodEnd()),
                new CurrencyCode(
                        entity.currencyCode()),
                new InitialBalance(
                        entity.initialBalance()),
                OperationalCloseState.valueOf(
                        entity.state()),
                entity.stateChangedAt(),
                entity.createdAt(),
                new AuditActor(
                        entity.createdByUserId(),
                        entity.createdByUsername()),
                entity.updatedAt(),
                new AuditActor(
                        entity.updatedByUserId(),
                        entity.updatedByUsername()));
    }

    public CloseStateTransitionEntity toEntity(
            CloseStateTransition transition) {

        Objects.requireNonNull(
                transition,
                "transition must not be null");

        String fromState =
                transition.fromState() == null
                        ? null
                        : transition.fromState().name();

        return CloseStateTransitionEntity.create(
                transition.id().value(),
                transition.closeId().value(),
                fromState,
                transition.toState().name(),
                transition.causeCode(),
                transition.detail(),
                null,
                null,
                null,
                transition.occurredAt(),
                transition.actor().userId(),
                transition.actor().username());
    }

}
