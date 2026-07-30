package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.AccountingSubmissionAttemptEntity;

/**
 * Internal Spring Data repository for accounting submission attempts.
 */
public interface AccountingSubmissionAttemptJpaRepository
        extends JpaRepository<
                AccountingSubmissionAttemptEntity,
                UUID> {

    Optional<AccountingSubmissionAttemptEntity>
            findFirstByCloseIdOrderByAttemptedAtDescIdDesc(
                    UUID closeId);

    List<AccountingSubmissionAttemptEntity>
            findAllByCloseIdOrderByAttemptedAtAscIdAsc(
                    UUID closeId);

}