# Data Model: 005-coral-stock

**Phase 1 output for `/speckit.plan`**  
**Date**: 2026-05-22  
**Spec**: specs/005-coral-stock/spec.md

---

## Entity Overview

```
CoralCatalogueEntry ──── (1:N) ──── CoralCatalogueI18n
       │
       └──── (1:N, optional) ──── TankCoralStock
                                        │
                   ┌────────────────────┴────────────────────┐
                   │                                         │
           CoralGrowthHistory                    CoralPolypCondition
           
TankCoralStock ──── (1:1, optional) ──── CoralPhoto
```

---

## Entity 1: TankCoralStock

**Table**: `coral_stock` (schema `sabi`)  
**Java entity**: `sabi-server/.../persistence/model/TankCoralStockEntity.java`  
**Analogous to**: `TankFishStockEntity` / `fish` table

### Fields

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `id` | `BIGINT(20) UNSIGNED` | NOT NULL | PK, AUTO_INCREMENT |
| `aquarium_id` | `BIGINT(20) UNSIGNED` | NOT NULL | FK → `aquarium.id` |
| `user_id` | `BIGINT(20) UNSIGNED` | NOT NULL | FK → `users.id`, ownership |
| `coral_catalogue_id` | `BIGINT(20) UNSIGNED` | NULL | FK → `coral_catalogue.id` (optional link) |
| `species_name` | `VARCHAR(255)` | NOT NULL | Free-text common/species name (mandatory) |
| `scientific_name` | `VARCHAR(255)` | NULL | Snapshot from catalogue at link time; user-editable |
| `classification` | `VARCHAR(5)` | NULL | `LPS` or `SPS` snapshot; user-editable |
| `care_level` | `VARCHAR(12)` | NULL | `EASY`, `MODERATE`, `DEMANDING` snapshot |
| `external_ref_url` | `VARCHAR(512)` | NULL | Optional external reference URL |
| `notes` | `TEXT` | NULL | Free-text user notes (unlimited length) |
| `added_on` | `DATE` | NOT NULL | Entry date (mandatory, not in future) |
| `departed_on` | `DATE` | NULL | Departure date (null = currently present) |
| `departure_reason` | `VARCHAR(30)` | NULL | Enum: DIED / SOLD / GIVEN_AWAY / MOVED_TO_OTHER_TANK / OTHER |
| `departure_note` | `TEXT` | NULL | Free-text departure note (max 500 chars, app-enforced) |
| `deleted_at` | `TIMESTAMP` | NULL | Soft-delete; set on aquarium deletion |
| `created_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `lastmod_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `optlock` | `INT UNSIGNED` | NOT NULL | Optimistic locking |

### Constraints & Indexes
- `PRIMARY KEY (id)`
- `INDEX idx_cs_aquarium (aquarium_id)`
- `INDEX idx_cs_user (user_id)`
- `INDEX idx_cs_deleted_at (deleted_at)` — supports `@SQLRestriction("deleted_at IS NULL")`
- `FK fk_cs_aquarium` → `aquarium(id)` ON DELETE CASCADE
- `FK fk_cs_user` → `users(id)`
- `FK fk_cs_catalogue` → `coral_catalogue(id)` ON DELETE SET NULL

### Validation Rules (application layer)
- `species_name`: mandatory, non-blank
- `added_on`: mandatory, `@PastOrPresent`
- `departed_on`: if set, must be ≥ `added_on` (FR-006)
- `departure_reason`: mandatory when `departed_on` is set (FR-005)
- `departure_note`: max 500 characters (FR-042)
- `classification`: if set, must be `LPS` or `SPS`
- `aquarium.waterType` must be `MARINE` (C-8, checked at service layer)

### State Transitions
```
(created) → active (departed_on == null)
active    → departed (departed_on != null, departure_reason set)
active    → deleted (physical delete; only possible when no departure record)
aquarium deleted → deleted_at set on all coral entries (soft-delete cascade)
```

---

## Entity 2: CoralGrowthHistory

**Table**: `coral_growth_history` (schema `sabi`)  
**Java entity**: `sabi-server/.../persistence/model/CoralGrowthHistoryEntity.java`  
**Analogous to**: `FishSizeHistoryEntity` / `fish_size_history` table

### Fields

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `id` | `BIGINT(20) UNSIGNED` | NOT NULL | PK, AUTO_INCREMENT |
| `coral_stock_id` | `BIGINT(20) UNSIGNED` | NOT NULL | FK → `coral_stock.id` |
| `measured_on` | `DATE` | NOT NULL | Date of measurement (mandatory, not in future) |
| `measurement_type` | `VARCHAR(30)` | NOT NULL | `SURFACE_AREA_CM2` / `SIZE_CM` / `VOLUME_CM3` / `BRANCH_COUNT` |
| `measurement_value` | `DECIMAL(8,1)` | NOT NULL | Positive; BRANCH_COUNT = integer (app-enforced) |

### Constraints & Indexes
- `PRIMARY KEY (id)`
- `INDEX idx_cgh_coral (coral_stock_id)`
- `FK fk_cgh_coral` → `coral_stock(id)` ON DELETE CASCADE
- Query order: `ORDER BY measured_on DESC` (date-descending per spec FR-016)

### Validation Rules
- `measured_on`: mandatory, `@PastOrPresent`, must not exceed `coral_stock.departed_on` if set (FR-015)
- `measurement_type`: mandatory, one of the four enum values (FR-013)
- `measurement_value`: mandatory, `> 0`; if type is `BRANCH_COUNT`, must be an integer (FR-014, C-9)

### Immutability
- `measurement_type` is immutable after creation (FR-039; editable fields: date and value only)

---

## Entity 3: CoralPolypCondition

**Table**: `coral_polyp_condition` (schema `sabi`)  
**Java entity**: `sabi-server/.../persistence/model/CoralPolypConditionEntity.java`

### Fields

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `id` | `BIGINT(20) UNSIGNED` | NOT NULL | PK, AUTO_INCREMENT |
| `coral_stock_id` | `BIGINT(20) UNSIGNED` | NOT NULL | FK → `coral_stock.id` |
| `observed_on` | `DATE` | NOT NULL | Date of observation (mandatory, not in future) |
| `condition` | `VARCHAR(30)` | NOT NULL | `VITAL` / `TISSUE_LOSS` / `PALE` / `LIMP` / `SIGNIFICANT_GROWTH` |

### Constraints & Indexes
- `PRIMARY KEY (id)`
- `INDEX idx_cpc_coral (coral_stock_id)`
- `FK fk_cpc_coral` → `coral_stock(id)` ON DELETE CASCADE
- Query order: `ORDER BY observed_on DESC` (FR-020)

### Validation Rules
- `observed_on`: mandatory, `@PastOrPresent`, must not exceed `coral_stock.departed_on` if set (FR-019)
- `condition`: mandatory, one of the five enum values (FR-018)
- Editable fields: `observed_on` and `condition` (FR-040)

---

## Entity 4: CoralCatalogueEntry

**Table**: `coral_catalogue` (schema `sabi`)  
**Java entity**: `sabi-server/.../persistence/model/CoralCatalogueEntity.java`  
**Analogous to**: `FishCatalogueEntryEntity` / `fish_catalogue` table

### Fields

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `id` | `BIGINT(20) UNSIGNED` | NOT NULL | PK, AUTO_INCREMENT |
| `status` | `VARCHAR(10)` | NOT NULL | DEFAULT `'PENDING'`; PENDING / PUBLIC / REJECTED |
| `proposer_user_id` | `BIGINT(20) UNSIGNED` | NULL | FK → `users.id`; NULL for admin-created |
| `proposal_date` | `DATE` | NULL | Date submitted |
| `scientific_name` | `VARCHAR(255)` | NOT NULL | Latin binomial; case-sensitive |
| `classification` | `VARCHAR(5)` | NOT NULL | `LPS` or `SPS` |
| `care_level` | `VARCHAR(12)` | NOT NULL | `EASY`, `MODERATE`, `DEMANDING` |
| `active_scientific_name` | `VARCHAR(255)` | VIRTUAL | Generated; enforces partial-unique on PENDING+PUBLIC entries |
| `created_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `lastmod_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `optlock` | `INT UNSIGNED` | NOT NULL | Optimistic locking |

### Constraints & Indexes
- `PRIMARY KEY (id)`
- `UNIQUE INDEX uq_coral_catalogue_active_name (active_scientific_name)` — partial uniqueness for PENDING+PUBLIC
- `INDEX idx_coral_catalogue_status (status)`
- `INDEX idx_coral_catalogue_proposer (proposer_user_id)`
- `FK fk_coral_catalogue_proposer` → `users(id)` ON DELETE SET NULL

### Virtual Column Pattern (same as fish catalogue V1_5_0_2)
```sql
ADD COLUMN `active_scientific_name` VARCHAR(255)
    GENERATED ALWAYS AS (
        IF(`status` IN ('PENDING', 'PUBLIC'), `scientific_name`, NULL)
    ) VIRTUAL;
CREATE UNIQUE INDEX `uq_coral_catalogue_active_name` ON `coral_catalogue` (`active_scientific_name`);
```

---

## Entity 5: CoralCatalogueI18n

**Table**: `coral_catalogue_i18n` (schema `sabi`)  
**Java entity**: `sabi-server/.../persistence/model/CoralCatalogueI18nEntity.java`  
**Analogous to**: `FishCatalogueI18nEntity` / `fish_catalogue_i18n` table

### Fields

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `id` | `BIGINT(20) UNSIGNED` | NOT NULL | PK, AUTO_INCREMENT |
| `catalogue_id` | `BIGINT(20) UNSIGNED` | NOT NULL | FK → `coral_catalogue.id` |
| `language_code` | `VARCHAR(2)` | NOT NULL | `de` / `en` / `es` / `fr` / `it` |
| `common_name` | `VARCHAR(255)` | NULL | Localised common name |
| `description` | `TEXT` | NULL | Max 2000 chars (app-enforced) |
| `reference_url` | `VARCHAR(512)` | NULL | Localised reference URL |
| `created_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `lastmod_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `optlock` | `INT UNSIGNED` | NOT NULL | Optimistic locking |

### Constraints & Indexes
- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_coral_catalogue_i18n_lang (catalogue_id, language_code)` — at most one per language per entry
- `FK fk_coral_i18n_entry` → `coral_catalogue(id)` ON DELETE CASCADE
- `FULLTEXT INDEX ft_coral_i18n_name (common_name)` — supports FR-030 search by common name

---

## Entity 6: CoralPhoto

**Table**: `coral_photo` (schema `sabi`)  
**Java entity**: `sabi-server/.../persistence/model/CoralPhotoEntity.java`  
**Analogous to**: `FishPhotoEntity` / `fish_photo` table  
**Storage**: Filesystem at `{sabi.photo.dir}/coral/{userId}/{coralStockId}.jpg` (not a DB BLOB; C-4)

### Fields

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `id` | `BIGINT(20) UNSIGNED` | NOT NULL | PK, AUTO_INCREMENT |
| `coral_stock_id` | `BIGINT(20) UNSIGNED` | NOT NULL | FK → `coral_stock.id` |
| `file_path` | `VARCHAR(512)` | NOT NULL | Relative path within photo volume |
| `content_type` | `VARCHAR(50)` | NOT NULL | `image/jpeg`, `image/png`, `image/webp`, `image/gif` |
| `upload_date` | `DATE` | NOT NULL | Date of upload |
| `created_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `lastmod_on` | `TIMESTAMP` | NOT NULL | Auditable |
| `optlock` | `INT UNSIGNED` | NOT NULL | Optimistic locking |

### Constraints
- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_coral_photo_stock (coral_stock_id)` — one photo per coral entry (v1)
- `FK fk_cp_coral_stock` → `coral_stock(id)` ON DELETE CASCADE

---

## Enum Types (sabi-boundary)

### CoralDepartureReason (NEW)

```java
package de.bluewhale.sabi.model;

public enum CoralDepartureReason implements Serializable {
    DIED,
    SOLD,
    GIVEN_AWAY,
    MOVED_TO_OTHER_TANK,
    OTHER;
    // @JsonValue / @JsonCreator — same pattern as DepartureReason
}
```

### CoralClassification (NEW)

```java
package de.bluewhale.sabi.model;

public enum CoralClassification implements Serializable {
    LPS,
    SPS;
}
```

### CoralCareLevel (NEW)

```java
package de.bluewhale.sabi.model;

public enum CoralCareLevel implements Serializable {
    EASY,
    MODERATE,
    DEMANDING;
}
```

### CoralGrowthType (NEW)

```java
package de.bluewhale.sabi.model;

public enum CoralGrowthType implements Serializable {
    SURFACE_AREA_CM2,
    SIZE_CM,
    VOLUME_CM3,
    BRANCH_COUNT;
}
```

### PolypCondition (NEW)

```java
package de.bluewhale.sabi.model;

public enum PolypCondition implements Serializable {
    VITAL,
    TISSUE_LOSS,
    PALE,
    LIMP,
    SIGNIFICANT_GROWTH;
}
```

### FishCatalogueStatus (REUSED for coral catalogue)

Reuse existing `FishCatalogueStatus` (PENDING / PUBLIC / REJECTED) for `CoralCatalogueEntry.status`.

---

## Transfer Objects (sabi-boundary)

### CoralStockEntryTo (NEW)

```java
@Data public class CoralStockEntryTo implements Serializable {
    private Long id;
    @NotNull private Long aquariumId;
    @NotBlank private String speciesName;          // mandatory free text
    private String scientificName;                  // snapshot or free text
    private CoralClassification classification;     // LPS / SPS snapshot
    private CoralCareLevel careLevel;               // snapshot
    @Pattern(regexp="^(https?://.*)?$") private String externalRefUrl;
    private String notes;                           // unlimited text
    @NotNull @PastOrPresent private LocalDate addedOn;
    private LocalDate departedOn;
    private CoralDepartureReason departureReason;
    @Size(max=500) private String departureNote;
    private Long coralCatalogueId;                  // optional catalogue link
    private boolean hasPhoto;
    private List<CoralGrowthHistoryTo> growthHistory = new ArrayList<>();
    private List<CoralPolypConditionTo> polypConditionHistory = new ArrayList<>();
}
```

### CoralGrowthHistoryTo (NEW)

```java
@Data public class CoralGrowthHistoryTo implements Serializable {
    private Long id;
    private Long coralStockEntryId;
    @NotNull @PastOrPresent private LocalDate measuredOn;
    @NotNull private CoralGrowthType measurementType;       // IMMUTABLE after creation
    @NotNull @DecimalMin("0.1") private BigDecimal measurementValue;
}
```

### CoralPolypConditionTo (NEW)

```java
@Data public class CoralPolypConditionTo implements Serializable {
    private Long id;
    private Long coralStockEntryId;
    @NotNull @PastOrPresent private LocalDate observedOn;
    @NotNull private PolypCondition condition;
}
```

### CoralCatalogueEntryTo (NEW)

```java
@Data public class CoralCatalogueEntryTo implements Serializable {
    private Long id;
    @NotBlank private String scientificName;
    @NotNull private CoralClassification classification;
    @NotNull private CoralCareLevel careLevel;
    private FishCatalogueStatus status;             // reused enum
    private Long proposerUserId;
    private LocalDate proposalDate;
    @Valid private List<CoralCatalogueI18nTo> i18nEntries = new ArrayList<>();
}
```

### CoralCatalogueI18nTo (NEW)

```java
@Data public class CoralCatalogueI18nTo implements Serializable {
    private Long id;
    private String languageCode;                    // de | en | es | fr | it
    private String commonName;
    @Size(max=2000) private String description;
    private String referenceUrl;
}
```

### CoralCatalogueSearchResultTo (NEW)

```java
@Data public class CoralCatalogueSearchResultTo implements Serializable {
    private Long id;
    private String scientificName;
    private String commonName;                      // resolved for requested lang
    private CoralClassification classification;
    private CoralCareLevel careLevel;
    private String referenceUrl;                    // resolved for requested lang
    private FishCatalogueStatus status;
}
```

### CoralDepartureRecordTo (NEW)

```java
@Data public class CoralDepartureRecordTo implements Serializable {
    @NotNull private LocalDate departureDate;
    @NotNull private CoralDepartureReason departureReason;
    @Size(max=500) private String departureNote;
}
```

### CoralExportTo (EXTENDED from existing stub)

```java
@Data public class CoralExportTo implements Serializable {
    private Long coralCatalogueId;
    private String scientificName;
    private String speciesName;                     // common/free-text name
    private String classification;                  // LPS / SPS snapshot
    private String addedOn;
    private String departedOn;
    private String departureReason;
    private String departureNote;
    private String notes;
    private List<CoralGrowthHistoryExportTo> growthHistory = new ArrayList<>();
    private List<CoralPolypConditionExportTo> polypConditionHistory = new ArrayList<>();
}
```

### CoralGrowthHistoryExportTo (NEW)

```java
@Data public class CoralGrowthHistoryExportTo implements Serializable {
    private String measuredOn;
    private String measurementType;
    private BigDecimal measurementValue;
}
```

### CoralPolypConditionExportTo (NEW)

```java
@Data public class CoralPolypConditionExportTo implements Serializable {
    private String observedOn;
    private String condition;
}
```

### PublicReefReportCoralTo (NEW)

```java
@Data public class PublicReefReportCoralTo implements Serializable {
    private String speciesName;
    private String classification;
    private Map<String, BigDecimal> latestGrowthByType; // type label → value
    private String latestPolypCondition;               // null if none
}
```

---

## Modified Entities (existing sabi-boundary classes)

### PublicReefReportTo (MODIFIED)

Add to `PublicReefReportTo.java`:
```java
@Schema(description = "Currently-present corals; null when includeCorals = false for this report link.")
private List<PublicReefReportCoralTo> coralInhabitants;  // null = not opted-in; empty = opted-in, no corals
```

---

## Modified Server Entities

### PublicReportLinkEntity (MODIFIED)

Add column:
```java
@Column(name = "include_corals", nullable = false)
@Basic
private boolean includeCorals = false;
```

---

## PublicReportLinkTo (MODIFIED in sabi-boundary)

Add field:
```java
private boolean includeCorals = false;
```

---

## Flyway Migrations (version1_7_0)

| Script | Purpose |
|--------|---------|
| `V1_7_0_1__addCoralCatalogueTable.sql` | Create `coral_catalogue` table |
| `V1_7_0_2__addCoralCatalogueI18nTable.sql` | Create `coral_catalogue_i18n` table |
| `V1_7_0_3__addCoralStockTable.sql` | Create `coral_stock` table |
| `V1_7_0_4__addCoralGrowthHistoryTable.sql` | Create `coral_growth_history` table |
| `V1_7_0_5__addCoralPolypConditionTable.sql` | Create `coral_polyp_condition` table |
| `V1_7_0_6__addCoralPhotoTable.sql` | Create `coral_photo` table |
| `V1_7_0_7__addIncludeCoralsToPublicReportLink.sql` | `ALTER TABLE public_report_link ADD COLUMN include_corals` |

