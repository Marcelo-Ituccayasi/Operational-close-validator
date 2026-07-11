# Operational Close Validator

Early validation system for detecting unsupported, unauthorized, or inconsistent operational events before accounting close.

## Problem

Operational closes frequently require manual rework because missing receipts, informal authorizations, or inconsistent records are detected only during final consolidation or after submission to accounting.

## MVP objective

Demonstrate that a registered operational event without the required evidence can be detected before close, generate a blocking alert, and prevent the close from advancing until the inconsistency is corrected and successfully revalidated.

## MVP workflow

1. Register an operational event.
2. Execute fixed business validations.
3. Generate a blocking alert when required evidence is missing.
4. Correct the cause of the alert.
5. Revalidate the event.
6. Consolidate the operational close.
7. Submit the validated close to accounting.

## Included event types

- Income
- Expense
- Discount
- Cancellation

## Project status

Product discovery, domain analysis, behavioral modeling, use cases, and MVP scope have been completed.

The approved documentation is being incorporated into the repository. Application implementation has not started yet.

## Documentation

Project documentation is organized under the `docs/` directory.

## Current approved baselines

- Domain Model v0.3
- State Machine v0.3
- Use Cases v0.2
- MVP Scope v0.3