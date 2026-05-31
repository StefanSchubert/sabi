# Quickstart: Invertebrate Stock (006) — Developer Onboarding

**Branch**: `006-invertebrate-tracking`  
**Updated**: 2026-05-30

---

## 1. Overview

This feature adds **Invertebrate Stock Management** (tank roster) and an **Invertebrate Catalogue** (community species reference) to Sabi. It is a direct structural twin of:

- **Fish Stock** (spec 002) — `sabi-server/.../persistence/model/TankFishStockEntity.java` and friends
- **Coral Stock** (spec 005) — `sabi-server/.../persistence/model/TankCoralStockEntity.java` and friends

**Golden rule**: if you are unsure about a pattern, look at the coral equivalent first.

---

## 2. Pre-requisites

- Java 25 (see `constitution.md`)
- Maven (wrapper included: `./mvnw`)
- Docker + Docker Compose (for local MariaDB + Flyway)
- Local environment set up per `DEVELOPERS_MANUAL.md`

---

## 3. Run Flyway Migrations

All 6 new migrations live in:
```
sabi-database/src/main/resources/db/migration/version1_8_0/
```

From the repo root:
```bash
./mvnw -pl sabi-database flyway:migrate -Dflyway.configFiles=sabi-database/src/main/resources/flyway.conf
```

Or via Docker Compose (dev environment):
```bash
cd devops/sabi_docker_sdk && docker-compose up -d db
# Flyway runs automatically on sabi-server startup
```

---

## 4. Module Layout at a Glance

| Module | What you add for this feature |
|--------|-------------------------------|
| `sabi-database` | 6 Flyway SQL scripts (version1_8_0) |
| `sabi-boundary` | 4 new enums, 8 new/extended TOs |
| `sabi-server` | 5 entities, 5 repositories, 2 mappers, 2 services (+impls), 3 REST controllers |
| `sabi-webclient` | 6 backing-bean controllers, 1 converter, 2 API-gateway service impls, 6 XHTML views |

---

## 5. Adding an Invertebrate Stock Entry (end-to-end trace)

```
User → invertebrateStockTab.xhtml
     → InvertebrateStockView.java (JSF backing bean)
     → InvertebrateStockServiceImpl (webclient API gateway)
         → POST /api/invertebrate/  [Bearer token]
     → InvertebrateStockController (sabi-server)
     → InvertebrateStockServiceImpl (sabi-server)
         → TankInvertebrateStockRepository.save()
         → InvertebrateWaterSensitivityRepository.saveAll()   ← if any units selected
     → DB: invertebrate_stock + invertebrate_water_sensitivity rows created
```

---

## 6. Catalogue Proposal Flow (end-to-end trace)

```
User → invertebrateCatalogueProposalForm.xhtml
     → InvertebrateCatalogueProposalView.java
     → InvertebrateCatalogueServiceImpl (webclient)
         → POST /api/invertebrate-catalogue/  [Bearer token]
     → InvertebrateCatalogueController (sabi-server)
     → InvertebrateCatalogueServiceImpl (sabi-server)
         → InvertebrateCatalogueRepository.save()  [status=PENDING]
     → Admin navigates to invertebrateCatalogueAdminView.xhtml
     → InvertebrateCatalogueAdminView.java
         → PUT /api/admin/invertebrate-catalogue/{id}/approve  [Admin Bearer]
     → InvertebrateCatalogueAdminController → service → status=PUBLIC
```

---

## 7. AI Export Extension

`UserProfileServiceImpl` already iterates aquariums to build `AquariumExportTo`. Add the invertebrates loop:

```java
// In UserProfileServiceImpl.buildAquariumExport(aquariumId):
List<TankInvertebrateStockEntity> inverts =
    tankInvertebrateStockRepository.findByAquariumIdAndUserIdAndDeletedAtIsNull(aquariumId, userId);
List<InvertebrateExportTo> exportList = inverts.stream()
    .map(invertebrateStockMapper::toExportTo)
    .collect(Collectors.toList());
aquariumExportTo.setInvertebrates(exportList);
```

---

## 8. House Reef Report Extension

`PublicReportServiceImpl.getReport()` must:
1. Check `reportLink.isIncludeInvertebrates()`.
2. If true: load active invertebrates for the aquarium, map to `PublicReefReportInvertebrateTo`, set on `PublicReefReportTo.setInvertebrateInhabitants(list)`.
3. If false: leave `invertebrateInhabitants` null (not opted-in semantics).

New endpoint `PUT /api/report/link/{aquariumId}/include-invertebrates` mirrors the existing `include-events` and `include-corals` endpoints in `PublicReportController`.

---

## 9. Water Sensitivity Multi-Select (JSF pattern)

The Water Sensitivity multi-select loads all available units:

```java
// In InvertebrateStockEntryView.java:
@PostConstruct
public void init() {
    availableUnits = unitService.getLocalizedUnitsForCurrentLanguage(); // existing UnitService call
}
// Bound to p:selectManyCheckbox or p:listbox selectedUnitIds List<Integer>
```

On save, pass `entry.setWaterSensitivityUnitIds(selectedUnitIds)` in the TO.

---

## 10. i18n Keys Checklist

All keys below must be present in **all 6** message bundle files:

```properties
# Tab and sections
invertebratestock.tab.label=Invertebrates
invertebratestock.section.active=Currently in Tank
invertebratestock.section.departed=Departed (Historical)

# Form fields
invertebratestock.form.speciesname.label=Species Name
invertebratestock.form.category.label=Taxonomic Category
invertebratestock.form.addedon.label=Added On
invertebratestock.form.notes.label=Notes
invertebratestock.form.refurl.label=Reference URL
invertebratestock.form.refurl.invalid=Must be a valid URL (http/https) or empty.
invertebratestock.form.carelevel.label=Care Level
invertebratestock.form.mobility.label=Mobility
invertebratestock.form.ecologicalrole.label=Ecological Role
invertebratestock.form.activitypattern.label=Activity Pattern
invertebratestock.form.watersensitivity.label=Water Sensitivity
invertebratestock.form.notspecified=Not specified

# Taxonomic categories
invertebratestock.category.CRUSTACEAN=Crustacean
invertebratestock.category.MOLLUSC=Mollusc
invertebratestock.category.ECHINODERM=Echinoderm
invertebratestock.category.WORM=Worm

# Mobility
invertebratestock.mobility.MOBILE=Mobile
invertebratestock.mobility.SESSILE=Sessile

# Ecological role
invertebratestock.ecologicalrole.CLEANUP_CREW=Cleanup Crew
invertebratestock.ecologicalrole.NEUTRAL=Neutral
invertebratestock.ecologicalrole.DETRIMENTAL=Detrimental

# Activity pattern
invertebratestock.activitypattern.DIURNAL=Diurnal
invertebratestock.activitypattern.NOCTURNAL=Nocturnal
invertebratestock.activitypattern.BOTH=Diurnal & Nocturnal

# Departure (may reuse fishstock.departure.reason.* keys — same enum values)
invertebratestock.departure.title=Record Departure
invertebratestock.departure.date.label=Departure Date
invertebratestock.departure.reason.label=Reason

# Catalogue
invertebratecatalogue.search.placeholder=Search invertebrate catalogue...
invertebratecatalogue.search.noresults=No invertebrate catalogue entries found.
invertebratecatalogue.propose.link=Propose new entry
invertebratecatalogue.form.scientificname.label=Scientific Name
invertebratecatalogue.form.category.label=Taxonomic Category
invertebratecatalogue.form.carelevel.label=Care Level
invertebratecatalogue.admin.title=Invertebrate Catalogue Administration
invertebratecatalogue.admin.pending.title=Pending Proposals
invertebratecatalogue.admin.approve=Approve
invertebratecatalogue.admin.reject=Reject

# Validation errors
invertebratestock.error.speciesname.required=Species name is required.
invertebratestock.error.category.required=Taxonomic category is required.
invertebratestock.error.addedon.required=Entry date is required.
invertebratestock.error.addedon.future=Entry date cannot be in the future.
invertebratestock.error.departure.datebefore=Departure date cannot be before the entry date.
invertebratestock.error.departure.note.toolong=Departure note must not exceed 500 characters.
invertebratestock.error.delete.hasdeparture=Cannot delete an invertebrate with a departure record.
invertebratestock.error.photo.toolarge=Photo exceeds the maximum allowed size of 5 MB.
invertebratestock.error.photo.invalidformat=Photo must be in JPEG, PNG, WebP, or GIF format.
```

---

## 11. Testing Checklist (per user story)

| Story | Minimum integration test |
|-------|--------------------------|
| P1 — Add invertebrate | `POST /api/invertebrate/` → 201; `GET /api/invertebrate/{aqId}/list` includes entry |
| P2 — Functional classifications | `PUT /api/invertebrate/{id}` with all 4 classification fields → 202; verify DB roundtrip |
| P3 — Departure | `PUT /api/invertebrate/{id}/departure` → 202; departed entry absent from active list |
| P4 — Catalogue link | `GET /api/invertebrate-catalogue/search?q=lysmata` returns results; stock entry stores `invertebrateCatalogueId` |
| P5 — Propose entry | `POST /api/invertebrate-catalogue/` → 201 (PENDING); visible to proposer only |
| P6 — Admin approve/reject | `PUT /api/admin/invertebrate-catalogue/{id}/approve` → 202; entry status PUBLIC |
| P7 — Reef report | Enable flag; `GET /api/public/report/{token}` → `invertebrateInhabitants` populated |
| P8 — AI export | Trigger AI export; `AquariumExportTo.invertebrates` has expected entries |

---

## 12. Key Files Reference

| File | Purpose |
|------|---------|
| `sabi-database/.../version1_8_0/V1_8_0_3__addInvertebrateStockTable.sql` | Core stock table DDL |
| `sabi-server/.../model/TankInvertebrateStockEntity.java` | JPA entity — follow `TankCoralStockEntity` exactly, add new columns |
| `sabi-server/.../rest/controller/InvertebrateStockController.java` | REST controller — follow `CoralStockController` exactly |
| `sabi-boundary/.../model/InvertebrateStockEntryTo.java` | CRUD TO — follow `CoralStockEntryTo` + add classification fields |
| `sabi-webclient/.../controller/InvertebrateStockView.java` | JSF backing bean — follow `CoralStockView` |
| `sabi-webclient/.../resources/secured/invertebrateStockTab.xhtml` | Tab fragment — follow `coralStockTab.xhtml` |
| `specs/006-invertebrate-tracking/contracts/invertebrate-api.yaml` | Full OpenAPI 3.0 contract |
| `specs/006-invertebrate-tracking/data-model.md` | DB schema, entity fields, TOs, state transitions |
