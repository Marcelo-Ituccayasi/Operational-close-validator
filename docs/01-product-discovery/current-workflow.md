# Current Workflow

**Version:** v0.2
**Status:** Approved baseline
**Process:** Daily cash operational close

## Purpose

This document describes the current operational workflow used to consolidate and validate the daily cash close before the introduction of the proposed product.

The objective is to identify:

- participating users;
- information required;
- operational steps;
- failure points;
- detection points;
- rework;
- consequences;
- opportunities for early validation.

## Process scope

The workflow focuses on the operational close involving:

- income;
- expenses;
- discounts;
- cancellations;
- supporting documents;
- management authorizations;
- physical cash;
- digital system records.

It does not describe the complete accounting close or fiscal reporting process.

## Trigger

The process begins at the end of the operational day or shift, when the responsible user must reconcile the registered movements and prepare the information for accounting.

## Participants

### Cash responsible user

Responsible for:

- receiving and managing physical cash;
- registering operational movements;
- counting the remaining cash;
- collecting supporting documents;
- identifying discrepancies.

### Administration responsible user

Responsible for:

- reviewing income and expenses;
- auditing supporting documents;
- comparing physical and digital information;
- correcting operational records;
- consolidating the close;
- preparing the information for accounting.

In some organizations, the cash and administration responsibilities may be performed by the same person.

### Management

Responsible for:

- authorizing extraordinary expenses;
- authorizing discounts;
- authorizing cancellations;
- approving exceptional operational decisions.

### Accounting

Receives the consolidated information after the operational close.

Accounting is not normally involved in the initial event registration but may detect errors after submission.

## Required information

The close requires access to:

- initial cash balance;
- registered income;
- registered expenses;
- discounts;
- cancellations;
- physical cash balance;
- invoices;
- receipts;
- payment vouchers;
- supporting documents;
- management authorizations;
- POS or payment reports;
- bank or transfer information when applicable;
- previous corrections or adjustments.

## Current workflow

### Step 1 — Collect operational evidence

The responsible user gathers:

- invoices;
- receipts;
- vouchers;
- expense documents;
- discount evidence;
- cancellation evidence;
- management authorizations.

The information may exist in physical documents, messages, spreadsheets, or operational systems.

### Step 2 — Count physical cash

The cash responsible user counts the physical money available at the end of the period.

### Step 3 — Obtain the digital operational report

The administration responsible user obtains the system report containing:

- income;
- expenses;
- discounts;
- cancellations;
- expected balance.

### Step 4 — Compare physical and digital information

The responsible user compares:

- physical cash;
- digital balance;
- supporting documents;
- authorized movements;
- registered adjustments.

### Step 5 — Identify differences

If the values do not match, the responsible user searches for:

- missing movements;
- duplicate records;
- incorrect amounts;
- unsupported expenses;
- informal authorizations;
- missing receipts;
- unregistered discounts;
- unregistered cancellations.

### Step 6 — Contact responsible parties

The administration responsible user may need to contact:

- management;
- cash personnel;
- suppliers;
- employees who performed the expense;
- other operational areas.

The communication normally occurs through calls, messages, or verbal confirmation.

### Step 7 — Correct operational information

The responsible user:

- adds missing movements;
- corrects amounts;
- attaches supporting evidence;
- formalizes authorizations;
- updates spreadsheets or operational systems.

### Step 8 — Repeat the reconciliation

After making corrections, the user repeats the comparison between:

- physical cash;
- digital records;
- supporting documents;
- authorizations.

### Step 9 — Consolidate the close

When the information appears consistent, the user calculates:

- total income;
- total expenses;
- expected balance;
- actual balance;
- differences or adjustments.

### Step 10 — Send information to accounting

The consolidated close is sent to accounting.

If accounting later detects an inconsistency, the information may be returned for correction.

## Main failure point

The representative failure occurs when:

> Management authorizes an expense, discount, cancellation, or exceptional movement, but the movement is not formally registered or communicated to administration before the operational cutoff.

The operational event exists in reality, but its digital representation, supporting evidence, or authorization is incomplete.

## Detection point

The inconsistency is normally detected during final reconciliation when:

- the physical cash does not match the digital balance;
- the supporting documents do not explain all movements;
- the system report does not include an authorized expense;
- a discount or cancellation lacks formal evidence.

The failure is therefore detected late, after the operational event has already occurred.

## Rework sequence

When the discrepancy is detected, the responsible user commonly performs the following rework:

1. Stops the closing process.
2. Reviews transactions individually.
3. Searches for physical or digital evidence.
4. Contacts the responsible person.
5. Confirms whether the movement was legitimate.
6. Requests the missing document or authorization.
7. Corrects the operational record.
8. Recalculates the balance.
9. Repeats the reconciliation.
10. Submits corrected information to accounting.

## Estimated time lost

The observed representative incident generated approximately three hours of rework.

For the general workflow, the provisional estimate is:

> Approximately 1–3 hours of additional work per affected close.

The actual duration depends on:

- number of movements;
- availability of the responsible parties;
- location of supporting documents;
- complexity of the discrepancy;
- number of systems that must be corrected.

## Observed consequences

Consequences supported by the observed workflow include:

- interruption of the closing process;
- repeated auditing;
- repeated data entry;
- delayed consolidation;
- extended working time;
- delayed submission to accounting.

## Inferred consequences

The following consequences are plausible but require additional evidence:

- reduced confidence in operational information;
- deterioration of the working environment;
- delayed management decisions;
- audit exposure;
- financial losses;
- reduced trust between management and administration.

## Current control mechanism

The current process relies primarily on human control:

- memory;
- experience;
- manual comparison;
- physical document review;
- calls and messages;
- repeated reconciliation.

There is no consistent early control that ensures every operational event has a verifiable state before consolidation.

## Operational gap

The principal gap is:

> The organization does not have a shared and verifiable mechanism that confirms whether every registered operational event has the required evidence, authorization, and consistent information before the close begins.

## Validation opportunity

The process presents an opportunity to validate events at the moment of:

- registration;
- document attachment;
- authorization;
- modification;
- consolidation;
- submission to accounting.

## Proposed future workflow

The proposed validation flow is:

1. Register an operational event.
2. Determine its required evidence.
3. Apply the corresponding business rules.
4. Assign an explicit state.
5. Generate an alert when a rule fails.
6. Prevent final validation while blocking inconsistencies remain.
7. Correct the cause.
8. Execute the validation again.
9. Consolidate the close only when the required conditions are satisfied.

## Emerging domain concept

The workflow analysis identifies the following central concept:

> **Operational Event**

An Operational Event represents any registered action or movement that can affect:

- cash;
- supporting documentation;
- authorizations;
- balances;
- the operational close.

Examples include:

- income;
- expense;
- discount;
- cancellation;
- supplier invoice;
- bank or POS cut;
- manual adjustment.

Each event should maintain a verifiable state such as:

- Registered;
- Pending support;
- Pending authorization;
- Pending external reconciliation;
- Observed;
- Validated.

## Main opportunity statement

> Validate operational events when they occur instead of auditing them only after the close fails.

## Current confidence

**Overall confidence:** High for the representative cash-close workflow.

The exact frequency, organizational impact, and applicability to other companies require additional investigation.