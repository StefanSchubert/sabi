# Research: 005-coral-stock

**Phase 0 output for `/speckit.plan`**  
**Date**: 2026-05-22  
**Spec**: specs/005-coral-stock/spec.md

---

## Decision 1 — CoralDepartureReason: New Enum vs Reusing `DepartureReason`

**Decision**: Introduce a new enum `CoralDepartureReason` in `sabi-boundary`.

**Rationale**: The existing `DepartureReason` enum (used by fish stock) has values `DECEASED`, `REMOVED_REHOMED`, `UNKNOWN` — these are fish-specific and do NOT match the spec values for coral: `DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`. Reusing the fish enum would require either adding coral-specific values to a fish-owned enum (violating single-responsibility) or keeping mismatched constants. A dedicated `CoralDepartureReason` enum mirrors the fish pattern and keeps boundary concerns isolated per feature.

**Alternatives considered**:
- Extend `DepartureReason` with coral values → rejected: pollutes fish enum, confusing cross-domain usage.
- Use `String` column with no enum → rejected: no compile-time safety, harder validation.

---

## Decision 2 — CoralCatalogueStatus: New Enum vs Reusing `FishCatalogueStatus`

**Decision**: Reuse `FishCatalogueStatus` (`PENDING`, `PUBLIC`, `REJECTED`) for coral catalogue entry status.

**Rationale**: The UGC workflow states (Pending/Public/Rejected) are fully identical to the fish catalogue workflow. There is no coral-specific status variant. Creating a `CoralCatalogueStatus` would be pure duplication. `FishCatalogueStatus` already has correct Jackson annotations and Java serialization support.

**Alternatives considered**:
- New `CoralCatalogueStatus` enum → rejected: pure duplication.
- Generic `UGCStatus` enum → possible future refactor, but deferred to avoid scope creep.

---

## Decision 3 — Growth Measurement Storage: Typed Column vs JSONB

**Decision**: Use relational columns — a `VARCHAR measurement_type` column and a `DECIMAL(8,1) measurement_value` column.

**Rationale**: The spec defines exactly four measurement types and two value precision rules (integer for BRANCH_COUNT, decimal for others). Relational columns allow indexed queries by type, DB-level constraints, and are consistent with the existing `fish_size_history` table pattern (single `size_cm DECIMAL(5,1)` column). JSON would complicate partial-update and sorting queries without benefit.

**Alternatives considered**:
- JSONB blob per measurement record → rejected: no indexing on type, over-engineered for 4 types.
- Separate columns per type → rejected: sparse rows, schema change required for each new type.

---

## Decision 4 — Polyp Condition Storage: VARCHAR vs ENUM Column

**Decision**: Store `polyp_condition` as `VARCHAR(30)` with an application-layer enum, matching how `departure_reason` is handled in the fish table.

**Rationale**: MariaDB's `ENUM` type requires ALTER TABLE to add new values — a disruptive migration. `VARCHAR(30)` with an application enum (Java side enforces the value set) allows future additions without DDL. The same pattern is used consistently across the project (e.g., `departure_reason VARCHAR(30)`, `status VARCHAR(10)`).

**Alternatives considered**:
- MariaDB `ENUM` type → rejected: inflexible for future additions.
- Integer codes → rejected: opaque, makes DB debugging harder.

---

## Decision 5 — Database Migration Version Directory

**Decision**: Add migrations to a new `version1_7_0/` directory under `sabi-database/src/main/resources/db/migration/`.

**Rationale**: The latest existing migration directory is `version1_6_0`. Feature 005 introduces substantial schema additions (5 new tables, 1 ALTER TABLE) that constitute a minor version bump. Following the established naming convention (`versionX_Y_Z`), the next version is `1.7.0`.

**Alternatives considered**:
- Continue at `version1_6_0` with sequential patch numbers → possible but semantically misleading for a feature-sized addition.
- New top-level version `2.0.0` → overkill; no breaking changes.

---

## Decision 6 — CoralExportTo: Extend Existing vs Full Replacement

**Decision**: Extend the existing `CoralExportTo` class.

**Rationale**: The class `CoralExportTo` already exists in `sabi-boundary` with two stub fields (`coralCatalogueId`, `scientificName`, `observedBehavior`). The spec requires substantially more fields. The class is not yet used by any service implementation (it was a placeholder). We extend it in place — no backwards-compatibility concern since no released endpoint uses it yet.

**Alternatives considered**:
- Delete and recreate → semantically equivalent; extension preferred for clarity.

---

## Decision 7 — Growth History Chart: p:chart from PrimeFaces vs External Library

**Decision**: Use PrimeFaces `<p:lineChart>` (already in the dependency tree as PrimeFaces 15.x) for the line chart toggle view.

**Rationale**: PrimeFaces `p:lineChart` is a first-class PrimeFaces component, requires no new dependency, and integrates naturally with JSF's component model. The `toggleable` pattern is already used in the fish size history view (`fishStockEntryPage.xhtml`). No new external JavaScript libraries are introduced — consistent with C-6 (no new runtime dependencies).

**Alternatives considered**:
- Chart.js via CDN → rejected: violates C-6 (external runtime service), adds async loading complexity.
- Apache ECharts → rejected: same concerns.

---

## Decision 8 — Photo Storage: Coral-specific directory vs shared `fish/` directory

**Decision**: Store coral photos in a separate configurable subdirectory of the existing photo volume (e.g., `photos/coral/{userId}/{coralId}.jpg`), structurally parallel to fish photos (`photos/fish/{userId}/{fishId}.jpg`).

**Rationale**: C-4 mandates same-volume storage as fish photos. Using a `coral/` subdirectory avoids any filename collision between fish and coral photo IDs (both are `BIGINT` sequences). The `photos/` root path is injected via Spring configuration (`sabi.photo.dir`), so no new Spring config key is needed — just a subdirectory convention.

**Alternatives considered**:
- Single flat directory → rejected: collision risk between fish and coral IDs longer term.
- Separate `sabi.coral.photo.dir` config key → rejected: unnecessary overhead.

---

## Decision 9 — Coral Stock Tab: saltwater-only guard

**Decision**: Guard the Coral Stock tab at JSF render level via `rendered="#{tankDetailView.tank.waterType == 'MARINE'}"` and at service level via an aquarium water-type check before saving coral entries.

**Rationale**: Spec assumption and C-8: coral stock is only valid for marine/saltwater aquariums. The aquarium's `waterType` attribute already exists in `AquariumTo`/`AquariumEntity`. A double-guard (UI + service) prevents both accidental display and data-integrity violations.

---

## Decision 10 — House Reef Report coral opt-in flag

**Decision**: Add a single `include_corals TINYINT(1)` boolean column to the existing `public_report_link` table — directly parallel to the `include_events` column added in spec 004 (V1_6_0_2).

**Rationale**: The spec says "same opt-in mechanism used for events in spec 004". The `include_events` pattern is already implemented and working. Reusing the exact same column pattern is the path of least resistance and keeps the table structure symmetric.

---

## Summary: All NEEDS CLARIFICATION Resolved

| Topic | Resolution |
|-------|-----------|
| CoralDepartureReason enum values | New enum: DIED, SOLD, GIVEN_AWAY, MOVED_TO_OTHER_TANK, OTHER |
| CoralCatalogueStatus | Reuse `FishCatalogueStatus` |
| Growth value storage | Single `DECIMAL(8,1)` column + `measurement_type VARCHAR(30)` |
| Migration version directory | `version1_7_0/` |
| CoralExportTo | Extend existing stub class |
| Chart component | PrimeFaces `p:lineChart` |
| Photo subdirectory | `photos/coral/{userId}/{coralId}.jpg` |
| Reef report opt-in | `include_corals TINYINT(1)` column in `public_report_link` |
| Saltwater-only guard | JSF rendered + service-layer waterType check |

