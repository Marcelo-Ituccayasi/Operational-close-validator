package com.marceloituccayasi.ocv.operationalclose.application;

/**
 * Evidence and authorization applicability determined for an Operational
 * Event.
 *
 * The definitive applicability rules belong to a later implementation
 * increment.
 */
public record OperationalEventRequirements(
        boolean evidenceRequired,
        boolean authorizationRequired) {
}