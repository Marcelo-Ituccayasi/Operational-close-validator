package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import java.util.List;
import java.util.Optional;

import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalCloseId;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalPeriod;

/**
 * Persistence contract required by Operational Close use cases.
 */
public interface OperationalCloseRepository {

    boolean existsByPeriod(
            OperationalPeriod period);

    void saveNew(
            OperationalClose operationalClose,
            CloseStateTransition initialTransition);

    Optional<OperationalClose> findById(
            OperationalCloseId closeId);

    List<OperationalClose> findAllByPeriodDescending();

}