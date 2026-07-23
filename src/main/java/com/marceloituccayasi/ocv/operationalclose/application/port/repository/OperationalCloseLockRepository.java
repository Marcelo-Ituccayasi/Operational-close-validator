package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;

/**
 * Persistence contract for loading an Operational Close with
 * an exclusive database lock inside an active transaction.
 */
public interface OperationalCloseLockRepository {

    Optional<OperationalClose> findByIdForUpdate(
            OperationalCloseId closeId);

}