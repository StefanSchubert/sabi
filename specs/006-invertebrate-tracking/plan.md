# Implementation Plan: Invertebrate Stock Management & Catalogue

**Branch**: `006-invertebrate-tracking` | **Date**: 2026-05-30 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/006-invertebrate-tracking/spec.md`

## Summary

Introduce **Tank Invertebrate Stock** and **Invertebrate Catalogue** to the Sabi aquarium management platform — fully symmetric with the existing Fish Stock (spec 002) and Coral Stock (spec 005) features. The implementation follows the established five-layer pattern (Flyway migration → JPA entity → Service → REST controller → JSF/PrimeFaces view), adding four new DB tables, four new enums, twelve new REST endpoints, and a new aquarium-detail tab. Invertebrate data is additionally wired into the existing AI-JSON export and House Reef Report, each requiring a small extension to their respective service layers and TOs.

---

## Technical Context

**Language/Version**: Java 25 (LTS), Spring Boot 4  
**Primary Dependencies**: Spring MVC, EclipseLink JPA, Flyway, springdoc-v2 (OpenAPI 3.x), Lombok, Jakarta Validation 3.x, JSF 2.3 + PrimeFaces 15.x  
**Storage**: MariaDB 10.x (`sabi` schema), Flyway migrations under `sabi-database/src/main/resources/db/migration/version1_8_0/`  
**Testing**: JUnit 5 + Spring Boot Test (integration tests); existing tests must remain green  
**Target Platform**: Linux/ARM (Raspberry Pi production), AMD64 Docker (dev/CI)  
**Project Type**: Multi-module Maven web service + server-side JSF frontend  
**Performance Goals**: Catalogue search ≤ 1 s for up to 500 entries (FR-028); unit multi-select populates ≤ 1 s; AI export: no perceptible regression vs. equivalent fish/coral payload  
**Constraints**: No breaking changes to existing REST API endpoints; photo storage on existing configurable filesystem volume (≤ 5 MB per file); JWT Bearer auth required for all write endpoints; no new external service dependencies  
**Scale/Scope**: Same user base / data volumes as fish and coral stock; ~4 new DB tables; ~12 new REST endpoints; ~8 new JSF views/fragments; 6 i18n message-bundle files updated

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | ISO 25010 Principle                               | Gate Question                                                        | Status |
|---|---------------------------------------------------|----------------------------------------------------------------------|--------|
| I | Funktionale Eignung (Functional Suitability)      | Issue/User-Story + Akzeptanzkriterien vorhanden?                     | ✅ 8 user stories with acceptance scenarios; 36 FRs defined in spec.md |
| II | Leistungseffizienz (Performance Efficiency)      | Ressourcen-Impact auf ARM/Raspberry Pi bewertet?                     | ✅ No new background threads or bulk queries; catalogue search indexed; water-sensitivity join bounded by existing unit table (~50 rows) |
| III | Kompatibilität (Compatibility)                 | API-Rückwärtskompatibilität und OpenAPI-Docs sichergestellt?          | ✅ All new endpoints are additions; `AquariumExportTo.invertebrates` and `PublicReportLinkTo.includeInvertebrates` are backward-compatible new fields; OpenAPI via springdoc-v2 |
| IV | Benutzbarkeit (Usability)                        | i18n (DE/EN) + Validierung + WCAG 2.1 AA berücksichtigt?             | ✅ 6 bundle files (DE/EN/ES/FR/IT + fallback); client + server validation; WCAG 2.1 AA on all new UI elements |
| V | Zuverlässigkeit (Reliability)                    | Tests grün, Fehlerbehandlung + Transaktionssicherheit geplant?       | ✅ @Transactional on service methods; departure date < entry date → 422; photo > 5 MB → 400 with message; at least one integration test per P1–P6 user story |
| VI | Sicherheit (Security)                           | Auth/Authz, keine Secrets im Code, OWASP-Scan geplant?               | ✅ All write endpoints require JWT Bearer; strict user isolation (FR-011); admin-only catalogue approval (FR-024/025); no new secrets |
| VII | Wartbarkeit (Maintainability)                  | Modulare Architektur + Flyway + JUnit-Tests eingeplant?               | ✅ Follows `sabi-boundary | sabi-server | sabi-webclient | sabi-database` module split; Flyway version `1_8_0`; JUnit integration tests per user story |
| VIII | Übertragbarkeit (Portability)                 | ARM + AMD64 kompatibel, Docker/Ansible-Deployment geprüft?           | ✅ Photo storage on existing configured volume; no platform-specific code; existing Docker / Ansible pipeline unchanged |

**Violations requiring justification**: None.

---

## Project Structure

### Documentation (this feature)

```text
specs/006-invertebrate-tracking/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── invertebrate-api.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created here)
```

### Source Code Layout

```text
# sabi-database  (DB migrations — version 1.8.0)
sabi-database/src/main/resources/db/migration/version1_8_0/
├── V1_8_0_1__addInvertebrateCatalogueTable.sql
├── V1_8_0_2__addInvertebrateCatalogueI18nTable.sql
├── V1_8_0_3__addInvertebrateStockTable.sql
├── V1_8_0_4__addInvertebrateWaterSensitivityTable.sql
├── V1_8_0_5__addInvertebratePhotoTable.sql
└── V1_8_0_6__addIncludeInvertebratesToPublicReportLink.sql

# sabi-boundary  (Transfer Objects & enums — shared between server and webclient)
sabi-boundary/src/main/java/de/bluewhale/sabi/model/
├── InvertebrateTaxonomicCategory.java      # enum: CRUSTACEAN | MOLLUSC | ECHINODERM | WORM
├── InvertebrateMobility.java               # enum: MOBILE | SESSILE
├── InvertebrateEcologicalRole.java         # enum: CLEANUP_CREW | NEUTRAL | DETRIMENTAL
├── InvertebrateActivityPattern.java        # enum: DIURNAL | NOCTURNAL | BOTH
├── InvertebrateStockEntryTo.java           # CRUD TO for invertebrate stock entries
├── InvertebrateDepartureRecordTo.java      # Departure record TO
├── InvertebrateCatalogueEntryTo.java       # Catalogue entry TO (proposal/full view)
├── InvertebrateCatalogueI18nTo.java        # i18n fields per language
├── InvertebrateCatalogueSearchResultTo.java
├── InvertebrateExportTo.java               # AI-JSON export TO
├── PublicReefReportInvertebrateTo.java     # House Reef Report TO
└── AquariumExportTo.java                   # EXTEND: add List<InvertebrateExportTo> invertebrates
    PublicReefReportTo.java                 # EXTEND: add List<PublicReefReportInvertebrateTo> invertebrateInhabitants
    PublicReportLinkTo.java                 # EXTEND: add boolean includeInvertebrates

# sabi-server  (JPA entities, repositories, services, REST controllers)
sabi-server/src/main/java/de/bluewhale/sabi/
├── persistence/model/
│   ├── InvertebrateCatalogueEntity.java
│   ├── InvertebrateCatalogueI18nEntity.java
│   ├── TankInvertebrateStockEntity.java
│   ├── InvertebrateWaterSensitivityEntity.java
│   └── InvertebratePhotoEntity.java
├── persistence/repositories/
│   ├── InvertebrateCatalogueRepository.java
│   ├── InvertebrateCatalogueI18nRepository.java
│   ├── TankInvertebrateStockRepository.java
│   ├── InvertebrateWaterSensitivityRepository.java
│   └── InvertebratePhotoRepository.java
├── mapper/
│   ├── InvertebrateStockMapper.java
│   └── InvertebrateCatalogueMapper.java
├── services/
│   ├── InvertebrateStockService.java          # interface
│   ├── InvertebrateStockServiceImpl.java
│   ├── InvertebrateCatalogueService.java      # interface
│   ├── InvertebrateCatalogueServiceImpl.java
│   └── InvertebrateStockExceptionCodes.java
└── rest/controller/
    ├── InvertebrateStockController.java
    ├── InvertebrateCatalogueController.java
    └── InvertebrateCatalogueAdminController.java

# sabi-server  (extend existing services for AI export + public report)
sabi-server/src/main/java/de/bluewhale/sabi/services/
├── UserProfileServiceImpl.java             # EXTEND: add invertebrates[] to AI export
└── PublicReportServiceImpl.java            # EXTEND: add invertebrateInhabitants + include-invertebrates flag

# sabi-webclient  (JSF views + backing beans + API gateway)
sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/
├── controller/
│   ├── InvertebrateStockView.java
│   ├── InvertebrateStockEntryView.java
│   ├── InvertebrateDepartureView.java
│   ├── InvertebrateEntryNavContext.java
│   ├── InvertebrateCatalogueProposalView.java
│   └── InvertebrateCatalogueAdminView.java
├── converter/
│   └── InvertebrateCatalogueSearchResultConverter.java
└── apigateway/
    ├── InvertebrateStockService.java         # interface
    ├── InvertebrateStockServiceImpl.java     # REST calls to sabi-server
    ├── InvertebrateCatalogueService.java     # interface
    └── InvertebrateCatalogueServiceImpl.java

sabi-webclient/src/main/resources/META-INF/resources/secured/
├── invertebrateStockTab.xhtml
├── invertebrateStockEntryPage.xhtml
├── invertebrateDepartureForm.xhtml
├── invertebrateCatalogueProposalForm.xhtml
├── invertebrateCatalogueI18nFields.xhtml
└── admin/
    └── invertebrateCatalogueAdminView.xhtml

sabi-webclient/src/main/resources/i18n/
├── messages.properties         # fallback + 5 language files — add new keys
├── messages_de.properties
├── messages_en.properties
├── messages_es.properties
├── messages_fr.properties
└── messages_it.properties
```

**Structure Decision**: Multi-module Maven layout exactly mirrors the fish (002) and coral (005) patterns across all four modules (`sabi-boundary`, `sabi-server`, `sabi-database`, `sabi-webclient`). Flyway version `1_8_0` is the next sequential version after `1_7_0` (coral stock). No new modules are introduced.

---

## Complexity Tracking

> No Constitution Check violations. No additional complexity justification required.
