# Tasks: Invertebrate Stock Management & Catalogue

**Feature**: `006-invertebrate-tracking` | **Branch**: `006-invertebrate-tracking`  
**Input**: Design documents from `specs/006-invertebrate-tracking/`  
**Prerequisites completed**: plan.md ✅ · spec.md ✅ · research.md ✅ · data-model.md ✅ · contracts/invertebrate-api.yaml ✅ · quickstart.md ✅

**Reference patterns** (follow exactly): `CoralStockController`, `CoralStockServiceImpl`, `CoralCatalogueController`, `TankCoralStockEntity`, `CoralStockMapper`  
**Module key**: `sabi-boundary` → shared TOs/enums · `sabi-server` → JPA/services/REST · `sabi-webclient` → JSF/beans/XHTML · `sabi-database` → Flyway  
**Jakarta EE 9+**: All imports use `jakarta.*` (not `javax.*`)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other [P]-marked tasks in the same phase (independent files)
- **[Story]**: User story label (US1–US8 from spec.md)
- Exact file paths included in every task

---

## Phase 1: Setup (Flyway Version Directory)

**Purpose**: Create the new Flyway version directory so all 6 migration scripts in Phase 2 have a place to land.

- [x] T001 Create Flyway migration directory `sabi-database/src/main/resources/db/migration/version1_8_0/` (add a `.gitkeep` placeholder so the directory is tracked before migration scripts are added)

---

## Phase 2: Foundational — Migrations, Enums, TOs, Entities, Repositories

**Purpose**: Everything ALL user stories depend on. Must be complete before any Phase 3+ work starts.

> ⚠️ **Flyway ordering constraint**: All six V1_8_0_* migrations must be written now. Flyway applies scripts in strict ascending version order; writing V1_8_0_3 before V1_8_0_1–2 and V1_8_0_5 after V1_8_0_4 would create an `out-of-order` error if the intermediate scripts are added later after the DB has already advanced. Write all six migration files up-front.

### Flyway Migrations

- [x] T002 Create `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_1__addInvertebrateCatalogueTable.sql` — mirror `coral_catalogue`; include `active_scientific_name` virtual generated column (`IF(status IN ('PENDING','PUBLIC'), scientific_name, NULL)`) and `UNIQUE idx_invert_catalogue_active_name (active_scientific_name)`; indexes on `status` and `proposer_user_id`; optlock column; FK `proposer_user_id → users.id ON DELETE SET NULL`
- [x] T003 [P] Create `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_2__addInvertebrateCatalogueI18nTable.sql` — mirror `coral_catalogue_i18n`; UNIQUE `(catalogue_id, language_code)`; FK `catalogue_id → invertebrate_catalogue.id ON DELETE CASCADE`; index on `language_code`
- [x] T004 [P] Create `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_3__addInvertebrateStockTable.sql` — mirror `coral_stock`; include nullable columns `mobility VARCHAR(10)`, `ecological_role VARCHAR(15)`, `activity_pattern VARCHAR(10)`, `taxonomic_category VARCHAR(12) NOT NULL`; soft-delete column `deleted_at TIMESTAMP NULL`; indexes on `aquarium_id`, `user_id`, `deleted_at`; FKs: `aquarium_id → aquarium.id ON DELETE CASCADE`, `user_id → users.id`, `invertebrate_catalogue_id → invertebrate_catalogue.id ON DELETE SET NULL`
- [x] T005 [P] Create `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_4__addInvertebrateWaterSensitivityTable.sql` — table `invertebrate_water_sensitivity`; UNIQUE `(invertebrate_stock_id, unit_id)`; FKs: `invertebrate_stock_id → invertebrate_stock.id ON DELETE CASCADE`, `unit_id → unit.id`; index on `unit_id`
- [x] T006 [P] Create `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_5__addInvertebratePhotoTable.sql` — table `invertebrate_photo`; mirror `coral_photo`; UNIQUE `invertebrate_stock_id` (one photo per entry); FK `invertebrate_stock_id → invertebrate_stock.id ON DELETE CASCADE`; columns: `file_path VARCHAR(512)`, `content_type VARCHAR(50)`, optlock, timestamps
- [x] T007 [P] Create `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_6__addIncludeInvertebratesToPublicReportLink.sql` — `ALTER TABLE public_report_link ADD COLUMN include_invertebrates TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'When 1, invertebrate stock is included in the public report'`

### sabi-boundary: Enums

- [x] T008 [P] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateTaxonomicCategory.java` — enum values: `CRUSTACEAN`, `MOLLUSC`, `ECHINODERM`, `WORM`; follow the existing boundary enum pattern (implements Serializable if needed); add Javadoc
- [x] T009 [P] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateMobility.java` — enum values: `MOBILE`, `SESSILE`
- [x] T010 [P] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateEcologicalRole.java` — enum values: `CLEANUP_CREW`, `NEUTRAL`, `DETRIMENTAL`
- [x] T011 [P] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateActivityPattern.java` — enum values: `DIURNAL`, `NOCTURNAL`, `BOTH`

### sabi-boundary: Core Transfer Objects

- [x] T012 [P] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateStockEntryTo.java` — `@Data` Lombok; fields: `id Long`, `aquariumId @NotNull Long`, `speciesName @NotBlank String`, `scientificName String`, `taxonomicCategory @NotNull InvertebrateTaxonomicCategory`, `careLevel InvertebrateCareLevel` (reuse `CoralCareLevel` or create alias if needed), `mobility InvertebrateMobility`, `ecologicalRole InvertebrateEcologicalRole`, `activityPattern InvertebrateActivityPattern`, `waterSensitivityUnitIds List<Integer>` (empty = none), `externalRefUrl @Pattern(regexp="^(https?://.*)?$") String`, `notes String`, `addedOn @NotNull @PastOrPresent LocalDate`, `departedOn LocalDate`, `departureReason` (reuse `CoralDepartureReason` or `FishDepartureReason` — values DIED/SOLD/GIVEN_AWAY/MOVED_TO_OTHER_TANK/OTHER are identical), `departureNote @Size(max=500) String`, `invertebrateCatalogueId Long`, `hasPhoto boolean`; implements Serializable
- [x] T013 [P] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateDepartureRecordTo.java` — `@Data`; fields: `departedOn @NotNull LocalDate`, `departureReason @NotNull` (reuse fish/coral departure reason enum), `departureNote @Size(max=500) String`; implements Serializable

### sabi-server: JPA Entities

- [x] T014 Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/TankInvertebrateStockEntity.java` — `@Entity @Table(name="invertebrate_stock", schema="sabi") @SQLRestriction("deleted_at IS NULL")`; mirror `TankCoralStockEntity` exactly; additional columns: `taxonomicCategory String`, `mobility String`, `ecologicalRole String`, `activityPattern String`; `waterSensitivities @OneToMany(mappedBy="invertebrateStockId", cascade=CascadeType.ALL, orphanRemoval=true)` → `InvertebrateWaterSensitivityEntity`; `catalogueEntry @ManyToOne @JoinColumn(name="invertebrate_catalogue_id", insertable=false, updatable=false)` → `InvertebrateCatalogueEntity`; `user @ManyToOne @JoinColumn(name="user_id")`; `deletedAt Timestamp`; optlock `@Version`
- [x] T015 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/InvertebrateWaterSensitivityEntity.java` — `@Entity @Table(name="invertebrate_water_sensitivity", schema="sabi")`; fields: `id Long`, `invertebrateStockId Long` (FK scalar), `unitId Integer`; no Lombok on entity (follow existing pattern)
- [x] T016 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/InvertebratePhotoEntity.java` — `@Entity @Table(name="invertebrate_photo", schema="sabi")`; mirror `CoralPhotoEntity`; fields: `id Long`, `invertebrateStockId Long` (FK scalar, unique), `filePath String`, `contentType String`; optlock; timestamps

### sabi-server: Repositories

- [x] T017 Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/TankInvertebrateStockRepository.java` — `extends JpaRepository<TankInvertebrateStockEntity, Long>`; mirror `TankCoralStockRepository`; required queries: `findAllByAquariumIdAndUserId(Long, Long)`, `findByIdAndUserId(Long, Long)` (ownership check), `findByAquariumIdAndUserIdAndDeletedAtIsNull(Long, Long)` (export/report use)
- [x] T018 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/InvertebrateWaterSensitivityRepository.java` — `extends JpaRepository<InvertebrateWaterSensitivityEntity, Long>`; add `deleteAllByInvertebrateStockId(Long)` and `findAllByInvertebrateStockId(Long)`
- [x] T019 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/InvertebratePhotoRepository.java` — `extends JpaRepository<InvertebratePhotoEntity, Long>`; mirror `CoralPhotoRepository`; add `findByInvertebrateStockId(Long)` and `deleteByInvertebrateStockId(Long)`

### sabi-server: Exception/Message Codes & Service Interface

- [x] T020 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateStockExceptionCodes.java` — mirror `CoralStockExceptionCodes`; include at minimum: `INVERT_NOT_FOUND`, `INVERT_NOT_OWNER`, `INVERT_HAS_DEPARTURE_RECORD`, `DEPARTURE_DATE_BEFORE_ENTRY`, `INVERT_PHOTO_TOO_LARGE`, `INVERT_PHOTO_INVALID_FORMAT`
- [x] T021 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateStockMessageCodes.java` — mirror `CoralStockMessageCodes`
- [x] T022 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateCatalogueExceptionCodes.java` — mirror `CoralCatalogueExceptionCodes`; include: `CATALOGUE_NOT_FOUND`, `CATALOGUE_NOT_OWNER`, `CATALOGUE_NAME_DUPLICATE`
- [x] T023 [P] Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateCatalogueMessageCodes.java` — mirror `CoralCatalogueMessageCodes`
- [x] T024 Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateStockService.java` — service interface; mirror `CoralStockService`; declare method signatures for: `listForAquarium`, `getById`, `create`, `update`, `delete`, `recordDeparture`, `uploadPhoto`, `getPhoto`, `deletePhoto`, `removeCatalogueLink`

**Checkpoint**: Foundation complete. Phase 3+ user story work can now begin.

---

## Phase 3: User Story 1 — Add an Invertebrate to My Aquarium (Priority: P1) 🎯 MVP

**Goal**: Full CRUD for a basic invertebrate stock entry (species name, date, category, notes, URL, photo). The aquarium detail page gains an "Invertebrates" tab. Users can add, view, edit, and delete entries.

**Independent Test**: `POST /api/invertebrate/` → 201; `GET /api/invertebrate/{aqId}/list` returns the entry with all saved fields; UI tab visible on aquarium detail page.

### sabi-server: Mapper + Service + Controller

- [x] T025 [US1] Create `sabi-server/src/main/java/de/bluewhale/sabi/mapper/InvertebrateStockMapper.java` — mirror `CoralStockMapper`; annotate with `@Mapper(componentModel="spring")`; implement `toTo(TankInvertebrateStockEntity) → InvertebrateStockEntryTo` and `toEntity(InvertebrateStockEntryTo) → TankInvertebrateStockEntity`; map all scalar fields including `taxonomicCategory`, `mobility`, `ecologicalRole`, `activityPattern`; ignore `waterSensitivities` collection (handled by service); set `hasPhoto` from `photoEntity != null`
- [x] T026 [US1] Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateStockServiceImpl.java` — implements `InvertebrateStockService`; annotate `@Service @Transactional`; mirror `CoralStockServiceImpl` exactly; implement: (1) **ownership security pattern** for every mutating method — `userRepository.getByEmail(email)` → `findByIdAndUserId(id, userId)` → reject with `INVERT_NOT_OWNER` if null; (2) `listForAquarium`: `findAllByAquariumIdAndUserId`; (3) `create`: validate `addedOn` ≤ today, save entity; (4) `update`: ownership check, save; (5) `delete`: ownership check, reject with `INVERT_HAS_DEPARTURE_RECORD` (409) if `departedOn != null`, hard delete; NOTE — water-sensitivity persistence is added in T039 (Phase 4)
- [x] T027 [US1] Create `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/InvertebrateStockController.java` — mirror `CoralStockController`; `@RestController @RequestMapping("/api/invertebrate")`; implement: `GET /{aquariumId}/list` → 200, `GET /{invertebrateId}` → 200/404, `POST /` → 201, `PUT /{invertebrateId}` → 202/400/403, `DELETE /{invertebrateId}` → 204/403/409; JWT `X-Auth-Token` header extraction; SpringDoc `@Tag(name="Invertebrate Stock")` + `@Operation` annotations per endpoint; ownership errors → 403

### sabi-webclient: API Gateway

- [x] T028 [P] [US1] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/InvertebrateStockService.java` — interface; mirror `CoralStockService` (webclient apigateway); declare: `listForAquarium`, `getById`, `create`, `update`, `delete`
- [x] T029 [US1] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/InvertebrateStockServiceImpl.java` — implements `InvertebrateStockService`; mirror `CoralStockServiceImpl` (webclient); use `RestHelper` to call `POST /api/invertebrate/`, `GET /api/invertebrate/{aqId}/list`, `GET /api/invertebrate/{id}`, `PUT /api/invertebrate/{id}`, `DELETE /api/invertebrate/{id}`; pass `X-Auth-Token` from session; map JSON responses to `InvertebrateStockEntryTo`

### sabi-webclient: Backing Beans

- [x] T030 [P] [US1] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateStockView.java` — `@Named @ViewScoped`; mirror `CoralStockView`; holds `List<InvertebrateStockEntryTo>` for active and departed sections; calls `InvertebrateStockService.listForAquarium`; actions: `delete`, `navigateToEntry`
- [x] T031 [P] [US1] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateStockEntryView.java` — `@Named @ViewScoped`; mirror `CoralStockEntryView`; holds `InvertebrateStockEntryTo`; actions: `save` (create/update), `cancel`; `@PostConstruct init()`: load entry if editing or blank `InvertebrateStockEntryTo` if adding; NOTE — water sensitivity unit loading added in T040 (Phase 4)
- [x] T032 [P] [US1] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateEntryNavContext.java` — `@Named @SessionScoped`; mirror `CoralEntryNavContext`; stores `selectedInvertebrateId` and `currentAquariumId` for navigation between the stock list and entry form
- [x] T033 [P] [US1] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/rest/InvertebratePhotoController.java` — `@RestController`; mirror `CoralPhotoController`; proxies photo upload (`POST /api/invertebrate/{id}/photo`), retrieval (`GET /api/invertebrate/{id}/photo`), and deletion (`DELETE /api/invertebrate/{id}/photo`) to sabi-server; handles multipart file; returns JSF-compatible response

### sabi-webclient: XHTML Views

- [x] T034 [US1] Create `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateStockTab.xhtml` — mirror `coralStockTab.xhtml`; PrimeFaces `<p:dataTable>` for active entries (no `departed_on`); collapsible `<p:fieldset>` for departed entries (collapsed by default); columns: species name, taxonomic category, added date, notes, actions (edit, departure, delete); wire action buttons to `InvertebrateStockView`; use `invertebratestock.*` i18n keys from Phase 3 i18n task
- [x] T035 [US1] Create `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateStockEntryPage.xhtml` — mirror `coralStockEntryPage.xhtml`; form fields: species name (required), entry date (`<p:calendar>`, required, not-future validation), taxonomic category (`<p:selectOneMenu>` bound to `InvertebrateTaxonomicCategory` enum, required), external reference URL (optional), notes (`<p:inputTextarea>`); photo upload widget (mirror coral entry); save/cancel buttons; validation messages wired to `invertebratestock.error.*` keys; NOTE — classification fields added in T041 (Phase 4)

### Wire tab into aquarium detail

- [x] T036 [US1] Edit `sabi-webclient/src/main/resources/META-INF/resources/secured/tankEditor.xhtml` — add `<ui:include src="/secured/invertebrateStockTab.xhtml"/>` tab alongside existing fish and coral tabs; no other changes to this file

### i18n: Base keys for User Story 1

- [x] T037 [P] [US1] Add i18n keys to ALL 6 message bundle files (`sabi-webclient/src/main/resources/i18n/messages.properties`, `messages_de.properties`, `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`, `messages_it.properties`) — add the following key groups with appropriate translations per language: tab label (`invertebratestock.tab.label`), section headers (`invertebratestock.section.active`, `invertebratestock.section.departed`), form field labels (species name, category, added on, notes, reference URL, care level), taxonomic category values (`invertebratestock.category.CRUSTACEAN/MOLLUSC/ECHINODERM/WORM`), validation error messages (`invertebratestock.error.speciesname.required`, `invertebratestock.error.category.required`, `invertebratestock.error.addedon.required`, `invertebratestock.error.addedon.future`, `invertebratestock.error.delete.hasdeparture`, `invertebratestock.error.photo.toolarge`, `invertebratestock.error.photo.invalidformat`), not-specified placeholder (`invertebratestock.form.notspecified`)

**Checkpoint**: US1 complete — invertebrate tab visible, basic CRUD functional end-to-end.

---

## Phase 4: User Story 2 — Set Functional Classifications (Priority: P2)

**Goal**: Users can set Mobility, Ecological Role, Activity Pattern, and Water Sensitivity on an invertebrate entry. All four classifications are persisted and displayed.

**Independent Test**: `PUT /api/invertebrate/{id}` with all four classification fields populated → 202; `GET /api/invertebrate/{id}` returns all four fields correctly; water sensitivity unit IDs match selected units.

### sabi-server: Extend mapper and service for water sensitivity

- [ ] T038 [US2] Update `sabi-server/src/main/java/de/bluewhale/sabi/mapper/InvertebrateStockMapper.java` — extend `toTo()` to map `entity.waterSensitivities` → `List<Integer> waterSensitivityUnitIds` (extract `unitId` from each `InvertebrateWaterSensitivityEntity`); extend `toEntity()` to build `InvertebrateWaterSensitivityEntity` list from `To.waterSensitivityUnitIds` (note: actual persistence is via service, mapper only creates the entity objects)
- [ ] T039 [US2] Update `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateStockServiceImpl.java` — in `create` and `update` methods: after saving the stock entity, call `invertebrateWaterSensitivityRepository.deleteAllByInvertebrateStockId(id)` then `saveAll()` the new sensitivity list from `waterSensitivityUnitIds`; wrap in the existing `@Transactional` boundary; no other changes

### sabi-webclient: Water sensitivity multi-select and classification dropdowns

- [ ] T040 [US2] Update `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateStockEntryView.java` — add `@PostConstruct` logic to load `availableUnits` via existing `UnitService.getLocalizedUnitsForCurrentLanguage()`; add `List<Integer> selectedUnitIds` bound to water sensitivity multi-select; wire selectedUnitIds → `entry.setWaterSensitivityUnitIds(selectedUnitIds)` on save
- [ ] T041 [US2] Update `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateStockEntryPage.xhtml` — add classification section: Mobility `<p:selectOneMenu>` (InvertebrateMobility enum, optional), Ecological Role `<p:selectOneMenu>` (InvertebrateEcologicalRole, optional), Activity Pattern `<p:selectOneMenu>` (InvertebrateActivityPattern, optional), Water Sensitivity `<p:selectManyCheckbox>` or `<p:listbox>` (bound to `availableUnits`/`selectedUnitIds`); display "Not specified" option for unset single-choice dimensions; wire to `InvertebrateStockEntryView`

### i18n: Functional classification labels

- [ ] T042 [P] [US2] Add i18n keys to all 6 message bundle files — mobility labels (`invertebratestock.mobility.MOBILE`, `invertebratestock.mobility.SESSILE`), ecological role labels (`invertebratestock.ecologicalrole.CLEANUP_CREW/NEUTRAL/DETRIMENTAL`), activity pattern labels (`invertebratestock.activitypattern.DIURNAL/NOCTURNAL/BOTH`), water sensitivity form label (`invertebratestock.form.watersensitivity.label`), form labels for all four classification fields (mobility, ecological role, activity pattern, care level)

**Checkpoint**: US2 complete — all four classification dimensions can be set, persisted, and displayed.

---

## Phase 5: User Story 3 — Record an Invertebrate Departure (Priority: P3)

**Goal**: Users can record a departure (date + reason + optional note) for a live invertebrate. The entry moves from the active list to the collapsed departed section. Departure date is validated against entry date.

**Independent Test**: Add an entry, `PUT /api/invertebrate/{id}/departure` → 202; `GET /api/invertebrate/{aqId}/list` no longer shows entry in active portion; departed entry is visible in historical section with correct date and reason.

### sabi-server: Departure endpoint

- [ ] T043 [US3] Update `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateStockServiceImpl.java` — implement `recordDeparture(Long id, InvertebrateDepartureRecordTo dto, String userEmail)`: (1) ownership check via `findByIdAndUserId`; (2) validate `dto.departedOn >= entity.addedOn` — throw `DEPARTURE_DATE_BEFORE_ENTRY` (422) if violated; (3) validate `departureNote` ≤ 500 chars; (4) set `departedOn`, `departureReason`, `departureNote` on entity and save
- [ ] T044 [US3] Update `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/InvertebrateStockController.java` — add `PUT /{invertebrateId}/departure` endpoint; accepts `@RequestBody @Valid InvertebrateDepartureRecordTo`; calls `invertebrateStockService.recordDeparture()`; returns 202 on success, 422 if date validation fails, 403 if not owner; add `@Operation` SpringDoc annotation

### sabi-webclient: Departure form

- [ ] T045 [P] [US3] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateDepartureView.java` — `@Named @ViewScoped`; mirror `CoralDepartureView`; holds `InvertebrateDepartureRecordTo`; `availableDepartureReasons` list (reuse existing departure reason enum — values DIED/SOLD/GIVEN_AWAY/MOVED_TO_OTHER_TANK/OTHER are identical to fish/coral); `save()` calls `InvertebrateStockServiceImpl.recordDeparture()` via webclient service; navigates back to stock tab on success
- [ ] T046 [P] [US3] Create `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateDepartureForm.xhtml` — mirror `coralDepartureForm.xhtml`; fields: departure date (`<p:calendar>`, required), departure reason (`<p:selectOneMenu>`, required), departure note (`<p:inputTextarea>`, max 500 chars); inline validation messages; save/cancel; wire to `InvertebrateDepartureView`
- [ ] T047 [US3] Update `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateStockTab.xhtml` — add "Record departure" action button to the active entries table; wire departure button to navigate to `invertebrateDepartureForm.xhtml` via `InvertebrateEntryNavContext`; verify the departed section (already present from T034) correctly filters on `departedOn IS NOT NULL`

### sabi-webclient: Wire departure service call

- [ ] T048 [US3] Update `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/InvertebrateStockServiceImpl.java` — add `recordDeparture(Long id, InvertebrateDepartureRecordTo dto)` REST call to `PUT /api/invertebrate/{id}/departure`; handle 422 response with user-visible error message

### i18n: Departure labels

- [ ] T049 [P] [US3] Add i18n keys to all 6 message bundle files — departure section: `invertebratestock.departure.title`, `invertebratestock.departure.date.label`, `invertebratestock.departure.reason.label`; departure validation errors: `invertebratestock.error.departure.datebefore`, `invertebratestock.error.departure.note.toolong`; note: departure reason values may reuse existing `fishstock.departure.reason.*` keys if the backing bean references them; add separate `invertebratestock.departure.reason.*` keys as aliases if required by the UI binding

**Checkpoint**: US3 complete — departure flow works end-to-end; active/departed separation correct.

---

## Phase 6: User Story 4 — Link an Invertebrate Entry to the Catalogue (Priority: P4)

**Goal**: When adding/editing an invertebrate, the user can search the catalogue, select an entry, and have scientific name, category, and care level auto-filled. The catalogue link is persisted. Users can also remove the link without deleting the entry.

**Independent Test**: With at least one PUBLIC catalogue entry, `GET /api/invertebrate-catalogue/search?q=lysmata` returns results; save an invertebrate with `invertebrateCatalogueId` set; `GET /api/invertebrate/{id}` returns that ID; `DELETE /api/invertebrate/{id}/catalogue-link` → 202 and removes the catalogue ID.

### sabi-server: Catalogue entities, repositories, mapper, service

- [ ] T050 [P] [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/InvertebrateCatalogueEntity.java` — `@Entity @Table(name="invertebrate_catalogue", schema="sabi")`; mirror `CoralCatalogueEntity`; fields: `id`, `scientificName`, `taxonomicCategory String`, `careLevel String`, `status String` (default "PENDING"), `proposerUserId Long`, `proposalDate LocalDate`; `i18nEntries @OneToMany(mappedBy="catalogueId", cascade=CascadeType.ALL)` → `InvertebrateCatalogueI18nEntity`; `proposer @ManyToOne @JoinColumn(name="proposer_user_id", insertable=false, updatable=false)`
- [ ] T051 [P] [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/InvertebrateCatalogueI18nEntity.java` — `@Entity @Table(name="invertebrate_catalogue_i18n", schema="sabi")`; mirror `CoralCatalogueI18nEntity`; fields: `id`, `catalogueId Long` (FK scalar), `languageCode String`, `commonName String`, `description String`, `refUrl String`
- [ ] T052 [P] [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/InvertebrateCatalogueRepository.java` — `extends JpaRepository<InvertebrateCatalogueEntity, Long>`; required queries: `findAllByStatus(String)` for admin list-pending; JPQL or Spring Data `findByScientificNameContainingIgnoreCaseAndStatusIn(String, List<String>)` for search; `findByScientificName(String)` for duplicate check
- [ ] T053 [P] [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/InvertebrateCatalogueI18nRepository.java` — `extends JpaRepository<InvertebrateCatalogueI18nEntity, Long>`; add `findByCatalogueIdAndLanguageCode(Long, String)` and `findAllByCatalogueId(Long)`

### sabi-boundary: Catalogue TOs

- [ ] T054 [P] [US4] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateCatalogueEntryTo.java` — `@Data`; mirror `CoralCatalogueEntryTo`; fields: `id Long`, `scientificName @NotBlank String`, `taxonomicCategory @NotNull InvertebrateTaxonomicCategory`, `careLevel @NotNull` (reuse `CoralCareLevel`), `status FishCatalogueStatus` (reuse existing enum — PENDING/PUBLIC/REJECTED values are identical), `proposerUserId Long`, `proposalDate LocalDate`, `i18nEntries @Valid List<InvertebrateCatalogueI18nTo>`; implements Serializable
- [ ] T055 [P] [US4] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateCatalogueI18nTo.java` — `@Data`; fields: `id Long`, `languageCode @NotBlank String`, `commonName @NotBlank String`, `description @Size(max=2000) String`, `refUrl String`; implements Serializable
- [ ] T056 [P] [US4] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateCatalogueSearchResultTo.java` — `@Data`; fields: `id Long`, `scientificName String`, `taxonomicCategory InvertebrateTaxonomicCategory`, `commonName String` (in user's language), `status FishCatalogueStatus`; implements Serializable

### sabi-server: Catalogue mapper, service, controller (search only)

- [ ] T057 [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/mapper/InvertebrateCatalogueMapper.java` — `@Mapper(componentModel="spring")`; mirror `CoralCatalogueMapper`; methods: `toTo(InvertebrateCatalogueEntity)`, `toSearchResultTo(InvertebrateCatalogueEntity, String languageCode)`, `toEntity(InvertebrateCatalogueEntryTo)`
- [ ] T058 [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateCatalogueService.java` — interface; mirror `CoralCatalogueService`; declare: `search(String query, String lang, String callerEmail)`, `propose(InvertebrateCatalogueEntryTo, String callerEmail)`, `getById(Long, String callerEmail)`, `update(InvertebrateCatalogueEntryTo, String callerEmail)`, `listPending()`, `approve(Long, String callerEmail)`, `reject(Long, String callerEmail)`
- [ ] T059 [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateCatalogueServiceImpl.java` — implements `InvertebrateCatalogueService`; `@Service @Transactional`; mirror `CoralCatalogueServiceImpl`; implement **search** method first (for US4): returns PUBLIC entries matching query + caller's own PENDING entries; uses `invertebrateCatalogueRepository` + i18n join; returns `List<InvertebrateCatalogueSearchResultTo>` — remaining methods (propose, approve, reject) added in Phase 7/8
- [ ] T060 [US4] Create `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/InvertebrateCatalogueController.java` — `@RestController @RequestMapping("/api/invertebrate-catalogue")`; mirror `CoralCatalogueController`; implement for US4: `GET /search?q=&lang=` → 200 list; SpringDoc `@Tag(name="Invertebrate Catalogue")` annotations; remaining POST/PUT endpoints added in Phase 7
- [ ] T061 [US4] Update `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/InvertebrateStockController.java` — add `DELETE /{invertebrateId}/catalogue-link` endpoint; calls service to set `invertebrateCatalogueId = null` on the stock entity (ownership check required); returns 202 with updated entry; add `@Operation` annotation

### sabi-webclient: Catalogue API gateway (search)

- [ ] T062 [P] [US4] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/InvertebrateCatalogueService.java` — interface; declare: `search(String query, String lang)`, `propose(InvertebrateCatalogueEntryTo)`, `getById(Long)`, `update(InvertebrateCatalogueEntryTo)`, `listPending()`, `approve(Long)`, `reject(Long)`
- [ ] T063 [US4] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/InvertebrateCatalogueServiceImpl.java` — implements `InvertebrateCatalogueService`; mirror `CoralCatalogueServiceImpl` (webclient); implement `search()` REST call to `GET /api/invertebrate-catalogue/search`; remaining methods added in Phase 7/8
- [ ] T064 [P] [US4] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/converter/InvertebrateCatalogueSearchResultConverter.java` — JSF `@FacesConverter`; mirror `CoralCatalogueSearchResultConverter`; converts `InvertebrateCatalogueSearchResultTo` ↔ String for `<p:autoComplete>` component binding

### sabi-webclient: Wire catalogue search into entry form

- [ ] T065 [US4] Update `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateStockEntryPage.xhtml` — add catalogue search `<p:autoComplete>` component (min 2 chars, calls `InvertebrateCatalogueServiceImpl.search()`); on item-select: auto-fill scientific name, taxonomic category, care level, and reference URL for current user language; add "Remove catalogue link" button (visible when `entry.invertebrateCatalogueId != null`); wire to `InvertebrateStockEntryView`
- [ ] T066 [US4] Update `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateStockEntryView.java` — add `removeCatalogueLink()` action method that calls `InvertebrateStockServiceImpl.removeCatalogueLink()` and refreshes the entry

### i18n: Catalogue search labels

- [ ] T067 [P] [US4] Add i18n keys to all 6 message bundle files — catalogue search: `invertebratecatalogue.search.placeholder`, `invertebratecatalogue.search.noresults`, `invertebratecatalogue.propose.link`; form labels: `invertebratecatalogue.form.scientificname.label`, `invertebratecatalogue.form.category.label`, `invertebratecatalogue.form.carelevel.label`

**Checkpoint**: US4 complete — catalogue search works in the entry form; catalogue link is stored and can be removed.

---

## Phase 7: User Story 5 — Propose a New Invertebrate Catalogue Entry (Priority: P5)

**Goal**: Any authenticated user can propose a new catalogue entry (scientific name, category, care level, i18n fields). The proposal is immediately visible and selectable only by the proposer (status PENDING). It appears in the admin's pending queue.

**Independent Test**: `POST /api/invertebrate-catalogue/` → 201 (PENDING); `GET /api/invertebrate-catalogue/search?q=<proposer's term>` shows the entry for the proposer; same search by a different user returns no result for the pending entry.

### sabi-server: Propose, get, update

- [ ] T068 [US5] Update `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateCatalogueServiceImpl.java` — implement `propose()`: (1) check for duplicate scientific name among PENDING/PUBLIC → if found, include warning code in result (non-blocking — still save); (2) set `status = "PENDING"`, `proposerUserId = callerUserId`, `proposalDate = today`; (3) save via repository; implement `getById()`: return entry only if PUBLIC or owned by caller; implement `update()`: ownership check (proposer or admin); save updated i18n entries
- [ ] T069 [US5] Update `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/InvertebrateCatalogueController.java` — add: `POST /` → 201 with `ResultToCatalogueEntry` (or 409 warning body if name collision); `GET /{catalogueId}` → 200/404; `PUT /{catalogueId}` → 202/403; `@Operation` annotations for all three endpoints

### sabi-webclient: Proposal form

- [ ] T070 [US5] Update `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/InvertebrateCatalogueServiceImpl.java` — implement `propose()` REST call to `POST /api/invertebrate-catalogue/`; implement `update()` REST call to `PUT /api/invertebrate-catalogue/{id}`; handle 409 (duplicate name warning) — display non-blocking warning message; implement `getById()` REST call
- [ ] T071 [P] [US5] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateCatalogueProposalView.java` — `@Named @ViewScoped`; mirror `CoralCatalogueProposalView`; holds `InvertebrateCatalogueEntryTo` with list of `InvertebrateCatalogueI18nTo` entries (one per language DE/EN/ES/FR/IT); `save()` calls `propose()` or `update()` depending on whether `id` is set; navigate to stock entry form after successful proposal so user can link the new entry
- [ ] T072 [P] [US5] Create `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateCatalogueI18nFields.xhtml` — reusable `<ui:composition>` fragment; mirror `coralCatalogueI18nFields.xhtml`; renders a `<p:tabView>` with one tab per language (DE/EN/ES/FR/IT); each tab: common name (required for at least one language), description (optional, max 2000 chars), reference URL (optional); wire to `InvertebrateCatalogueProposalView`
- [ ] T073 [P] [US5] Create `sabi-webclient/src/main/resources/META-INF/resources/secured/invertebrateCatalogueProposalForm.xhtml` — mirror `coralCatalogueProposalForm.xhtml`; form fields: scientific name (required, unique-check warning), taxonomic category (required, `<p:selectOneMenu>`), care level (required, `<p:selectOneMenu>`); include `<ui:include src="/secured/invertebrateCatalogueI18nFields.xhtml"/>`; save/cancel; duplicate-name warning message displayed when server returns 409 body

### i18n: Catalogue proposal labels

- [ ] T074 [P] [US5] Add i18n keys to all 6 message bundle files — catalogue administration prefix: `invertebratecatalogue.admin.title`, `invertebratecatalogue.admin.pending.title`, `invertebratecatalogue.admin.approve`, `invertebratecatalogue.admin.reject`; catalogue status labels: `invertebratecatalogue.status.PENDING`, `invertebratecatalogue.status.PUBLIC`, `invertebratecatalogue.status.REJECTED`; duplicate name warning: `invertebratecatalogue.warning.name.duplicate`

**Checkpoint**: US5 complete — proposals visible to proposer only; appear in admin queue.

---

## Phase 8: User Story 6 — Admin Approves or Rejects a Catalogue Proposal (Priority: P6)

**Goal**: Admins see a list of all PENDING invertebrate catalogue proposals. They can approve (→ PUBLIC, visible to all) or reject (→ REJECTED, invisible to all) any proposal.

**Independent Test**: Propose a catalogue entry as a regular user; log in as admin; `GET /api/admin/invertebrate-catalogue/pending` returns the proposal; `PUT /api/admin/invertebrate-catalogue/{id}/approve` → 202; regular user search now returns the entry.

### sabi-server: Admin service methods + controller

- [ ] T075 [US6] Update `sabi-server/src/main/java/de/bluewhale/sabi/services/InvertebrateCatalogueServiceImpl.java` — implement `listPending()`: return all entries with `status = "PENDING"` (no user-filter; admin sees all); implement `approve(Long id, String adminEmail)`: load by id, set `status = "PUBLIC"`, save; implement `reject(Long id, String adminEmail)`: load by id, set `status = "REJECTED"`, save
- [ ] T076 [US6] Create `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/InvertebrateCatalogueAdminController.java` — `@RestController @RequestMapping("/api/admin/invertebrate-catalogue")`; mirror `CoralCatalogueAdminController`; endpoints: `GET /pending` → 200 list (ADMIN role required), `PUT /{catalogueId}/approve` → 202, `PUT /{catalogueId}/reject` → 202; enforce admin-only access (follow existing role-check pattern from `CoralCatalogueAdminController`); SpringDoc `@Tag(name="Invertebrate Catalogue Admin")` annotations

### sabi-webclient: Admin view

- [ ] T077 [P] [US6] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/InvertebrateCatalogueAdminService.java` + `InvertebrateCatalogueAdminServiceImpl.java` — interface + implementation; mirror `CoralCatalogueAdminService/Impl`; `listPending()` calls `GET /api/admin/invertebrate-catalogue/pending`; `approve(Long)` calls `PUT .../approve`; `reject(Long)` calls `PUT .../reject`
- [ ] T078 [P] [US6] Create `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/InvertebrateCatalogueAdminView.java` — `@Named @ViewScoped`; mirror `CoralCatalogueAdminView`; `@PostConstruct init()` loads pending proposals; `approve(Long id)` and `reject(Long id)` actions call admin service then reload list
- [ ] T079 [P] [US6] Create `sabi-webclient/src/main/resources/META-INF/resources/secured/admin/invertebrateCatalogueAdminView.xhtml` — mirror `coralCatalogueAdminView.xhtml`; `<p:dataTable>` showing proposer, submission date, scientific name, category, care level; "Approve" and "Reject" action buttons per row; page title from `invertebratecatalogue.admin.title` key
- [ ] T080 [US6] Edit `sabi-webclient/src/main/resources/META-INF/resources/secured/admin/catalogueDashboard.xhtml` — add navigation link/button to `invertebrateCatalogueAdminView.xhtml` alongside existing fish and coral catalogue admin links

**Checkpoint**: US6 complete — full governance loop: propose → admin approves → entry visible to all users.

---

## Phase 9: User Story 7 — Invertebrate Data in House Reef Report (Priority: P7)

**Goal**: Public House Reef Report optionally includes currently-present invertebrates (species name, category, functional classifications, water sensitivity unit names), controlled by a per-report-link opt-in flag.

**Independent Test**: Enable invertebrate opt-in for a report link; `GET /api/public/report/{token}` response includes `invertebrateInhabitants` array with correct data; disable opt-in; same GET returns `invertebrateInhabitants` as null.

### sabi-server: Extend public report entities, service, controller

- [ ] T081 [US7] Edit `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/PublicReportLinkEntity.java` — add `includeInvertebrates boolean` field; `@Column(name="include_invertebrates")`; default false
- [ ] T082 [P] [US7] Edit `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReportLinkTo.java` — add `boolean includeInvertebrates = false`; backward-compatible new field (default false)
- [ ] T083 [P] [US7] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReefReportInvertebrateTo.java` — `@Data`; fields: `speciesName String`, `taxonomicCategory String`, `mobility String` (null if not set; localised label), `ecologicalRole String` (localised), `activityPattern String` (localised), `waterSensitivityUnitNames List<String>` (localised unit names); implements Serializable
- [ ] T084 [P] [US7] Edit `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReefReportTo.java` — add `List<PublicReefReportInvertebrateTo> invertebrateInhabitants` field; null = opted-out; empty list = opted-in but no active invertebrates
- [ ] T085 [US7] Edit `sabi-server/src/main/java/de/bluewhale/sabi/services/PublicReportServiceImpl.java` — in `getReport()` method: (1) check `reportLink.isIncludeInvertebrates()`; (2) if true: load active invertebrates via `tankInvertebrateStockRepository.findByAquariumIdAndUserIdAndDeletedAtIsNull()`; map each to `PublicReefReportInvertebrateTo` (resolve localised unit names from `localizedUnitRepository`); set `publicReefReportTo.setInvertebrateInhabitants(list)`; (3) if false: leave field null; mirror the existing coral opt-in logic exactly
- [ ] T086 [US7] Edit `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/PublicReportController.java` — add `PUT /api/report/link/{aquariumId}/include-invertebrates` endpoint; mirror existing `include-corals` endpoint; accepts `boolean` request body; updates `publicReportLink.includeInvertebrates`; returns 202; JWT auth required

### sabi-webclient: Opt-in toggle + report view

- [ ] T087 [US7] Edit `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/PublicReportServiceImpl.java` — add REST call for `PUT /api/report/link/{aquariumId}/include-invertebrates`; wire to opt-in toggle in UI
- [ ] T088 [US7] Edit `sabi-webclient/src/main/resources/META-INF/resources/secured/reportView.xhtml` — add invertebrate opt-in toggle control (`<p:toggleSwitch>` or `<p:selectBooleanCheckbox>`) alongside existing fish/coral opt-in toggles; bind to `PublicReportView.includeInvertebrates`; update on change
- [ ] T089 [US7] Edit `sabi-webclient/src/main/resources/META-INF/resources/houseReefReport.xhtml` — add `<p:fieldset>` or `<h:panelGroup>` for invertebrate section (rendered when `report.invertebrateInhabitants != null`); `<p:dataTable>` showing species name, category, functional classification labels; display water sensitivity unit names as comma-separated list or badge chips; label from `invertebratestock.tab.label`

### i18n: House reef report labels

- [ ] T090 [P] [US7] Add i18n keys to all 6 message bundle files — reef report section: `housereefReport.invertebrates.section.title`, `housereefReport.invertebrates.empty`, `housereefReport.invertebrates.optin.label`; use existing `invertebratestock.category.*` and classification keys already added in Phases 3–4

**Checkpoint**: US7 complete — public report correctly includes/excludes invertebrate data based on opt-in flag.

---

## Phase 10: User Story 8 — Invertebrate Data in AI-JSON Export (Priority: P8)

**Goal**: The AI-JSON export for a user includes an `invertebrates` array per aquarium. Each element carries scientific name, category, dates, functional classifications, water sensitivity units, and departure data (if any).

**Independent Test**: Trigger AI export for a user with invertebrates; response JSON includes `invertebrates` in each aquarium object; entries with all classification fields set export those fields correctly; departed invertebrates included with `departedOn` set.

### sabi-boundary: Export TOs

- [ ] T091 [P] [US8] Create `sabi-boundary/src/main/java/de/bluewhale/sabi/model/InvertebrateExportTo.java` — `@Data`; fields: `catalogueId Long`, `scientificName String`, `speciesName String`, `taxonomicCategory String` (enum name string), `addedOn String` (ISO date), `departedOn String` (ISO date; null if active), `departureReason String` (null if active), `departureNote String`, `notes String`, `mobility String` (null if not set), `ecologicalRole String` (null if not set), `activityPattern String` (null if not set), `waterSensitivityUnits List<WaterSensitivityUnitRef>` (each: unitId, unitSign, nameEn — reuse or create simple record); implements Serializable
- [ ] T092 [P] [US8] Edit `sabi-boundary/src/main/java/de/bluewhale/sabi/model/AquariumExportTo.java` — add `List<InvertebrateExportTo> invertebrates = new ArrayList<>()` field; backward-compatible (defaults to empty list)

### sabi-server: Export mapper extension + service extension

- [ ] T093 [US8] Update `sabi-server/src/main/java/de/bluewhale/sabi/mapper/InvertebrateStockMapper.java` — add `toExportTo(TankInvertebrateStockEntity entity)` method returning `InvertebrateExportTo`; map all export fields; format `addedOn`/`departedOn` as ISO date strings; map `waterSensitivities` to `List<WaterSensitivityUnitRef>` (load unit sign and English name via unit lookup or inject `LocalizedUnitRepository` into mapper using `@Autowired` + `uses` directive on `@Mapper`)
- [ ] T094 [US8] Edit `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java` — in the aquarium-export loop: inject `TankInvertebrateStockRepository`; for each aquarium, call `findByAquariumIdAndUserIdAndDeletedAtIsNull()` (include both active and departed — spec US8 scenario 3: departed are always exported); map each entity via `invertebrateStockMapper.toExportTo()`; set `aquariumExportTo.setInvertebrates(exportList)`; mirror the existing coral export loop exactly

**Checkpoint**: US8 complete — AI export enriched with full invertebrate data per aquarium.

---

## Final Phase: Polish, Integration Tests & Cross-Cutting Concerns

**Purpose**: Integration tests per user story (explicitly required by the constitution check: "at least one integration test per P1–P6 user story"), final i18n completeness verification, and cross-cutting security/regression validation.

### Integration Tests (one per user story — P1 through P6 minimum)

- [ ] T095 [P] Create integration test in `sabi-server/src/test/java/de/bluewhale/sabi/services/InvertebrateStockServiceTest.java` — **US1**: `POST /api/invertebrate/` → assert 201 and persisted entry; `GET /api/invertebrate/{aqId}/list` → assert entry in response with all mandatory fields; test authentication rejection (401) for unauthenticated request; mirror `CoralStockServiceTest` setup pattern
- [ ] T096 [P] Add test method to `InvertebrateStockServiceTest` — **US2**: `PUT /api/invertebrate/{id}` with Mobility=MOBILE, EcologicalRole=CLEANUP_CREW, ActivityPattern=NOCTURNAL, and two waterSensitivityUnitIds set → assert 202; `GET /api/invertebrate/{id}` returns all four classification fields correctly; assert water sensitivity join rows in DB
- [ ] T097 [P] Add test method to `InvertebrateStockServiceTest` — **US3**: record departure (`PUT /api/invertebrate/{id}/departure`) with valid date and reason → assert 202; assert entry absent from active list; assert 422 when departure date is before entry date; assert 500-char note truncation rejects with 400
- [ ] T098 [P] Create integration test in `sabi-server/src/test/java/de/bluewhale/sabi/services/InvertebrateCatalogueServiceTest.java` — **US4**: with a seeded PUBLIC catalogue entry, `GET /api/invertebrate-catalogue/search?q=<partial_name>` returns entry; save stock entry with `invertebrateCatalogueId` set; verify link stored; `DELETE /api/invertebrate/{id}/catalogue-link` → 202 and `invertebrateCatalogueId` null
- [ ] T099 [P] Add test method to `InvertebrateCatalogueServiceTest` — **US5**: `POST /api/invertebrate-catalogue/` → 201 (PENDING); searching as proposer returns entry; searching as different user does NOT return entry; verify entry appears in admin pending list; **US6** (continuation): admin `PUT /api/admin/invertebrate-catalogue/{id}/approve` → 202; non-proposer search now returns entry as PUBLIC; admin reject → entry invisible to all
- [ ] T100 [P] Add test method to `sabi-server/src/test/java/de/bluewhale/sabi/services/PublicReportServiceTest.java` — **US7**: with opt-in enabled, `GET /api/public/report/{token}` includes `invertebrateInhabitants`; with opt-in disabled, field is null; departed invertebrate does NOT appear

### Final i18n completeness sweep

- [ ] T101 Verify all 6 message bundle files (`sabi-webclient/src/main/resources/i18n/messages*.properties`) contain all ~40 new keys listed in `specs/006-invertebrate-tracking/quickstart.md §10`; add any missing keys; spot-check German (`messages_de.properties`) and English (`messages_en.properties`) translations for accuracy; ensure `messages.properties` (fallback) contains all keys with English defaults

### Cross-cutting security verification

- [ ] T102 [P] Audit all mutating `InvertebrateStockServiceImpl` methods (create, update, delete, recordDeparture, uploadPhoto, deletePhoto, removeCatalogueLink) — confirm each follows the ownership security pattern: (1) `userRepository.getByEmail(userEmail)`, (2) `findByIdAndUserId(id, userId)`, (3) reject with 403/INVERT_NOT_OWNER if null; no method should accept a request for another user's invertebrate entry
- [ ] T103 [P] Audit `InvertebrateCatalogueAdminController` — confirm `@PreAuthorize("hasRole('ADMIN')")` (or equivalent role-check annotation used by existing admin controllers) is present on all three endpoints; verify that a non-admin token returns 403

### Full build and regression check

- [ ] T104 Run `./mvnw -pl sabi-boundary,sabi-server,sabi-webclient,sabi-database clean install -DskipTests=false` from the repo root and confirm all existing tests remain green; address any compilation errors from extended TOs or entity changes before merging

---

## Dependency Graph (User Story Completion Order)

```
Phase 2 (Foundational)
   │
   ├─► Phase 3 (US1 — Add Invertebrate)     ← MVP: ship this alone if needed
   │       │
   │       ├─► Phase 4 (US2 — Classifications)   ← depends on US1 entity + mapper
   │       │       │
   │       │       └─► Phase 5 (US3 — Departure)   ← depends on US1 stock entry existing
   │       │
   │       └─► Phase 6 (US4 — Catalogue Link)   ← depends on stock entry (US1)
   │               │
   │               └─► Phase 7 (US5 — Propose)   ← depends on catalogue search (US4)
   │                       │
   │                       └─► Phase 8 (US6 — Admin)   ← depends on propose (US5)
   │
   ├─► Phase 9 (US7 — Reef Report)     ← depends on US1 + US2 + US3
   │
   └─► Phase 10 (US8 — AI Export)     ← depends on US1 + US2
```

**US1 is the MVP increment**: Phases 1–3 can be developed, deployed, and released independently. All other user stories require US1 to be complete.

**US4–US6 can be treated as a single catalogue sub-epic**: If the catalogue is not yet ready, US1–US3 still provide full value as a free-text invertebrate stock.

---

## Parallel Execution Opportunities

| Phase | Parallelizable task groups |
|-------|---------------------------|
| Phase 2 | T002–T007 (6 migrations); T008–T011 (4 enums); T012–T013 (2 TOs); T014–T016 (3 entities after migrations); T017–T019 (3 repositories); T020–T023 (4 exception/message code files) |
| Phase 3 | T028 + T030–T033 (webclient beans and REST proxy, independent files); T034–T035 (two XHTML files) |
| Phase 6 | T050–T053 (entities + repositories); T054–T056 (3 boundary TOs); T062 + T064 (interface + converter) |
| Phase 7 | T071–T073 (backing bean + 2 XHTML files) |
| Phase 8 | T077–T079 (admin service pair + backing bean + XHTML) |
| Phase 9 | T082–T084 (3 boundary TO edits); T087–T089 (webclient service + 2 XHTML edits) |
| Phase 10 | T091–T092 (2 boundary TO edits) |
| Final | T095–T100 (integration tests, independent methods) |

---

## Implementation Strategy

1. **Start with Phase 1 + Phase 2** entirely — migrations and boundary objects must compile cleanly before any module-crossing work.
2. **Phase 3 (US1) is the MVP** — complete and verify end-to-end before moving forward.
3. **Phases 4 and 5 in sequence** — both are low-risk extensions of the stock entity and service already built in Phase 3.
4. **Phases 6–8 as a catalogue sub-epic** — can be deferred to a second sprint; US1–US3 are independently shippable.
5. **Phases 9 and 10 independently** — Reef Report and AI Export touch different server-side services and can be developed in parallel by different developers after US1–US3 are stable.
6. **Final Phase last** — integration tests, i18n sweep, and build validation before PR review.
