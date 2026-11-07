# Data Model: Water Maintenance Tracking

**Feature**: `007-water-maintenance-tracking` | **Date**: 2026-10-26 (reworked 2026-11-06)
**Migrations**: `V1_8_0_15__addAquariumMaintenanceTables.sql` + `V1_8_0_16__generalizeAquariumMaintenance.sql` (sabi-database, `version1_8_0/`)

> **Rework 2026-11-06**: the event table was generalized from `aquarium_water_change` to
> `aquarium_maintenance` so that carbon replacements are stored with the same mechanism as
> water changes (regular interval in the profile + individual events). `V1_8_0_16` renames
> the table, adds `maintenance_type` (NOT NULL DEFAULT 'WATER_CHANGE') and makes
> `percentage` nullable. All existing rows are water changes by definition.

## Overview

Two tables, both owned by an aquarium via a scalar `aquarium_id` FK with `ON DELETE CASCADE` (tank deletion removes all maintenance data — FR-019, same mechanism as `aquarium_dosing`). No JPA relations, no schema prefix in SQL (Flyway config supplies the `sabi` schema), audit columns + optimistic lock via the shared `Auditable` mapped superclass.

```text
aquarium (existing)
   │ 1
   ├──< aquarium_maintenance             (N:1, one row per recorded maintenance event)
   │        maintenance_type = WATER_CHANGE | CARBON_REPLACEMENT
   │
   └─── aquarium_maintenance_profile     (1:1, lazily created; regular interval + carbon setting)
```

---

## Entity 1: `TankMaintenanceProfileEntity` → table `aquarium_maintenance_profile`

**Java**: `de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity extends Auditable`
**Lifecycle**: lazily created on first PUT; reads/exports fall back to defaults when the row is missing (research D2).

| Column | SQL type | Java field | Type | Constraints |
|--------|----------|-----------|------|-------------|
| `id` | BIGINT(20) UNSIGNED AI PK | `id` | `Long` | generated IDENTITY |
| `aquarium_id` | BIGINT(20) UNSIGNED NOT NULL | `aquariumId` | `Long` | UNIQUE, FK → `aquarium.id` ON DELETE CASCADE |
| `regular_interval_days` | INT NULL | `regularIntervalDays` | `Integer` | 1–365 when set; must be set together with `regular_percentage` |
| `regular_percentage` | INT NULL | `regularPercentage` | `Integer` | 1–100 when set; must be set together with `regular_interval_days` |
| `carbon_enabled` | TINYINT(1) NOT NULL DEFAULT 0 | `carbonEnabled` | `Boolean` | — |
| `carbon_mode` | VARCHAR(32) NULL | `carbonMode` | `CarbonMode` (`@Enumerated(STRING)`) | `CONTINUOUS` \| `INTERVAL`; NOT NULL when enabled; NULL when disabled |
| `carbon_replacement_interval_days` | INT NULL | `carbonReplacementIntervalDays` | `Integer` | 1–365; NOT NULL when `carbon_mode = INTERVAL`; NULL otherwise |
| `created_on` / `lastmod_on` / `optlock` | (Auditable) | — | — | standard audit + optimistic lock |

**State rules (enforced in service, FR-010/FR-011)**:
- `carbon_enabled = false` ⇒ service nulls `carbon_mode` and `carbon_replacement_interval_days` before persisting (deterministic display + export).
- Regular interval: both-or-neither; clearing = both NULL (US1 scenario 5).
- The profile row never auto-generates maintenance events from the interval (FR-006).

## Entity 2: `MaintenanceEventEntity` → table `aquarium_maintenance`

**Java**: `de.bluewhale.sabi.persistence.model.MaintenanceEventEntity extends Auditable`
(renamed from `WaterChangeEntity`; table renamed by `V1_8_0_16`)

| Column | SQL type | Java field | Type | Constraints |
|--------|----------|-----------|------|-------------|
| `id` | BIGINT(20) UNSIGNED AI PK | `id` | `Long` | generated IDENTITY |
| `aquarium_id` | BIGINT(20) UNSIGNED NOT NULL | `aquariumId` | `Long` | FK → `aquarium.id` ON DELETE CASCADE |
| `maintenance_type` | VARCHAR(32) NOT NULL | `maintenanceType` | `String` (enum name) | `WATER_CHANGE` \| `CARBON_REPLACEMENT`; fixed at creation, never merged on update |
| `changed_on` | DATE NOT NULL | `changedOn` | `LocalDate` | date-only (no time); MUST NOT be in the future (API validation, FR-002); past dates allowed (FR-006) |
| `percentage` | INT NULL | `percentage` | `Integer` | 1–100, integer — **required for `WATER_CHANGE`, NULL for `CARBON_REPLACEMENT`** (API validation) |
| `note` | VARCHAR(200) NULL | `note` | `String` | optional, max 200 chars (edge case: > 200 rejected) |
| `created_on` / `lastmod_on` / `optlock` | (Auditable) | — | — | standard audit + optimistic lock |

**Index**: `KEY idx_aquarium_water_change_aquarium_changed_on (aquarium_id, changed_on)` (index name kept from `V1_8_0_15` — renaming would be pure churn) — list query is `findByAquariumIdOrderByChangedOnDesc` (newest first, FR-005); export reads the same index. No N+1: one profile query + one events query per tank (constitution II).

---

## Transfer objects (sabi-boundary, `de.bluewhale.sabi.model`)

### `MaintenanceEventType` (enum)

`WATER_CHANGE`, `CARBON_REPLACEMENT` — with `@JsonValue`/`@JsonCreator` (case-insensitive, `DosingType` precedent).

### `MaintenanceEventTo` (request/response for event CRUD)

| Field | Type | Validation |
|-------|------|-----------|
| `id` | `Long` | server-set; null on create |
| `aquariumId` | `Long` | set by server from URL path (never trusted from body) |
| `maintenanceType` | `MaintenanceEventType` | `@NotNull`; fixed at creation — the merge does NOT pick it up on update |
| `changedOn` | `LocalDate` | `@NotNull`; `@AssertTrue` not-in-future (`!changedOn.isAfter(LocalDate.now())`) |
| `percentage` | `Integer` | nullable; `@Min(1) @Max(100)` when present; `@AssertTrue`: required for `WATER_CHANGE`, must be absent for `CARBON_REPLACEMENT` |
| `note` | `String` | `@Size(max = 200)`; trimmed to null in service |
| `createdOn` / `updatedOn` | `LocalDateTime` | server-set (mapped from `lastmodOn`) |
| `optlock` | `long` | server-set; echoed back on PUT (409 on conflict) |

Type-specific rules are enforced by `@AssertTrue` methods on the TO (400 via `RestJSR303ExceptionHandler`), plus defense-in-depth in the service: the service strips `percentage` for `CARBON_REPLACEMENT` and rejects a `WATER_CHANGE` update whose merged percentage ends up null (403 via `errorOr`).

### `WaterChangeIntervalTo` (body of `PUT …/water-change-interval`)

| Field | Type | Validation |
|-------|------|-----------|
| `intervalDays` | `Integer` | nullable; `@Min(1) @Max(365)` when present |
| `percentage` | `Integer` | nullable; `@Min(1) @Max(100)` when present |
| — | — | `@AssertTrue`: both set or both null (clearing = both null) |

### `ActivatedCarbonTo` (body of `PUT …/activated-carbon`)

| Field | Type | Validation |
|-------|------|-----------|
| `enabled` | `Boolean` | `@NotNull` |
| `mode` | `CarbonMode` | nullable; required when enabled (`@AssertTrue`) |
| `replacementIntervalDays` | `Integer` | nullable; `@Min(1) @Max(365)` when present; required when `mode == INTERVAL` (`@AssertTrue`) |

Service behaviour: `enabled == false` ⇒ `mode` and `replacementIntervalDays` are nulled before persisting (FR-011).

### `CarbonMode` (enum)

`CONTINUOUS`, `INTERVAL` — with `@JsonValue`/`JsonCreator` (case-insensitive, `DosingType` precedent).

### `MaintenanceTo` (response of `GET …/maintenance`)

```text
{
  "profile": { regularIntervalDays, regularPercentage, carbonEnabled, carbonMode, carbonReplacementIntervalDays },
  "events": [ MaintenanceEventTo … ]   // newest first, mixed types (water changes + carbon replacements)
}
```

Defaults when no profile row exists: all interval fields null, `carbonEnabled = false`, mode/interval null. The webclient splits `events` by `maintenanceType` into the two UI sections (water changes / carbon replacements).

### `DosingSuggestionsTo` (response of `GET /api/tank/dosing/suggestions`)

| Field | Type | Notes |
|-------|------|-------|
| `productNames` | `List<String>` | distinct, case-insensitive dedup, sorted; values the user already recorded in any of their ACTIVE tanks |
| `categories` | `List<String>` | same rules |
| `dosingMethods` | `List<String>` | same rules |

Backs the autocomplete of the dosing form (rework 2026-11-06, issue 4). Blank values are ignored; lists are empty when the user has no dosing records yet.

---

## Export DTOs (sabi-boundary, `de.bluewhale.sabi.model`) — contract in `contracts/ai-export-maintenance-block.md`

- `MaintenanceExportTo` { `regularInterval`: `RegularIntervalExportTo | null`; `events`: `List<MaintenanceEventRecordExportTo>` (empty when none); `activatedCarbon`: `ActivatedCarbonExportTo` }
- `MaintenanceEventRecordExportTo` { `type`: String (`WATER_CHANGE`/`CARBON_REPLACEMENT`); `date`: String (`yyyy-MM-dd`); `percentage`: Integer (null for carbon replacements); `note`: String (null when absent) }
- `RegularIntervalExportTo` { `intervalDays`: Integer; `percentage`: Integer }
- `ActivatedCarbonExportTo` { `enabled`: Boolean; `mode`: String (`CONTINUOUS`/`INTERVAL`, null when disabled or continuous); `replacementIntervalDays`: Integer (null unless INTERVAL) }

All fields String-dates / enum-names, no PII — identical serialization style to `DosingExportTo`.

---

## Mapper

`de.bluewhale.sabi.mapper.MaintenanceMapper` — MapStruct `@Mapper(componentModel = "spring")`, `@BeanMapping(ignoreByDefault = true)` with explicit `@Mapping` lists, methods: `mapEntityToTo` / `mapEntitiesToTos` (maintenance events), `mapProfileEntityToTo`, `mapIntervalToToProfileEntity` / merge semantics for upsert — structure copied from `DosingMapper`. The event merge deliberately does NOT map `maintenanceType` (type is fixed at creation); a default method `stringToMaintenanceType` maps the stored String to the enum.
