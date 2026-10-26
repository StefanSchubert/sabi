# Data Model: Water Maintenance Tracking

**Feature**: `007-water-maintenance-tracking` | **Date**: 2026-10-26
**Migration**: `V1_8_0_15__addAquariumMaintenanceTables.sql` (sabi-database, `version1_8_0/`)

## Overview

Two new tables, both owned by an aquarium via a scalar `aquarium_id` FK with `ON DELETE CASCADE` (tank deletion removes all maintenance data — FR-019, same mechanism as `aquarium_dosing`). No JPA relations, no schema prefix in SQL (Flyway config supplies the `sabi` schema), audit columns + optimistic lock via the shared `Auditable` mapped superclass.

```text
aquarium (existing)
   │ 1
   ├──< aquarium_water_change            (N:1, one row per recorded water change)
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
- The profile row never auto-generates water change records from the interval (FR-006).

## Entity 2: `WaterChangeEntity` → table `aquarium_water_change`

**Java**: `de.bluewhale.sabi.persistence.model.WaterChangeEntity extends Auditable`

| Column | SQL type | Java field | Type | Constraints |
|--------|----------|-----------|------|-------------|
| `id` | BIGINT(20) UNSIGNED AI PK | `id` | `Long` | generated IDENTITY |
| `aquarium_id` | BIGINT(20) UNSIGNED NOT NULL | `aquariumId` | `Long` | FK → `aquarium.id` ON DELETE CASCADE |
| `changed_on` | DATE NOT NULL | `changedOn` | `LocalDate` | date-only (no time); MUST NOT be in the future (API validation, FR-002); past dates allowed (FR-006) |
| `percentage` | INT NOT NULL | `percentage` | `Integer` | 1–100, integer (FR-002) |
| `note` | VARCHAR(200) NULL | `note` | `String` | optional, max 200 chars (edge case: > 200 rejected) |
| `created_on` / `lastmod_on` / `optlock` | (Auditable) | — | — | standard audit + optimistic lock |

**Index**: `KEY idx_aquarium_water_change_aquarium_changed_on (aquarium_id, changed_on)` — list query is `findByAquariumIdOrderByChangedOnDesc` (newest first, FR-005); export reads the same index. No N+1: one profile query + one records query per tank (constitution II).

---

## Transfer objects (sabi-boundary, `de.bluewhale.sabi.model`)

### `WaterChangeTo` (request/response for water change CRUD)

| Field | Type | Validation |
|-------|------|-----------|
| `id` | `Long` | server-set; null on create |
| `aquariumId` | `Long` | set by server from URL path (never trusted from body) |
| `changedOn` | `LocalDate` | `@NotNull`; `@AssertTrue` not-in-future (`!changedOn.isAfter(LocalDate.now())`) |
| `percentage` | `Integer` | `@NotNull @Min(1) @Max(100)` |
| `note` | `String` | `@Size(max = 200)`; trimmed to null in service |
| `createdOn` / `updatedOn` | `LocalDateTime` | server-set (mapped from `lastmodOn`) |
| `optlock` | `long` | server-set; echoed back on PUT (409 on conflict) |

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

`CONTINUOUS`, `INTERVAL` — with `@JsonValue`/`@JsonCreator` (case-insensitive, `DosingType` precedent).

### `MaintenanceTo` (response of `GET …/maintenance`)

```text
{
  "profile": { regularIntervalDays, regularPercentage, carbonEnabled, carbonMode, carbonReplacementIntervalDays },
  "waterChanges": [ WaterChangeTo … ]   // newest first
}
```

Defaults when no profile row exists: all interval fields null, `carbonEnabled = false`, mode/interval null.

---

## Export DTOs (sabi-boundary, `de.bluewhale.sabi.model`) — contract in `contracts/ai-export-maintenance-block.md`

- `MaintenanceExportTo` { `waterChanges`: `WaterChangesExportTo`; `activatedCarbon`: `ActivatedCarbonExportTo` }
- `WaterChangesExportTo` { `regularInterval`: `RegularIntervalExportTo | null`; `records`: `List<WaterChangeRecordExportTo>` (empty when none) }
- `RegularIntervalExportTo` { `intervalDays`: Integer; `percentage`: Integer }
- `WaterChangeRecordExportTo` { `date`: String (`yyyy-MM-dd`); `percentage`: Integer; `note`: String (null when absent) }
- `ActivatedCarbonExportTo` { `enabled`: Boolean; `mode`: String (`CONTINUOUS`/`INTERVAL`, null when disabled or continuous); `replacementIntervalDays`: Integer (null unless INTERVAL) }

All fields String-dates / enum-names, no PII — identical serialization style to `DosingExportTo`.

---

## Mapper

`de.bluewhale.sabi.mapper.MaintenanceMapper` — MapStruct `@Mapper(componentModel = "spring")`, `@BeanMapping(ignoreByDefault = true)` with explicit `@Mapping` lists, methods: `mapEntityToTo` / `mapEntitiesToTos` (water changes), `mapProfileEntityToTo`, `mapIntervalToToProfileEntity` / merge semantics for upsert — structure copied from `DosingMapper`.
