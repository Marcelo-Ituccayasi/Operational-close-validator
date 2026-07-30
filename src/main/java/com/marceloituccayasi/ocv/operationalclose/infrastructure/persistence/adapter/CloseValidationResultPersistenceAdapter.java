package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.CloseValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleScope;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.CloseValidationResultPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.ValidationResultJpaRepository;

/**
 * JPA implementation of Close Validation Result persistence.
 */
@Repository
public class CloseValidationResultPersistenceAdapter
        implements CloseValidationResultRepository {

    private final ValidationResultJpaRepository
            validationResultJpaRepository;

    private final CloseValidationResultPersistenceMapper mapper;

    public CloseValidationResultPersistenceAdapter(
            ValidationResultJpaRepository
                    validationResultJpaRepository,
            CloseValidationResultPersistenceMapper mapper) {

        this.validationResultJpaRepository =
                Objects.requireNonNull(
                        validationResultJpaRepository);

        this.mapper =
                Objects.requireNonNull(
                        mapper);
    }

    @Override
    public void saveNew(
            CloseValidationResult validationResult) {

        Objects.requireNonNull(
                validationResult,
                "validationResult must not be null");

        if (!validationResult.current()) {
            throw new IllegalArgumentException(
                    "new validation result must be current");
        }

        validationResultJpaRepository.saveAndFlush(
                mapper.toEntity(
                        validationResult));
    }

    @Override
    public Optional<CloseValidationResult> findById(
            ValidationResultId validationResultId) {

        Objects.requireNonNull(
                validationResultId,
                "validationResultId must not be null");

        return validationResultJpaRepository
                .findCloseResultById(
                        validationResultId.value())
                .map(
                        mapper::toDomain);
    }

    @Override
    public Optional<CloseValidationResult>
            findCurrentByCloseIdAndRuleCode(
                    OperationalCloseId closeId,
                    ValidationRuleCode ruleCode) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        Objects.requireNonNull(
                ruleCode,
                "ruleCode must not be null");

        if (ruleCode.scope()
                != ValidationRuleScope.CLOSE) {

            throw new IllegalArgumentException(
                    "close result lookup requires a close-scoped rule");
        }

        return validationResultJpaRepository
                .findByCloseIdAndRuleCodeAndCurrentTrue(
                        closeId.value(),
                        ruleCode.persistentValue())
                .map(
                        mapper::toDomain);
    }

    @Override
    public void saveInvalidation(
            CloseValidationResult validationResult) {

        Objects.requireNonNull(
                validationResult,
                "validationResult must not be null");

        if (validationResult.current()) {
            throw new IllegalArgumentException(
                    "invalidation persistence requires "
                            + "an invalidated validation result");
        }

        validationResultJpaRepository.saveAndFlush(
                mapper.toEntity(
                        validationResult));
    }

}