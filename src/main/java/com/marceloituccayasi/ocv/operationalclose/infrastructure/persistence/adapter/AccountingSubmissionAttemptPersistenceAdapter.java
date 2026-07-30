package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.AccountingSubmissionAttemptRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttempt;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.AccountingSubmissionAttemptEntity;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.mapper.AccountingSubmissionAttemptPersistenceMapper;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.AccountingSubmissionAttemptJpaRepository;
import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository.SubmissionAttemptIssueJpaRepository;

/**
 * JPA implementation of accounting submission attempt persistence.
 */
@Repository
public class AccountingSubmissionAttemptPersistenceAdapter
        implements AccountingSubmissionAttemptRepository {

    private final AccountingSubmissionAttemptJpaRepository
            attemptJpaRepository;

    private final SubmissionAttemptIssueJpaRepository
            issueJpaRepository;

    private final AccountingSubmissionAttemptPersistenceMapper mapper;

    public AccountingSubmissionAttemptPersistenceAdapter(
            AccountingSubmissionAttemptJpaRepository
                    attemptJpaRepository,
            SubmissionAttemptIssueJpaRepository issueJpaRepository,
            AccountingSubmissionAttemptPersistenceMapper mapper) {

        this.attemptJpaRepository =
                Objects.requireNonNull(
                        attemptJpaRepository);

        this.issueJpaRepository =
                Objects.requireNonNull(
                        issueJpaRepository);

        this.mapper =
                Objects.requireNonNull(
                        mapper);
    }

    @Override
    public void saveNew(
            AccountingSubmissionAttempt attempt) {

        Objects.requireNonNull(
                attempt,
                "attempt must not be null");

        attemptJpaRepository.saveAndFlush(
                mapper.toEntity(
                        attempt));

        if (!attempt.issues().isEmpty()) {
            issueJpaRepository.saveAllAndFlush(
                    mapper.toIssueEntities(
                            attempt));
        }
    }

    @Override
    public Optional<AccountingSubmissionAttempt> findById(
            AccountingSubmissionAttemptId attemptId) {

        Objects.requireNonNull(
                attemptId,
                "attemptId must not be null");

        return attemptJpaRepository
                .findById(
                        attemptId.value())
                .map(
                        this::toDomain);
    }

    @Override
    public Optional<AccountingSubmissionAttempt> findLatestByCloseId(
            OperationalCloseId closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return attemptJpaRepository
                .findFirstByCloseIdOrderByAttemptedAtDescIdDesc(
                        closeId.value())
                .map(
                        this::toDomain);
    }

    @Override
    public List<AccountingSubmissionAttempt>
            findAllByCloseIdOrderByAttemptedAt(
                    OperationalCloseId closeId) {

        Objects.requireNonNull(
                closeId,
                "closeId must not be null");

        return attemptJpaRepository
                .findAllByCloseIdOrderByAttemptedAtAscIdAsc(
                        closeId.value())
                .stream()
                .map(
                        this::toDomain)
                .toList();
    }

    private AccountingSubmissionAttempt toDomain(
            AccountingSubmissionAttemptEntity entity) {

        return mapper.toDomain(
                entity,
                issueJpaRepository
                        .findAllBySubmissionAttemptIdOrderByIdAsc(
                                entity.id()));
    }

}