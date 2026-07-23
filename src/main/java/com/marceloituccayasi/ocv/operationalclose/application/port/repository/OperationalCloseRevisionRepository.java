package com.marceloituccayasi.ocv.operationalclose.application.port.repository;

import com.marceloituccayasi.ocv.operationalclose.domain.CloseStateTransition;
import com.marceloituccayasi.ocv.operationalclose.domain.OperationalClose;

/**
 * Persistence contract for revisions of an already locked Operational Close.
 */
public interface OperationalCloseRevisionRepository {

    void saveRevision(
            OperationalClose operationalClose);

    void appendStateTransition(
            CloseStateTransition stateTransition);

}