# Problem Statement

**Version:** v0.2
**Status:** Approved baseline
**Selected cluster:** C-001 — Operational inconsistencies during critical closing processes

## Main problem

Operational closing processes involving cash, supporting documents, and stock generate avoidable rework because critical information arrives late, is not validated when it is registered, or does not match between physical records and digital systems.

These inconsistencies are commonly discovered during final consolidation or after the information has already been submitted to another area, rather than at the moment when the operational event occurs.

## Affected users

### Primary users

- Administration personnel responsible for recording operational movements.
- Cash or treasury personnel responsible for reconciling balances.
- Users responsible for consolidating the operational close.

### Secondary users

- Finance personnel preparing balances or accounting information.
- Logistics personnel validating stock before dispatch.
- Management personnel authorizing extraordinary movements.
- Accounting personnel receiving the consolidated close.

## Operational context

The problem appears during processes such as:

- daily cash reconciliation;
- registration of income and expenses;
- validation of discounts and cancellations;
- reception of supporting documents;
- supplier invoice processing;
- reconciliation of physical and digital balances;
- preparation of information for accounting;
- stock validation before dispatch.

The process is especially vulnerable when information is distributed between:

- physical invoices or receipts;
- spreadsheets;
- ERP or POS records;
- messages and verbal authorizations;
- personal files;
- external providers such as banks or suppliers.

## Observed failure pattern

A representative case showed the following sequence:

1. Administration collected and reviewed the invoices from the previous operational period.
2. The information was manually consolidated for accounting.
3. Management requested the updated balance.
4. The physical and digital values did not match.
5. A previously authorized movement had not been correctly registered or communicated.
6. Administration repeated the audit and data-entry process.
7. The accounting information had to be corrected and submitted again.

The incident generated approximately three hours of rework.

## Root cause hypothesis

The principal working hypothesis is:

> There is no early and coordinated control mechanism that verifies whether operational events have the required supporting evidence, authorization, and consistent data before they are included in the final close.

The problem is therefore not limited to incorrect data entry.

It also involves:

- informal authorizations;
- missing or late supporting documents;
- delayed communication between areas;
- manual transcription;
- lack of a verifiable event state;
- validation performed only at the end of the process.

## Current resolution method

Organizations commonly resolve these inconsistencies through additional manual effort:

- reviewing transactions one by one;
- searching for physical documents;
- calling or messaging the responsible person;
- requesting missing authorizations;
- correcting spreadsheets or ERP records;
- reopening previously consolidated information;
- extending the workday;
- resubmitting the close to accounting.

## Why the current method is insufficient

The current approach is insufficient because it is reactive.

It attempts to repair inconsistencies after they have already affected the closing process rather than preventing them when the operational event is registered.

It also depends heavily on:

- individual memory;
- informal communication;
- physical documents;
- manual comparison;
- staff experience;
- availability of external parties.

The process therefore does not scale reliably and remains vulnerable to repeated human error.

## Consequences

### Direct consequences

- Repeated review of operational movements.
- Manual correction of balances.
- Reopening of previously consolidated closes.
- Delayed submission to accounting.
- Extended working hours.
- Dispatch or operational delays.

### Potential organizational consequences

The following consequences are considered plausible but require additional evidence before being treated as confirmed:

- reduced confidence in operational information;
- deterioration of the internal working environment;
- delayed management decisions;
- increased audit exposure;
- financial losses caused by unresolved discrepancies.

## Estimated impact

Based on the available observations and investigated cases:

- P-001 may generate approximately 1–3 hours of rework per incident.
- P-010 may generate approximately 3 hours of rework per affected close.
- P-011 may generate approximately 2 hours of delay per affected route.

The combined operational impact is provisionally estimated at:

> **10–15 hours of avoidable work per week**, equivalent to approximately **520–780 hours per year**.

This estimate has **medium confidence** and must be validated through additional operational measurements.

## Product opportunity

The identified opportunity is to provide:

> **An early operational validation layer that detects inconsistencies before they become closing rework.**

The proposed product should verify whether registered operational events:

- contain the required data;
- have supporting documentation when required;
- have formal authorization when required;
- maintain a verifiable state;
- satisfy the applicable business rules;
- are eligible to be included in a validated close.

## Product boundary

The proposed product is not intended to replace:

- the ERP;
- the POS;
- the accounting system;
- electronic invoicing;
- complete inventory management;
- banking platforms;
- physical control procedures.

Its purpose is to operate as a validation and control layer over registered operational events.

## Problem statement

> Administration, cash, finance, and logistics teams experience rework and delayed operational closes because registered movements, supporting documents, authorizations, and balances are not consistently validated before final consolidation. Existing processes detect these failures too late and resolve them through manual review, communication, and correction.

## Initial product hypothesis

> If operational events are validated when they are registered and their unresolved inconsistencies remain visible through explicit states and alerts, the organization can reduce closing rework, prevent unsupported movements from being ignored, and improve the reliability of the information submitted to accounting.

## Core product principle

> The primary problem is not the ability to register information. The primary problem is the inability to verify its operational consistency before the close.

## Confidence level

**Overall confidence:** Medium–high

- Direct evidence supports the existence of late-detected cash-close inconsistencies.
- Investigated cases support similar patterns involving supplier documents and stock.
- The estimated time and economic impact require further measurement.
- The effectiveness of early validation remains a product hypothesis to be tested through the MVP.