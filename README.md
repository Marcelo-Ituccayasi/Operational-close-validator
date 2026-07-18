# Operational Close Validator

Early validation system for detecting unsupported, unauthorized, or inconsistent operational events before operational close and submission to accounting.

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

Product discovery, domain analysis, system behavior, MVP scope, technical design, and the implementation plan are approved and incorporated into the repository.

Application implementation is in progress. IP-00 establishes the reproducible Spring Boot build, PostgreSQL integration testing, Flyway migration pipeline, architecture checks, coverage reporting, and CI gates.

## Development verification

Prerequisites:

- Java 25
- A Docker Engine capable of running Linux containers

Run `.\mvnw.cmd verify` on Windows or `./mvnw verify` on Linux and macOS.

The verification starts PostgreSQL through Testcontainers and executes Flyway, the automated test suite, ArchUnit, and JaCoCo. IP-00 verification does not require real application secrets.

## Documentation

Project documentation is organized under the `docs/` directory.

### Product discovery

- [Problem Map v0.2.1](docs/01-product-discovery/problem-map.md)
- [Problem Statement v0.2](docs/01-product-discovery/problem-statement.md)
- [Product Thesis v0.2](docs/01-product-discovery/product-thesis.md)
- [Current Workflow v0.2](docs/01-product-discovery/current-workflow.md)

### Domain analysis

- [Failure Mode Analysis v0.1](docs/02-domain-analysis/failure-mode-analysis.md)
- [Validation Rules v0.2](docs/02-domain-analysis/validation-rules.md)
- [Domain Model v0.3](docs/02-domain-analysis/domain-model.md)

### System behavior

- [State Machine v0.3](docs/03-system-behavior/state-machine.md)
- [Use Cases v0.2](docs/03-system-behavior/use-cases.md)

### Product scope

- [MVP Scope v0.3](docs/04-product-scope/mvp-scope.md)

### Decision records

- [ADR-0001 — Final validation can return a validated close to blocked](docs/decisions/ADR-0001-final-validation-blocks-close.md)