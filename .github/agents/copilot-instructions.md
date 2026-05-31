# sabi.git Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-05-30

## Active Technologies
- Java 21 (constitution lists Java 25 as target; project POM targets Java 21 — stay on what the project builds) + Spring Boot 3.x, JSF 2.3 / PrimeFaces 15.x (JoinFaces), EclipseLink JPA, MariaDB 10.x, Flyway, Lombok, springdoc-openapi-v2 (005-coral-stock)
- MariaDB (sabi schema); coral photos on configurable filesystem volume (`sabi.photo.dir/coral/`) (005-coral-stock)
- Java 25 (LTS), Spring Boot 4 + Spring MVC, EclipseLink JPA, Flyway, springdoc-v2 (OpenAPI 3.x), Lombok, Jakarta Validation 3.x, JSF 2.3 + PrimeFaces 15.x (006-invertebrate-tracking)
- MariaDB 10.x (`sabi` schema), Flyway migrations under `sabi-database/src/main/resources/db/migration/version1_8_0/` (006-invertebrate-tracking)

- Java 25, Spring Boot 4 (001-ai-data-export)

## Project Structure

Dieses Repository ist als Multi-Module-Projekt organisiert. Relevante Top-Level-Module sind u. a.:

```text
sabi-server/
sabi-webclient/
sabi-boundary/
```

## Commands

# Add commands for Java 25, Spring Boot 4

## Code Style

Java 25, Spring Boot 4: Follow standard conventions

## Recent Changes
- 006-invertebrate-tracking: Added Java 25 (LTS), Spring Boot 4 + Spring MVC, EclipseLink JPA, Flyway, springdoc-v2 (OpenAPI 3.x), Lombok, Jakarta Validation 3.x, JSF 2.3 + PrimeFaces 15.x
- 005-coral-stock: Added Java 21 (constitution lists Java 25 as target; project POM targets Java 21 — stay on what the project builds) + Spring Boot 3.x, JSF 2.3 / PrimeFaces 15.x (JoinFaces), EclipseLink JPA, MariaDB 10.x, Flyway, Lombok, springdoc-openapi-v2

- 001-ai-data-export: Added Java 25, Spring Boot 4

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
