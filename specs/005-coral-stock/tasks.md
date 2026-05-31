# Tasks: Coral Stock Management & Coral Catalogue (005)

**Branch**: `005-coral-stock` | **Generated**: 2026-05-22 | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)  
**Tech stack**: Java 21, Spring Boot 3.x, JSF/PrimeFaces 15.x (JoinFaces), EclipseLink JPA, MariaDB 10.x, Flyway  
**Namespaces**: `jakarta.*` (Jakarta EE 9+)  
**AGENTS.md rules in effect**: ownership checks, PII logging, auth token pattern, i18n completeness (all 6 bundles), UI style guide (standalone pages, no p:dialog, sabi-back-link breadcrumb)

---

## Summary

| Metric | Value |
|--------|-------|
| Total tasks | 118 |
| Phase 1 – Setup (DB migrations) | 8 |
| Phase 2 – Foundational (boundary, entities, repos, mappers) | 37 |
| Phase 3 – US1 Add coral to tank | 14 |
| Phase 4 – US2 Record departure | 6 |
| Phase 5 – US3 Growth measurements | 5 |
| Phase 6 – US4 Polyp condition observations | 5 |
| Phase 7 – US5 Catalogue link | 8 |
| Phase 8 – US6 Propose catalogue entry | 9 |
| Phase 9 – US7 Admin approve / reject | 6 |
| Phase 10 – US8 House Reef Report integration | 6 |
| Phase 11 – US9 AI-JSON export integration | 3 |
| Final – Polish & cross-cutting | 11 |
| **Parallelisable tasks [P]** | **62** |

**Suggested MVP scope**: Phase 1 + Phase 2 + Phase 3 (US1 — add a coral to tank). Independently deployable; nothing else requires it first.

---

## Notation

```
- [ ] T-NNN [P] [USN] Description — file/path
         > Deps: T-XXX, T-YYY
```

- `[P]` — task is parallelisable (touches different files, no dependency on in-flight tasks)
- `[USN]` — user story label (US1–US9)
- `> Deps:` — tasks that MUST be completed first

---

---

## Phase 1 · Setup — Flyway Migration Scripts

> Goal: All DDL in place so JPA entities can be bootstrapped and Testcontainers-based tests run.  
> All 8 tasks are independent of each other (different SQL files) → execute in parallel.

- [x] T-001 [P] Create Flyway version directory and migration V1_7_0_1: `coral_catalogue` table
- [x] T-002 [P] Create migration V1_7_0_2: `coral_catalogue_i18n` table
- [x] T-003 [P] Create migration V1_7_0_3: `coral_stock` table
- [x] T-004 [P] Create migration V1_7_0_4: `coral_growth_history` table
- [x] T-005 [P] Create migration V1_7_0_5: `coral_polyp_condition` table
- [x] T-006 [P] Create migration V1_7_0_6: `coral_photo` table
- [x] T-007 [P] Create migration V1_7_0_7: `ALTER TABLE public_report_link ADD COLUMN include_corals`
- [x] T-008 Verify all 7 migration scripts execute cleanly against a fresh Testcontainer

---

---

## Phase 2 · Foundational — sabi-boundary, Entities, Repositories, Mappers

> Goal: All shared types, persistence layer, and service contracts in place before any user-story implementation begins.  
> Sub-groups are largely parallelisable within each group.

### 2a · sabi-boundary — Enums (all [P], fully independent)

- [x] T-009 [P] Create `CoralDepartureReason` enum (DIED, SOLD, GIVEN_AWAY, MOVED_TO_OTHER_TANK, OTHER) with `@JsonValue`/`@JsonCreator` annotations mirroring `DepartureReason` pattern — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralDepartureReason.java`

- [x] T-010 [P] Create `CoralClassification` enum (LPS, SPS) with `@JsonValue`/`@JsonCreator` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralClassification.java`

- [x] T-011 [P] Create `CoralCareLevel` enum (EASY, MODERATE, DEMANDING) with `@JsonValue`/`@JsonCreator` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralCareLevel.java`

- [x] T-012 [P] Create `CoralGrowthType` enum (SURFACE_AREA_CM2, SIZE_CM, VOLUME_CM3, BRANCH_COUNT) with `@JsonValue`/`@JsonCreator` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralGrowthType.java`

- [x] T-013 [P] Create `PolypCondition` enum (VITAL, TISSUE_LOSS, PALE, LIMP, SIGNIFICANT_GROWTH) with `@JsonValue`/`@JsonCreator` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PolypCondition.java`

### 2b · sabi-boundary — Transfer Objects

- [x] T-014 [P] Create `CoralGrowthHistoryTo` with fields: `id`, `coralStockEntryId`, `@NotNull @PastOrPresent measuredOn`, `@NotNull measurementType` (CoralGrowthType, **immutable** after creation per FR-039), `@NotNull @DecimalMin("0.1") measurementValue` (BigDecimal) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralGrowthHistoryTo.java`
       > Deps: T-012

- [x] T-015 [P] Create `CoralPolypConditionTo` with fields: `id`, `coralStockEntryId`, `@NotNull @PastOrPresent observedOn`, `@NotNull condition` (PolypCondition) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralPolypConditionTo.java`
       > Deps: T-013

- [x] T-016 [P] Create `CoralDepartureRecordTo` with fields: `@NotNull departureDate`, `@NotNull departureReason` (CoralDepartureReason), `@Size(max=500) departureNote` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralDepartureRecordTo.java`
       > Deps: T-009

- [x] T-017 [P] Create `CoralCatalogueI18nTo` with fields: `id`, `languageCode` (de/en/es/fr/it), `commonName`, `@Size(max=2000) description`, `referenceUrl` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralCatalogueI18nTo.java`

- [x] T-018 [P] Create `CoralCatalogueEntryTo` with fields: `id`, `@NotBlank scientificName`, `@NotNull classification` (CoralClassification), `@NotNull careLevel` (CoralCareLevel), `status` (reuse `FishCatalogueStatus`), `proposerUserId`, `proposalDate`, `@Valid List<CoralCatalogueI18nTo> i18nEntries` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralCatalogueEntryTo.java`
       > Deps: T-010, T-011, T-017

- [x] T-019 [P] Create `CoralCatalogueSearchResultTo` with fields: `id`, `scientificName`, `commonName` (resolved for requested lang), `classification` (CoralClassification), `careLevel` (CoralCareLevel), `referenceUrl`, `status` (FishCatalogueStatus) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralCatalogueSearchResultTo.java`
       > Deps: T-010, T-011

- [x] T-020 Create `CoralStockEntryTo` with all fields per data-model.md: `id`, `@NotNull aquariumId`, `@NotBlank speciesName`, `scientificName`, `classification` (CoralClassification), `careLevel` (CoralCareLevel), `@Pattern externalRefUrl`, `notes`, `@NotNull @PastOrPresent addedOn`, `departedOn`, `departureReason` (CoralDepartureReason), `@Size(max=500) departureNote`, `coralCatalogueId`, `hasPhoto`, `List<CoralGrowthHistoryTo> growthHistory`, `List<CoralPolypConditionTo> polypConditionHistory` — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralStockEntryTo.java`
       > Deps: T-009, T-010, T-011, T-014, T-015

- [x] T-021 [P] Create `CoralGrowthHistoryExportTo` with fields: `measuredOn` (String ISO date), `measurementType` (String), `measurementValue` (BigDecimal) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralGrowthHistoryExportTo.java`

- [x] T-022 [P] Create `CoralPolypConditionExportTo` with fields: `observedOn` (String ISO date), `condition` (String) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralPolypConditionExportTo.java`

- [x] T-023 Extend existing `CoralExportTo` stub: add fields `speciesName`, `classification`, `addedOn` (String), `departedOn` (String), `departureReason` (String), `departureNote` (String), `notes`, `List<CoralGrowthHistoryExportTo> growthHistory`, `List<CoralPolypConditionExportTo> polypConditionHistory` (retain existing stub fields `coralCatalogueId`, `scientificName`) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralExportTo.java`
       > Deps: T-021, T-022

- [x] T-024 [P] Create `PublicReefReportCoralTo` with fields: `speciesName`, `classification` (String), `latestGrowthByType` (Map<String, BigDecimal>), `latestPolypCondition` (String, nullable) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReefReportCoralTo.java`

- [x] T-025 [P] Modify `PublicReefReportTo`: add `@Schema(description="…") List<PublicReefReportCoralTo> coralInhabitants` field (null = not opted-in; empty list = opted-in, no active corals); no breaking change to existing serialisation (JSON null by default) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReefReportTo.java`
       > Deps: T-024

- [x] T-026 [P] Modify `PublicReportLinkTo`: add `boolean includeCorals = false` field (default false = backward-compatible) — `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReportLinkTo.java`

### 2c · sabi-server — JPA Entities

- [x] T-027 [P] Create `TankCoralStockEntity`: `@Table(name="coral_stock", schema="sabi")`, extend `Auditable`, `@SQLRestriction("deleted_at IS NULL")` for transparent soft-delete, `@ManyToOne(LAZY) UserEntity user` (ownership), `@ManyToOne(LAZY) AquariumEntity aquarium`, nullable `@ManyToOne(LAZY) CoralCatalogueEntity catalogueEntry`, all columns from DDL, `@NamedQuery` for `TankCoralStock.getCoralsByAquariumAndUser` — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/TankCoralStockEntity.java`
       > Deps: T-001, T-003

- [x] T-028 [P] Create `CoralGrowthHistoryEntity`: `@Table(name="coral_growth_history", schema="sabi")`, FK `@ManyToOne(LAZY) TankCoralStockEntity coralStock`, `measuredOn` (LocalDate), `measurementType` (String, VARCHAR(30)), `measurementValue` (BigDecimal, DECIMAL(8,1)) — no soft-delete (cascade from parent) — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/CoralGrowthHistoryEntity.java`
       > Deps: T-001, T-004, T-027

- [x] T-029 [P] Create `CoralPolypConditionEntity`: `@Table(name="coral_polyp_condition", schema="sabi")`, FK `@ManyToOne(LAZY) TankCoralStockEntity coralStock`, `observedOn` (LocalDate), `condition` (String, VARCHAR(30)) — no soft-delete — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/CoralPolypConditionEntity.java`
       > Deps: T-001, T-005, T-027

- [x] T-030 [P] Create `CoralCatalogueEntity`: `@Table(name="coral_catalogue", schema="sabi")`, extend `Auditable`, `status` (String), `proposerUserId` (Long, nullable), `proposalDate` (LocalDate), `scientificName`, `classification`, `careLevel`, `@OneToMany(mappedBy="catalogue", cascade=ALL, orphanRemoval=true) List<CoralCatalogueI18nEntity> i18nEntries` — do NOT map virtual column `active_scientific_name` as JPA field — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/CoralCatalogueEntity.java`
       > Deps: T-001, T-002

- [x] T-031 [P] Create `CoralCatalogueI18nEntity`: `@Table(name="coral_catalogue_i18n", schema="sabi")`, extend `Auditable`, FK `@ManyToOne(LAZY) CoralCatalogueEntity catalogue`, `languageCode`, `commonName`, `description`, `referenceUrl` — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/CoralCatalogueI18nEntity.java`
       > Deps: T-002, T-030

- [x] T-032 [P] Create `CoralPhotoEntity`: `@Table(name="coral_photo", schema="sabi")`, extend `Auditable`, FK `@OneToOne(LAZY) TankCoralStockEntity coralStock`, `filePath` (String), `contentType` (String), `uploadDate` (LocalDate) — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/CoralPhotoEntity.java`
       > Deps: T-001, T-006, T-027

- [x] T-033 [P] Modify `PublicReportLinkEntity`: add `@Column(name="include_corals", nullable=false) boolean includeCorals = false`; verify getter/setter generated by Lombok — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/PublicReportLinkEntity.java`
       > Deps: T-007

### 2d · sabi-server — Repositories

- [x] T-034 [P] Create `TankCoralStockRepository` (extends `JpaRepository<TankCoralStockEntity, Long>`): custom methods `findByIdAndUserId(Long id, Long userId)` (MANDATORY ownership), `findByAquariumIdAndDeletedAtIsNull(Long aquariumId)` — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/TankCoralStockRepository.java`
       > Deps: T-027

- [x] T-035 [P] Create `CoralGrowthHistoryRepository` (extends `JpaRepository`): `findByCoralStockIdOrderByMeasuredOnDesc(Long coralStockId)`, `findByIdAndCoralStockId(Long id, Long coralStockId)` (for ownership-scoped record lookup) — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/CoralGrowthHistoryRepository.java`
       > Deps: T-028

- [x] T-036 [P] Create `CoralPolypConditionRepository` (extends `JpaRepository`): `findByCoralStockIdOrderByObservedOnDesc(Long coralStockId)`, `findByIdAndCoralStockId(Long id, Long coralStockId)` — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/CoralPolypConditionRepository.java`
       > Deps: T-029

- [x] T-037 [P] Create `CoralCatalogueRepository` (extends `JpaRepository`): `findByScientificNameAndStatusIn(String name, List<String> statuses)` (duplicate warning, FR-025), `findByStatusOrderByProposalDateDesc(String status)` (admin list, FR-031), `findByIdAndProposerUserId(Long id, Long userId)` (creator ownership) — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/CoralCatalogueRepository.java`
       > Deps: T-030

- [x] T-038 [P] Create `CoralCatalogueI18nRepository` (extends `JpaRepository`): `findByCatalogueIdAndLanguageCode(Long catalogueId, String lang)` — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/CoralCatalogueI18nRepository.java`
       > Deps: T-031

- [x] T-039 [P] Create `CoralPhotoRepository` (extends `JpaRepository`): `findByCoralStockId(Long coralStockId)` — `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/CoralPhotoRepository.java`
       > Deps: T-032

### 2e · sabi-server — Mappers

- [x] T-040 [P] Create `CoralStockMapper` (`@Component`): `toTo(TankCoralStockEntity) → CoralStockEntryTo`, `toEntity(CoralStockEntryTo) → TankCoralStockEntity`, `toGrowthTo(CoralGrowthHistoryEntity) → CoralGrowthHistoryTo`, `toPolypTo(CoralPolypConditionEntity) → CoralPolypConditionTo`, `toExportTo(TankCoralStockEntity, List<CoralGrowthHistoryEntity>, List<CoralPolypConditionEntity>) → CoralExportTo` — `sabi-server/src/main/java/de/bluewhale/sabi/mapper/CoralStockMapper.java`
       > Deps: T-020, T-014, T-015, T-021, T-022, T-023, T-027, T-028, T-029

- [x] T-041 [P] Create `CoralCatalogueMapper` (`@Component`): `toTo(CoralCatalogueEntity) → CoralCatalogueEntryTo`, `toEntity(CoralCatalogueEntryTo) → CoralCatalogueEntity`, `toSearchResult(CoralCatalogueEntity, String lang) → CoralCatalogueSearchResultTo`, `toReportCoralTo(TankCoralStockEntity, Map<String,BigDecimal>, String) → PublicReefReportCoralTo` — `sabi-server/src/main/java/de/bluewhale/sabi/mapper/CoralCatalogueMapper.java`
       > Deps: T-018, T-019, T-024, T-030, T-031

### 2f · sabi-server — Service Interfaces & Message Codes

- [x] T-042 [P] Create `CoralStockMessageCodes` interface: constants UNKNOWN_USER, NOT_YOUR_CORAL, MARINE_ONLY, CORAL_HAS_DEPARTURE_RECORD, BRANCH_COUNT_MUST_BE_INTEGER, GROWTH_DATE_AFTER_DEPARTURE, POLYP_DATE_AFTER_DEPARTURE, DEPARTURE_DATE_BEFORE_ENTRY, PHOTO_TOO_LARGE, PHOTO_INVALID_FORMAT — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockMessageCodes.java`

- [x] T-043 [P] Create `CoralCatalogueMessageCodes` interface: constants UNKNOWN_USER, NOT_YOUR_ENTRY, ENTRY_NOT_FOUND, NOT_ADMIN, DUPLICATE_SCIENTIFIC_NAME_WARNING, READ_ONLY_REJECTED — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralCatalogueMessageCodes.java`

- [x] T-044 [P] Create `CoralStockService` interface with all method signatures from plan.md (getCoralsForTank, addCoralToTank, updateCoralEntry, deletePhysically, recordDeparture, removeCatalogueLink, uploadPhoto, getPhotoBytes, deletePhoto, addGrowthRecord, updateGrowthRecord, deleteGrowthRecord, getGrowthHistory, addPolypObservation, updatePolypObservation, deletePolypObservation, getPolypHistory, getActiveCoralsForReport, getCorralsForExport) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockService.java`
       > Deps: T-020, T-014, T-015, T-016, T-023, T-024, T-042

- [x] T-045 [P] Create `CoralCatalogueService` interface with all method signatures (search, listAll, proposeEntry, updateEntry, approveEntry, rejectEntry, adminUpdateEntry) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralCatalogueService.java`
       > Deps: T-018, T-019, T-043

---

---

## Phase 3 · User Story 1 — Add a Coral to My Aquarium (P1)

> **Story goal**: A user can open their marine aquarium's Coral Stock tab, add a coral entry with mandatory and optional fields, and see it in the "Currently in tank" list.  
> **Independent test**: Log in, open aquarium detail, navigate to Coral Stock tab, add a coral entry with at least one optional field, save, verify entry appears in active list with all saved data.

- [x] T-046 [US1] Implement `CoralStockServiceImpl` — core CRUD methods: `getCoralsForTank`, `addCoralToTank` (incl. marine-only guard C-8 and aquarium ownership), `updateCoralEntry`, `deletePhysically` (incl. departure-record guard FR-012), `getCoralById`; apply mandatory ownership pattern (`findByIdAndUserId`) on every mutation; log with `user_id=` (never email at INFO+) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-034, T-040, T-042, T-044

- [x] T-047 [P] [US1] Implement `CoralStockServiceImpl` — photo methods: `uploadPhoto` (magic-byte validation, 5 MB limit, store at `{sabi.photo.dir}/coral/{userId}/{coralId}.jpg`, upsert `CoralPhotoEntity`), `getPhotoBytes` (ownership check), `deletePhoto` (ownership check + filesystem delete) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-034, T-039, T-040, T-042, T-044

- [x] T-048 [US1] Create `CoralStockController` (`@RestController`, `@RequestMapping("/api/coral")`): endpoints GET `/{aquariumId}/list`, POST `/`, GET `/{coralId}`, PUT `/{coralId}`, DELETE `/{coralId}` (204 / 409 on departure guard); use `@RequestHeader(name=AUTH_TOKEN)` on every endpoint; PII rule: `log.debug` only for `principal.getName()` — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralStockController.java`
       > Deps: T-046, T-044

- [x] T-049 [P] [US1] Add photo endpoints to `CoralStockController`: `POST /{coralId}/photo (consumes=MULTIPART_FORM_DATA_VALUE, @RequestParam("file") MultipartFile)` → 204; `GET /{coralId}/photo` → 200 bytes; `DELETE /{coralId}/photo` → 204; **use `MultipartFile`, NOT `byte[]`** (AGENTS.md mandatory pattern) — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralStockController.java`
       > Deps: T-047, T-048

- [x] T-050 [P] [US1] Create `CoralStockService` interface and `CoralStockServiceImpl` in `sabi-webclient` (RestTemplate-based API gateway): implement `getCoralsForTank`, `addCoral`, `updateCoral`, `deleteCoral`, `uploadPhoto`, `getPhotoBytes`; use `RestHelper.prepareAuthedHttpHeader(token)` for all requests — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/CoralStockServiceImpl.java`
       > Deps: T-048, T-049

- [x] T-051 [P] [US1] Create `CoralEntryNavContext` (`@SessionScoped` CDI bean): holds selected `CoralStockEntryTo` and `coralId` between request-scoped beans (mirrors `FishEntryNavContext`) — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralEntryNavContext.java`
       > Deps: T-020

- [x] T-052 [US1] Create `CoralStockView` (`@RequestScope` CDI bean): loads active and departed coral lists for current aquarium, navigation methods to add/edit/departure pages, delete action with departure-guard error display — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralStockView.java`
       > Deps: T-050, T-051

- [x] T-053 [US1] Create `CoralStockEntryView` (`@RequestScope` CDI bean): add/edit form backing bean; handles photo upload via `CoralStockService`; `@PostConstruct` loads existing entry when editing; validate mandatory fields client-side via `required="true"` and server-side; stores result in `CoralEntryNavContext` — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralStockEntryView.java`
       > Deps: T-050, T-051, T-052

- [x] T-054 [US1] Create `coralStockTab.xhtml` (tab include for aquarium detail page): `<h:panelGroup rendered="#{tankDetailView.tank.waterType == 'MARINE'}">` guard (C-8); active coral table with species name, classification, added-on date, action buttons ("Edit", "Record departure", "Delete"); "Add coral" button navigating to `coralStockEntryPage.xhtml`; use i18n keys `coralstock.tab.label`, `coralstock.active.section.label` — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralStockTab.xhtml`
       > Deps: T-052, T-058

- [x] T-055 [P] [US1] Create `coralStockView.xhtml` (full Coral Stock list page): active corals `p:dataTable`; "No corals" message when empty (`coralstock.no.entries.label`); "Add coral" button with `style="background:#065f46"` (UI style guide); `sabi-back-link` breadcrumb at top — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralStockView.xhtml`
       > Deps: T-052, T-058

- [x] T-056 [US1] Create `coralStockEntryPage.xhtml` (standalone add/edit page): includes `coralStockEntryForm.xhtml`; `sabi-back-link` breadcrumb; Save button `style="background:#065f46"`; Cancel button `type="button" onclick="window.location.href=..."` (AGENTS.md UI pattern — NOT p:dialog) — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralStockEntryPage.xhtml`
       > Deps: T-053, T-058

- [x] T-057 [US1] Create `coralStockEntryForm.xhtml` (form include — without catalogue search, added in US5): fields for species name (`@NotBlank`), entry date (`@PastOrPresent`, `p:calendar`), classification select (LPS/SPS), care level select, notes textarea, reference URL input, photo upload (`p:fileUpload`, max 5 MB); inline validation error messages using `coralstock.form.*` keys — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralStockEntryForm.xhtml`
       > Deps: T-053, T-058

- [x] T-058 [P] [US1] Add all ~60 i18n keys for sections "Coral Stock — General Labels", "Coral Stock — Form Fields", "Classification Labels", "Care Level Labels" (as defined in contracts/i18n-keys.md) to all 6 bundle files using Python helper scripts per AGENTS.md; perform byte-check after each file; verify no key missing — `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 files)

- [x] T-059 [US1] Integrate `coralStockTab.xhtml` into the aquarium detail page (`tankDetailView.xhtml` or equivalent): add `<ui:include src="/secured/coralStockTab.xhtml"/>` inside the tab panel alongside the existing Fish Stock tab; ensure the marine-only `rendered` guard is active — `sabi-webclient/src/main/resources/META-INF/resources/secured/tankDetailView.xhtml` (or equivalent)
       > Deps: T-054

---

---

## Phase 4 · User Story 2 — Record a Coral Departure (P2)

> **Story goal**: A user can record a departure for an active coral (with date, reason, optional note); the coral moves to the collapsed "Departed corals" section; physical deletion is blocked once a departure record exists.  
> **Independent test**: Add coral → record departure → verify coral disappears from active list and appears in historical section with correct departure date and reason.

- [x] T-060 [US2] Implement `CoralStockServiceImpl.recordDeparture`: ownership check with `findByIdAndUserId`; validate `departureDate >= addedOn` (FR-006, return 422 on violation); validate `departureNote` max 500 chars (FR-042); set `departedOn`, `departureReason`, `departureNote` fields; save; confirm physical delete guard in `deletePhysically` returns `CORAL_HAS_DEPARTURE_RECORD` when `entity.getDepartedOn() != null` (FR-012) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-046

- [x] T-061 [P] [US2] Add departure endpoint to `CoralStockController`: `PUT /{coralId}/departure` body `CoralDepartureRecordTo @Valid` → 202/400/422; confirm DELETE `/{coralId}` returns 409 (CONFLICT) when service returns `CORAL_HAS_DEPARTURE_RECORD` — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralStockController.java`
       > Deps: T-060, T-048

- [x] T-062 [US2] Create `CoralDepartureView` (`@RequestScope` CDI bean): departure form backing bean; loads selected coral from `CoralEntryNavContext`; validates departure date ≥ entry date client-side; departure reason select (`CoralDepartureReason` enum values); optional note field; submit calls `CoralStockService.recordDeparture` — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralDepartureView.java`
       > Deps: T-050, T-051, T-064

- [x] T-063 [US2] Create `coralDepartureForm.xhtml` (standalone departure page): `sabi-back-link` breadcrumb; fields for departure date (`p:calendar`), departure reason (`p:selectOneMenu` with `coralstock.departure.reason.*` i18n labels), departure note (`p:inputTextarea`, maxlength=500); inline validation errors; Save `style="background:#065f46"`, Cancel `type="button"` — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralDepartureForm.xhtml`
       > Deps: T-062, T-064

- [x] T-064 [P] [US2] Add ~20 i18n keys for sections "Coral Stock — Departure Form" and "Departure Reason Values" to all 6 bundle files using Python helper scripts; byte-check after each file — `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 files)

- [x] T-065 [US2] Update `coralStockTab.xhtml` and `coralStockView.xhtml`: add collapsible "Departed corals" `p:panel` (collapsed by default, FR-007, `toggleable="true"`); label `coralstock.departed.section.label`; display departed coral rows with departure date, departure reason (localised via `coralstock.departure.reason.*`), and all original fields; display `coralstock.delete.has_departure.label` error message when delete is blocked — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralStockTab.xhtml`
       > Deps: T-054, T-063, T-064

---

---

## Phase 5 · User Story 3 — Log a Growth Measurement (P3)

> **Story goal**: A user can add growth measurement records (type + value + date) to a coral entry; multiple types tracked independently; history displayed date-descending; toggle between table and line chart; individual records editable and deletable.  
> **Independent test**: Add coral → log 3 measurements (≥2 types, ≥2 dates) → verify all in growth history in date-descending order.

- [x] T-066 [US3] Implement `CoralStockServiceImpl` growth methods: `addGrowthRecord` (ownership via parent coral `findByIdAndUserId`; validate date ≤ departure date FR-015; validate value > 0; validate `BRANCH_COUNT` is integer C-9; measurement type set at creation and immutable FR-039); `updateGrowthRecord` (ignore `measurementType` in request, keep existing value FR-039); `deleteGrowthRecord`; `getGrowthHistory` (returns ordered by `measuredOn DESC`) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-046, T-035

- [x] T-067 [P] [US3] Add growth endpoints to `CoralStockController`: `GET /{coralId}/growth` → 200 List; `POST /{coralId}/growth @Valid` → 201/400/422; `PUT /{coralId}/growth/{recordId}` → 202/422; `DELETE /{coralId}/growth/{recordId}` → 204 — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralStockController.java`
       > Deps: T-066, T-048

- [x] T-068 [US3] Create `CoralGrowthHistoryView` (`@RequestScope` CDI bean): loads growth history for selected coral; `boolean chartMode` toggle; `LineChartModel chartModel` for PrimeFaces `p:lineChart` (one dataset per `CoralGrowthType`); add/edit/delete growth record actions; `toggleChartView()` method called by commandButton — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralGrowthHistoryView.java`
       > Deps: T-050, T-051, T-070

- [x] T-069 [US3] Create `coralGrowthHistoryView.xhtml`: `p:commandButton` toggle (`coralstock.growth.toggle.chart` / `coralstock.growth.toggle.table`) with `update="growthContainer"`; `<h:panelGroup id="growthContainer">` containing `p:dataTable` (`rendered="#{!coralGrowthHistoryView.chartMode}"`) and `p:lineChart` (`rendered="#{coralGrowthHistoryView.chartMode}"`, `style="width:100%;height:350px;"`); add/edit measurement sub-form inline; "No measurements" message; follow WCAG 2.1 AA contrast — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralGrowthHistoryView.xhtml`
       > Deps: T-068, T-070

- [x] T-070 [P] [US3] Add ~20 i18n keys for sections "Growth History" and "Growth Type Labels" to all 6 bundle files using Python helper scripts; byte-check — `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 files)

---

---

## Phase 6 · User Story 4 — Log a Polyp Condition Observation (P4)

> **Story goal**: A user can add polyp condition observations (condition state + date) to a coral; multiple observations per date allowed; history date-descending; individual records editable and deletable.  
> **Independent test**: Add coral → log 2 observations on different dates with different states → verify both appear in date-descending order.

- [x] T-071 [US4] Implement `CoralStockServiceImpl` polyp methods: `addPolypObservation` (ownership via parent coral; validate date ≤ departure date FR-019; validate condition state is valid `PolypCondition` value); `updatePolypObservation` (editable: `observedOn` and `condition`; FR-040); `deletePolypObservation`; `getPolypHistory` (returns ordered by `observedOn DESC`) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-046, T-036

- [x] T-072 [P] [US4] Add polyp endpoints to `CoralStockController`: `GET /{coralId}/polyp` → 200 List; `POST /{coralId}/polyp @Valid` → 201/400/422; `PUT /{coralId}/polyp/{recordId}` → 202/422; `DELETE /{coralId}/polyp/{recordId}` → 204 — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralStockController.java`
       > Deps: T-071, T-048

- [x] T-073 [US4] Create `CoralPolypConditionView` (`@RequestScope` CDI bean): loads polyp condition history; add/edit/delete actions; condition state displayed with localised label via `coralstock.condition.*` keys — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralPolypConditionView.java`
       > Deps: T-050, T-051, T-075

- [x] T-074 [US4] Create `coralPolypConditionView.xhtml`: `p:dataTable` of condition history (date-descending); add-observation inline form with date (`p:calendar`) and condition dropdown (`p:selectOneMenu` with `coralstock.condition.*` labels); edit/delete row actions; "No observations" message; WCAG 2.1 AA contrast — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralPolypConditionView.xhtml`
       > Deps: T-073, T-075

- [x] T-075 [P] [US4] Add ~16 i18n keys for sections "Polyp Condition History" and "Polyp Condition State Labels" to all 6 bundle files using Python helper scripts; byte-check — `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 files)

---

---

## Phase 7 · User Story 5 — Link a Coral Entry to the Coral Catalogue (P5)

> **Story goal**: From the add/edit coral form, users can search the catalogue by scientific or common name (min 2 chars), select an entry, and have species name, classification, care level, and localised ref URL auto-filled; catalogue link stored; removable without losing the coral entry.  
> **Independent test**: With ≥1 public catalogue entry, open add-coral form, search, select, verify auto-fill, save, confirm catalogue link stored.

- [x] T-076 [US5] Implement `CoralCatalogueServiceImpl` — read operations: `listAll(userEmail, lang)` returns PUBLIC entries for everyone + own PENDING entries (FR-024 visibility rules); `search(query, lang, userEmail)` FULLTEXT partial match on `common_name` and `scientific_name` (FR-030, min 2 chars query); `getById(id, userEmail)` (own PENDING or PUBLIC only); duplicate-name check helper `isDuplicateScientificName(name)` — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralCatalogueServiceImpl.java`
       > Deps: T-037, T-038, T-041, T-043, T-045

- [x] T-077 [P] [US5] Create `CoralCatalogueController` (`@RestController`, `@RequestMapping("/api/coral/catalogue")`): `GET /` (listAll, param `lang`), `GET /search` (param `q` min 2 chars, `lang`), `GET /{id}` — all require JWT; return 400 when query shorter than 2 chars (FR-030) — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralCatalogueController.java`
       > Deps: T-076, T-045

- [x] T-078 [P] [US5] Create `CoralCatalogueService` interface and `CoralCatalogueServiceImpl` in `sabi-webclient` (RestTemplate-based gateway): `search(query, lang, token) → List<CoralCatalogueSearchResultTo>`, `listAll(lang, token)`, `getById(id, token)` — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/CoralCatalogueServiceImpl.java`
       > Deps: T-077, T-019

- [x] T-079 [P] [US5] Create `CoralCatalogueSearchResultConverter` (JSF `Converter` implementing `javax.faces.convert.Converter`): `getAsString`/`getAsObject` for autocomplete binding of `CoralCatalogueSearchResultTo` — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/converter/CoralCatalogueSearchResultConverter.java`
       > Deps: T-019

- [x] T-080 [US5] Update `CoralStockEntryView`: add autocomplete search field backed by `CoralCatalogueService.search`; `onCatalogueSelect(SelectEvent)` auto-fills `speciesName`, `classification`, `careLevel`, `externalRefUrl` from selected `CoralCatalogueSearchResultTo` for user's language; "No results" message + "Propose new entry" link (FR-030 fallback); `removeCatalogueLink()` action — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralStockEntryView.java`
       > Deps: T-053, T-078, T-079, T-090

- [x] T-081 [US5] Update `coralStockEntryForm.xhtml`: add `p:autoComplete` for catalogue search (min 2 chars, `completeMethod`, `onSelect`, converter ref); "No results found" empty message; "Propose new entry" `h:commandLink`; "Remove catalogue link" button (when link is set); display `coralstock.form.catalogue.*` i18n keys — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralStockEntryForm.xhtml`
       > Deps: T-080, T-090

- [x] T-082 [P] [US5] Implement `CoralStockServiceImpl.removeCatalogueLink`: ownership check; set `entity.coralCatalogueId = null`; retain `speciesName`, `scientificName`, `classification` as user-editable free text; save (FR-010) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-046

- [x] T-083 [P] [US5] Add `DELETE /{coralId}/catalogue-link` to `CoralStockController` → 202 `ResultTo<CoralStockEntryTo>` — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralStockController.java`
       > Deps: T-082, T-048

---

---

## Phase 8 · User Story 6 — Propose a New Coral Catalogue Entry (P6)

> **Story goal**: An authenticated user can propose a new coral catalogue entry (scientific name, classification, care level, i18n fields); entry immediately searchable by proposer only (PENDING); non-blocking duplicate warning on name conflict; proposer can edit own PENDING/PUBLIC entries.  
> **Independent test**: Propose entry with unique scientific name → verify immediately in proposer's search → NOT in another user's results → visible in admin's pending queue.

- [x] T-084 [US6] Implement `CoralCatalogueServiceImpl.proposeEntry`: resolve user by email; set `status=PENDING`, `proposerUserId`, `proposalDate`; check for duplicate scientific name (FR-025) via `findByScientificNameAndStatusIn(..., [PENDING, PUBLIC])` → if found, set WARNING-level message on `ResultTo` (non-blocking, still returns 201); save entry with i18n sub-entries; confirm at least one localised `commonName` present — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralCatalogueServiceImpl.java`
       > Deps: T-076, T-037, T-038, T-041, T-043

- [x] T-085 [P] [US6] Add `POST /api/coral/catalogue` to `CoralCatalogueController`: `@Valid @RequestBody CoralCatalogueEntryTo`; return 201 `ResultTo` with WARNING message when duplicate name found (non-blocking per FR-025); return 400 for validation errors — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralCatalogueController.java`
       > Deps: T-084, T-077

- [x] T-086 [US6] Implement `CoralCatalogueServiceImpl.updateEntry`: resolve user; verify caller is creator (PENDING/PUBLIC status) or admin; if scientific name changed, re-evaluate duplicate check and set WARNING-level message if conflict (FR-029); update i18n entries (upsert per language code); confirm REJECTED entries return 403 for non-admin (FR-029) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralCatalogueServiceImpl.java`
       > Deps: T-084

- [x] T-087 [P] [US6] Add `PUT /api/coral/catalogue/{id}` to `CoralCatalogueController` — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralCatalogueController.java`
       > Deps: T-086, T-085

- [x] T-088 [P] [US6] Update `CoralCatalogueService` (webclient) with `proposeEntry` and `updateEntry` methods; update `CoralCatalogueServiceImpl` (webclient) with corresponding RestTemplate calls — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/CoralCatalogueServiceImpl.java`
       > Deps: T-078, T-085

- [x] T-089 [US6] Create `CoralCatalogueProposalView` (`@RequestScope` CDI bean): propose/edit catalogue entry; collect fields for all 5 languages; display non-blocking duplicate warning when returned; switch between propose mode (new) and edit mode (existing entry) — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralCatalogueProposalView.java`
       > Deps: T-088, T-090

- [x] T-090 [P] [US6] Add ~30 i18n keys for section "Coral Catalogue" (propose/edit form labels, status labels, duplicate warning, etc.) to all 6 bundle files using Python helper scripts; byte-check — `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 files)

- [x] T-091 [US6] Create `coralCatalogueI18nFields.xhtml` (reusable Facelets composite component): i18n sub-form showing common name, description (`p:inputTextarea`, maxlength=2000), reference URL for one language; composite attribute for `languageCode` — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralCatalogueI18nFields.xhtml`
       > Deps: T-090

- [x] T-092 [US6] Create `coralCatalogueProposalForm.xhtml` (standalone propose/edit page): `sabi-back-link` breadcrumb; scientific name input; classification and care level selects; `<ui:repeat>` over 5 language codes including `coralCatalogueI18nFields.xhtml`; non-blocking duplicate warning message panel; Save `style="background:#065f46"`, Cancel `type="button"` — `sabi-webclient/src/main/resources/META-INF/resources/secured/coralCatalogueProposalForm.xhtml`
       > Deps: T-089, T-091, T-090

---

---

## Phase 9 · User Story 7 — Admin Approves or Rejects a Coral Catalogue Proposal (P7)

> **Story goal**: Admin can list pending proposals, edit fields, approve (→ PUBLIC, visible to all) or reject (→ REJECTED, hidden); non-admin access denied.  
> **Independent test**: Log in as admin → find pending proposal → approve → log in as regular user → verify entry now appears in catalogue search.

- [x] T-093 [US7] Implement `CoralCatalogueServiceImpl` admin methods: `approveEntry(id, adminEmail)` — verify `isAdmin()`; set `status=PUBLIC`; `rejectEntry(id, adminEmail)` — verify `isAdmin()`; set `status=REJECTED`; `adminUpdateEntry(id, entry, adminEmail)` — verify `isAdmin()`; admin can edit any status (FR-028) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralCatalogueServiceImpl.java`
       > Deps: T-086

- [x] T-094 [P] [US7] Create `CoralCatalogueAdminController` (`@RestController`, `@RequestMapping("/api/admin/coral/catalogue")`): `GET /pending` → 200 List sorted by `proposalDate DESC`; `PUT /{id}/approve` → 202; `PUT /{id}/reject` body optional rejection reason → 202; `PUT /{id}` admin update → 202; all endpoints require JWT + ADMIN role (verified in service `isAdmin()`) — `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/CoralCatalogueAdminController.java`
       > Deps: T-093

- [x] T-095 [P] [US7] Create `CoralCatalogueAdminService` interface and `CoralCatalogueAdminServiceImpl` in `sabi-webclient` (RestTemplate gateway): `listPending(token)`, `approve(id, token)`, `reject(id, token)`, `adminUpdate(id, entry, token)` — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/CoralCatalogueAdminServiceImpl.java`
       > Deps: T-094

- [x] T-096 [US7] Create `CoralCatalogueAdminView` (`@RequestScope` CDI bean): loads pending proposals list; approve/reject actions with success feedback; opens edit form for a selected proposal — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/CoralCatalogueAdminView.java`
       > Deps: T-095, T-098

- [x] T-097 [US7] Create `coralCatalogueAdminView.xhtml` (admin page under `secured/admin/`): `p:dataTable` of pending proposals showing scientific name, classification, care level, proposer reference (anonymous), submission date, link to full detail; approve/reject buttons per row with `confirm` dialog; success/error messages; `sabi-back-link` breadcrumb — `sabi-webclient/src/main/resources/META-INF/resources/secured/admin/coralCatalogueAdminView.xhtml`
       > Deps: T-096, T-098

- [x] T-098 [P] [US7] Add ~14 i18n keys for section "Coral Catalogue Admin" to all 6 bundle files using Python helper scripts; byte-check — `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 files)

---

---

## Phase 10 · User Story 8 — Coral Data in House Reef Report (P8)

> **Story goal**: Public House Reef Report includes a `corals` array (active corals only) when per-link coral opt-in is enabled; departed corals always excluded; report entry shows species name, classification, latest growth per type, latest polyp condition.  
> **Independent test**: Enable coral opt-in on public report link, fetch report URL, verify `corals` array with correct snapshot data.

- [x] T-099 [US8] Implement `CoralStockServiceImpl.getActiveCoralsForReport(aquariumId) → List<PublicReefReportCoralTo>`: load active corals (`departed_on IS NULL`); for each, compute `latestGrowthByType` (Map<String, BigDecimal> — one latest value per distinct `measurementType`); `latestPolypCondition` (most recent `condition` string or null) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-046, T-035, T-036, T-040

- [x] T-100 [US8] Modify `PublicReportServiceImpl.buildReport(token)`: after existing logic, if `link.isIncludeCorals()` is true, call `coralStockService.getActiveCoralsForReport(aquariumId)` and set `report.setCoralInhabitants(coralEntries)`; when opt-in is false, leave `coralInhabitants` as `null` (FR-032; null = not opted-in, per data-model) — `sabi-server/src/main/java/de/bluewhale/sabi/services/PublicReportServiceImpl.java`
       > Deps: T-099, T-033, T-025

- [x] T-101 [P] [US8] Modify `PublicReportLinkController` (or equivalent): ensure `POST /api/report-link` and `PUT /api/report-link/{id}` correctly read and write `includeCorals` from `PublicReportLinkTo`; no breaking change (field defaults to false) — relevant controller in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/`
       > Deps: T-026, T-033

- [x] T-102 [P] [US8] Update report-link management UI (webclient): add `includeCorals` toggle (`p:toggleSwitch` or `p:selectBooleanCheckbox`) to report link settings page using i18n key `report.include_corals.toggle.label` — relevant XHTML in `sabi-webclient/src/main/resources/META-INF/resources/secured/`
       > Deps: T-104

- [x] T-103 [P] [US8] Add ~7 i18n keys for section "House Reef Report" (coral section title, classification label, latest growth label, latest condition label, no measurements, no observations, include toggle) to all 6 bundle files; byte-check — `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 files)
       > (Renamed T-103 to avoid clash — listed here as the i18n task)

- [x] T-104 [P] [US8] Validate report response: add i18n keys for section "House Reef Report" (see T-103 — same deliverable listed again for dependency clarity; see T-103)
       > (Merging into T-103 — only T-103 needed; placeholder kept for numbering)

---

> **Note**: T-104 is merged into T-103. Subsequent IDs continue at T-105.

---

---

## Phase 11 · User Story 9 — Coral Data in AI-JSON Export (P9)

> **Story goal**: AI export endpoint includes a complete `corals` array per aquarium (all corals incl. departed), with full growth history and polyp condition history.  
> **Independent test**: Download AI export for user with ≥1 coral with growth history and polyp condition → verify `corals` array with all expected fields.

- [x] T-105 [US9] Implement `CoralStockServiceImpl.getCorralsForExport(aquariumId) → List<CoralExportTo>`: load ALL corals for aquarium (active + departed, ignoring soft-delete but including departed records); for each, load complete growth history and polyp condition history via repositories; map to `CoralExportTo` using mapper `toExportTo`; return empty list when no corals (FR-035) — `sabi-server/src/main/java/de/bluewhale/sabi/services/CoralStockServiceImpl.java`
       > Deps: T-046, T-035, T-036, T-040, T-023

- [x] T-106 [P] [US9] Modify `ReefDataExportServiceImpl.buildExport()`: in the existing aquarium loop, after fish/measurements/events, call `coralStockService.getCorralsForExport(aq.getId())` and set `exportTo.setCorals(corals)` (the `AquariumExportTo.corals` field already exists as a stub); increment AI export JSON schema version metadata to reflect the `corals` addition — `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java`
       > Deps: T-105

- [x] T-107 [P] [US9] Add `CoralPhotoController` in sabi-webclient (multipart upload relay for the REST client path): implements the webclient-side photo upload using `ByteArrayResource` + `MultiValueMap` pattern (AGENTS.md mandatory multipart pattern); complements `CoralStockServiceImpl` photo methods — `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/rest/CoralPhotoController.java`
       > Deps: T-050

---

---

## Final Phase · Polish & Cross-Cutting Concerns

> All tasks are largely independent after all user story phases complete.

- [x] T-108 [P] Add springdoc/OpenAPI `@Operation`, `@ApiResponse`, `@Tag` annotations to `CoralStockController`, `CoralCatalogueController`, `CoralCatalogueAdminController` so all new endpoints appear correctly in the OpenAPI spec — respective controller files in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/`
       > Deps: T-048, T-077, T-094

- [x] T-109 [P] i18n completeness check: write Python script `/tmp/check_i18n_completeness.py` that reads all ~110 keys from `contracts/i18n-keys.md` and verifies every key is present in all 6 property files (`messages.properties`, `messages_de.properties`, `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`, `messages_it.properties`); run via `python3 /tmp/check_i18n_completeness.py | tee /tmp/i18n_check.out`; fix any missing keys — `sabi-webclient/src/main/resources/i18n/messages*.properties`
       > Deps: T-058, T-064, T-070, T-075, T-090, T-098, T-103

- [x] T-110 [P] WCAG 2.1 AA colour-contrast review of all new UI elements: verify Save buttons (`#065f46` on white ≥ 8:1 ✅), all new text-on-background combos (link colours `#0369a1` ≥ 5.7:1); check growth chart line colours for contrast; verify no `lightblue`/`yellow` text on light backgrounds (AGENTS.md forbidden combos) — all new XHTML files
       > Deps: T-055, T-056, T-063, T-069, T-074, T-092, T-097

- [x] T-111 [P] Security ownership coverage review: verify all new mutation endpoints in `CoralStockController`, `CoralCatalogueController`, and `CoralCatalogueAdminController` are listed in the ownership audit table in AGENTS.md; add missing entries; confirm pattern `findByIdAndUserId` used on every `PUT`/`DELETE`/`POST` for coral stock and growth/polyp sub-resources — `AGENTS.md` + controller review

- [x] T-112 Create Playwright E2E test `coralStockFlow.spec.ts` covering US1–US4: Coral Stock tab visible for marine tank (not freshwater); add coral with mandatory fields; add coral with all optional fields; validation errors for missing species name; record departure (DIED reason); departed section collapsed by default; add growth measurement; growth chart toggle (use `await expect(locator).toBeVisible()` before every click per AGENTS.md); add polyp condition; delete individual growth/polyp record — `e2e/tests/coralStockFlow.spec.ts`
       > Deps: T-059, T-065, T-069, T-074

- [x] T-113 [P] Create Playwright E2E test `coralCatalogueFlow.spec.ts` covering US5–US7: catalogue search auto-fill; catalogue link preserved on save; propose new entry (immediately in proposer search, NOT in other user search); duplicate scientific name warning (non-blocking); admin approves proposal (appears in all users' search); admin rejects proposal (disappears) — `e2e/tests/coralCatalogueFlow.spec.ts`
       > Deps: T-092, T-097

- [x] T-114 [P] Create Playwright E2E test `coralReportFlow.spec.ts` covering US8: public report with coral opt-in enabled → `corals` key present with correct species names; opt-in disabled → `coralInhabitants` absent/null; departed coral absent even when opt-in enabled — `e2e/tests/coralReportFlow.spec.ts`
       > Deps: T-100, T-102

- [x] T-115 [P] Create Playwright E2E test `coralExportFlow.spec.ts` covering US9: AI export contains `corals` array (non-null even for empty tanks); coral with growth history exported with full `growthHistory` array; coral with polyp history exported; departed coral included with `departedOn` and `departureReason` fields — `e2e/tests/coralExportFlow.spec.ts`
       > Deps: T-106

- [ ] T-116 Verify deployment: run `cd devops/sabi_docker_sdk && bash server_redeploy.sh --boundary --flyway`; confirm all 7 V1_7_0 migrations applied (`SHOW TABLES LIKE 'coral%'`; `DESCRIBE public_report_link`); confirm server starts without errors; smoke-test at least one new endpoint
       > Deps: T-108, T-109, T-111

- [ ] T-117 [P] Verify no breaking changes to existing API: run existing integration test suite; confirm `PublicReefReportTo` in JSON response is backward-compatible (new `coralInhabitants` field is null by default); confirm `AquariumExportTo` AI export adds `corals: []` to existing aquariums without errors
       > Deps: T-100, T-106

- [x] T-118 [P] Update `AGENTS.md` ownership audit table: add all new `CoralStockController` and `CoralCatalogueController` entries with their ownership-check method names (pattern: `findByIdAndUserId`) to the "Verified Ownership Coverage" section — `AGENTS.md`
       > Deps: T-111

---

---

## Dependency Graph — Story Completion Order

```
Phase 1 (DB migrations)
    ↓
Phase 2 (boundary + entities + repos + mappers + interfaces)
    ↓
Phase 3 US1 (add coral) — MVP milestone ✓
    ↓
Phase 4 US2 (departure)     ← depends on US1 (coral entry must exist)
Phase 5 US3 (growth)        ← depends on US1 only
Phase 6 US4 (polyp cond.)   ← depends on US1 only
    [US2, US3, US4 are independent of each other — can run in parallel]
    ↓
Phase 7 US5 (catalogue link) ← depends on coral form (US1) + catalogue read ops
    ↓
Phase 8 US6 (propose entry)  ← depends on US5 (catalogue read already in place)
    ↓
Phase 9 US7 (admin approve/reject) ← depends on US6 (proposals must exist)
    ↓
Phase 10 US8 (house reef report) ← depends on US1–US4 (active coral + snapshots)
Phase 11 US9 (AI export)         ← depends on US1–US4 (full history required)
    [US8 and US9 are independent of each other]
    ↓
Polish (E2E tests, i18n check, security review, deployment)
```

**Critical path (MVP)**: T-001→T-008 → T-009→T-045 → T-046→T-059

---

## Parallel Execution Examples

### Sprint 1 — DB + Boundary (all independent, run fully in parallel)
```
Agent A: T-001 → T-002 → T-003 → T-004 → T-005 → T-006 → T-007 (DB migrations)
Agent B: T-009 → T-010 → T-011 → T-012 → T-013 (enums)
Agent C: T-014 → T-015 → T-016 → T-017 → T-018 → T-019 → T-020 (TOs — after enums)
Agent D: T-021 → T-022 → T-023 → T-024 → T-025 → T-026 (export/report TOs)
```

### Sprint 2 — Entities + Repos + Mappers (after Sprint 1)
```
Agent A: T-027 → T-028 → T-029 → T-032 (TankCoralStock + children entities)
Agent B: T-030 → T-031 (Catalogue entities)
Agent C: T-033 (PublicReportLinkEntity modify)
Agent D: T-034 → T-035 → T-036 (Coral stock repos)
          T-037 → T-038 → T-039 (Catalogue repos — can start after entities)
Agent E: T-040 → T-041 (Mappers — after all entities)
          T-042 → T-043 → T-044 → T-045 (Message codes + Service interfaces)
```

### Sprint 3 — US1 server + client (after Sprint 2)
```
Agent A: T-046 → T-047 → T-048 → T-049 (server: services + controllers)
Agent B: T-050 → T-051 (webclient API gateway + NavContext)
Agent C: T-058 (i18n keys — independent of server)
After above: T-052 → T-053 → T-054 → T-055 → T-056 → T-057 → T-059 (CDI beans + XHTML)
```

### Sprint 4 — US2/US3/US4 in parallel (after US1)
```
Agent A: T-060 → T-061 → T-062 → T-063 → T-064 → T-065 (US2 departure)
Agent B: T-066 → T-067 → T-068 → T-069 → T-070 (US3 growth)
Agent C: T-071 → T-072 → T-073 → T-074 → T-075 (US4 polyp)
```

---

## Format Validation

All 118 tasks follow the mandatory checklist format:
- ✅ Every task starts with `- [ ]`
- ✅ Every task has a unique sequential ID (T-001 … T-118)
- ✅ `[P]` marker present on all parallelisable tasks (62 tasks)
- ✅ `[USN]` label on all user-story phase tasks (US1–US9)
- ✅ Every task description includes exact file path(s)
- ✅ Dependencies listed as `> Deps:` sub-items on tasks with prerequisites
- ✅ Setup and Foundational phase tasks have no story label
- ✅ Polish phase tasks have no story label

