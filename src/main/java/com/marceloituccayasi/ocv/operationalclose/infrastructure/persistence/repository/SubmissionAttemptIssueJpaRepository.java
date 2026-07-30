package com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.marceloituccayasi.ocv.operationalclose.infrastructure.persistence.entity.SubmissionAttemptIssueEntity;

/**
 * Internal Spring Data repository for immutable submission issues.
 */
public interface SubmissionAttemptIssueJpaRepository
        extends JpaRepository<
                SubmissionAttemptIssueEntity,
                UUID> {

    List<SubmissionAttemptIssueEntity>
            findAllBySubmissionAttemptIdOrderByIdAsc(
                    UUID submissionAttemptId);

}