package com.marceloituccayasi.ocv.operationalclose.infrastructure.submission;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.marceloituccayasi.ocv.operationalclose.application.port.repository.AccountingSubmissionAttemptRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.CloseValidationResultRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.ConsolidationRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseLockRepository;
import com.marceloituccayasi.ocv.operationalclose.application.port.repository.OperationalCloseRevisionRepository;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttempt;
import com.marceloituccayasi.ocv.operationalclose.domain.AccountingSubmissionAttemptId;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.CloseValidationResult;
import com.marceloituccayasi.ocv.operationalclose.domain.Consolidation;
import com.marceloituccayasi.ocv.operationalclose.domain.ConsolidationId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationResultId;
import com.marceloituccayasi.ocv.operationalclose.domain.ValidationRuleCode;

final class AccountingSubmissionIntegrationTestSupport {

    private AccountingSubmissionIntegrationTestSupport() {
    }

    enum FailurePoint {
        NONE,
        AFTER_VALIDATION_RESULT_SAVE,
        AFTER_ATTEMPT_SAVE,
        AFTER_CONSOLIDATION_INVALIDATION,
        AFTER_CLOSE_REVISION_SAVE,
        AFTER_TRANSITION_SAVE
    }

    static final class InjectedSubmissionFailure
            extends RuntimeException {

        InjectedSubmissionFailure(
                FailurePoint failurePoint) {

            super(
                    "Injected accounting submission failure at "
                            + failurePoint.name());
        }
    }

    static final class FailureController {

        private final AtomicReference<FailurePoint> activeFailure =
                new AtomicReference<>(
                        FailurePoint.NONE);

        void reset() {
            activeFailure.set(
                    FailurePoint.NONE);
        }

        void failAt(
                FailurePoint failurePoint) {

            activeFailure.set(
                    Objects.requireNonNull(
                            failurePoint));
        }

        void throwIfActive(
                FailurePoint failurePoint) {

            if (activeFailure.compareAndSet(
                    failurePoint,
                    FailurePoint.NONE)) {

                throw new InjectedSubmissionFailure(
                        failurePoint);
            }
        }
    }

    static final class LockProbe {

        private final AtomicBoolean enabled =
                new AtomicBoolean();

        private final AtomicInteger startedAttempts =
                new AtomicInteger();

        private final AtomicInteger acquiredLocks =
                new AtomicInteger();

        private volatile CountDownLatch firstLockAcquired =
                new CountDownLatch(
                        1);

        private volatile CountDownLatch secondAttemptStarted =
                new CountDownLatch(
                        1);

        private volatile CountDownLatch releaseFirstLock =
                new CountDownLatch(
                        1);

        void reset() {
            enabled.set(
                    false);

            startedAttempts.set(
                    0);

            acquiredLocks.set(
                    0);

            firstLockAcquired =
                    new CountDownLatch(
                            1);

            secondAttemptStarted =
                    new CountDownLatch(
                            1);

            releaseFirstLock =
                    new CountDownLatch(
                            1);
        }

        void holdFirstLock() {
            reset();

            enabled.set(
                    true);
        }

        void beforeAcquire() {
            if (!enabled.get()) {
                return;
            }

            int started =
                    startedAttempts.incrementAndGet();

            if (started == 2) {
                secondAttemptStarted.countDown();
            }
        }

        void afterAcquire() {
            if (!enabled.get()) {
                return;
            }

            int acquired =
                    acquiredLocks.incrementAndGet();

            if (acquired != 1) {
                return;
            }

            firstLockAcquired.countDown();

            awaitRequired(
                    releaseFirstLock,
                    "first transaction was not released");
        }

        boolean awaitFirstLock() {
            return awaitObserved(
                    firstLockAcquired);
        }

        boolean awaitSecondAttempt() {
            return awaitObserved(
                    secondAttemptStarted);
        }

        void releaseFirstLock() {
            releaseFirstLock.countDown();
        }

        int startedAttempts() {
            return startedAttempts.get();
        }

        private static boolean awaitObserved(
                CountDownLatch latch) {

            try {
                return latch.await(
                        5,
                        TimeUnit.SECONDS);
            }
            catch (InterruptedException exception) {
                Thread.currentThread()
                        .interrupt();

                throw new IllegalStateException(
                        "concurrency observation was interrupted",
                        exception);
            }
        }

        private static void awaitRequired(
                CountDownLatch latch,
                String timeoutMessage) {

            try {
                if (!latch.await(
                        10,
                        TimeUnit.SECONDS)) {

                    throw new IllegalStateException(
                            timeoutMessage);
                }
            }
            catch (InterruptedException exception) {
                Thread.currentThread()
                        .interrupt();

                throw new IllegalStateException(
                        "concurrency control was interrupted",
                        exception);
            }
        }
    }

    static final class BlockingCloseLockRepository
            implements OperationalCloseLockRepository {

        private final OperationalCloseLockRepository delegate;

        private final LockProbe lockProbe;

        BlockingCloseLockRepository(
                OperationalCloseLockRepository delegate,
                LockProbe lockProbe) {

            this.delegate =
                    Objects.requireNonNull(
                            delegate);

            this.lockProbe =
                    Objects.requireNonNull(
                            lockProbe);
        }

        @Override
        public Optional<OperationalClose> findByIdForUpdate(
                OperationalCloseId closeId) {

            lockProbe.beforeAcquire();

            Optional<OperationalClose> result =
                    delegate.findByIdForUpdate(
                            closeId);

            if (result.isPresent()) {
                lockProbe.afterAcquire();
            }

            return result;
        }
    }

    static final class FaultInjectingCloseValidationResultRepository
            implements CloseValidationResultRepository {

        private final CloseValidationResultRepository delegate;

        private final FailureController failureController;

        FaultInjectingCloseValidationResultRepository(
                CloseValidationResultRepository delegate,
                FailureController failureController) {

            this.delegate =
                    Objects.requireNonNull(
                            delegate);

            this.failureController =
                    Objects.requireNonNull(
                            failureController);
        }

        @Override
        public void saveNew(
                CloseValidationResult validationResult) {

            delegate.saveNew(
                    validationResult);

            failureController.throwIfActive(
                    FailurePoint.AFTER_VALIDATION_RESULT_SAVE);
        }

        @Override
        public Optional<CloseValidationResult> findById(
                ValidationResultId validationResultId) {

            return delegate.findById(
                    validationResultId);
        }

        @Override
        public Optional<CloseValidationResult>
                findCurrentByCloseIdAndRuleCode(
                        OperationalCloseId closeId,
                        ValidationRuleCode ruleCode) {

            return delegate.findCurrentByCloseIdAndRuleCode(
                    closeId,
                    ruleCode);
        }

        @Override
        public void saveInvalidation(
                CloseValidationResult validationResult) {

            delegate.saveInvalidation(
                    validationResult);
        }
    }

    static final class FaultInjectingAttemptRepository
            implements AccountingSubmissionAttemptRepository {

        private final AccountingSubmissionAttemptRepository delegate;

        private final FailureController failureController;

        FaultInjectingAttemptRepository(
                AccountingSubmissionAttemptRepository delegate,
                FailureController failureController) {

            this.delegate =
                    Objects.requireNonNull(
                            delegate);

            this.failureController =
                    Objects.requireNonNull(
                            failureController);
        }

        @Override
        public void saveNew(
                AccountingSubmissionAttempt attempt) {

            delegate.saveNew(
                    attempt);

            failureController.throwIfActive(
                    FailurePoint.AFTER_ATTEMPT_SAVE);
        }

        @Override
        public Optional<AccountingSubmissionAttempt> findById(
                AccountingSubmissionAttemptId attemptId) {

            return delegate.findById(
                    attemptId);
        }

        @Override
        public Optional<AccountingSubmissionAttempt>
                findLatestByCloseId(
                        OperationalCloseId closeId) {

            return delegate.findLatestByCloseId(
                    closeId);
        }

        @Override
        public List<AccountingSubmissionAttempt>
                findAllByCloseIdOrderByAttemptedAt(
                        OperationalCloseId closeId) {

            return delegate.findAllByCloseIdOrderByAttemptedAt(
                    closeId);
        }
    }

    static final class FaultInjectingConsolidationRepository
            implements ConsolidationRepository {

        private final ConsolidationRepository delegate;

        private final FailureController failureController;

        FaultInjectingConsolidationRepository(
                ConsolidationRepository delegate,
                FailureController failureController) {

            this.delegate =
                    Objects.requireNonNull(
                            delegate);

            this.failureController =
                    Objects.requireNonNull(
                            failureController);
        }

        @Override
        public void saveNew(
                Consolidation consolidation) {

            delegate.saveNew(
                    consolidation);
        }

        @Override
        public Optional<Consolidation> findById(
                ConsolidationId consolidationId) {

            return delegate.findById(
                    consolidationId);
        }

        @Override
        public Optional<Consolidation> findCurrentByCloseId(
                OperationalCloseId closeId) {

            return delegate.findCurrentByCloseId(
                    closeId);
        }

        @Override
        public List<Consolidation>
                findAllByCloseIdOrderByCompletedAt(
                        OperationalCloseId closeId) {

            return delegate.findAllByCloseIdOrderByCompletedAt(
                    closeId);
        }

        @Override
        public void saveInvalidation(
                Consolidation consolidation) {

            delegate.saveInvalidation(
                    consolidation);

            failureController.throwIfActive(
                    FailurePoint
                            .AFTER_CONSOLIDATION_INVALIDATION);
        }
    }

    static final class FaultInjectingCloseRevisionRepository
            implements OperationalCloseRevisionRepository {

        private final OperationalCloseRevisionRepository delegate;

        private final FailureController failureController;

        FaultInjectingCloseRevisionRepository(
                OperationalCloseRevisionRepository delegate,
                FailureController failureController) {

            this.delegate =
                    Objects.requireNonNull(
                            delegate);

            this.failureController =
                    Objects.requireNonNull(
                            failureController);
        }

        @Override
        public void saveRevision(
                OperationalClose operationalClose) {

            delegate.saveRevision(
                    operationalClose);

            failureController.throwIfActive(
                    FailurePoint.AFTER_CLOSE_REVISION_SAVE);
        }

        @Override
        public void appendStateTransition(
                CloseStateTransition stateTransition) {

            delegate.appendStateTransition(
                    stateTransition);
        }

        @Override
        public void appendConsolidationStateTransition(
                CloseStateTransition stateTransition,
                ConsolidationId consolidationId) {

            delegate.appendConsolidationStateTransition(
                    stateTransition,
                    consolidationId);
        }

        @Override
        public void appendSubmissionStateTransition(
                CloseStateTransition stateTransition,
                ValidationResultId validationResultId,
                AccountingSubmissionAttemptId submissionAttemptId) {

            delegate.appendSubmissionStateTransition(
                    stateTransition,
                    validationResultId,
                    submissionAttemptId);

            failureController.throwIfActive(
                    FailurePoint.AFTER_TRANSITION_SAVE);
        }

        @Override
        public void appendSubmissionStateTransition(
                CloseStateTransition stateTransition,
                ValidationResultId validationResultId,
                ConsolidationId consolidationId,
                AccountingSubmissionAttemptId submissionAttemptId) {

            delegate.appendSubmissionStateTransition(
                    stateTransition,
                    validationResultId,
                    consolidationId,
                    submissionAttemptId);

            failureController.throwIfActive(
                    FailurePoint.AFTER_TRANSITION_SAVE);
        }
    }

}