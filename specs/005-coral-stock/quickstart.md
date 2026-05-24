# Quickstart: 005-coral-stock

**Phase 1 output for `/speckit.plan`**  
**Date**: 2026-05-22

This guide gives a developer the minimal context to start implementing 005-coral-stock without reading the full spec.

---

## What this feature adds

Coral stock management — fully symmetric with fish stock (spec 002). Two coupled sub-features:

1. **Tank Coral Stock** — per-aquarium coral roster with growth measurements and polyp condition history
2. **Coral Catalogue** — community-maintained species reference with UGC proposal/approval workflow

Plus integrations into the existing AI-JSON export and House Reef Report.

---

## Where to start (recommended order)

### Step 1 — Database (sabi-database module)

Create 7 Flyway migrations in `sabi-database/src/main/resources/db/migration/version1_7_0/`:

```
V1_7_0_1__addCoralCatalogueTable.sql
V1_7_0_2__addCoralCatalogueI18nTable.sql
V1_7_0_3__addCoralStockTable.sql
V1_7_0_4__addCoralGrowthHistoryTable.sql
V1_7_0_5__addCoralPolypConditionTable.sql
V1_7_0_6__addCoralPhotoTable.sql
V1_7_0_7__addIncludeCoralsToPublicReportLink.sql
```

See `data-model.md` for exact DDL. Look at `V1_5_0_3__addFishCatalogueI18nTable.sql` and `V1_5_0_8__addFishSizeHistory.sql` as DDL references.

### Step 2 — Boundary (sabi-boundary module)

New enums in `de.bluewhale.sabi.model`:
- `CoralDepartureReason` (DIED, SOLD, GIVEN_AWAY, MOVED_TO_OTHER_TANK, OTHER)
- `CoralClassification` (LPS, SPS)
- `CoralCareLevel` (EASY, MODERATE, DEMANDING)
- `CoralGrowthType` (SURFACE_AREA_CM2, SIZE_CM, VOLUME_CM3, BRANCH_COUNT)
- `PolypCondition` (VITAL, TISSUE_LOSS, PALE, LIMP, SIGNIFICANT_GROWTH)

New TOs in `de.bluewhale.sabi.model`:
- `CoralStockEntryTo`, `CoralGrowthHistoryTo`, `CoralPolypConditionTo`
- `CoralCatalogueEntryTo`, `CoralCatalogueI18nTo`, `CoralCatalogueSearchResultTo`
- `CoralDepartureRecordTo`
- `CoralGrowthHistoryExportTo`, `CoralPolypConditionExportTo`
- `CoralExportTo` (extend existing stub!)
- `PublicReefReportCoralTo`

Modify existing:
- `PublicReefReportTo` — add `List<PublicReefReportCoralTo> coralInhabitants`
- `PublicReportLinkTo` — add `boolean includeCorals`

Look at `FishStockEntryTo`, `FishCatalogueEntryTo`, `FishCatalogueI18nTo` as reference.

### Step 3 — Server: JPA Entities and Repositories (sabi-server module)

New entities in `de.bluewhale.sabi.persistence.model`:
- `TankCoralStockEntity` — with `@SQLRestriction("deleted_at IS NULL")`, `@ManyToOne UserEntity user`
- `CoralGrowthHistoryEntity`
- `CoralPolypConditionEntity`
- `CoralCatalogueEntity`
- `CoralCatalogueI18nEntity`
- `CoralPhotoEntity`

Look at `TankFishStockEntity`, `FishSizeHistoryEntity`, `FishCatalogueEntryEntity` as reference.

New repositories in `de.bluewhale.sabi.persistence.repositories`:
- `TankCoralStockRepository` — must have `findByIdAndUserId(Long id, Long userId)` for ownership checks
- `CoralGrowthHistoryRepository`
- `CoralPolypConditionRepository`
- `CoralCatalogueRepository` — with `findByScientificNameAndStatusIn(...)` for duplicate check
- `CoralCatalogueI18nRepository`
- `CoralPhotoRepository`

### Step 4 — Server: Services (sabi-server module)

New service interfaces + implementations in `de.bluewhale.sabi.services`:
- `CoralStockService` / `CoralStockServiceImpl` — all CRUD + departure + growth + polyp ops
- `CoralCatalogueService` / `CoralCatalogueServiceImpl` — search, propose, update
- `CoralCatalogueMessageCodes`, `CoralStockMessageCodes`

Ownership check pattern (MANDATORY — AGENTS.md):
```java
TankCoralStockEntity entity = coralStockRepository.findByIdAndUserId(coralId, user.getId());
if (entity == null) {
    return new ResultTo<>(dto, Message.error(CoralStockMessageCodes.NOT_YOUR_CORAL, coralId));
}
```

Extend existing services:
- `ReefDataExportServiceImpl` — populate `corals` array in each `AquariumExportTo`
- `PublicReportServiceImpl` — populate `coralInhabitants` when `includeCorals == true`

### Step 5 — Server: REST Controllers (sabi-server module)

New controllers in `de.bluewhale.sabi.rest.controller`:
- `CoralStockController` — `@RequestMapping("api/coral")`
- `CoralCatalogueController` — `@RequestMapping("api/coral/catalogue")`
- `CoralCatalogueAdminController` — `@RequestMapping("api/admin/coral/catalogue")`

Modify existing:
- `PublicReportLinkController` — handle `includeCorals` flag in create/update

Look at `FishStockController`, `FishCatalogueController`, `FishCatalogueAdminController` as reference.

### Step 6 — Server: Mappers

New mappers in `de.bluewhale.sabi.mapper`:
- `CoralStockMapper`
- `CoralCatalogueMapper`

### Step 7 — Webclient: API Gateways (sabi-webclient module)

New interfaces + impls in `de.bluewhale.sabi.webclient.apigateway`:
- `CoralStockService` / `CoralStockServiceImpl` (RestTemplate calls to `/api/coral/...`)
- `CoralCatalogueService` / `CoralCatalogueServiceImpl`
- `CoralCatalogueAdminService` / `CoralCatalogueAdminServiceImpl`

Look at `FishStockServiceImpl`, `FishCatalogueServiceImpl` as reference.

### Step 8 — Webclient: JSF/CDI Beans

New beans in `de.bluewhale.sabi.webclient.controller`:
- `CoralStockView` — list view (active + departed sections), `@RequestScope`
- `CoralStockEntryView` — add/edit form, `@RequestScope`
- `CoralDepartureView` — departure recording, `@RequestScope`
- `CoralGrowthHistoryView` — growth measurement list + chart toggle, `@RequestScope`
- `CoralPolypConditionView` — polyp condition list, `@RequestScope`
- `CoralCatalogueProposalView` — propose/edit form, `@RequestScope`
- `CoralCatalogueAdminView` — admin approval list, `@RequestScope`
- `CoralEntryNavContext` — session-scoped nav context (analogous to `FishEntryNavContext`)

New converter:
- `CoralCatalogueSearchResultConverter` (analogous to `FishCatalogueSearchResultConverter`)

### Step 9 — Webclient: XHTML Pages

New pages in `sabi-webclient/src/main/resources/META-INF/resources/secured/`:
- `coralStockTab.xhtml` — inline tab on aquarium detail page (show only for `waterType == MARINE`)
- `coralStockView.xhtml` — full coral list page
- `coralStockEntryPage.xhtml` — add/edit coral form (standalone page, NOT a dialog)
- `coralStockEntryForm.xhtml` — form include
- `coralDepartureForm.xhtml` — departure recording form (standalone page, NOT a dialog)
- `coralGrowthHistoryView.xhtml` — growth history table + line chart toggle
- `coralPolypConditionView.xhtml` — polyp condition history table
- `coralCatalogueProposalForm.xhtml` — propose/edit catalogue entry form
- `coralCatalogueI18nFields.xhtml` — reusable i18n fields component

New admin page in `secured/admin/`:
- `coralCatalogueAdminView.xhtml`

**UI style rules (MANDATORY — AGENTS.md)**:
- Input forms → standalone pages, NEVER `p:dialog`
- Every page → `sabi-back-link` breadcrumb + Cancel button
- Save button → `background: #065f46`
- `coralStockTab.xhtml` → only `rendered="#{tankDetailView.tank.waterType == 'MARINE'}"`

### Step 10 — i18n

Add new keys to all 6 files:
- `messages.properties` (fallback/EN)
- `messages_de.properties`
- `messages_en.properties`
- `messages_es.properties`
- `messages_fr.properties`
- `messages_it.properties`

See `contracts/i18n-keys.md` for the complete key list.

### Step 11 — E2E Tests

Add Playwright spec files in `e2e/tests/`:
- `coralStockFlow.spec.ts` (US1–US4)
- `coralCatalogueFlow.spec.ts` (US5–US7)
- `coralReportFlow.spec.ts` (US8)
- `coralExportFlow.spec.ts` (US9)

---

## Key Constraints Checklist

- [ ] `TankCoralStockRepository.findByIdAndUserId()` — ownership on every mutation
- [ ] No PII (email) in INFO+ logs — use `user.getId()` only
- [ ] Photo upload: `MultipartFile` not `byte[]` in controller signature
- [ ] `@SQLRestriction("deleted_at IS NULL")` on `TankCoralStockEntity`
- [ ] Coral stock tab: `rendered` guard for `MARINE` water type only
- [ ] departure_date >= added_on: validated both client-side and server-side
- [ ] growth/polyp date ≤ departed_on (if set): validated both sides
- [ ] All 6 i18n files: no missing keys
- [ ] No `p:dialog` for input forms — standalone pages only
- [ ] Docker redeploy: `bash server_redeploy.sh --boundary --flyway`

---

## Useful Reference Files

| What to study | File |
|--------------|------|
| Fish stock entity | `sabi-server/.../persistence/model/TankFishStockEntity.java` |
| Fish stock controller | `sabi-server/.../rest/controller/FishStockController.java` |
| Fish size history DDL | `sabi-database/.../V1_5_0_8__addFishSizeHistory.sql` |
| Fish catalogue UGC DDL | `sabi-database/.../V1_5_0_2__extendFishCatalogueForUGC.sql` |
| Fish catalogue i18n DDL | `sabi-database/.../V1_5_0_3__addFishCatalogueI18nTable.sql` |
| Report link opt-in DDL | `sabi-database/.../V1_6_0_2__addIncludeEventsToPublicReportLink.sql` |
| Fish stock JSF view | `sabi-webclient/.../controller/FishStockView.java` |
| AI export TO ancestor | `sabi-boundary/.../model/AquariumExportTo.java` |
| House reef report TO | `sabi-boundary/.../model/PublicReefReportTo.java` |
| Ownership check AGENTS.md | Section "Backend Security: Ownership Checks" |
| Photo upload AGENTS.md | Section "Multipart endpoints: MultipartFile instead of byte[]" |

