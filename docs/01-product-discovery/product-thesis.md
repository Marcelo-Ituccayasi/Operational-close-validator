# Product Thesis

**Version:** v0.2
**Status:** Approved baseline
**Product:** Operational Close Validator

## Purpose

This document defines the central product hypothesis derived from the initial problem discovery process.

The thesis describes the user, operational problem, expected outcome, product boundary, principal risks, and evidence required to validate the opportunity.

It does not define the technical architecture or implementation strategy.

## Main thesis

Organizations lose time and confidence in their operational information because existing systems register transactions but do not consistently validate missing evidence, informal authorizations, or data inconsistencies before critical closing processes.

The product opportunity is to create an early operational validation layer that detects these inconsistencies before they become closing rework.

## Target user

### Primary user

The primary user is the person responsible for administration or cash operations who:

- registers income and expenses;
- collects supporting documents;
- reviews operational movements;
- reconciles physical and digital information;
- consolidates the operational close;
- prepares information for accounting.

### Secondary users

Secondary users may include:

- finance personnel preparing balances;
- logistics personnel reconciling stock before dispatch;
- management personnel authorizing extraordinary movements;
- accounting personnel receiving validated closing information.

The MVP focuses only on the administration or cash-responsible user.

## Jobs to be done

### Administration and cash operations

> When preparing the operational close, I need every registered movement to have the required evidence and authorization so that I can consolidate the period without repeating the audit manually.

### Finance

> When preparing balances, I need the operational information to be complete and validated so that I do not have to return the close for correction.

### Logistics

> When preparing a dispatch, I need digital information to reflect the operational reality so that the route is not delayed by late inconsistencies.

## User pain

The main pain is the lack of agreement between:

- registered operational movements;
- physical or digital supporting documents;
- formal authorizations;
- physical cash;
- system balances;
- stock information;
- external information received after the operational cutoff.

The failure is commonly detected during final consolidation rather than when the event is registered.

## Current workaround

Organizations commonly compensate for these failures through:

- manual reviews;
- transaction-by-transaction comparison;
- spreadsheets;
- calls and messages;
- verbal confirmations;
- searches for physical documents;
- overtime;
- reopening of previously consolidated information;
- resubmission to accounting.

## Why the current workaround is insufficient

The current approach is reactive and depends heavily on individual effort.

It does not reliably prevent:

- unsupported expenses;
- informal discounts or cancellations;
- duplicate or incorrect amounts;
- missing supplier documents;
- late external reconciliations;
- operational events without a verifiable state.

The process also becomes more fragile when experienced employees leave or when operational volume increases.

## Desired outcome

The desired outcome is an operational closing process where:

- every registered event has an explicit state;
- missing evidence is detected early;
- required authorizations are verifiable;
- inconsistencies generate visible alerts;
- blocking issues prevent the close from advancing;
- corrections trigger a new validation;
- the information sent to accounting has passed a final quality gate.

## Product hypothesis

> If registered operational events are validated when they are created or modified, and unresolved inconsistencies remain visible through explicit states and alerts, organizations can reduce closing rework and prevent unsupported or unauthorized movements from being ignored during consolidation.

## Core product principle

> The principal problem is not registering information. The principal problem is validating its operational consistency before the close.

## Proposed product

The proposed product is:

> An early operational validation layer that evaluates registered events, applies fixed or configurable business rules, generates alerts, and controls whether an operational close can advance.

The product initially focuses on:

- income;
- expenses;
- discounts;
- cancellations;
- supporting documents;
- authorizations;
- validations;
- alerts;
- operational close states.

## Product boundary

The product is not intended to replace:

- an ERP;
- a POS;
- an accounting platform;
- electronic invoicing;
- complete inventory management;
- banking systems;
- complete supplier management;
- physical control procedures.

It operates as a validation and control layer over operational information that has already been registered.

## Explicit non-goals

The product will not initially provide:

- complete accounting;
- tax calculation;
- electronic invoice issuance;
- complete inventory management;
- payroll;
- banking reconciliation automation;
- supplier portals;
- multi-branch administration;
- advanced financial reporting.

## Initial value proposition

> Detect unsupported, unauthorized, or inconsistent operational events before they become closing rework.

## Primary risk

The principal operational risk is:

> The product can only validate events that have been registered.

If a real-world movement occurs but is never entered into the system, the validation layer cannot detect it by itself.

This remains a residual operational risk outside the control of the initial product.

## Additional risks

- Resistance to changing current operational habits.
- Incomplete or delayed event registration.
- Excessive alerts that users begin to ignore.
- Incorrectly configured validation rules.
- Blocking rules that interrupt legitimate operations.
- Insufficient evidence about the actual frequency of the problem.
- Difficulty integrating with existing systems in later versions.

## Evidence required

The product thesis should be validated with additional evidence such as:

- time between an operational event and its registration;
- time between document receipt and reconciliation;
- frequency of missing receipts;
- frequency of informal authorizations;
- number of closes reopened;
- number of corrections requested by accounting;
- overtime associated with closing activities;
- average time required to resolve a discrepancy;
- percentage of inconsistencies detected before final consolidation.

## Success indicators

Potential success indicators include:

- reduction in time spent auditing the close;
- percentage of blocking inconsistencies detected before consolidation;
- percentage of registered events with complete supporting evidence;
- reduction in reopened closes;
- reduction in accounting returns;
- reduction in manual calls or messages required to resolve discrepancies;
- time required to move an event from pending or observed to validated.

## MVP hypothesis

The MVP will test the following narrower hypothesis:

> A registered expense without a required receipt or authorization can be detected automatically, generate a blocking alert, prevent the operational close from being validated, and allow the close to advance only after the cause is corrected and successfully revalidated.

## MVP success condition

The hypothesis will be considered technically demonstrated when the MVP can:

1. Register an operational event.
2. Detect missing evidence or authorization.
3. Generate a blocking alert.
4. Block the operational close.
5. Accept the missing evidence or authorization.
6. Revalidate the event successfully.
7. Resolve the alert through revalidation.
8. Validate the operational close.
9. Submit the close to accounting as an internal state transition.

## Current confidence

**Overall confidence:** Medium–high

The operational problem is supported by direct observation and investigated cases.

However:

- the estimated economic impact requires additional measurement;
- the behavior of users has not yet been validated through a working prototype;
- the effectiveness of the validation approach remains a product hypothesis;
- the scalability of the approach beyond cash closing has not yet been demonstrated.