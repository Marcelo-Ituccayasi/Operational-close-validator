package com.marceloituccayasi.ocv.operationalclose.presentation.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.marceloituccayasi.ocv.operationalclose.application.CreateOperationalCloseCommand;

/**
 * Mutable web form used to bind Operational Close creation fields.
 */
public final class OperationalCloseForm {

    private String periodStart;
    private String periodEnd;
    private String currencyCode;
    private String initialBalance;

    public String getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(
            String periodStart) {

        this.periodStart = periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(
            String periodEnd) {

        this.periodEnd = periodEnd;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(
            String currencyCode) {

        this.currencyCode = currencyCode;
    }

    public String getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(
            String initialBalance) {

        this.initialBalance = initialBalance;
    }

    public CreateOperationalCloseCommand toCommand() {
        try {
            return new CreateOperationalCloseCommand(
                    LocalDate.parse(
                            requiredValue(periodStart)),
                    LocalDate.parse(
                            requiredValue(periodEnd)),
                    requiredValue(currencyCode),
                    new BigDecimal(
                            requiredValue(initialBalance)));
        }
        catch (DateTimeParseException
                | NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Los datos ingresados no tienen el formato esperado.",
                    exception);
        }
    }

    private static String requiredValue(
            String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Todos los campos son obligatorios.");
        }

        return value.trim();
    }

}
