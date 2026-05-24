# Implementation Plan: Coral Stock Management & Coral Catalogue

**Branch**: `005-coral-stock` | **Date**: 2026-05-22 | **Spec**: [specs/005-coral-stock/spec.md](./spec.md)  
**Input**: Feature specification from `/specs/005-coral-stock/spec.md`

---

## Summary

Add full coral stock management to Sabi — the marine-aquarium equivalent of the existing fish stock feature (spec 002). Users can document which corals live in each of their saltwater tanks, record regular growth measurements (surface area, size, volume, branch count) and polyp health observations (vital, tissue loss, pale, limp, significant growth), upload a photo per coral, and link corals to a community-maintained coral catalogue with the same UGC proposal/approval workflow as the fish catalogue. Coral data is wired into the existing AI-JSON export and the House Reef Report (behind an opt-in flag). All 42 functional requirements from the spec are addressed.

---

## Technical Context

**Language/Version**: Java 21 (constitution lists Java 25 as target; project POM targets Java 21 — stay on what the project builds)  
**Primary Dependencies**: Spring Boot 3.x, JSF 2.3 / PrimeFaces 15.x (JoinFaces), EclipseLink JPA, MariaDB 10.x, Flyway, Lombok, springdoc-openapi-v2  
**Storage**: MariaDB (sabi schema); coral photos on configurable filesystem volume (`sabi.photo.dir/coral/`)  
**Testing**: JUnit 5, Spring Test, Testcontainers (MariaDB), Playwright (E2E)  
**Target Platform**: Linux ARM64 (Raspberry Pi production), AMD64 (development)  
**Project Type**: Multi-module Maven web service + web client  
**Performance Goals**: Catalogue search ≤ 1 s for 500 entries (FR-030); API endpoints ≤ 2 s (constitution Principle II)  
**Constraints**: No new external runtime service dependencies (C-6); no breaking changes to existing APIs (constitution Principle III); i18n all 6 files mandatory (FR-037)  
**Scale/Scope**: Single marine tank-per-user; up to 200 growth records per coral without perceptible delay (NFR)

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | ISO 25010 Principle | Gate Question | Status |
|---|---------------------|---------------|--------|
| I | Funktionale Eignung | Issue/User-Story + Akzeptanzkriterien vorhanden? | ✅ spec.md contains 9 user stories with acceptance criteria and 42 FRs |
| II | Leistungseffizienz | Ressourcen-Impact auf ARM/Raspberry Pi bewertet? | ✅ Catalogue uses FULLTEXT + partial-unique index; foreign-key indexes on all join columns; growth/polyp fetched lazily |
| III | Kompatibilität | API-Rückwärtskompatibilität und OpenAPI-Docs sichergestellt? | ✅ New `/api/coral/*` endpoints don't touch existing ones; `PublicReefReportTo` and `AquariumExportTo` additions are additive (null/empty defaults) |
| IV | Benutzbarkeit | i18n (DE/EN) + Validierung + WCAG 2.1 AA berücksichtigt? | ✅ 110 new keys, all 6 files; client + server validation; WCAG check planned; coral tab guarded by `waterType == MARINE` |
| V | Zuverlässigkeit | Tests grün, Fehlerbehandlung + Transaktionssicherheit geplant? | ✅ Integration tests per user story; JPA `@Transactional` on all service write ops; departure/date validation double-enforced |
| VI | Sicherheit | Auth/Authz, keine Secrets im Code, OWASP-Scan geplant? | ✅ All write ops require JWT; `findByIdAndUserId` ownership on every mutation; admin-only catalogue approval; photo magic-byte validation |
| VII | Wartbarkeit | Modulare Architektur + Flyway + JUnit-Tests eingeplant? | ✅ 7 Flyway migrations in `version1_7_0/`; boundary / server / webclient split maintained; JUnit per service |
| VIII | Übertragbarkeit | ARM + AMD64 kompatibel, Docker/Ansible-Deployment geprüft? | ✅ No platform-specific code; same volume mount for coral photos as fish photos; redeploy via `server_redeploy.sh --boundary --flyway` |

**Post-Phase-1 constitution re-check**: All gates remain ✅. No DDL changes break existing tables. `CoralExportTo` extension is source-compatible. `PublicReefReportTo.coralInhabitants` defaults to `null` (not opted-in) — existing callers unaffected.

---

## Project Structure

### Documentation (this feature)

```text
specs/005-coral-stock/
├── plan.md              ← This file
├── spec.md              ← Feature specification
├── research.md          ← Phase 0: all decisions resolved
├── data-model.md        ← Phase 1: entities, enums, TOs, migrations
├── quickstart.md        ← Phase 1: developer entry guide
├── contracts/
│   ├── rest-api.md      ← REST endpoint contracts
│   └── i18n-keys.md     ← All ~110 new i18n keys
└── tasks.md             ← Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code

```text
sabi-boundary/src/main/java/de/bluewhale/sabi/model/
├── CoralDepartureReason.java          [NEW enum]
├── CoralClassification.java           [NEW enum]
├── CoralCareLevel.java                [NEW enum]
├── CoralGrowthType.java               [NEW enum]
├── PolypCondition.java                [NEW enum]
├── CoralStockEntryTo.java             [NEW TO]
├── CoralGrowthHistoryTo.java          [NEW TO]
├── CoralPolypConditionTo.java         [NEW TO]
├── CoralDepartureRecordTo.java        [NEW TO]
├── CoralCatalogueEntryTo.java         [NEW TO]
├── CoralCatalogueI18nTo.java          [NEW TO]
├── CoralCatalogueSearchResultTo.java  [NEW TO]
├── CoralExportTo.java                 [EXTEND existing stub]
├── CoralGrowthHistoryExportTo.java    [NEW TO]
├── CoralPolypConditionExportTo.java   [NEW TO]
├── PublicReefReportCoralTo.java       [NEW TO]
├── PublicReefReportTo.java            [MODIFY — add coralInhabitants]
└── PublicReportLinkTo.java            [MODIFY — add includeCorals]

sabi-database/src/main/resources/db/migration/version1_7_0/
├── V1_7_0_1__addCoralCatalogueTable.sql
├── V1_7_0_2__addCoralCatalogueI18nTable.sql
├── V1_7_0_3__addCoralStockTable.sql
├── V1_7_0_4__addCoralGrowthHistoryTable.sql
├── V1_7_0_5__addCoralPolypConditionTable.sql
├── V1_7_0_6__addCoralPhotoTable.sql
└── V1_7_0_7__addIncludeCoralsToPublicReportLink.sql

sabi-server/src/main/java/de/bluewhale/sabi/
├── persistence/model/
│   ├── TankCoralStockEntity.java      [NEW]
│   ├── CoralGrowthHistoryEntity.java  [NEW]
│   ├── CoralPolypConditionEntity.java [NEW]
│   ├── CoralCatalogueEntity.java      [NEW]
│   ├── CoralCatalogueI18nEntity.java  [NEW]
│   ├── CoralPhotoEntity.java          [NEW]
│   └── PublicReportLinkEntity.java    [MODIFY — add includeCorals column]
├── persistence/repositories/
│   ├── TankCoralStockRepository.java  [NEW — must have findByIdAndUserId()]
│   ├── CoralGrowthHistoryRepository.java [NEW]
│   ├── CoralPolypConditionRepository.java [NEW]
│   ├── CoralCatalogueRepository.java  [NEW]
│   ├── CoralCatalogueI18nRepository.java [NEW]
│   └── CoralPhotoRepository.java      [NEW]
├── mapper/
│   ├── CoralStockMapper.java          [NEW]
│   └── CoralCatalogueMapper.java      [NEW]
├── services/
│   ├── CoralStockService.java         [NEW interface]
│   ├── CoralStockServiceImpl.java     [NEW]
│   ├── CoralStockMessageCodes.java    [NEW]
│   ├── CoralCatalogueService.java     [NEW interface]
│   ├── CoralCatalogueServiceImpl.java [NEW]
│   └── CoralCatalogueMessageCodes.java [NEW]
│   ├── ReefDataExportServiceImpl.java [MODIFY — populate corals[]]
│   └── PublicReportServiceImpl.java   [MODIFY — populate coralInhabitants]
└── rest/controller/
    ├── CoralStockController.java      [NEW — api/coral]
    ├── CoralCatalogueController.java  [NEW — api/coral/catalogue]
    └── CoralCatalogueAdminController.java [NEW — api/admin/coral/catalogue]

sabi-server/src/test/java/de/bluewhale/sabi/
├── persistence/
│   ├── TankCoralStockRepositoryTest.java    [NEW]
│   └── CoralCatalogueRepositoryTest.java    [NEW]
├── services/
│   ├── CoralStockServiceTest.java           [NEW]
│   ├── CoralCatalogueServiceTest.java       [NEW]
│   └── CoralCataloguePerformanceTest.java   [NEW — FR-030 ≤ 1s for 500 entries]
└── rest/controller/
    ├── CoralStockControllerTest.java        [NEW]
    ├── CoralCatalogueControllerTest.java    [NEW]
    └── CoralCatalogueAdminControllerTest.java [NEW]

sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/
├── apigateway/
│   ├── CoralStockService.java               [NEW interface]
│   ├── CoralStockServiceImpl.java           [NEW — RestTemplate]
│   ├── CoralCatalogueService.java           [NEW interface]
│   ├── CoralCatalogueServiceImpl.java       [NEW]
│   ├── CoralCatalogueAdminService.java      [NEW interface]
│   └── CoralCatalogueAdminServiceImpl.java  [NEW]
├── controller/
│   ├── CoralStockView.java                  [NEW — @RequestScope list view]
│   ├── CoralStockEntryView.java             [NEW — @RequestScope add/edit]
│   ├── CoralEntryNavContext.java            [NEW — @SessionScoped]
│   ├── CoralDepartureView.java              [NEW — @RequestScope]
│   ├── CoralGrowthHistoryView.java          [NEW — @RequestScope + chart toggle]
│   ├── CoralPolypConditionView.java         [NEW — @RequestScope]
│   ├── CoralCatalogueProposalView.java      [NEW — @RequestScope]
│   └── CoralCatalogueAdminView.java         [NEW — @RequestScope]
├── converter/
│   └── CoralCatalogueSearchResultConverter.java [NEW]
└── rest/
    └── CoralPhotoController.java            [NEW — multipart upload relay]

sabi-webclient/src/main/resources/META-INF/resources/
├── secured/
│   ├── coralStockTab.xhtml              [NEW — tab include, MARINE-only]
│   ├── coralStockView.xhtml             [NEW — full list page]
│   ├── coralStockEntryPage.xhtml        [NEW — standalone add/edit page]
│   ├── coralStockEntryForm.xhtml        [NEW — form include]
│   ├── coralDepartureForm.xhtml         [NEW — standalone departure page]
│   ├── coralGrowthHistoryView.xhtml     [NEW — table + chart toggle]
│   ├── coralPolypConditionView.xhtml    [NEW — condition history table]
│   ├── coralCatalogueProposalForm.xhtml [NEW — propose/edit catalogue entry]
│   └── coralCatalogueI18nFields.xhtml   [NEW — reusable i18n field component]
└── secured/admin/
    └── coralCatalogueAdminView.xhtml    [NEW]

sabi-webclient/src/main/resources/i18n/
├── messages.properties          [MODIFY — ~110 new keys]
├── messages_de.properties       [MODIFY]
├── messages_en.properties       [MODIFY]
├── messages_es.properties       [MODIFY]
├── messages_fr.properties       [MODIFY]
└── messages_it.properties       [MODIFY]

e2e/tests/
├── coralStockFlow.spec.ts       [NEW — US1–US4]
├── coralCatalogueFlow.spec.ts   [NEW — US5–US7]
├── coralReportFlow.spec.ts      [NEW — US8]
└── coralExportFlow.spec.ts      [NEW — US9]
```

---

## Architecture Overview

The coral stock feature follows the strict module separation already established by `FishStockController` / `FishStockService` / `TankFishStockEntity`:

```
Browser (JSF/Facelets)
    ↓  AJAX / form POST
CoralStockView / CoralStockEntryView (sabi-webclient, @RequestScope CDI)
    ↓  RestTemplate + Bearer JWT
CoralStockController (sabi-server, @RestController api/coral)
    ↓  @Autowired
CoralStockServiceImpl (sabi-server, @Service + @Transactional)
    ↓  JPA
TankCoralStockRepository → coral_stock table (MariaDB)
```

Catalogue path:
```
CoralCatalogueProposalView → CoralCatalogueController (api/coral/catalogue)
                           → CoralCatalogueAdminController (api/admin/coral/catalogue)
                           → CoralCatalogueServiceImpl → coral_catalogue / coral_catalogue_i18n
```

Export integrations:
```
ReefDataExportServiceImpl.buildExport()
  → loads coral stock per aquarium via CoralStockService
  → populates AquariumExportTo.corals (already in AquariumExportTo)

PublicReportServiceImpl.buildReport(token)
  → checks publicReportLink.includeCorals
  → if true: loads active corals + latest snapshots → PublicReefReportTo.coralInhabitants
```

---

## Database Schema Details

### V1_7_0_1 — coral_catalogue

```sql
CREATE TABLE `coral_catalogue` (
    `id`                   BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `status`               VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
                               COMMENT 'PENDING | PUBLIC | REJECTED',
    `proposer_user_id`     BIGINT(20) UNSIGNED NULL,
    `proposal_date`        DATE         NULL,
    `scientific_name`      VARCHAR(255) NOT NULL,
    `classification`       VARCHAR(5)   NOT NULL COMMENT 'LPS | SPS',
    `care_level`           VARCHAR(12)  NOT NULL COMMENT 'EASY | MODERATE | DEMANDING',
    `active_scientific_name` VARCHAR(255) GENERATED ALWAYS AS (
                               IF(`status` IN ('PENDING','PUBLIC'), `scientific_name`, NULL)
                           ) VIRTUAL,
    `created_on`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`              INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uq_coral_catalogue_active_name` (`active_scientific_name`),
    INDEX `idx_coral_catalogue_status` (`status`),
    INDEX `idx_coral_catalogue_proposer` (`proposer_user_id`),
    CONSTRAINT `fk_coral_catalogue_proposer`
        FOREIGN KEY (`proposer_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
```

### V1_7_0_2 — coral_catalogue_i18n

```sql
CREATE TABLE `coral_catalogue_i18n` (
    `id`            BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `catalogue_id`  BIGINT(20) UNSIGNED NOT NULL,
    `language_code` VARCHAR(2)          NOT NULL COMMENT 'de | en | es | fr | it',
    `common_name`   VARCHAR(255)        NULL,
    `description`   TEXT                NULL     COMMENT 'max 2000 chars enforced at app layer',
    `reference_url` VARCHAR(512)        NULL,
    `created_on`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`       INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_coral_catalogue_i18n_lang` (`catalogue_id`, `language_code`),
    FULLTEXT INDEX `ft_coral_i18n_name` (`common_name`),
    CONSTRAINT `fk_coral_i18n_entry`
        FOREIGN KEY (`catalogue_id`) REFERENCES `coral_catalogue` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
```

### V1_7_0_3 — coral_stock

```sql
CREATE TABLE `coral_stock` (
    `id`                 BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`        BIGINT(20) UNSIGNED NOT NULL,
    `user_id`            BIGINT(20) UNSIGNED NOT NULL,
    `coral_catalogue_id` BIGINT(20) UNSIGNED NULL,
    `species_name`       VARCHAR(255) NOT NULL,
    `scientific_name`    VARCHAR(255) NULL,
    `classification`     VARCHAR(5)   NULL  COMMENT 'LPS | SPS; snapshot at link time',
    `care_level`         VARCHAR(12)  NULL  COMMENT 'snapshot',
    `external_ref_url`   VARCHAR(512) NULL,
    `notes`              TEXT         NULL,
    `added_on`           DATE         NOT NULL,
    `departed_on`        DATE         NULL,
    `departure_reason`   VARCHAR(30)  NULL
                             COMMENT 'DIED | SOLD | GIVEN_AWAY | MOVED_TO_OTHER_TANK | OTHER',
    `departure_note`     TEXT         NULL  COMMENT 'max 500 chars enforced at app layer',
    `deleted_at`         TIMESTAMP    NULL  DEFAULT NULL COMMENT 'Soft-delete',
    `created_on`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`            INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_cs_aquarium` (`aquarium_id`),
    INDEX `idx_cs_user` (`user_id`),
    INDEX `idx_cs_deleted_at` (`deleted_at`),
    CONSTRAINT `fk_cs_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cs_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    CONSTRAINT `fk_cs_catalogue`
        FOREIGN KEY (`coral_catalogue_id`) REFERENCES `coral_catalogue` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
```

### V1_7_0_4 — coral_growth_history

```sql
CREATE TABLE `coral_growth_history` (
    `id`                BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `coral_stock_id`    BIGINT(20) UNSIGNED NOT NULL,
    `measured_on`       DATE                NOT NULL,
    `measurement_type`  VARCHAR(30)         NOT NULL
                            COMMENT 'SURFACE_AREA_CM2 | SIZE_CM | VOLUME_CM3 | BRANCH_COUNT',
    `measurement_value` DECIMAL(8,1)        NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_cgh_coral` (`coral_stock_id`),
    CONSTRAINT `fk_cgh_coral`
        FOREIGN KEY (`coral_stock_id`) REFERENCES `coral_stock` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
```

### V1_7_0_5 — coral_polyp_condition

```sql
CREATE TABLE `coral_polyp_condition` (
    `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `coral_stock_id` BIGINT(20) UNSIGNED NOT NULL,
    `observed_on`    DATE                NOT NULL,
    `condition`      VARCHAR(30)         NOT NULL
                         COMMENT 'VITAL | TISSUE_LOSS | PALE | LIMP | SIGNIFICANT_GROWTH',
    PRIMARY KEY (`id`),
    INDEX `idx_cpc_coral` (`coral_stock_id`),
    CONSTRAINT `fk_cpc_coral`
        FOREIGN KEY (`coral_stock_id`) REFERENCES `coral_stock` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
```

### V1_7_0_6 — coral_photo

```sql
CREATE TABLE `coral_photo` (
    `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `coral_stock_id` BIGINT(20) UNSIGNED NOT NULL,
    `file_path`      VARCHAR(512)        NOT NULL,
    `content_type`   VARCHAR(50)         NOT NULL,
    `upload_date`    DATE                NOT NULL,
    `created_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`        INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_coral_photo_stock` (`coral_stock_id`),
    CONSTRAINT `fk_cp_coral_stock`
        FOREIGN KEY (`coral_stock_id`) REFERENCES `coral_stock` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
```

### V1_7_0_7 — extend public_report_link

```sql
ALTER TABLE `public_report_link`
    ADD COLUMN `include_corals` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'When 1, currently-present corals are included in the public report';
```

---

## sabi-boundary Changes

### New Enums

| Class | Values | Notes |
|-------|--------|-------|
| `CoralDepartureReason` | DIED, SOLD, GIVEN_AWAY, MOVED_TO_OTHER_TANK, OTHER | New — coral-specific; distinct from fish `DepartureReason` |
| `CoralClassification` | LPS, SPS | New |
| `CoralCareLevel` | EASY, MODERATE, DEMANDING | New |
| `CoralGrowthType` | SURFACE_AREA_CM2, SIZE_CM, VOLUME_CM3, BRANCH_COUNT | New |
| `PolypCondition` | VITAL, TISSUE_LOSS, PALE, LIMP, SIGNIFICANT_GROWTH | New |

All enums: `@JsonValue` / `@JsonCreator` annotations — mirror `FishCatalogueStatus` pattern.

### New Transfer Objects (summary)

| Class | Key Fields | Analogous To |
|-------|-----------|--------------|
| `CoralStockEntryTo` | speciesName, addedOn, departedOn, CoralDepartureReason, growthHistory, polypConditionHistory | `FishStockEntryTo` |
| `CoralGrowthHistoryTo` | measuredOn, CoralGrowthType (immutable), BigDecimal measurementValue | `FishSizeHistoryTo` |
| `CoralPolypConditionTo` | observedOn, PolypCondition | — |
| `CoralDepartureRecordTo` | departureDate, CoralDepartureReason, @Size(max=500) departureNote | `FishDepartureRecordTo` |
| `CoralCatalogueEntryTo` | scientificName, CoralClassification, CoralCareLevel, FishCatalogueStatus, i18nEntries | `FishCatalogueEntryTo` |
| `CoralCatalogueI18nTo` | languageCode, commonName, @Size(max=2000) description, referenceUrl | `FishCatalogueI18nTo` |
| `CoralCatalogueSearchResultTo` | id, scientificName, commonName, CoralClassification, CoralCareLevel, referenceUrl, FishCatalogueStatus | `FishCatalogueSearchResultTo` |
| `CoralExportTo` | coralCatalogueId, scientificName, speciesName, classification, addedOn, departedOn, departureReason, departureNote, notes, growthHistory, polypConditionHistory | extend existing stub |
| `CoralGrowthHistoryExportTo` | measuredOn (String), measurementType (String), measurementValue (BigDecimal) | `FishSizeHistoryExportTo` |
| `CoralPolypConditionExportTo` | observedOn (String), condition (String) | — |
| `PublicReefReportCoralTo` | speciesName, classification, latestGrowthByType (Map), latestPolypCondition | — |

### Modified Existing Classes

| Class | Change |
|-------|--------|
| `PublicReefReportTo` | Add `List<PublicReefReportCoralTo> coralInhabitants` (null when not opted-in) |
| `PublicReportLinkTo` | Add `boolean includeCorals = false` |

---

## sabi-server Changes

### JPA Entities

All new entities extend `Auditable` (for `created_on`, `lastmod_on`, `optlock`).

**`TankCoralStockEntity`**:
- `@Table(name = "coral_stock", schema = "sabi")`
- `@SQLRestriction("deleted_at IS NULL")` — transparent soft-delete filter
- `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") UserEntity user` — ownership
- Named query `TankCoralStock.getCoralsByAquariumAndUser` (analogous to `TankFishStock.getUsersFish`)
- `@NamedQuery` for ownership-scoped lookups

**`CoralGrowthHistoryEntity`** / **`CoralPolypConditionEntity`**:
- Simple `@Entity` with FK to `coral_stock.id`; no soft-delete (child rows cascade-deleted with parent)

**`CoralCatalogueEntity`**:
- `@Table(name = "coral_catalogue", schema = "sabi")`
- Virtual column `active_scientific_name` defined in DDL — NOT mapped as JPA field
- `@OneToMany(mappedBy = "catalogue", cascade = CascadeType.ALL, orphanRemoval = true) List<CoralCatalogueI18nEntity> i18nEntries`

**`PublicReportLinkEntity`** (MODIFY):
- Add `@Column(name = "include_corals") boolean includeCorals = false`

### Repositories

| Repository | Key Custom Methods |
|-----------|-------------------|
| `TankCoralStockRepository` | `findByIdAndUserId(Long id, Long userId)` — MANDATORY for ownership; `findByAquariumIdAndDeletedAtIsNull(Long aquariumId)` |
| `CoralGrowthHistoryRepository` | `findByCoralStockIdOrderByMeasuredOnDesc(Long coralStockId)` |
| `CoralPolypConditionRepository` | `findByCoralStockIdOrderByObservedOnDesc(Long coralStockId)` |
| `CoralCatalogueRepository` | `findByScientificNameAndStatusIn(String name, List<FishCatalogueStatus> statuses)` — duplicate warning; `findByStatusOrderByProposalDateDesc(FishCatalogueStatus status)` |
| `CoralCatalogueI18nRepository` | `findByCatalogueIdAndLanguageCode(Long catalogueId, String lang)` |
| `CoralPhotoRepository` | `findByCoralStockId(Long coralStockId)` |

### Services

#### CoralStockServiceImpl

Key method signatures:
```java
List<CoralStockEntryTo> getCoralsForTank(Long aquariumId, String userEmail);
ResultTo<CoralStockEntryTo> addCoralToTank(CoralStockEntryTo entry, String userEmail);
ResultTo<CoralStockEntryTo> updateCoralEntry(CoralStockEntryTo entry, String userEmail);
ResultTo<CoralStockEntryTo> deletePhysically(Long coralId, String userEmail);
ResultTo<CoralStockEntryTo> recordDeparture(Long coralId, CoralDepartureRecordTo departure, String userEmail);
ResultTo<CoralStockEntryTo> removeCatalogueLink(Long coralId, String userEmail);
void uploadPhoto(Long coralId, byte[] bytes, String contentType, String userEmail);
byte[] getPhotoBytes(Long coralId, String userEmail);
void deletePhoto(Long coralId, String userEmail);
ResultTo<CoralGrowthHistoryTo> addGrowthRecord(Long coralId, CoralGrowthHistoryTo record, String userEmail);
ResultTo<CoralGrowthHistoryTo> updateGrowthRecord(Long coralId, Long recordId, CoralGrowthHistoryTo record, String userEmail);
ResultTo<Void> deleteGrowthRecord(Long coralId, Long recordId, String userEmail);
List<CoralGrowthHistoryTo> getGrowthHistory(Long coralId, String userEmail);
ResultTo<CoralPolypConditionTo> addPolypObservation(Long coralId, CoralPolypConditionTo obs, String userEmail);
ResultTo<CoralPolypConditionTo> updatePolypObservation(Long coralId, Long recordId, CoralPolypConditionTo obs, String userEmail);
ResultTo<Void> deletePolypObservation(Long coralId, Long recordId, String userEmail);
List<CoralPolypConditionTo> getPolypHistory(Long coralId, String userEmail);
```

**Ownership check pattern** (required on EVERY mutation per AGENTS.md):
```java
UserEntity user = userRepository.getByEmail(userEmail);
if (user == null) return new ResultTo<>(null, Message.error(CoralStockMessageCodes.UNKNOWN_USER, userEmail));
TankCoralStockEntity entity = coralStockRepository.findByIdAndUserId(coralId, user.getId());
if (entity == null) return new ResultTo<>(null, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL, coralId));
```

**Marine-only guard** (on add/put):
```java
AquariumEntity aquarium = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(entry.getAquariumId(), user.getId());
if (aquarium == null || !"MARINE".equals(aquarium.getWaterType())) {
    return new ResultTo<>(null, Message.error(CoralStockMessageCodes.MARINE_ONLY));
}
```

**Delete guard** (FR-012):
```java
if (entity.getDepartedOn() != null) {
    return new ResultTo<>(mapper.toTo(entity),
        Message.error(CoralStockMessageCodes.CORAL_HAS_DEPARTURE_RECORD, coralId));
}
```

**Growth measurement type immutability** (FR-039):
On `updateGrowthRecord`, load the existing record and ignore any `measurementType` in the request — always keep the DB value.

**BRANCH_COUNT integer validation** (C-9):
```java
if (CoralGrowthType.BRANCH_COUNT == record.getMeasurementType()) {
    if (record.getMeasurementValue().scale() > 0 &&
        record.getMeasurementValue().stripTrailingZeros().scale() > 0) {
        return new ResultTo<>(record, Message.error(CoralStockMessageCodes.BRANCH_COUNT_MUST_BE_INTEGER));
    }
}
```

#### CoralCatalogueServiceImpl

Key methods:
```java
List<CoralCatalogueSearchResultTo> search(String query, String lang, String userEmail);
List<CoralCatalogueSearchResultTo> listAll(String userEmail, String lang);
ResultTo<CoralCatalogueEntryTo> proposeEntry(CoralCatalogueEntryTo entry, String userEmail);
ResultTo<CoralCatalogueEntryTo> updateEntry(Long id, CoralCatalogueEntryTo entry, String userEmail);
ResultTo<CoralCatalogueEntryTo> approveEntry(Long id, String adminEmail);   // admin only
ResultTo<CoralCatalogueEntryTo> rejectEntry(Long id, String adminEmail);    // admin only
ResultTo<CoralCatalogueEntryTo> adminUpdateEntry(Long id, CoralCatalogueEntryTo entry, String adminEmail); // admin any status
```

**Search visibility rules** (FR-024, FR-030):  
`listAll` / `search` returns: `status = PUBLIC` for everyone, plus `status = PENDING AND proposer_user_id = user.id` for personalised pending visibility.

**Duplicate warning** (FR-025):  
On `proposeEntry` and `updateEntry` (when scientific name changes), call `coralCatalogueRepository.findByScientificNameAndStatusIn(name, List.of(PENDING, PUBLIC))` — if result is non-empty, set a WARNING-level message on the `ResultTo` but return 201/202 (non-blocking).

#### Modified: ReefDataExportServiceImpl

Extend the aquarium loop in `buildExport()`:
```java
for (AquariumEntity aq : aquariums) {
    AquariumExportTo exportTo = mapper.toExportTo(aq);
    // ... existing measurement/plague/fish/events population ...
    List<CoralExportTo> corals = coralStockService.getCorralsForExport(aq.getId());
    exportTo.setCorals(corals);   // AquariumExportTo.corals already exists
    result.getAquariums().add(exportTo);
}
```

#### Modified: PublicReportServiceImpl

Extend `buildReport(token)`:
```java
if (link.isIncludeCorals()) {
    List<PublicReefReportCoralTo> coralEntries = coralStockService.getActiveCoralsForReport(aquariumId);
    report.setCoralInhabitants(coralEntries);  // null when not opted-in; list (possibly empty) when opted-in
}
```

Each `PublicReefReportCoralTo` contains:
- `speciesName`, `classification` snapshots
- `latestGrowthByType`: Map<String, BigDecimal> — one latest value per distinct measurement type
- `latestPolypCondition`: most recent `PolypCondition` value (or null)

### REST Controllers

#### CoralStockController — `api/coral`

Mirrors `FishStockController` exactly. Key endpoints:

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/{aquariumId}/list` | All corals for a tank | 202 List |
| POST | `/` | Add coral | 201 ResultTo |
| GET | `/{coralId}` | Single coral | 202 |
| PUT | `/{coralId}` | Update coral | 202 ResultTo |
| DELETE | `/{coralId}` | Physical delete | 204 / 409 (has departure) |
| PUT | `/{coralId}/departure` | Record departure | 202 / 422 |
| DELETE | `/{coralId}/catalogue-link` | Remove catalogue link | 202 |
| POST | `/{coralId}/photo` | Upload photo (MultipartFile!) | 204 |
| GET | `/{coralId}/photo` | Download photo | 200 bytes |
| DELETE | `/{coralId}/photo` | Delete photo | 204 |
| GET | `/{coralId}/growth` | Growth history | 200 List |
| POST | `/{coralId}/growth` | Add growth record | 201 ResultTo |
| PUT | `/{coralId}/growth/{recordId}` | Update growth record | 202 ResultTo |
| DELETE | `/{coralId}/growth/{recordId}` | Delete growth record | 204 |
| GET | `/{coralId}/polyp` | Polyp condition history | 200 List |
| POST | `/{coralId}/polyp` | Add polyp observation | 201 ResultTo |
| PUT | `/{coralId}/polyp/{recordId}` | Update polyp observation | 202 ResultTo |
| DELETE | `/{coralId}/polyp/{recordId}` | Delete polyp observation | 204 |

**Logging rule** (AGENTS.md PII): Use `principal.getName()` only in `log.debug()`. Never at INFO+.

**Photo upload signature** (AGENTS.md mandatory):
```java
@PostMapping(value = "/{coralId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Void> uploadPhoto(
    @PathVariable Long coralId,
    @RequestParam("file") MultipartFile file,   // ← MultipartFile, NOT byte[]
    @RequestHeader(name = AUTH_TOKEN) String token,
    Principal principal) { ... }
```

#### CoralCatalogueController — `api/coral/catalogue`

Mirrors `FishCatalogueController`:

| Method | Path | Description |
|--------|------|-------------|
| GET | `` | List all (PUBLIC + own PENDING) |
| GET | `/search` | Search (min 2 chars, FULLTEXT on common_name + scientific_name) |
| POST | `` | Propose new entry |
| GET | `/{id}` | Get single entry |
| PUT | `/{id}` | Update (creator or admin) |

#### CoralCatalogueAdminController — `api/admin/coral/catalogue`

Mirrors `FishCatalogueAdminController`:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/pending` | List pending proposals |
| PUT | `/{id}/approve` | Approve |
| PUT | `/{id}/reject` | Reject |
| PUT | `/{id}` | Admin edit any status |

---

## sabi-webclient Changes

### API Gateways

Three service interfaces + implementations using RestTemplate (mirrors `FishStockServiceImpl`):
- `CoralStockServiceImpl` → `api/coral/...`
- `CoralCatalogueServiceImpl` → `api/coral/catalogue/...`
- `CoralCatalogueAdminServiceImpl` → `api/admin/coral/catalogue/...`

### CDI Beans

| Bean | Scope | Responsibility |
|------|-------|----------------|
| `CoralStockView` | `@RequestScope` | Load active/departed lists, navigate to add/edit/departure pages |
| `CoralStockEntryView` | `@RequestScope` | Add/edit form; catalogue search autocomplete; photo upload |
| `CoralEntryNavContext` | `@SessionScoped` | Pass selected coral between request-scoped beans (mirrors `FishEntryNavContext`) |
| `CoralDepartureView` | `@RequestScope` | Departure form with date/reason/note |
| `CoralGrowthHistoryView` | `@RequestScope` | Table + line-chart toggle (PrimeFaces `p:lineChart`) |
| `CoralPolypConditionView` | `@RequestScope` | Polyp condition table |
| `CoralCatalogueProposalView` | `@RequestScope` | Propose / edit catalogue entry; show i18n fields for all 5 languages |
| `CoralCatalogueAdminView` | `@RequestScope` | Admin pending list with approve/reject actions |

### XHTML Pages

Each input page follows the mandatory AGENTS.md UI style:
- Standalone page (NOT `p:dialog`)
- `sabi-back-link` breadcrumb at top
- Cancel button: `type="button" onclick="window.location.href=..."` 
- Save button: `style="background:#065f46"`

**`coralStockTab.xhtml`** (tab include on aquarium detail page):
```xml
<h:panelGroup rendered="#{tankDetailView.tank.waterType == 'MARINE'}">
    <!-- Active corals table + Departed corals collapsible section -->
    <!-- "Add coral" button navigating to coralStockEntryPage.xhtml -->
</h:panelGroup>
```

**`coralGrowthHistoryView.xhtml`** (FR-041 table/chart toggle):
```xml
<p:commandButton value="#{msg['coralstock.growth.toggle.chart']}"
                 actionListener="#{coralGrowthHistoryView.toggleChartView}"
                 update="growthContainer" />
<h:panelGroup id="growthContainer">
    <p:dataTable rendered="#{!coralGrowthHistoryView.chartMode}" .../>
    <p:lineChart rendered="#{coralGrowthHistoryView.chartMode}"
                 model="#{coralGrowthHistoryView.chartModel}"
                 style="width:100%;height:350px;" .../>
</h:panelGroup>
```

---

## i18n

~110 new keys across all 6 bundle files. See `contracts/i18n-keys.md` for the complete key list.

**Writing rule** (AGENTS.md — most reliable method): Use Python helper scripts at `/tmp/` to append keys — do NOT use `insert_edit_into_file` directly on `.properties` files (known silent-failure issue). Use a byte-check after every write.

Key sections:
- Coral stock form labels + validation messages
- Classification labels (LPS, SPS)
- Care level labels (EASY, MODERATE, DEMANDING)
- Growth type labels (SURFACE_AREA_CM2, SIZE_CM, VOLUME_CM3, BRANCH_COUNT)
- Polyp condition labels (VITAL, TISSUE_LOSS, PALE, LIMP, SIGNIFICANT_GROWTH)
- Departure reason labels (DIED, SOLD, GIVEN_AWAY, MOVED_TO_OTHER_TANK, OTHER)
- Coral catalogue form labels + UGC workflow messages
- House Reef Report coral section labels

---

## AI Export Extension (FR-035, FR-036)

**Already wired**: `AquariumExportTo.corals` field (`List<CoralExportTo>`) already exists in the codebase but is never populated. `ReefDataExportServiceImpl` must be extended to populate it.

**`CoralExportTo` must be extended** (existing stub has only 3 fields):
```java
// Add to existing CoralExportTo.java:
private String speciesName;
private String classification;
private String addedOn;
private String departedOn;
private String departureReason;
private String departureNote;
private String notes;
private List<CoralGrowthHistoryExportTo> growthHistory = new ArrayList<>();
private List<CoralPolypConditionExportTo> polypConditionHistory = new ArrayList<>();
```

Historical corals (departed) **are included** in the export (FR-036, US9-AC4).

---

## House Reef Report Extension (FR-032, FR-033, FR-034)

1. **DB**: `ALTER TABLE public_report_link ADD COLUMN include_corals TINYINT(1)` (V1_7_0_7)
2. **Model**: `PublicReportLinkEntity.includeCorals`, `PublicReportLinkTo.includeCorals`
3. **Service**: `PublicReportServiceImpl` — gate on `link.isIncludeCorals()`; populate `report.setCoralInhabitants(...)`
4. **Response TO**: `PublicReefReportTo.coralInhabitants` — `null` = not opted-in; empty list = opted-in but no active corals
5. **UI toggle**: Report link management page — add `include_corals` toggle (key: `report.include_corals.toggle.label`)

Only **active** corals (no `departed_on`) are included (FR-034).  
Each coral shows: speciesName, classification, latest growth per type, latest polyp condition.

---

## E2E Tests (Playwright)

### coralStockFlow.spec.ts — US1–US4

| Test | User Story | Key Assertions |
|------|-----------|----------------|
| Coral Stock tab visible for marine tank | US1-AC1 | Tab visible; NOT visible for freshwater tank |
| Add coral with mandatory fields | US1-AC2 | Coral appears in active list |
| Add coral with all optional fields | US1-AC3 | All fields persisted, reference URL opens in new tab |
| Validation: missing species name | US1-AC4 | Inline error, no save |
| Record departure (DIED reason) | US2-AC1 | Coral moves to departed section |
| Departure date before entry date: validation error | US2-AC2 | Inline error shown |
| Departed section collapsed by default | US2-AC4 | Section collapsed; expandable |
| Add growth measurement (SIZE_CM) | US3-AC1 | Appears at top of growth history |
| Growth history date-descending order | US3-AC2 | All records descending |
| Negative growth value: validation error | US3-AC3 | Inline error |
| Delete individual growth record | US3-AC4 | Only that record removed |
| Growth chart toggle (table ↔ chart) | US3-AC7 | `await expect(locator).toBeVisible()` before click |
| Add polyp condition (PALE) | US4-AC1 | Appears at top of polyp history |
| Delete individual polyp observation | US4-AC4 | Only that record removed |

**AGENTS.md Playwright rules**: Always `await expect(locator).toBeVisible()` before click; no `force: true` as standard; screenshot for visual verification.

### coralCatalogueFlow.spec.ts — US5–US7

| Test | Assertions |
|------|-----------|
| Catalogue search auto-fill | After select: species name, classification, care level filled |
| Catalogue link preserved on save | After save, link ID stored |
| Propose new entry | Immediately in proposer's search; NOT in other user's search |
| Duplicate scientific name warning | Non-blocking warning shown; still submittable |
| Pending entry not visible to other users | Cross-user isolation verified |
| Admin approves proposal | Entry appears in all users' search after approval |
| Admin rejects proposal | Entry disappears for all users including proposer |

### coralReportFlow.spec.ts — US8

| Test | Assertions |
|------|-----------|
| Report with coral opt-in enabled | `corals` key present in JSON; correct species names |
| Report with coral opt-in disabled | `coralInhabitants` absent/null |
| Departed coral not in report | Departed coral absent even when opt-in enabled |

### coralExportFlow.spec.ts — US9

| Test | Assertions |
|------|-----------|
| AI export contains `corals` array | Non-null even for tanks with no corals (empty array) |
| Coral with growth history exported | Full `growthHistory` array present |
| Coral with polyp history exported | Full `polypConditionHistory` array present |
| Departed coral included in export | `departedOn` and `departureReason` set |

---

## Security Checklist

| Control | Implementation |
|---------|---------------|
| All write ops require JWT | `@RequestHeader(name = AUTH_TOKEN, required = true)` on all mutating endpoints |
| Ownership: coral stock | `coralStockRepository.findByIdAndUserId(coralId, user.getId())` before every mutation |
| Ownership: growth/polyp records | Load parent coral with ownership check before any sub-record mutation |
| Ownership: catalogue entries | Creator check (`entry.getProposerUserId().equals(user.getId())`) OR admin role; creator of PENDING/PUBLIC only |
| Admin-only approval | `isAdmin()` check in `CoralCatalogueServiceImpl` before `approveEntry`/`rejectEntry` |
| Photo upload: magic bytes | Server-side magic-byte inspection (not filename extension) |
| Photo upload: size limit | 5 MB enforced; reject with 400 + `coralstock.form.photo.too_large` message |
| Marine-only guard | `aquarium.waterType == MARINE` checked before `addCoralToTank` |
| No PII in INFO+ logs | `log.debug` for `principal.getName()`; `log.info("...user_id={}", user.getId())` |

---

## Complexity Tracking

No constitution violations. All gates ✅.

---

## Deployment Checklist

After implementing, redeploy with:
```bash
cd devops/sabi_docker_sdk && bash server_redeploy.sh --boundary --flyway
```

- `--boundary`: rebuilds sabi-boundary JAR (new enums and TOs)
- `--flyway`: applies `version1_7_0/*` migrations (7 DDL scripts)

Verify migration applied:
```sql
SHOW TABLES LIKE 'coral%';
-- Expected: coral_catalogue, coral_catalogue_i18n, coral_stock, coral_growth_history, coral_polyp_condition, coral_photo
DESCRIBE public_report_link;
-- Expected: include_corals TINYINT(1)
```

