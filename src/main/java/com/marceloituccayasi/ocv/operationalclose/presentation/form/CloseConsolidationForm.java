package com.marceloituccayasi.ocv.operationalclose.presentation.form;

import java.math.BigDecimal;
import java.util.UUID;

import com.marceloituccayasi.ocv.operationalclose.application.CompleteOperationalCloseConsolidationCommand;

/**
 * Mutable web form used to bind Operational Close consolidation input.
 */
public final class CloseConsolidationForm {

    private String actualBalance;

    public String getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(
            String actualBalance) {

        this.actualBalance =
                actualBalance;
    }

    public CompleteOperationalCloseConsolidationCommand toCommand(
            UUID closeId) {

        if (closeId == null) {
            throw new IllegalArgumentException(
                    "El identificador del cierre es obligatorio.");
        }

        try {
            return new CompleteOperationalCloseConsolidationCommand(
                    closeId,
                    new BigDecimal(
                            requiredValue(
                                    actualBalance)));
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "El saldo real no tiene un formato decimal válido.",
                    exception);
        }
    }

    private static String requiredValue(
            String value) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "El saldo real es obligatorio.");
        }

        return value.trim();
    }

}