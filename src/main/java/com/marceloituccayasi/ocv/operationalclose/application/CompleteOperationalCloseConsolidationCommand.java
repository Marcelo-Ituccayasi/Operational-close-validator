package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Input required to complete an Operational Close consolidation.
 *
 * @param closeId Operational Close to consolidate
 * @param actualBalance non-negative actual balance informed by the user
 */
public record CompleteOperationalCloseConsolidationCommand(
        UUID closeId,
        BigDecimal actualBalance) {
}