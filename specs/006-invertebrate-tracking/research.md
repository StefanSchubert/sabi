# Research: Invertebrate Stock Management & Catalogue (006)

**Branch**: `006-invertebrate-tracking`  
**Generated**: 2026-05-30  
**Method**: Codebase analysis — Fish Stock (spec 002) and Coral Stock (spec 005) as primary reference

---

## 1. DB Migration Strategy

### Decision
Use **Flyway version directory `version1_8_0`** (next sequential after `1_7_0` which contained coral stock). Create 6 migration files following the naming convention established in `1_7_0`.

### Rationale
The project enforces Flyway immutability: existing migration scripts are never modified. The version directory pattern (`version1_N_0`) maps cleanly to minor-version releases. All 6 tables needed by this feature (catalogue, catalogue_i18n, stock, water_sensitivity, photo, report_link flag) are additive — no existing tables are dropped or structurally broken.

### Migration Files
| File | Content |
|------|---------|
| `V1_8_0_1__addInvertebrateCatalogueTable.sql` | New `invertebrate_catalogue` table — mirrors structure of `coral_catalogue` post-005 migration; includes `active_scientific_name` virtual column for the partial-unique index |
| `V1_8_0_2__addInvertebrateCatalogueI18nTable.sql` | New `invertebrate_catalogue_i18n` — mirrors `coral_catalogue_i18n` |
| `V1_8_0_3__addInvertebrateStockTable.sql` | New `invertebrate_stock` — mirrors `coral_stock`; adds `taxonomic_category`, `mobility`, `ecological_role`, `activity_pattern` VARCHAR columns |
| `V1_8_0_4__addInvertebrateWaterSensitivityTable.sql` | New `invertebrate_water_sensitivity` join table (invertebrate_stock_id × unit_id) |
| `V1_8_0_5__addInvertebratePhotoTable.sql` | New `invertebrate_photo` — mirrors `coral_photo` / `fish_photo` |
| `V1_8_0_6__addIncludeInvertebratesToPublicReportLink.sql` | `ALTER TABLE public_report_link ADD COLUMN include_invertebrates TINYINT(1) NOT NULL DEFAULT 0` |

### Alternatives Considered
- Storing functional classifications in a separate classifications table: rejected — 3 of the 4 dimensions are single-choice and would add 3 one-row join records per entry for no normalisation benefit. Only water-sensitivity (true multi-value) gets its own join table.
- Reusing the existing `coral_catalogue` table with a `type` discriminator: rejected — species cardinality and eventual data volumes are similar; a dedicated table keeps FK constraints clean and avoids widening an existing migration.

---

## 2. Functional Classification Storage

### Decision
Store **Mobility, Ecological Role, and Activity Pattern** as **VARCHAR columns** (`VARCHAR(20)`) directly on the `invertebrate_stock` table, nullable (optional). Store **Water Sensitivity** in a separate **join table** `invertebrate_water_sensitivity` (FK to `invertebrate_stock` + FK to `unit`).

### Rationale
The three single-choice dimensions fit naturally as nullable enum-backed columns (the same approach used for `departure_reason` in fish and coral stock). The multi-select Water Sensitivity has true multiplicity and must be a separate join table to avoid comma-serialisation anti-patterns.

### Alternatives Considered
- Storing all four as bit-flags or JSON: rejected — makes queries and index use impossible; violates project's maintainability standard.
- Enum columns at the DB level: rejected — MariaDB enum columns are hard to extend; VARCHAR + application-layer enum constraint is the established Sabi pattern.

---

## 3. Catalogue Pattern (UGC Workflow)

### Decision
Mirror the **exact coral catalogue UGC workflow**: `PENDING → PUBLIC / REJECTED` status transitions. Partial-unique index via generated virtual column `active_scientific_name` (same technique as `coral_catalogue` in V1_7_0_1). Reuse `FishCatalogueStatus` enum (already in sabi-boundary) for the status field — it is domain-agnostic.

### Rationale
The coral catalogue (spec 005) already established this pattern in the codebase. Reusing `FishCatalogueStatus` avoids introducing a third identical status enum in `sabi-boundary`. The virtual-column partial-unique-index approach is already proven in production.

### Alternatives Considered
- Separate `InvertebrateCatalogueStatus` enum: no benefit — values are identical; adds unnecessary duplication.

---

## 4. Photo Storage

### Decision
Follow the **coral photo pattern** exactly: store a `invertebrate_photo` table with `file_path` and `content_type` columns; actual bytes live on the configurable filesystem volume (same root as fish/coral photos). Max 5 MB enforced at the service layer; JPEG, PNG, WebP, GIF accepted (same set as coral).

### Rationale
Photo storage is already abstracted behind the service layer. No object storage or CDN is used; all photos live on the same filesystem volume. The `InvertebratePhotoEntity` + `InvertebratePhotoRepository` pattern is a direct mirror of `CoralPhotoEntity`.

---

## 5. AI Export Integration

### Decision
Extend `AquariumExportTo` (sabi-boundary) with `List<InvertebrateExportTo> invertebrates = new ArrayList<>()`. Populate it in `UserProfileServiceImpl` alongside the existing fish and coral export logic. `InvertebrateExportTo` carries: `catalogueId`, `scientificName`, `speciesName`, `taxonomicCategory`, `addedOn` (ISO date string), `departedOn`, `departureReason`, `departureNote`, `notes`, `mobility`, `ecologicalRole`, `activityPattern`, `waterSensitivityUnits` (list of unit ID + English name pairs).

### Rationale
`AquariumExportTo` already has `List<CoralExportTo> corals` and `List<FishExportTo> fish`. Adding a parallel field is the only non-breaking extension pattern — all existing consumers see the new field as an optional addition (spec assumption section §"AI-JSON export schema version incremented").

---

## 6. House Reef Report Integration

### Decision
- Add `boolean includeInvertebrates` to `PublicReportLinkTo` and to the `public_report_link` DB table (migration V1_8_0_6).
- Add `List<PublicReefReportInvertebrateTo> invertebrateInhabitants` to `PublicReefReportTo` (null when opt-in is off; empty list when opted-in but no active invertebrates).
- Expose `PUT /api/report/link/{aquariumId}/include-invertebrates` endpoint — mirrors the existing `include-events` and `include-corals` endpoints on `PublicReportController`.

### Rationale
Spec 005 already introduced the coral opt-in pattern. This feature simply repeats it for invertebrates. The `null` vs. empty-list semantics (opted-out vs. opted-in-but-empty) are identical to corals.

### Alternatives Considered
- A single "include all fauna" flag: rejected — users may want to share fish/corals but not invertebrates; per-type opt-in is consistent with the established pattern.

---

## 7. API Endpoint Design

### Decision
Three new REST controllers, exactly mirroring fish/coral:

| Controller | Base path | Purpose |
|---|---|---|
| `InvertebrateStockController` | `/api/invertebrate` | CRUD, departure, photo, catalogue-link removal |
| `InvertebrateCatalogueController` | `/api/invertebrate-catalogue` | Search, propose, get, update (authenticated) |
| `InvertebrateCatalogueAdminController` | `/api/admin/invertebrate-catalogue` | List pending, approve, reject, edit (admin role) |

Full endpoint list in `contracts/invertebrate-api.yaml`.

---

## 8. Water Sensitivity Multi-Select (Unit Table)

### Decision
The `unit` and `localized_unit` tables are **pre-existing** and already used by the Measurement feature. No new units are added by this feature. The invertebrate add/edit form will load units via the existing `UnitController` (already returns localised unit names for the current user language). The Water Sensitivity multiselect widget is a `<p:selectManyCheckbox>` or `<p:listbox>` bound to a `List<Integer>` of unit IDs on the backing bean.

### Rationale
Reusing the existing unit endpoint avoids duplicate data loading logic. Since the unit table is small (~50 rows), no pagination or autocomplete is needed — a full list can be rendered in the multi-select widget without performance concerns on ARM.

---

## 9. i18n Keys Strategy

### Decision
All new message keys follow the existing `coralstock.*` and `fishstock.*` naming conventions, prefixed with `invertebratestock.*` for stock UI and `invertebratecatalogue.*` for catalogue UI. Keys to be added to all 6 message bundle files (DE, EN, ES, FR, IT + fallback `messages.properties`).

Categories of new keys:
- Tab label, section headers
- Form field labels and placeholders
- Validation error messages
- Taxonomic category labels (4 values × 6 languages)
- Functional classification labels (Mobility × 2, Ecological Role × 3, Activity Pattern × 3)
- Departure reason labels (5 values — can reuse existing `fishstock.departure.reason.*` keys if applicable)
- Catalogue status labels (reuse existing fish catalogue status keys)
- Care level labels (reuse existing fish/coral `catalogue.carelevel.*` keys)
- Admin view labels
- Error / success messages

### Alternatives Considered
- Reusing `fishstock.departure.reason.*` keys for departure reasons: **accepted** — the enum values are identical (`DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`); the backing bean can reference the existing keys.

---

## 10. Open Questions Resolved

| # | Question | Resolution |
|---|----------|-----------|
| 1 | Which Flyway version directory? | `version1_8_0` (next after `1_7_0`) |
| 2 | Should `InvertebrateCatalogueStatus` be a new enum? | No — reuse `FishCatalogueStatus` (PENDING/PUBLIC/REJECTED) |
| 3 | How does Water Sensitivity map to unit IDs? | FK join table to existing `unit` table; IDs are stable integers |
| 4 | Does `PublicReportLinkTo` need a new boolean field? | Yes — `includeInvertebrates`; migration V1_8_0_6 adds the DB column |
| 5 | Departure reason i18n reuse? | Reuse `fishstock.departure.reason.*` keys; values identical across all three stock types |
| 6 | `AquariumExportTo` field name? | `invertebrates` (parallel to `fish` and `corals` existing fields) |
