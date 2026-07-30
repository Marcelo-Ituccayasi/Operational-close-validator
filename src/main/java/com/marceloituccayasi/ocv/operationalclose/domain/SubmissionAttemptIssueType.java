package com.marceloituccayasi.ocv.operationalclose.domain;

/**
 * Structured causes supported for a rejected VR-008 evaluation.
 */
public enum SubmissionAttemptIssueType {
    EVENT_NOT_VALIDATED,
    BLOCKING_ALERT,
    VALIDATION_RESULT_FAILED,
    VALIDATION_RESULT_STALE,
    CONSOLIDATION_MISSING,
    CONSOLIDATION_STALE,
    OTHER_CRITICAL_INCONSISTENCY
}