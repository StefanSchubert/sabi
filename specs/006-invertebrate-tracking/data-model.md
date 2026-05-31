# Data Model: Invertebrate Stock Management & Catalogue (006)

**Branch**: `006-invertebrate-tracking`  
**Generated**: 2026-05-30

---

## Entity Overview

```
invertebrate_catalogue ←──────────────────────────────┐
  id, scientific_name, taxonomic_category,            │
  care_level, status, proposer_user_id, proposal_date  │
        │ 1                                            │
        │ *                                            │
invertebrate_catalogue_i18n                           │
  id, catalogue_id, language_code,                    │
  common_name, description, ref_url                   │
                                                      │
invertebrate_stock ──────────────────────────────────►┘
  id, aquarium_id, user_id,                  (nullable FK)
  invertebrate_catalogue_id (nullable),
  species_name, scientific_name,
  taxonomic_category, care_level,
  mobility, ecological_role, activity_pattern,
  external_ref_url, notes,
  added_on, departed_on,
  departure_reason, departure_note,
  deleted_at, created_on, lastmod_on, optlock
        │ 1
        │ *
invertebrate_water_sensitivity
  id, invertebrate_stock_id, unit_id

invertebrate_stock ──► invertebrate_photo
  id, invertebrate_stock_id,
  file_path, content_type,
  created_on, lastmod_on, optlock

public_report_link (EXISTING — ALTER)
  + include_invertebrates  TINYINT(1) NOT NULL DEFAULT 0
```

---

## Tables

### `invertebrate_catalogue`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT UNSIGNED | PK AUTO_INCREMENT | |
| `scientific_name` | VARCHAR(255) | NOT NULL | Latin name (unique among PENDING/PUBLIC via virtual column) |
| `taxonomic_category` | VARCHAR(12) | NOT NULL | `CRUSTACEAN` \| `MOLLUSC` \| `ECHINODERM` \| `WORM` |
| `care_level` | VARCHAR(12) | NOT NULL | `EASY` \| `MODERATE` \| `DEMANDING` |
| `status` | VARCHAR(10) | NOT NULL DEFAULT `PENDING` | `PENDING` \| `PUBLIC` \| `REJECTED` |
| `proposer_user_id` | BIGINT UNSIGNED | NULL | FK → `users.id` ON DELETE SET NULL |
| `proposal_date` | DATE | NULL | |
| `active_scientific_name` | VARCHAR(255) | VIRTUAL GENERATED | `IF(status IN ('PENDING','PUBLIC'), scientific_name, NULL)` — used by unique index |
| `created_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| `lastmod_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | |
| `optlock` | INT UNSIGNED | NOT NULL DEFAULT 0 | |

**Indexes**:
- `UNIQUE idx_invert_catalogue_active_name (active_scientific_name)` — partial unique across PENDING/PUBLIC entries
- `idx_invert_catalogue_status (status)`
- `idx_invert_catalogue_proposer (proposer_user_id)`

---

### `invertebrate_catalogue_i18n`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT UNSIGNED | PK AUTO_INCREMENT | |
| `catalogue_id` | BIGINT UNSIGNED | NOT NULL | FK → `invertebrate_catalogue.id` ON DELETE CASCADE |
| `language_code` | VARCHAR(5) | NOT NULL | `DE` \| `EN` \| `ES` \| `FR` \| `IT` |
| `common_name` | VARCHAR(255) | NOT NULL | Localised common name |
| `description` | VARCHAR(2000) | NULL | Localised description |
| `ref_url` | VARCHAR(512) | NULL | Localised reference URL |
| `created_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| `lastmod_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | |
| `optlock` | INT UNSIGNED | NOT NULL DEFAULT 0 | |

**Indexes**:
- `UNIQUE uq_invert_cat_i18n (catalogue_id, language_code)` — one row per language per catalogue entry
- `idx_invert_cat_i18n_lang (language_code)` — used by search queries

---

### `invertebrate_stock`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT UNSIGNED | PK AUTO_INCREMENT | |
| `aquarium_id` | BIGINT UNSIGNED | NOT NULL | FK → `aquarium.id` ON DELETE CASCADE |
| `user_id` | BIGINT UNSIGNED | NOT NULL | FK → `users.id` (ownership) |
| `invertebrate_catalogue_id` | BIGINT UNSIGNED | NULL | FK → `invertebrate_catalogue.id` ON DELETE SET NULL |
| `species_name` | VARCHAR(255) | NOT NULL | Free-text common/species name |
| `scientific_name` | VARCHAR(255) | NULL | Snapshot from catalogue at link time; user-editable |
| `taxonomic_category` | VARCHAR(12) | NOT NULL | `CRUSTACEAN` \| `MOLLUSC` \| `ECHINODERM` \| `WORM` |
| `care_level` | VARCHAR(12) | NULL | `EASY` \| `MODERATE` \| `DEMANDING` snapshot |
| `mobility` | VARCHAR(10) | NULL | `MOBILE` \| `SESSILE` (optional) |
| `ecological_role` | VARCHAR(15) | NULL | `CLEANUP_CREW` \| `NEUTRAL` \| `DETRIMENTAL` (optional) |
| `activity_pattern` | VARCHAR(10) | NULL | `DIURNAL` \| `NOCTURNAL` \| `BOTH` (optional) |
| `external_ref_url` | VARCHAR(512) | NULL | |
| `notes` | TEXT | NULL | Unlimited free text |
| `added_on` | DATE | NOT NULL | Not in the future (app-layer validation) |
| `departed_on` | DATE | NULL | NULL = currently present |
| `departure_reason` | VARCHAR(30) | NULL | `DIED` \| `SOLD` \| `GIVEN_AWAY` \| `MOVED_TO_OTHER_TANK` \| `OTHER` |
| `departure_note` | TEXT | NULL | Max 500 chars (app-layer validation) |
| `deleted_at` | TIMESTAMP | NULL | Soft-delete; set on aquarium cascade deletion |
| `created_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| `lastmod_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | |
| `optlock` | INT UNSIGNED | NOT NULL DEFAULT 0 | |

**Indexes**:
- `idx_is_aquarium (aquarium_id)`
- `idx_is_user (user_id)`
- `idx_is_deleted_at (deleted_at)` — used by soft-delete filter

**Business Rules**:
- `added_on` ≤ today (app-layer; checked in `InvertebrateStockServiceImpl`)
- `departed_on` ≥ `added_on` when both non-null (app-layer; returns exception code `DEPARTURE_DATE_BEFORE_ENTRY`)
- Physical delete only allowed when `departed_on IS NULL` (app-layer; returns `INVERT_HAS_DEPARTURE_RECORD`)
- `species_name` mandatory; `taxonomic_category` mandatory

---

### `invertebrate_water_sensitivity`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT UNSIGNED | PK AUTO_INCREMENT | |
| `invertebrate_stock_id` | BIGINT UNSIGNED | NOT NULL | FK → `invertebrate_stock.id` ON DELETE CASCADE |
| `unit_id` | INT UNSIGNED | NOT NULL | FK → `unit.id` |
| `created_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | |

**Indexes**:
- `UNIQUE uq_iws (invertebrate_stock_id, unit_id)` — prevents duplicate sensitivity references
- `idx_iws_unit (unit_id)`

---

### `invertebrate_photo`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT UNSIGNED | PK AUTO_INCREMENT | |
| `invertebrate_stock_id` | BIGINT UNSIGNED | NOT NULL UNIQUE | FK → `invertebrate_stock.id` ON DELETE CASCADE; one photo per invertebrate |
| `file_path` | VARCHAR(512) | NOT NULL | Absolute path on the photo volume |
| `content_type` | VARCHAR(50) | NOT NULL | `image/jpeg` \| `image/png` \| `image/webp` \| `image/gif` |
| `created_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| `lastmod_on` | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE | |
| `optlock` | INT UNSIGNED | NOT NULL DEFAULT 0 | |

---

### `public_report_link` (ALTER — existing table)

**Added column**:
- `include_invertebrates` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'When 1, invertebrate stock is included in the public report'

---

## JPA Entities (sabi-server)

### `InvertebrateCatalogueEntity`

```java
@Table(name = "invertebrate_catalogue", schema = "sabi")
@Entity
// Fields: id, scientificName, taxonomicCategory (String), careLevel (String),
//         status (String, default "PENDING"), proposerUserId, proposalDate,
//         i18nEntries (OneToMany → InvertebrateCatalogueI18nEntity),
//         proposer (ManyToOne → UserEntity, insertable=false updatable=false)
```

### `InvertebrateCatalogueI18nEntity`

```java
@Table(name = "invertebrate_catalogue_i18n", schema = "sabi")
@Entity
// Fields: id, catalogueId (FK scalar), languageCode, commonName, description, refUrl
//         catalogue (ManyToOne → InvertebrateCatalogueEntity, insertable=false updatable=false)
```

### `TankInvertebrateStockEntity`

```java
@Table(name = "invertebrate_stock", schema = "sabi")
@Entity
@SQLRestriction("deleted_at IS NULL")
// Fields: id, aquariumId, user (ManyToOne UserEntity), invertebrateCatalogueId,
//         speciesName, scientificName, taxonomicCategory, careLevel,
//         mobility, ecologicalRole, activityPattern,
//         externalRefUrl, notes, addedOn, departedOn,
//         departureReason, departureNote, deletedAt,
//         catalogueEntry (ManyToOne → InvertebrateCatalogueEntity, insertable=false updatable=false),
//         waterSensitivities (OneToMany → InvertebrateWaterSensitivityEntity, cascade ALL orphanRemoval)
```

### `InvertebrateWaterSensitivityEntity`

```java
@Table(name = "invertebrate_water_sensitivity", schema = "sabi")
@Entity
// Fields: id, invertebrateStockId, unitId
```

### `InvertebratePhotoEntity`

```java
@Table(name = "invertebrate_photo", schema = "sabi")
@Entity
// Fields: id, invertebrateStockId, filePath, contentType
```

---

## Transfer Objects (sabi-boundary)

### New enums

```java
// InvertebrateTaxonomicCategory: CRUSTACEAN, MOLLUSC, ECHINODERM, WORM
// InvertebrateMobility:          MOBILE, SESSILE
// InvertebrateEcologicalRole:    CLEANUP_CREW, NEUTRAL, DETRIMENTAL
// InvertebrateActivityPattern:   DIURNAL, NOCTURNAL, BOTH
```

### `InvertebrateStockEntryTo`

```java
@Data
public class InvertebrateStockEntryTo implements Serializable {
    private Long id;
    @NotNull Long aquariumId;
    @NotBlank String speciesName;
    String scientificName;
    @NotNull InvertebrateTaxonomicCategory taxonomicCategory;
    InvertebrateCareLevel careLevel;           // EASY | MODERATE | DEMANDING (from catalogue)
    InvertebrateMobility mobility;             // optional
    InvertebrateEcologicalRole ecologicalRole; // optional
    InvertebrateActivityPattern activityPattern; // optional
    List<Integer> waterSensitivityUnitIds;    // unit IDs; empty list = no sensitivity set
    @Pattern(regexp = "^(https?://.*)?$") String externalRefUrl;
    String notes;
    @NotNull @PastOrPresent LocalDate addedOn;
    LocalDate departedOn;
    InvertebrateDepartureReason departureReason; // reuse enum: DIED|SOLD|GIVEN_AWAY|MOVED_TO_OTHER_TANK|OTHER
    @Size(max = 500) String departureNote;
    Long invertebrateCatalogueId;  // nullable
    boolean hasPhoto;
}
```

### `InvertebrateDepartureRecordTo`

```java
@Data
public class InvertebrateDepartureRecordTo implements Serializable {
    @NotNull LocalDate departedOn;
    @NotNull InvertebrateDepartureReason departureReason;
    @Size(max = 500) String departureNote;
}
```

### `InvertebrateCatalogueEntryTo`

```java
@Data
public class InvertebrateCatalogueEntryTo implements Serializable {
    Long id;
    @NotBlank String scientificName;
    @NotNull InvertebrateTaxonomicCategory taxonomicCategory;
    @NotNull InvertebrateCareLevel careLevel;
    FishCatalogueStatus status;     // reuse: PENDING | PUBLIC | REJECTED
    Long proposerUserId;
    LocalDate proposalDate;
    @Valid List<InvertebrateCatalogueI18nTo> i18nEntries;
}
```

### `InvertebrateCatalogueI18nTo`

```java
@Data
public class InvertebrateCatalogueI18nTo implements Serializable {
    Long id;
    @NotBlank String languageCode;  // DE | EN | ES | FR | IT
    @NotBlank String commonName;
    @Size(max = 2000) String description;
    String refUrl;
}
```

### `InvertebrateCatalogueSearchResultTo`

```java
@Data
public class InvertebrateCatalogueSearchResultTo implements Serializable {
    Long id;
    String scientificName;
    InvertebrateTaxonomicCategory taxonomicCategory;
    String commonName;  // in user's language
    FishCatalogueStatus status;
}
```

### `InvertebrateExportTo` (AI export)

```java
@Data
public class InvertebrateExportTo implements Serializable {
    Long catalogueId;           // nullable
    String scientificName;
    String speciesName;
    String taxonomicCategory;   // enum name string
    String addedOn;             // ISO date
    String departedOn;          // ISO date; null if active
    String departureReason;     // null if active
    String departureNote;       // null if none
    String notes;
    String mobility;            // null if not set
    String ecologicalRole;      // null if not set
    String activityPattern;     // null if not set
    List<WaterSensitivityUnitRef> waterSensitivityUnits; // [{unitId, unitSign, nameEn}]
}
```

### `PublicReefReportInvertebrateTo` (House Reef Report)

```java
@Data
public class PublicReefReportInvertebrateTo implements Serializable {
    String speciesName;
    String taxonomicCategory;
    String mobility;            // null if not set; localised label
    String ecologicalRole;      // null if not set; localised label
    String activityPattern;     // null if not set; localised label
    List<String> waterSensitivityUnitNames;  // localised unit names
}
```

### Extended TOs

| TO | Extension |
|----|-----------|
| `AquariumExportTo` | `+ List<InvertebrateExportTo> invertebrates = new ArrayList<>()` |
| `PublicReefReportTo` | `+ List<PublicReefReportInvertebrateTo> invertebrateInhabitants` (null = not opted-in) |
| `PublicReportLinkTo` | `+ boolean includeInvertebrates = false` |

---

## State Transitions

### Invertebrate Stock Entry

```
ACTIVE (departed_on IS NULL)
  ├─► edit fields (PUT /api/invertebrate/{id})
  ├─► record departure (PUT /api/invertebrate/{id}/departure)
  │     → departed_on + departure_reason set → DEPARTED
  └─► physical delete (DELETE /api/invertebrate/{id})
        → only allowed when no departure record → hard delete

DEPARTED (departed_on IS NOT NULL)
  └─► read-only (detail view still shows all fields)
      physical delete BLOCKED (409 CONFLICT)
```

### Catalogue Entry

```
[User submits proposal]
  └─► PENDING (visible only to proposer)
        ├─► Admin APPROVE → PUBLIC (visible to all)
        └─► Admin REJECT  → REJECTED (invisible to all)
```

---

## Validation Rules Summary

| Field | Rule | Layer |
|-------|------|-------|
| `species_name` | Not blank | Client + Server (@NotBlank) |
| `taxonomic_category` | Not null, must be valid enum | Client + Server (@NotNull) |
| `added_on` | Not null, not in the future | Client + Server (@PastOrPresent) |
| `departed_on` | ≥ `added_on` when set | Server (service layer, 422) |
| `departure_reason` | Required when `departed_on` set | Server (service layer) |
| `departure_note` | Max 500 characters | Client + Server (@Size) |
| `external_ref_url` | Must match `^(https?://.*)?$` or empty | Client + Server (@Pattern) |
| `notes` | No length limit | — |
| Photo upload | ≤ 5 MB; JPEG/PNG/WebP/GIF only | Server (service layer, 400) |
| Catalogue `scientific_name` | Unique among PENDING/PUBLIC entries | DB (partial unique index via virtual column) |
| Catalogue `description` (i18n) | Max 2000 characters | Client + Server (@Size) |
| Physical delete | Only when `departed_on IS NULL` | Server (service layer, 409) |
