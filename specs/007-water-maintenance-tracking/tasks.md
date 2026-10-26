# Tasks: Water Maintenance Tracking & AI Export Integration

**Input**: Design documents from `/specs/007-water-maintenance-tracking/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY for this feature — spec NFR (Maintainability) requires "at least one integration test per P1–P4 user story" and research D10 defines the full test strategy (Mockito service tests, `@MockitoBean` controller ITs, export tests, Playwright E2E).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Include exact file paths in descriptions

## Path Conventions

Multi-module Maven project (per plan.md): `sabi-database/` (Flyway), `sabi-boundary/` (DTOs/TOs), `sabi-server/` (REST backend), `sabi-webclient/` (JSF frontend), `e2e/` (Playwright).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Shared prerequisites used by multiple user stories (no new project initialization needed — existing codebase).

- [ ] T001 [P] Add all new i18n keys (~20 `maintenance.*` keys + `menu.maintenanceView.l`) to all six message bundles in `sabi-webclient/src/main/resources/i18n/` — `messages.properties` (fallback), `messages_de.properties`, `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`, `messages_it.properties`; naming per research D12 (`menu.<page>.l`, `<feature>.page.title`, `<feature>.field.<name>.l`, `<feature>.list.empty.t`, `<feature>.delete.success.t`); use `\uXXXX` escapes in `messages_de.properties` (its existing convention), raw UTF-8 in the other files; run a byte-check script after every bundle edit (AGENTS.md mandatory rule — never raw Latin-1 bytes, never U+FFFD)

**Checkpoint**: All UI stories (US1–US3) can reference their labels without blocking on i18n

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T002 Create Flyway migration `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_15__addAquariumMaintenanceTables.sql` (current head is `V1_8_0_14`; backticked identifiers, NO schema prefix, `ENGINE = InnoDB DEFAULT CHARSET = utf8`, following `V1_8_0_12` dosing migration exactly) with both tables:
  - `aquarium_maintenance_profile`: `id BIGINT(20) UNSIGNED AUTO_INCREMENT PK`; `aquarium_id BIGINT(20) UNSIGNED NOT NULL` UNIQUE, FK → `aquarium.id` ON DELETE CASCADE; `regular_interval_days INT NULL` (1–365 when set; must be set together with `regular_percentage`); `regular_percentage INT NULL` (1–100 when set; must be set together with `regular_interval_days`); `carbon_enabled TINYINT(1) NOT NULL DEFAULT 0`; `carbon_mode VARCHAR(32) NULL` (`CONTINUOUS` | `INTERVAL`, NOT NULL when enabled, NULL when disabled); `carbon_replacement_interval_days INT NULL` (1–365; NOT NULL when `carbon_mode = INTERVAL`, NULL otherwise); audit columns `created_on`/`lastmod_on` + `optlock INT UNSIGNED DEFAULT 0`
  - `aquarium_water_change`: `id BIGINT(20) UNSIGNED AUTO_INCREMENT PK`; `aquarium_id BIGINT(20) UNSIGNED NOT NULL`, FK → `aquarium.id` ON DELETE CASCADE; `changed_on DATE NOT NULL` (date-only, no time component); `percentage INT NOT NULL` (1–100, integer); `note VARCHAR(200) NULL`; audit columns + `optlock INT UNSIGNED DEFAULT 0`; index `KEY idx_aquarium_water_change_aquarium_changed_on (aquarium_id, changed_on)`
- [ ] T003 [P] Create JPA entities in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/` — `TankMaintenanceProfileEntity.java` (extends `Auditable`, table `aquarium_maintenance_profile`, `@Enumerated(STRING)` for `carbonMode`) and `WaterChangeEntity.java` (extends `Auditable`, table `aquarium_water_change`, `LocalDate changedOn`)
- [ ] T004 [P] Create repositories in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/` — `TankMaintenanceProfileRepository.java` (methods: `findByAquariumId`, `save`) and `WaterChangeRepository.java` (methods: `findByAquariumIdOrderByChangedOnDesc`, `findByIdAndAquariumId`)
- [ ] T005 [P] Create transfer objects in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/` — `CarbonMode.java` (enum `CONTINUOUS` | `INTERVAL` with `@JsonValue`/`@JsonCreator`, case-insensitive, `DosingType` precedent), `WaterChangeTo.java` (`changedOn` `@NotNull` + `@AssertTrue` not-in-future `!changedOn.isAfter(LocalDate.now())`; `percentage` `@NotNull @Min(1) @Max(100)`; `note` `@Size(max = 200)`), `WaterChangeIntervalTo.java` (`intervalDays` nullable `@Min(1) @Max(365)`; `percentage` nullable `@Min(1) @Max(100)`; `@AssertTrue`: both set or both null — clearing = both null), `ActivatedCarbonTo.java` (`enabled` `@NotNull`; `mode` required when enabled; `replacementIntervalDays` nullable `@Min(1) @Max(365)`, required when `mode == INTERVAL`), `MaintenanceProfileTo.java`, `MaintenanceTo.java` (profile + `waterChanges[]`)
- [ ] T006 Create MapStruct mapper `sabi-server/src/main/java/de/bluewhale/sabi/mapper/MaintenanceMapper.java` (`@Mapper(componentModel = "spring")`, `@BeanMapping(ignoreByDefault = true)` with explicit `@Mapping` lists; methods `mapEntityToTo`/`mapEntitiesToTos`, `mapProfileEntityToTo`, upsert merge semantics — structure copied from `DosingMapper`)

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel

---

## Phase 3: User Story 1 — Record Water Changes and Define a Regular Interval (Priority: P1) 🎯 MVP

**Goal**: Core maintenance data — individual water change records (date + percentage 1–100 + optional note ≤ 200 chars, back-dating allowed) plus the optional regular exchange interval (days 1–365 + percentage 1–100) per tank, with full CRUD UI in the new standalone Maintenance page.

**Independent Test**: Log in as a user with at least one aquarium, open the Maintenance section (header menu), set a regular interval, record two water changes (one with today's date, one back-dated), edit one, and verify the list displays everything correctly in chronological order (newest first).

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T007 [P] [US1] Create service unit test `sabi-server/src/test/java/de/bluewhale/sabi/services/MaintenanceServiceTest.java` (`@ExtendWith(MockitoExtension.class)`, tag `ServiceTest`): ownership rejection at tank level AND record level (403), interval upsert/clear semantics (both-or-neither, clear = both NULL), future-date rejection, trim-to-null normalization
- [ ] T008 [P] [US1] Create controller IT `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/MaintenanceControllerIT.java` (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@MockitoBean` repositories, tag `ModuleTest`, extends `CommonTestController` — the `DosingControllerIT` pattern): 201/200 happy paths, 403 non-owner on ALL endpoints including GET (SC-006), 400 Bean Validation failures (percentage 0/101, future date, note > 200 chars), 409 optimistic-lock on PUT with stale `optlock`

### Implementation for User Story 1

- [ ] T009 [US1] Implement service in `sabi-server/src/main/java/de/bluewhale/sabi/services/MaintenanceService.java` (interface) and `MaintenanceServiceImpl.java` — `resolveOwnedAquarium` helper (`userRepository.getByEmail(userEmail)` → `aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId())`, null ⇒ 403 on every operation INCLUDING reads), interval upsert/clear (idempotent, creates profile row if missing; both-or-neither), water change create/update/delete (record-level load via `findByIdAndAquariumId`), trim-to-null normalization (`DosingServiceImpl.normalize()` precedent), PII-safe logging (`aquarium_id=` only, never the email)
- [ ] T010 [US1] Implement REST controller `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/MaintenanceController.java` (`@RequestScope`, dosing controller pattern) with 5 endpoints: `GET /api/tank/{tankId}/maintenance` (full block, defaults when no profile row: `carbonEnabled = false`, interval fields null, `waterChanges = []`), `PUT …/maintenance/water-change-interval`, `POST …/maintenance/water-changes` (201), `PUT …/maintenance/water-changes/{recordId}` (409 on optlock mismatch), `DELETE …/maintenance/water-changes/{recordId}`; all with `@Operation`/`@ApiResponses` springdoc annotations; 403 non-owner (bare, dosing `errorOr(...)` precedent)
- [ ] T011 [P] [US1] Create webclient gateway in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/` — `MaintenanceService.java` (interface) and `MaintenanceServiceImpl.java` (extends `APIServiceImpl`; URI built as `sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + tankId + "/maintenance…"` — no new `Endpoint` enum entry; auth via `RestHelper.prepareAuthedHttpHeader(token)`)
- [ ] T012 [US1] Create view bean `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/MaintenanceView.java` (`@Named @RequestScope extends AbstractControllerTools`, viewMap-persisted tank selection — the `DosingView` pattern, NOT session-scoped `UserSession.selectedTank`) and add `MAINTENANCE_VIEW_PAGE("maintenanceView")` to `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/utils/PageRegister.java`
- [ ] T013 [US1] Create page `sabi-webclient/src/main/resources/META-INF/resources/secured/maintenanceView.xhtml` (masterLayout template, `sabi-back-link` breadcrumb + Cancel button per UI_StyleGuide) with the **Water Changes** section: regular-interval form (days 1–365 + percentage 1–100, save/clear), new-record form (date picker defaulting to today with `maxdate` = today, percentage 1–100, note ≤ 200 chars), `p:dataTable` list newest first with edit/delete icon buttons; inline client-side validation with localised `p:messages`; save button `background:#065f46` per UI_StyleGuide
- [ ] T014 [P] [US1] Add header menu entry in `sabi-webclient/src/main/resources/META-INF/resources/template/header.xhtml` — new `p:menuitem` after the dosing entry (`outcome="/secured/maintenanceView"`, icon `pi pi-tint`, label `menu.maintenanceView.l`)

**Checkpoint**: User Story 1 fully functional — water changes + regular interval recordable and displayed independently (MVP)

---

## Phase 4: User Story 2 — Cross-Link to the Existing Dosing Feature (Priority: P2)

**Goal**: "Supplements & dosing" section on the Maintenance page with localised hint text and a link to the Dosing view, opening it with the same tank pre-selected (no new supplement data model — Clarifications 2026-10-26).

**Independent Test**: Open the Maintenance page for a tank, verify the supplements section shows the hint text and the link to the Dosing feature; follow the link and verify the Dosing view opens with the tank pre-selected; record a dosing entry there and verify it appears in the Dosing list.

### Implementation for User Story 2

- [ ] T015 [P] [US2] Add the **Supplements & dosing** section to `sabi-webclient/src/main/resources/META-INF/resources/secured/maintenanceView.xhtml` — localised hint text explaining that supplement usage is documented in the existing Dosing feature (FR-007) + link/button navigating to `/secured/dosingView.xhtml?aquariumId=<selected tank id>`
- [ ] T016 [P] [US2] Modify `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/DosingView.java` — `init()` accepts a validated `aquariumId` request parameter: only when the viewMap selection is empty AND no single-tank auto-select applies, read `aquariumId` from the request parameters and accept it ONLY if it belongs to the user's own tank list (validated against `tanks` loaded in init); read-only path, no new endpoint

**Checkpoint**: User Stories 1 AND 2 both work independently

---

## Phase 5: User Story 3 — Configure Activated Carbon Filtration (Priority: P3)

**Goal**: Per-tank activated carbon setting (enabled; mode `CONTINUOUS` | `INTERVAL`; replacement interval 1–365 days required only for INTERVAL mode), with deterministic clearing on disable (FR-011).

**Independent Test**: In a tank's Maintenance section, enable activated carbon in interval mode with a replacement interval, save and verify display; switch to continuous mode and verify the replacement interval is no longer required or shown; disable carbon and verify the section shows "not used".

### Implementation for User Story 3

- [ ] T017 [US3] Extend `sabi-server/src/main/java/de/bluewhale/sabi/services/MaintenanceServiceImpl.java` — carbon upsert: `enabled` required; enabled ⇒ `mode` required; `mode == INTERVAL` ⇒ `replacementIntervalDays` (1–365) required; `enabled == false` ⇒ service nulls `mode` and `replacementIntervalDays` before persisting (FR-011, deterministic display + export)
- [ ] T018 [US3] Add endpoint `PUT /api/tank/{tankId}/maintenance/activated-carbon` to `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/MaintenanceController.java` (body `ActivatedCarbonTo`; 400 on validation failure; 200 → updated `MaintenanceTo.profile`; 403 non-owner; `@Operation`/`@ApiResponses` annotations)
- [ ] T019 [P] [US3] Add the **Activated Carbon** section to `sabi-webclient/src/main/resources/META-INF/resources/secured/maintenanceView.xhtml` — enable checkbox, mode select (`CONTINUOUS`/`INTERVAL`, localised labels), replacement-interval input (days 1–365, required only in INTERVAL mode), save; display current setting at a glance or "not used" (localised) when disabled

**Checkpoint**: All user stories independently functional (US4 export depends on US1–US3 data existing)

---

## Phase 6: User Story 4 — Maintenance Data in the AI-JSON Export (Priority: P4)

**Goal**: Always-present per-aquarium `maintenance` block in the AI-JSON export (spec 001) with exact values, ISO-8601 dates, no PII, and `sabiSchemaVersion` bumped minor `1.0` → `1.1` (additive only — FR-015/FR-016).

**Independent Test**: With at least one tank containing a regular interval, two water change records, and an activated carbon setting, download the AI export and verify the `maintenance` block contains all values exactly as entered; a tank without maintenance data still gets the full block with `regularInterval: null`, `records: []`, carbon disabled.

### Tests for User Story 4 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T020 [P] [US4] Update `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/UserProfileControllerTest.java` — `testExportMetaBlockIsComplete`: expected schema version `"1.0"` → `"1.1"`; `testExportAquariumStructureContainsAllSubArrayKeys`: additionally assert the `maintenance` key is present in every aquarium block
- [ ] T021 [P] [US4] Extend `sabi-server/src/test/java/de/bluewhale/sabi/services/ReefDataExportServiceTest.java` — round-trip test mirroring `testDosingRecordsAreIncludedInExport`: profile + records present with exact values (dates `yyyy-MM-dd`, newest first, note as entered); FR-016 empty-defaults case for an aquarium with no maintenance data (`regularInterval: null`, `records: []`, carbon disabled)

### Implementation for User Story 4

- [ ] T022 [US4] Create export DTOs in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/` — `MaintenanceExportTo.java`, `WaterChangesExportTo.java` (`regularInterval` object | null + `records` list, empty when none), `RegularIntervalExportTo.java` (`intervalDays`, `percentage`), `WaterChangeRecordExportTo.java` (`date` String `yyyy-MM-dd`, `percentage`, `note` null when absent), `ActivatedCarbonExportTo.java` (`enabled` Boolean, `mode` String `CONTINUOUS`/`INTERVAL` null when disabled or continuous, `replacementIntervalDays` Integer null unless INTERVAL) — Lombok `@Data`, String dates, enum `.name()`, no PII (identical serialization style to `DosingExportTo`)
- [ ] T023 [US4] Modify `sabi-boundary/src/main/java/de/bluewhale/sabi/model/ReefDataExportTo.java` (`SCHEMA_VERSION` `"1.0"` → `"1.1"`, one line) and `sabi-boundary/src/main/java/de/bluewhale/sabi/model/AquariumExportTo.java` (+ field `private MaintenanceExportTo maintenance = new MaintenanceExportTo();` — single object, always present per FR-016)
- [ ] T024 [US4] Modify `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java` — inject `TankMaintenanceProfileRepository` + `WaterChangeRepository`; add builder `buildMaintenanceExport(Long aquariumId)` (null-guarded `.toString()` date idiom, mode via enum name — identical to `buildDosingExports`); call it from `buildAquariumExport()` next to `setDosings(...)`; exactly 2 queries per tank (profile + records, no N+1 — constitution II)

**Checkpoint**: Export contains the complete `maintenance` block (SC-002); all four stories delivered

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements and validations that affect multiple user stories

- [ ] T025 [P] Create Playwright E2E `e2e/tests/maintenanceFlow.spec.ts` (`coralStockFlow.spec.ts` pattern): login → open Maintenance view → select tank via PrimeFaces selectOneMenu → set/clear regular interval → record water change (today + back-dated) → edit/delete → carbon on/off/interval validation → cross-link opens dosing view with tank pre-selected; REAL clicks only (no `force: true`, no `page.evaluate(() => el.click())` — AGENTS.md rule), explicit visibility assertions, CSS-visibility checks (computed width/height > 0 for all interactive elements), cleanup of created records
- [ ] T026 [P] i18n completeness check (SC-005): write `/tmp/check_i18n_maintenance.py` verifying every new `maintenance.*` key + `menu.maintenanceView.l` exists in all six bundles under `sabi-webclient/src/main/resources/i18n/`; run it and confirm zero missing keys (missing keys are a release blocker — FR-017)
- [ ] T027 Run `quickstart.md` validation — all 5 scenarios: (1) water changes & interval, (2) cross-link with tank pre-selection, (3) activated carbon incl. validation errors, (4) AI export vs `contracts/ai-export-maintenance-block.md` incl. FR-016 empty shape and additive-only comparison, (5) security & deletion — cross-user 403 curl checks on all six endpoints + tank-deletion cascade verification (FR-018/FR-019)
- [ ] T028 Run the full backend suite `cd sabi-server && mvn test` (CI gate — constitution V) and redeploy the local stack with `cd devops/sabi_docker_sdk && bash server_redeploy.sh --flyway` (applies `V1_8_0_15`)
- [ ] T029 [P] Add the feature entry to `CHANGELOG.md` (repository root) under a new next-release heading following the existing structure — Feature: per-aquarium Maintenance page (water changes + regular exchange interval, supplements cross-link to Dosing, activated carbon filtering) and `maintenance` block in the AI-JSON export (`sabiSchemaVersion` 1.0 → 1.1); maintainer finalises the release number at release time

---

## Phase 8: ISO 25010 Quality Gates *(SABI-Konstitution v1.0.0)*

**Purpose**: Sicherstellen, dass alle acht ISO 25010 Qualitätsmerkmale vor dem Merge erfüllt sind.

- [ ] QGATE-I   Funktionale Eignung: Alle Akzeptanzszenarien aus spec.md abgehakt (US1: 7, US2: 3, US3: 5, US4: 5 Szenarien) inkl. Edge Cases (Percentage out of range, future-dated record, back-dating, tank deletion, cross-user access, note > 200 chars, carbon interval out of range)?
- [ ] QGATE-II  Leistungseffizienz: Maintenance-Seite lädt < 2 s (Constitution II); Export-Generierung bleibt im ~3 s Budget bei bis zu 500 Water-Change-Records pro Tank (SC-003); Queries indiziert über `(aquarium_id, changed_on)`, keine N+1 (exakt 2 Queries pro Tank im Export)?
- [ ] QGATE-III Kompatibilität: OpenAPI-Doku aktuell via springdoc `@Operation`/`@ApiResponses` auf allen 6 Endpunkten; Export-Erweiterung ausschließlich additiv, `sabiSchemaVersion` Minor-Bump 1.0 → 1.1, alle bestehenden Felder unverändert (FR-015)?
- [ ] QGATE-IV  Benutzbarkeit: i18n vollständig in allen 6 Bundles (SC-005, zero missing keys), WCAG 2.1 AA Kontraste gemäß UI_StyleGuide (Save `#065f46`, Links `#0369a1`/`#075985`), Water-Change-Erfassung in ≤ 3 Interaktionen (SC-001)?
- [ ] QGATE-V   Zuverlässigkeit: CI grün (Maven + CodeQL); `@Transactional` Services ohne Partial State; 409 auf Optimistic-Lock-Konflikt; Tank-Deletion kaskadiert Maintenance-Daten konsistent (FR-019)?
- [ ] QGATE-VI  Sicherheit: OWASP Dependency Check grün (keine neuen Dependencies); 403 für Non-Owners auf ALLEN 6 Endpunkten inkl. Reads (SC-006); PII-sicheres Logging (`aquarium_id=` only) und keine PII im Export (FR-014)?
- [ ] QGATE-VII Wartbarkeit: JUnit-Tests für alle P1–P4 Stories vorhanden (ServiceTest + ModuleTest + Export-Tests); Flyway-Migration `V1_8_0_15` korrekt, bestehende Migrationsskripte unverändert?
- [ ] QGATE-VIII Übertragbarkeit: Docker-Build AMD64 + ARM OK (keine neuen Runtime-Dependencies, kein plattformspezifischer Code); Ansible/Deployment unverändert?

**Checkpoint**: Alle zutreffenden QGATE-Tasks abgehakt → Feature ist merge-bereit

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3–6)**: All depend on Foundational phase completion
  - US1 (P1) is the MVP and has no dependencies on other stories
  - US2/US3 (P2/P3) share `maintenanceView.xhtml` with US1 → run sequentially after US1 (same file), but are independently testable
  - US4 (P4) depends on US1–US3 producing data, but its export code is independently testable via the FR-016 empty-defaults case
- **Polish (Phase 7)**: Depends on all desired user stories being complete
- **Quality Gates (Phase 8)**: Final gate before merge

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — no dependencies on other stories
- **User Story 2 (P2)**: Can start after US1 (shared `maintenanceView.xhtml` file) — independently testable per spec
- **User Story 3 (P3)**: Can start after US1 (shared `maintenanceView.xhtml` + controller file) — independently testable per spec
- **User Story 4 (P4)**: Can start after Foundational for its code (export DTOs + service builder); full validation needs US1–US3 data

### Within Each User Story

- Tests MUST be written and FAIL before implementation (US1: T007/T008, US4: T020/T021)
- Models/entities before services, services before endpoints, endpoints before UI
- Core implementation before integration

### Parallel Opportunities

- Phase 1: T001 alone (single file set, one task)
- Phase 2: T003, T004, T005 in parallel (different files; all depend on T002 only for compilation context — entities/repos/TOs are independent of each other); T006 after T003 + T005
- US1: T007 + T008 (tests) in parallel; T011 + T014 in parallel with the service/controller chain (T009 → T010, T012 → T013)
- US2: T015 + T016 in parallel (different files)
- US3: T019 (XHTML section) in parallel with T017 → T018
- US4: T020 + T021 (tests) in parallel; T023 after T022, T024 after both
- Polish: T025 + T026 + T029 in parallel; T027/T028 after all stories

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (write FIRST, must FAIL):
Task: "Create service unit test in sabi-server/src/test/java/de/bluewhale/sabi/services/MaintenanceServiceTest.java"
Task: "Create controller IT in sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/MaintenanceControllerIT.java"

# Launch independent UI tasks in parallel with the backend chain:
Task: "Create webclient gateway in sabi-webclient/.../apigateway/MaintenanceService.java + MaintenanceServiceImpl.java"
Task: "Add header menu entry in sabi-webclient/.../template/header.xhtml"

# Backend chain (sequential): service → controller
Task: "Implement MaintenanceService + MaintenanceServiceImpl in sabi-server/.../services/"
Task: "Implement MaintenanceController (5 endpoints) in sabi-server/.../rest/controller/"

# UI chain (sequential): view bean → XHTML page
Task: "Create MaintenanceView.java + PageRegister entry in sabi-webclient/.../"
Task: "Create maintenanceView.xhtml (Water Changes section) in sabi-webclient/.../secured/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (i18n keys)
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently (spec US1 Independent Test + T007/T008 green)
5. Deploy/demo if ready — the core data ("how often do you change water and to which percentage?") is already usable

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo (cross-link live)
4. Add User Story 3 → Test independently → Deploy/Demo (carbon settings live)
5. Add User Story 4 → Test independently → Deploy/Demo (AI export complete — the feature's explicit purpose)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T003/T004/T005 in parallel)
2. Once Foundational is done:
   - Developer A: User Story 1 (backend chain T009 → T010)
   - Developer B: User Story 1 UI (T011 → T012 → T013, T014)
   - After US1: Developer A takes US2/US3 backend, Developer B takes US4 (export DTOs + tests are independent of the UI)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD for US1/US4 per research D10)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same-file conflicts (note: T013/T015/T019 all edit `maintenanceView.xhtml` — keep them in separate phases, never parallel), cross-story dependencies that break independence
- i18n edits: byte-check after every bundle change (AGENTS.md mandatory rule); `messages_de.properties` uses `\uXXXX` escapes
- XHTML changes ALWAYS require a server restart before testing (no Facelets hot-reload — AGENTS.md)
