# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Initial repository foundation.
- Documentation directory structure.
- Source, test, and infrastructure placeholders.
- Approved product discovery baselines.
- Approved domain analysis baselines.
- Approved system behavior baselines.
- Approved MVP scope baseline.
- ADR-0001 covering the final validation transition from Validated to Blocked.

- Spring Boot Maven project targeting Java 25.
- Maven Wrapper configured with Maven 3.9.16.
- Local and test profiles with UTC defaults.
- PostgreSQL 18.4 integration testing through Testcontainers.
- Initial Flyway migration pipeline.
- ArchUnit architecture verification and JaCoCo coverage reporting.
- GitHub Actions CI workflow running Maven verification on pull requests and `main`.

- Modular package boundaries for Operational Close Management and Identity and Access.
- Framework-independent `AuthenticatedPrincipal` application contract.
- Application ports for clock, current actor, transaction execution, and future repositories.
- System clock and Spring `TransactionTemplate` infrastructure adapters.
- Explicit `READ_COMMITTED` transaction configuration with integration tests for commit and rollback.
- Dedicated persistence packages for JPA entities, Spring Data repositories, mappers, and adapters.
- ArchUnit rules enforcing module, layer, application contract, and persistence boundaries.

### Changed

- Disabled JPA Open EntityManager in View so persistence access remains inside explicit application boundaries.

- Clarified the product description and its boundary with accounting submission.
- Aligned close, event, validation result, alert, and consolidation terminology.
- Normalized documentation metadata formatting for GitHub rendering.