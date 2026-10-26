# Implementation Plan: Water Maintenance Tracking & AI Export Integration

**Branch**: `007-water-maintenance-tracking` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-water-maintenance-tracking/spec.md`

## Summary

Add a per-aquarium **Maintenance** page so reef keepers can document what Sabi's measurements cannot capture: how often and to which percentage water is changed, and whether activated carbon filtering runs. A standalone `maintenanceView.xhtml` (dosing-view pattern) records individual water change events (date + percentage 1–100 + optional note, back-dating allowed), an optional regular exchange interval (days + percentage) per tank, and the activated carbon setting (enabled; CONTINUOUS/INTERVAL mode; replacement interval 1–365 d). A cross-link section points to the existing Dosing feature for supplements (no new supplement model — Clarifications 2026-10-26). The collected data is added to the AI-JSON export (spec 001) as an always-present per-aquarium `maintenance` block, with `sabiSchemaVersion` bumped minor from `1.0` → `1.1`.

**Technical approach** (from [research.md](research.md)): two new tables (`aquarium_maintenance_profile` 1:1, `aquarium_water_change` N:1) with FK `ON DELETE CASCADE`; six tank-scoped REST endpoints under `/api/tank/{tankId}/maintenance…` following the dosing controller pattern (`@RequestScope`, `resolveOwnedAquarium`, 403 on non-owner incl. reads, Bean Validation with `@AssertTrue` cross-field rules); MapStruct mapper; DTOs in sabi-boundary; export via new `MaintenanceExportTo` DTOs + one-line schema bump. No new external dependencies, no WebSecurityConfig changes, no deployment changes.

## Technical Context

**Language/Version**: Java 25 (LTS, constitution VIII)  
**Primary Dependencies**: Spring Boot 4.1.1 (REST, Bean Validation, springdoc OpenAPI 3.x), JSF 2.3 + PrimeFaces 15.x via JoinFaces 6.1.2 (webclient), EclipseLink JPA + MariaDB, MapStruct 1.6.3, Jackson 3.x (`tools.jackson`) for the export  
**Storage**: MariaDB — two new tables `aquarium_maintenance_profile`, `aquarium_water_change` (Flyway migration `V1_8_0_15`, no schema prefix, FK cascade to `aquarium`)  
**Testing**: JUnit 5 + Mockito (service unit tests, tag `ServiceTest`), `@SpringBootTest` + Testcontainers + `@MockitoBean` (controller ITs, tag `ModuleTest`), Playwright E2E (`e2e/tests/maintenanceFlow.spec.ts`)  
**Target Platform**: Docker containers, ARM64 (production Raspberry Pi) + AMD64 (dev); webclient served by JoinFaces/Spring Boot  
**Project Type**: web-service (REST backend `sabi-server`) + JSF web application (`sabi-webclient`), shared DTOs in `sabi-boundary`, migrations in `sabi-database`  
**Performance Goals**: Maintenance page loads within the existing < 2 s page budget (constitution II); export generation stays within the existing ~3 s budget with up to 500 water change records per tank (SC-003)  
**Constraints**: No new external runtime dependencies; date-only precision for water changes; ownership check on every operation (403 incl. reads, SC-006); no PII in logs or export; additive-only export extension  
**Scale/Scope**: 2 tables, 6 REST endpoints, 1 new XHTML page + menu entry, ~20 i18n keys × 6 bundles, 2 export DTO trees; all existing views/endpoints unchanged except `DosingView.init()` (tank pre-selection via query param) and the export service/schema version

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | ISO 25010 Principle                               | Gate Question                                                       | Status |
|---|---------------------------------------------------|---------------------------------------------------------------------|--------|
| I | Funktionale Eignung (Functional Suitability)      | Issue/User-Story + Akzeptanzkriterien vorhanden?                    | ✅ — 4 user stories with acceptance scenarios (spec.md), requirements checklist fully green (checklists/requirements.md, 2026-10-25) |
| II | Leistungseffizienz (Performance Efficiency)      | Ressourcen-Impact auf ARM/Raspberry Pi bewertet?                    | ✅ — indexed queries `(aquarium_id, changed_on)`; export adds exactly 2 queries per tank (profile + records, no N+1); no new dependencies; page within existing < 2 s budget |
| III | Kompatibilität (Compatibility)                 | API-Rückwärtskompatibilität und OpenAPI-Docs sichergestellt?         | ✅ — export extension additive only, minor bump 1.0 → 1.1 (Clarification #4); new endpoints documented via springdoc `@Operation`/`@ApiResponses`; no breaking changes to existing endpoints |
| IV | Benutzbarkeit (Usability)                        | i18n (DE/EN) + Validierung + WCAG 2.1 AA berücksichtigt?            | ✅ — all new keys in 6 bundles (FR-017, SC-005); client + server validation with localised messages; save button `#065f46` (8:1) / links per UI_StyleGuide; recording a water change ≤ 3 interactions (SC-001) |
| V | Zuverlässigkeit (Reliability)                    | Tests grün, Fehlerbehandlung + Transaktionssicherheit geplant?      | ✅ — `@Transactional` services, optimistic locking (`optlock`, 409), transactional cascade on tank delete; unit + IT + E2E tests planned per user story (NFR Reliability) |
| VI | Sicherheit (Security)                           | Auth/Authz, keine Secrets im Code, OWASP-Scan geplant?              | ✅ — JWT on all endpoints; `resolveOwnedAquarium` ownership check incl. reads (SC-006); 403 for non-owners; PII-safe logging (`aquarium_id=` only) and no PII in export (FR-014); no new dependencies ⇒ existing OWASP scan unchanged |
| VII | Wartbarkeit (Maintainability)                  | Modulare Architektur + Flyway + JUnit-Tests eingeplant?              | ✅ — boundary/server/webclient/database module split respected; single new Flyway migration `V1_8_0_15` (existing scripts immutable); JUnit tests for every P1–P4 story |
| VIII | Übertragbarkeit (Portability)                 | ARM + AMD64 kompatibel, Docker/Ansible-Deployment geprüft?          | ✅ — no new runtime dependencies, no platform-specific code; Docker images and Ansible playbooks unchanged (spec NFR Portability) |

**Post-Phase-1 re-evaluation**: all gates remain ✅ — the design (two tables, six endpoints, DTO-based export, lazy profile) introduces no new violations.

**Violations requiring justification**: none.

## Project Structure

### Documentation (this feature)

```text
specs/007-water-maintenance-tracking/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output — decisions D1–D12, all unknowns resolved
├── data-model.md        # Phase 1 output — entities, TOs, validation, mapper
├── quickstart.md        # Phase 1 output — runnable validation scenarios
├── contracts/
│   ├── maintenance-api.md            # Phase 1 output — 6 REST endpoints
│   └── ai-export-maintenance-block.md # Phase 1 output — export block + schema bump
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
sabi-database/src/main/resources/db/migration/version1_8_0/
└── V1_8_0_15__addAquariumMaintenanceTables.sql          # NEW: both tables, FK ON DELETE CASCADE

sabi-boundary/src/main/java/de/bluewhale/sabi/model/
├── CarbonMode.java                  # NEW enum CONTINUOUS | INTERVAL (@JsonValue/@JsonCreator)
├── WaterChangeTo.java               # NEW TO + Bean Validation (not-in-future @AssertTrue)
├── WaterChangeIntervalTo.java       # NEW TO — body of PUT …/water-change-interval
├── ActivatedCarbonTo.java           # NEW TO — body of PUT …/activated-carbon
├── MaintenanceProfileTo.java        # NEW TO — profile view (GET response part)
├── MaintenanceTo.java               # NEW TO — GET response: profile + waterChanges[]
├── MaintenanceExportTo.java         # NEW export DTO (+ nested WaterChanges/RegularInterval/Record/ActivatedCarbon ExportTo)
├── ReefDataExportTo.java            # CHANGED: SCHEMA_VERSION "1.0" → "1.1"
└── AquariumExportTo.java            # CHANGED: + field `maintenance` (always present)

sabi-server/src/main/java/de/bluewhale/sabi/
├── persistence/model/
│   ├── TankMaintenanceProfileEntity.java      # NEW extends Auditable (table aquarium_maintenance_profile)
│   └── WaterChangeEntity.java                 # NEW extends Auditable (table aquarium_water_change)
├── persistence/repositories/
│   ├── TankMaintenanceProfileRepository.java  # NEW: findByAquariumId, save
│   └── WaterChangeRepository.java             # NEW: findByAquariumIdOrderByChangedOnDesc, findByIdAndAquariumId
├── mapper/MaintenanceMapper.java              # NEW MapStruct (DosingMapper pattern)
├── services/
│   ├── MaintenanceService.java                # NEW interface
│   ├── MaintenanceServiceImpl.java            # NEW: resolveOwnedAquarium, upsert semantics, normalize/trim
│   └── ReefDataExportServiceImpl.java         # CHANGED: + buildMaintenanceExport(aquariumId) in buildAquariumExport
└── rest/controller/MaintenanceController.java # NEW @RequestScope REST, 6 endpoints, OpenAPI annotations

sabi-server/src/test/java/de/bluewhale/sabi/
├── services/MaintenanceServiceTest.java       # NEW (Mockito, tag ServiceTest)
├── rest/controller/MaintenanceControllerIT.java # NEW (@MockitoBean repos, tag ModuleTest)
├── rest/controller/UserProfileControllerTest.java # CHANGED: T030 "1.0"→"1.1"; T031 + maintenance key
└── services/ReefDataExportServiceTest.java    # CHANGED: + maintenance round-trip test (incl. FR-016 defaults)

sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/
├── controller/MaintenanceView.java            # NEW bean (@Named @RequestScope extends AbstractControllerTools, viewMap tank selection)
├── apigateway/MaintenanceService.java         # NEW gateway interface
├── apigateway/MaintenanceServiceImpl.java     # NEW extends APIServiceImpl (URI via Endpoint.TANKS + "/maintenance…")
├── utils/PageRegister.java                    # CHANGED: + MAINTENANCE_VIEW_PAGE("maintenanceView")
└── controller/DosingView.java                 # CHANGED: init() accepts validated `aquariumId` request parameter (cross-link pre-selection)

sabi-webclient/src/main/resources/
├── META-INF/resources/secured/maintenanceView.xhtml  # NEW page (3 sections, dosingView layout pattern)
├── META-INF/resources/template/header.xhtml          # CHANGED: + p:menuitem (pi pi-tint) after dosing entry
└── i18n/messages.properties + messages_{de,en,es,fr,it}.properties  # CHANGED: + maintenance.* keys (all 6)

e2e/tests/maintenanceFlow.spec.ts              # NEW Playwright E2E (coralStockFlow pattern, real clicks only)
```

**Structure Decision**: Existing modular layout is extended in place — no new modules, no new directories beyond the established package conventions. Backend follows the dosing feature's layering (entity → repository → MapStruct mapper → service with ownership helper → `@RequestScope` controller), webclient follows the dosing-view pattern (gateway impl + request-scoped view bean + XHTML under `secured/`). The export change is confined to sabi-boundary DTOs + one builder method in `ReefDataExportServiceImpl`.

## Complexity Tracking

> No Constitution Check violations — this section is intentionally empty (no justification needed).
