package com.marceloituccayasi.ocv.operationalclose.application;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input data required to create an Operational Close.
 *
 * @param periodStart first date of the period
 * @param periodEnd last date of the period
 * @param currencyCode three-letter currency code
 * @param initialBalance non-negative initial balance
 */
public record CreateOperationalCloseCommand(
        LocalDate periodStart,
        LocalDate periodEnd,
        String currencyCode,
        BigDecimal initialBalance) {
}
