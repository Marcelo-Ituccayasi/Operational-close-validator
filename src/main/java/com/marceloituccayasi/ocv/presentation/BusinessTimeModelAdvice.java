package com.marceloituccayasi.ocv.presentation;

import java.util.Objects;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes business-zone date-time formatting to server-rendered views.
 */
@ControllerAdvice
public final class BusinessTimeModelAdvice {

    private final BusinessDateTimeFormatter businessDateTimeFormatter;

    public BusinessTimeModelAdvice(
            BusinessDateTimeFormatter businessDateTimeFormatter) {

        this.businessDateTimeFormatter =
                Objects.requireNonNull(
                        businessDateTimeFormatter);
    }

    @ModelAttribute("businessTime")
    public BusinessDateTimeFormatter businessTime() {
        return businessDateTimeFormatter;
    }

}