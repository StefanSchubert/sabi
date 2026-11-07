# Phase 0 Research: Water Maintenance Tracking & AI Export Integration

**Feature**: `007-water-maintenance-tracking` | **Date**: 2026-10-26
**Method**: Codebase analysis (dosing feature spec 004/`V1_8_0_12`, AI export spec 001, coral/invertebrate stock patterns) + constitution/DEVELOPERS_MANUAL review. No external research needed — every integration point has an established in-repo precedent.

---

## D1: Storage layout — two new tables (not columns on `aquarium`)

**Decision**: Two new tables in the existing `version1_8_0` Flyway directory:
- `aquarium_maintenance_profile` — 1:1 with `aquarium`, holds the regular water exchange interval and the activated carbon setting.
- `aquarium_water_change` — N:1 with `aquarium`, one row per recorded water change event.

**Rationale**:
- The spec's Key Entities section defines `TankMaintenanceProfile` as a distinct entity (one profile per aquarium).
- Matches the established pattern for all per-tank data (`aquarium_dosing` V1_8_0_12, `aquarium_event`, coral/invertebrate stock tables): standalone table + scalar `aquarium_id` FK with `ON DELETE CASCADE`, no JPA relations, no schema prefix in SQL.
- FR-019 (tank deletion removes all maintenance data) is satisfied by the DB foreign-key cascade — the exact mechanism `aquarium_dosing` already relies on (verified: `TankServiceImpl.removeTank()` deletes the aquarium row; InnoDB cascades the dosing rows; no Java-side cleanup needed).
- No `ALTER TABLE aquarium` required.

**Alternatives considered**:
- *Columns on `aquarium`*: fewer joins, but violates the spec's entity model, touches a core table, and mixes concerns (profile + event history in one place).
- *JSON column for water changes*: not queryable, breaks the JPA/EclipseLink pattern used everywhere.

## D2: Profile lifecycle — lazy row, defaults on read

**Decision**: The profile row is created lazily on the first `PUT`. `GET` and the AI export return a **default profile** (null regular interval, carbon disabled) when no row exists. No backfill migration.

**Rationale**: FR-016 requires the export `maintenance` block to always be present with a stable structure. Service-level defaults achieve that without writing rows for tanks whose owners never use the feature (the existing production DB would otherwise need a data migration).

**Alternatives considered**:
- *Backfill migration creating one profile row per existing aquarium*: rejected — data-dependent migration, writes rows for a feature most tanks may never use.

## D3: API shape — tank-scoped sub-resources under `/api/tank/{tankId}/maintenance`

**Decision**: Six REST endpoints, all JWT-protected (`Authorization: Bearer …`), all ownership-checked:

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/tank/{tankId}/maintenance` | Full block: profile + water change records (newest first) |
| PUT | `/api/tank/{tankId}/maintenance/water-change-interval` | Upsert/clear the regular interval |
| PUT | `/api/tank/{tankId}/maintenance/activated-carbon` | Upsert the carbon setting |
| POST | `/api/tank/{tankId}/maintenance/water-changes` | Create a water change record |
| PUT | `/api/tank/{tankId}/maintenance/water-changes/{recordId}` | Update a water change record |
| DELETE | `/api/tank/{tankId}/maintenance/water-changes/{recordId}` | Delete a water change record |

**Rationale**: The established convention for tank-scoped sub-resources is reusing `Endpoint.TANKS` (`/api/tank`) and appending the path in the webclient gateway impl — exactly what `DosingServiceImpl` does (`TANKS.getPath() + "/" + aquariumId + "/dosings"`). No new `Endpoint` enum entry required.

## D4: Profile updates — two partial PUT endpoints (not one full-profile PUT)

**Decision**: The regular interval and the carbon setting are updated via **separate** endpoints (see D3), each validating only its own fields.

**Rationale**:
- The UI has independent save buttons per section (US1 scenario 4: "sets a regular interval … and saves"; US3: carbon save). Partial updates map 1:1 to the UX.
- A single full-profile PUT would force the webclient to echo the other section's state on every save — a stale-state clobber risk (user edits carbon, saves the interval section → carbon silently re-saved with stale values).
- Validation stays local per section (no cross-section `@AssertTrue` needed).

**Alternatives considered**:
- *Single `PUT /maintenance/profile`*: rejected for the clobber/stale-state reasons above.
- *Two tables, one endpoint each with query flags*: rejected — less RESTful than path segments.

## D5: AI export extension — DTO in sabi-boundary, always-present block, minor schema bump

**Decision**:
- New `MaintenanceExportTo` (+ nested `WaterChangesExportTo`, `RegularIntervalExportTo`, `WaterChangeRecordExportTo`, `ActivatedCarbonExportTo`) in `de.bluewhale.sabi.model` (sabi-boundary), Lombok `@Data`, String dates, enum `.name()` — identical style to `DosingExportTo`.
- New field `private MaintenanceExportTo maintenance = new MaintenanceExportTo();` on `AquariumExportTo` (single object, **always present** — FR-016; all other sub-arrays are always-present lists, same precedent).
- New builder `buildMaintenanceExport(Long aquariumId)` in `ReefDataExportServiceImpl` (injects the two new repositories), called from `buildAquariumExport()` next to `setDosings(...)`.
- `ReefDataExportTo.SCHEMA_VERSION`: `"1.0"` → `"1.1"` (one line). This is a **minor** bump for the additive block, per spec Clarification #4 (2026-10-26); C-3's "bump on breaking changes" mandate remains in force for future major changes.
- Water change dates exported as `yyyy-MM-dd` (`LocalDate.toString()`), matching the `inceptionDate` date-only precedent; carbon `mode` exported as enum name String (`CONTINUOUS`/`INTERVAL`), matching `dosingType`.

**Test impact (verified in code)**:
- `UserProfileControllerTest.testExportMetaBlockIsComplete` hardcodes `"1.0"` (line ~332) → must be updated to `"1.1"`.
- `testExportAquariumStructureContainsAllSubArrayKeys` (T031) → extend to assert the `maintenance` key.
- Add a service-level round-trip test in `ReefDataExportServiceTest` mirroring `testDosingRecordsAreIncludedInExport`, including the FR-016 empty-defaults case.

**Alternatives considered**:
- *Map-based export*: rejected — the codebase uses DTOs exclusively (Jackson 3.x `JsonMapper`, no `Map<String,Object>` in the export path).

## D6: Water change date precision — `LocalDate`, future dates rejected at the API

**Decision**: `WaterChangeEntity.changedOn` is a `LocalDate` (date-only, per spec Assumptions). The form defaults to "today". Future dates are rejected server-side via a Bean Validation `@AssertTrue` method comparing against `LocalDate.now()` (same mechanism as `DosingTo.isDosingEndDateValid()`), plus client-side `maxdate` on the PrimeFaces date picker. Back-dating (past dates) is explicitly allowed (FR-006).

**Alternatives considered**:
- *`LocalDateTime`*: rejected — the spec mandates date-only precision; a time component would be noise for an AI consumer.

## D7: UI — standalone page following the dosing-view pattern; cross-link pre-selection via query parameter

**Decision**:
- New `secured/maintenanceView.xhtml` (masterLayout template, `sabi-back-link` to userportal, tank selector `p:selectOneMenu` with viewMap-persisted selection — the newer `DosingView` pattern, not the older session-scoped `UserSession.selectedTank`).
- Three sub-sections in one page: (1) Water Changes — regular-interval form + new-record form + `p:dataTable` list (newest first, edit/delete icon buttons); (2) Supplements & dosing — static localised hint + link to `dosingView.xhtml`; (3) Activated Carbon — enable checkbox, mode select, replacement-interval input, save.
- Header menu: new `p:menuitem` in `template/header.xhtml` after the dosing entry (`outcome="/secured/maintenanceView"`, icon `pi pi-tint`), label `menu.maintenanceView.l`.
- `PageRegister`: add `MAINTENANCE_VIEW_PAGE("maintenanceView")` for consistency (optional but cheap).
- **Cross-link pre-selection**: no existing mechanism passes a tank between views (verified: no `getRequestParameter("aquariumId")` anywhere in the webclient). To make FR-007's "same tank pre-selected where supported" real, `DosingView.init()` gains minimal query-parameter support: if the viewMap selection is empty and no single-tank auto-select applies, read `aquariumId` from the request parameters and accept it **only if it belongs to the user's own tank list** (validated against `tanks` loaded in init). The maintenance link then navigates to `/secured/dosingView.xhtml?aquariumId=<id>`. This is a small, contained change to `DosingView.init()` (read-only path, no new endpoint).

**Alternatives considered**:
- *Session-scoped `UserSession.selectedTank`*: the older pattern (coral views); the dosing-view pattern is the current house standard for standalone tank pages and keeps the page self-contained.
- *No pre-selection (rely on single-tank auto-select only)*: rejected — for multi-tank users the cross-link would open the dosing view with an empty tank selector, defeating FR-007's purpose.

## D8: Validation — Bean Validation on the TOs (dosing precedent) + service-side normalization

**Decision**:
- `WaterChangeTo`: `@NotNull LocalDate changedOn` + `@AssertTrue` not-in-future; `@NotNull @Min(1) @Max(100) Integer percentage`; `@Size(max = 200) String note`.
- Interval body TO: `Integer intervalDays` (`@Min(1) @Max(365)` when present), `Integer percentage` (`@Min(1) @Max(100)` when present); `@AssertTrue` both-or-neither (clearing = both null).
- Carbon body TO: `Boolean enabled`; `CarbonMode mode` (enum with `@JsonValue`/`@JsonCreator`, `DosingType` precedent); `Integer replacementIntervalDays` (`@Min(1) @Max(365)` when present); `@AssertTrue`: enabled ⇒ mode required; mode == INTERVAL ⇒ replacementIntervalDays required.
- FR-011 ("cleared or ignored" when disabled): the service **nulls** `mode` and `replacementIntervalDays` when `enabled == false` (deterministic, so the export never shows a stale mode).
- Text fields trimmed to null in the service (`DosingServiceImpl.normalize()` precedent).
- Webclient: inline client-side validation (required, ranges) with localised `p:messages`; server remains the authority.

## D9: Ownership — `resolveOwnedAquarium` on every operation, 403 for non-owners including reads

**Decision**: Every service method resolves the caller via `userRepository.getByEmail(userEmail)` and loads the tank with `aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId())` (the exact `DosingServiceImpl.resolveOwnedAquarium` helper). Null ⇒ 403 on all six endpoints — **including the GET**. Record-level operations additionally load via `findByIdAndAquariumId` (dosing precedent). Logging uses `aquarium_id=` only, never the email (PII rule).

**Note on deviation**: `DosingServiceImpl.listDosingsForTank` returns an empty list for non-owners (200). Maintenance intentionally deviates to 403 on reads because SC-006 requires cross-user **read** attempts to be rejected and the block contains tank-specific data. AGENTS.md allows 403 for non-owner access; consistency is kept within the new feature (all six endpoints 403).

## D10: Test strategy — Mockito service tests + `@MockitoBean` controller ITs + export tests + Playwright E2E

**Decision**:
- **Service unit tests** (`MaintenanceServiceTest`, `@ExtendWith(MockitoExtension.class)`, tag `ServiceTest`): ownership rejection (tank + record level), interval upsert/clear semantics, carbon validation + null-on-disable, future-date rejection, trim-to-null.
- **Controller IT** (`MaintenanceControllerIT`, `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@MockitoBean` repositories, tag `ModuleTest`, extends `CommonTestController` — the `DosingControllerIT` pattern): 201/200 happy paths, 403 non-owner (create/update/delete/get), 400 Bean Validation failures, 409 optimistic-lock on PUT.
- **Export tests**: update `UserProfileControllerTest` T030 (`"1.0"` → `"1.1"`) and T031 (add `maintenance` key); new round-trip test in `ReefDataExportServiceTest` (profile + records present with exact values; FR-016 defaults for an empty tank).
- **E2E** (`e2e/tests/maintenanceFlow.spec.ts`, `coralStockFlow.spec.ts` pattern): login → open maintenance view → select tank via PrimeFaces selectOneMenu → set interval → record water change (today + back-dated) → edit/delete → carbon on/off/interval validation → cross-link opens dosing view with tank pre-selected. Real Playwright clicks only (no `force: true`, no `page.evaluate` clicks — AGENTS.md rule), CSS-visibility assertions, cleanup of created records.
- **i18n completeness** (SC-005): script check that every new key exists in all 6 bundle files; byte-check after bundle edits (AGENTS.md mandatory rule).

## D11: Flyway migration — `V1_8_0_15__addAquariumMaintenanceTables.sql`

**Decision**: One new migration in `sabi-database/src/main/resources/db/migration/version1_8_0/` (current head is `V1_8_0_14__addEmailReminderColumns.sql`). Both tables in a single script, following the `V1_8_0_12` dosing migration exactly: backticked identifiers, **no schema prefix** (Flyway config supplies the `sabi` schema), `BIGINT(20) UNSIGNED AUTO_INCREMENT` PK, scalar FK with `ON DELETE CASCADE`, composite index `(aquarium_id, changed_on)`, audit columns `created_on`/`lastmod_on` + `optlock INT UNSIGNED DEFAULT 0`, `ENGINE = InnoDB DEFAULT CHARSET = utf8`.

## D12: i18n — ~20 new keys in all 6 bundles, house naming convention

**Decision**: New keys under the `maintenance.*` namespace plus `menu.maintenanceView.l`, added to all six files in `sabi-webclient/src/main/resources/i18n/` (`messages.properties` fallback + de/en/es/fr/it). Naming follows the dosing block: `menu.<page>.l`, `<feature>.page.title`, `<feature>.field.<name>.l`, `<feature>.list.empty.t`, `<feature>.delete.success.t`. Non-ASCII characters use `\uXXXX` escapes in `messages_de.properties` (its current convention); other files already use raw UTF-8. After every bundle edit: byte-check script per AGENTS.md.

---

## Dependency / version confirmation (SBOM, generated 2026-10-26)

| Component | Version | Relevance |
|-----------|---------|-----------|
| Spring Boot (sabi-server) | 4.1.1 | `@RequestScope` controllers, Bean Validation, springdoc OpenAPI 3.x (constitution III) |
| Jackson (tools.jackson, sabi-server) | 3.1.5 (+ jackson-databind 2.21.5) | Export serialization via `JsonMapper`; DTOs only, no Maps |
| MapStruct (sabi-server) | 1.6.3 | `MaintenanceMapper` follows `DosingMapper` (`@Mapper(componentModel = "spring")`, `ignoreByDefault`) |
| JoinFaces (sabi-webclient) | 6.1.2 | Manages JSF + PrimeFaces 15.x (constitution stack) |
| MariaDB + EclipseLink JPA + Flyway | per constitution | Schema evolution via new migration only (D11) |

No new external dependencies are introduced by this feature (spec NFR Portability).

> **Addendum 2026-11-06 (rework)**: The event table was generalized from `aquarium_water_change`
> to `aquarium_maintenance` with a `maintenance_type` discriminator so that carbon replacements
> are stored with the same mechanism as water changes (see `data-model.md` and `tasks.md`
> "Rework 2026-11-06" for the full scope). No new dependencies were introduced by the rework.
