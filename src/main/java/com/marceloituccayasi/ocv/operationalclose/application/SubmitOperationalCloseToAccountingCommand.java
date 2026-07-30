package com.marceloituccayasi.ocv.operationalclose.application;

import java.util.UUID;

/**
 * Input required to execute the final accounting submission control.
 *
 * @param closeId Operational Close to evaluate and submit
 */
public record SubmitOperationalCloseToAccountingCommand(
        UUID closeId) {
}