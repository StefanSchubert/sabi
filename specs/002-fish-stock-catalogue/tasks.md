# Tasks: Fish Stock Management & Fish Catalogue

**Feature Branch**: `002-fish-stock-catalogue`  
**Input**: `specs/002-fish-stock-catalogue/plan.md` + `spec.md`  
**Erstellt**: 2026-04-05  
**Schätzung**: 80 Implementierungs-Tasks + 8 Quality-Gates  

**Tests**: Integration- und Unit-Tests sind verpflichtend (NFR Wartbarkeit) — je mindestens ein Integrationstest pro User Story P1–P5.

**Organisation**: Tasks sind nach User Stories gruppiert (unabhängige Implementierung und Testbarkeit je Story). Die Phasen 1 + 2 sind Voraussetzung für alle User Stories.

## Format: `[ID] [P?] [Story?] Beschreibung mit Dateipfad`

- **[P]**: Kann parallel laufen (verschiedene Dateien, keine offenen Abhängigkeiten)
- **[US1–US6]**: Welcher User Story der Task zugeordnet ist
- Exakte Dateipfade sind in jeder Task-Beschreibung enthalten

---

## Phase 1: Setup (Verzeichnisstruktur)

**Zweck**: Neue Verzeichnisse anlegen und Branch verifizieren — keine Logik, kein Code

- [x] T001 Flyway-Migrationsverzeichnis `sabi-database/src/main/resources/db/migration/version1_5_0/` anlegen und leere `.gitkeep`-Datei committen, um das Verzeichnis zu tracken

---

## Phase 2: Foundational (DB-Schema · Entities · Repositories · DTOs · Mapper · i18n)

**Zweck**: Alle Bausteine, die von jeder User Story benötigt werden. Kein Feature-Code ist ohne diese Phase implementierbar.

**⚠️ KRITISCH**: Kein User-Story-Code kann starten, bevor Phase 2 abgeschlossen ist.

### 2a — Flyway-Migrationen (abhängig von T001)

- [x] T002 [P] Flyway-Migration `V1_5_0_1__extendFishTableForStockManagement.sql` erstellen in `sabi-database/src/main/resources/db/migration/version1_5_0/` — ALTER TABLE fish: ADD COLUMNS common_name NOT NULL, scientific_name NULL, external_ref_url NULL, departure_reason VARCHAR(30) NULL, deleted_at TIMESTAMP NULL, optlock; MODIFY fish_catalogue_id NULL; Datenmigration common_name aus nickname; Index idx_fish_deleted_at. Acceptance: Script ist idempotent und schlägt bei Re-Run nicht fehl.
- [x] T003 [P] Flyway-Migration `V1_5_0_2__extendFishCatalogueForUGC.sql` erstellen in `sabi-database/src/main/resources/db/migration/version1_5_0/` — ADD COLUMNS status VARCHAR(10) NOT NULL DEFAULT 'PUBLIC', proposer_user_id BIGINT NULL, proposal_date DATE NULL, optlock; Virtual Column active_scientific_name; UNIQUE INDEX uq_fish_catalogue_active_name; INDEX idx_fish_catalogue_status; FK fk_fish_catalogue_proposer → users.id ON DELETE SET NULL. Acceptance: Unique-Constraint lässt zwei REJECTED-Einträge mit gleichem scientificName zu.
- [x] T004 [P] Flyway-Migration `V1_5_0_3__addFishCatalogueI18nTable.sql` erstellen in `sabi-database/src/main/resources/db/migration/version1_5_0/` — CREATE TABLE fish_catalogue_i18n (id, catalogue_id FK, language_code VARCHAR(2), common_name NULL, description TEXT NULL, reference_url NULL, created_on, lastmod_on, optlock); UNIQUE KEY uq_catalogue_i18n_lang(catalogue_id, language_code); FULLTEXT INDEX ft_i18n_common_name(common_name). Acceptance: FULLTEXT-Index existiert und MATCH AGAINST-Query funktioniert.
- [x] T005 [P] Flyway-Migration `V1_5_0_4__addFishPhotoTable.sql` erstellen in `sabi-database/src/main/resources/db/migration/version1_5_0/` — CREATE TABLE fish_photo (id, fish_id FK fish UNIQUE, file_path VARCHAR(512), content_type VARCHAR(50), file_size BIGINT, upload_date DATE, created_on, lastmod_on, optlock); CASCADE DELETE bei fish-Löschung. Acceptance: UNIQUE KEY auf fish_id — pro Fisch nur ein Foto.
- [x] T006 Flyway-Migration `V1_5_0_5__migrateExistingFishCatalogueToI18n.sql` erstellen in `sabi-database/src/main/resources/db/migration/version1_5_0/` (nach T003+T004) — INSERT INTO fish_catalogue_i18n: bestehende description → EN description, bestehende meerwasserwiki_url → EN reference_url. Acceptance: Kein Datenverlust; Einträge ohne description/URL erzeugen keinen NULL-Row.

### 2b — JPA Entities (abhängig von T002–T006)

- [x] T007 `FishEntity.java` umbenennen → `TankFishStockEntity.java` in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/TankFishStockEntity.java` — neue Felder: commonName (NOT NULL), scientificName (nullable), externalRefUrl (nullable), departureReason (nullable String), observedBehavior (TEXT nullable), deletedAt (LocalDateTime nullable), optlock; Typ von addedOn auf LocalDate vereinheitlichen; @SQLRestriction("deleted_at IS NULL"); @NamedQuery "TankFishStock.getUsersFish" mit Aquarium-Ownership-Prüfung; @ManyToOne UserEntity LAZY; fish_catalogue_id bleibt als nullable Long (kein Join-Fetch). Acceptance: MVN compile grün; @SQLRestriction filtert soft-gelöschte Einträge.
- [x] T008 `FishCatalogueEntity.java` umbenennen → `FishCatalogueEntryEntity.java` in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/FishCatalogueEntryEntity.java` — neue Felder: status (VARCHAR 10), proposerUserId (nullable Long), proposalDate (LocalDate nullable); @OneToMany(cascade=ALL, orphanRemoval=true) List\<FishCatalogueI18nEntity\> i18nEntries LAZY; @Deprecated Felder description + meerwasserwikiUrl behalten (Removal erst in v1.6); @ManyToOne UserEntity proposer (insertable=false, updatable=false) für Admin-Queries; optlock. Acceptance: Lazy-Load der i18n-Einträge funktioniert.
- [x] T009 [P] `FishCatalogueI18nEntity.java` neu erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/FishCatalogueI18nEntity.java` — @Entity @Table(fish_catalogue_i18n) mit @UniqueConstraint(catalogue_id, language_code); Felder: id, catalogueId (Long, FK-Spalte), languageCode (de/en/es/fr/it), commonName (nullable), description (@Column(columnDefinition="TEXT") nullable), referenceUrl (nullable); @ManyToOne FishCatalogueEntryEntity LAZY; optlock. Acceptance: UniqueConstraint verhindert doppelten Sprachcode pro Katalog-Eintrag.
- [x] T010 [P] `FishPhotoEntity.java` neu erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/FishPhotoEntity.java` — @Entity @Table(fish_photo); Felder: id, fishId (Long, FK-Spalte), filePath (VARCHAR 512), contentType (VARCHAR 50), fileSize (Long Bytes), uploadDate (LocalDate); @OneToOne TankFishStockEntity LAZY (insertable=false, updatable=false); optlock. Acceptance: Kein BLOB im Entity — nur Pfad-Metadaten.

### 2c — JPA Repositories (abhängig von T007–T010)

- [x] T011 `FishRepository.java` und `FishRepositoryCustom.java`/`FishRepositoryCustomImpl.java` → `TankFishStockRepository.java` refaktorieren in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/TankFishStockRepository.java` — extends JpaRepository\<TankFishStockEntity, Long\>; Custom-Queries: findAllByAquariumIdAndUserIdOrderByAddedOnDesc, findByIdAndUserId (Ownership-Check), existsByIdAndExodusOnIsNotNull (departure-Guard für FR-024). Acceptance: @SQLRestriction filtert automatisch soft-gelöschte Einträge in allen Queries.
- [x] T012 `FishCatalogueRepository.java` erweitern in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/FishCatalogueRepository.java` — neue JPQL-Queries: searchByQueryAndLang (LIKE '%q%' auf scientific_name + i18n.common_name, Sichtbarkeit: PUBLIC ODER (PENDING UND proposerUserId = :userId)); findAllByStatusOrderByProposalDateAsc (Admin Pending-Liste); existsByScientificNameAndStatusIn (Duplikat-Check für PENDING/PUBLIC). Acceptance: searchByQueryAndLang gibt nur PUBLIC + eigene PENDING zurück.
- [x] T013 [P] `FishCatalogueI18nRepository.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/FishCatalogueI18nRepository.java` — extends JpaRepository\<FishCatalogueI18nEntity, Long\>; findByCatalogueIdAndLanguageCode. Acceptance: CRUD-Operationen auf i18n-Einträgen kompilieren.
- [x] T014 [P] `FishPhotoRepository.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/FishPhotoRepository.java` — extends JpaRepository\<FishPhotoEntity, Long\>; findByFishId(Long fishId): Optional\<FishPhotoEntity\>; deleteByFishId. Acceptance: Optional ist leer wenn kein Foto vorhanden.

### 2d — DTOs und Enums in sabi-boundary (parallel zu 2c)

- [x] T015 [P] `DepartureReason.java` enum erstellen in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/DepartureReason.java` — Werte: DECEASED, REMOVED_REHOMED, UNKNOWN. Acceptance: Enum ist serialisierbar (Jackson).
- [x] T016 [P] `FishCatalogueStatus.java` enum erstellen in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishCatalogueStatus.java` — Werte: PENDING, PUBLIC, REJECTED. Acceptance: Enum ist serialisierbar (Jackson).
- [x] T017 `FishStockEntryTo.java` erstellen in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishStockEntryTo.java` — Felder: Long id, Long aquariumId, @NotBlank String commonName, String scientificName (nullable), String nickname (nullable), @URL String externalRefUrl (nullable), @NotNull @PastOrPresent LocalDate addedOn, LocalDate exodusOn (nullable), DepartureReason departureReason (nullable), String observedBehavior (nullable), Long fishCatalogueId (nullable), boolean hasPhoto. Acceptance: Bean-Validation-Constraints kompilieren.
- [x] T018 [P] `FishDepartureRecordTo.java` erstellen in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishDepartureRecordTo.java` — Felder: @NotNull LocalDate departureDate, @NotNull DepartureReason departureReason. Acceptance: Fehlermeldung wenn departureDate null.
- [x] T019 `FishCatalogueEntryTo.java` erstellen in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishCatalogueEntryTo.java` — Felder: Long id, @NotBlank String scientificName, FishCatalogueStatus status, Long proposerUserId (nullable), LocalDate proposalDate, List\<FishCatalogueI18nTo\> i18nEntries. Acceptance: i18nEntries kann leer (aber nicht null) sein.
- [x] T020 [P] `FishCatalogueI18nTo.java` erstellen in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishCatalogueI18nTo.java` — Felder: Long id (nullable für neue Einträge), String languageCode (de/en/es/fr/it), String commonName (nullable), @Size(max=2000) String description (nullable), String referenceUrl (nullable). Acceptance: @Size(max=2000) wird durch Bean-Validation geprüft.
- [x] T021 [P] `FishCatalogueSearchResultTo.java` erstellen in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishCatalogueSearchResultTo.java` — Felder: Long id, String scientificName, String commonName (sprachspezifisch aufgelöst), String referenceUrl (sprachspezifisch), FishCatalogueStatus status. Acceptance: Leichtgewichtiges DTO ohne i18n-Liste.
- [x] T022 `Endpoint.java` in `sabi-boundary/src/main/java/de/bluewhale/sabi/api/Endpoint.java` um drei neue Konstanten erweitern: FISH_STOCK("/api/fish"), FISH_CATALOGUE("/api/fish/catalogue"), FISH_CATALOGUE_ADMIN("/api/admin/fish/catalogue"). Acceptance: Kompilierung im sabi-boundary + sabi-server + sabi-webclient erfolgreich.
- [x] T023 [P] `FishTo.java` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishTo.java` und `FishCatalogueTo.java` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishCatalogueTo.java` mit `@Deprecated` markieren. Acceptance: Bestehende Tests kompilieren noch.

### 2e — Exception- und Message-Codes (parallel zu 2d)

- [x] T024 [P] `FishStockExceptionCodes.java` und `FishStockMessageCodes.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/services/` — ExceptionCodes: FISH_NOT_FOUND, FISH_NOT_YOURS, FISH_HAS_DEPARTURE_RECORD, FISH_PHOTO_TOO_LARGE, FISH_PHOTO_INVALID_FORMAT, AQUARIUM_NOT_YOURS; MessageCodes: FISH_CREATED, FISH_UPDATED, FISH_DEPARTURE_RECORDED, FISH_DELETED, FISH_PHOTO_UPLOADED, CATALOGUE_LINK_REMOVED. Acceptance: implements ExceptionCode/MessageCode Interface.
- [x] T025 [P] `FishCatalogueExceptionCodes.java` und `FishCatalogueMessageCodes.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/services/` — ExceptionCodes: CATALOGUE_ENTRY_NOT_FOUND, CATALOGUE_ENTRY_NOT_YOURS, CATALOGUE_REJECTED_READ_ONLY, CATALOGUE_ADMIN_REQUIRED; MessageCodes: CATALOGUE_ENTRY_PROPOSED, CATALOGUE_ENTRY_APPROVED, CATALOGUE_ENTRY_REJECTED, CATALOGUE_DUPLICATE_WARNING, CATALOGUE_ENTRY_UPDATED. Acceptance: implements ExceptionCode/MessageCode Interface.

### 2f — MapStruct Mapper (abhängig von T007–T021)

- [x] T026 `FishStockMapper.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/mapper/FishStockMapper.java` — @Mapper(componentModel="spring", uses={MappingUtils.class}); mapTo2Entity(FishStockEntryTo → TankFishStockEntity): ignore user + deletedAt; mapEntity2To(TankFishStockEntity → FishStockEntryTo): hasPhoto via java-Expression (photo != null); LocalDate↔LocalDate Mapping. Acceptance: MapStruct-Generierung in sabi-database/target/generated-sources kompiliert ohne Warnung.
- [x] T027 `FishCatalogueMapper.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/mapper/FishCatalogueMapper.java` — @Mapper(componentModel="spring"); mapTo2Entity + mapEntity2To für FishCatalogueEntryEntity↔FishCatalogueEntryTo; mapI18nTo2Entity + mapI18nEntity2To; mapEntity2SearchResult(entity, @Context String lang) mit resolveI18nName / resolveI18nUrl (Sprach-Fallback: angeforderte Sprache → en → erster verfügbarer Eintrag). Acceptance: Fallback-Logik wird per Unit-Test verifiziert.

### 2g — i18n Bundle-Befüllung (kann parallel zu 2c–2f laufen)

- [x] T028 Alle 52 i18n-Keys in alle 6 Bundle-Dateien eintragen: `sabi-webclient/src/main/resources/i18n/messages.properties` (EN-Fallback), `messages_de.properties`, `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`, `messages_it.properties` — Schlüssel-Gruppen: fishstock.tab.*, fishstock.add.*, fishstock.current.*, fishstock.departed.*, fishstock.empty.*, fishstock.form.*, fishstock.departure.*, fishstock.delete.*, fishcatalogue.scientificname.*, fishcatalogue.status.*, fishcatalogue.propose.*, fishcatalogue.tab.*, fishcatalogue.i18n.*, fishcatalogue.admin.*. Alle Sonderzeichen als \uXXXX-Escapes. WCAG-Hinweis: Statuslabel-Farben in zugehörigen XHTML-Templates: PENDING #92400e auf #fef3c7, PUBLIC #065f46 auf #d1fae5, REJECTED #991b1b auf #fee2e2. Acceptance: Python-Bytecheck-Skript zeigt keine Latin-1-Bytes, kein \uFFFD, alle 52 Keys in allen 6 Dateien vorhanden.

**Checkpoint Phase 2**: `mvn compile` im Root grün; MapStruct generiert FishStockMapperImpl + FishCatalogueMapperImpl; 5 Flyway-Scripts vorhanden; Repository-Tests kompilieren.

---

## Phase 3: User Story 1 — Fisch zum Becken hinzufügen (Priority: P1) 🎯 MVP

**Ziel**: Ein authentifizierter Benutzer kann einen Fischeintrag mit allen Pflicht- und optionalen Feldern (inkl. Foto) für sein Becken anlegen, anzeigen, bearbeiten und löschen.

**Independent Test**: Login als User mit mindestens einem Aquarium → Fish Stock Tab öffnen → Fisch mit allen Feldern (common name, entry date, behaviour notes, reference URL, Foto) anlegen → Fisch erscheint in "Aktuell im Becken"-Liste mit allen gespeicherten Daten. REST-API: POST /api/fish returns 201, GET /api/fish/{aquariumId}/list returns 202 mit Eintrag.

### Service-Schicht (T029–T031)

- [x] T029 [P] `PhotoStorageService.java` Interface + `PhotoStorageServiceImpl.java` in `sabi-server/src/main/java/de/bluewhale/sabi/services/PhotoStorageService.java` und `PhotoStorageServiceImpl.java` — @Service @ConfigurationProperties("sabi.fish.photo"); Methoden: store(userId, fishId, bytes, contentType) → relativer Pfad, load(relativePath) → byte[], delete(relativePath), resolveExtension(contentType); Dateiname-Schema: {storage-dir}/{userId}/{fishId}.{ext}; Magic-Byte-Validierung: JPEG (FF D8 FF), PNG (89 50 4E 47), WebP (52 49 46 46 + 57 45 42 50), GIF (47 49 46 38); max-size-bytes aus Properties. Acceptance: Ungültige Magic-Bytes werfen FishPhotoInvalidFormatException; Upload > 5 MB wirft FishPhotoTooLargeException.
- [x] T030 `FishStockService.java` Interface + `FishStockServiceImpl.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/services/FishStockService.java` und `FishStockServiceImpl.java` — @Service @Transactional(readOnly=true); Methoden: addFishToTank (Aquarium-Ownership prüfen; wenn fishCatalogueId != null → catalogueEntry laden, scientificName + localisierten referenceUrl als Snapshot kopieren FR-009); updateFishEntry; recordDeparture (departureDate >= addedOn prüfen FR-006); deletePhysically (exodusOn != null → FISH_HAS_DEPARTURE_RECORD Error FR-024); getFishForTank; removeCatalogueLink; uploadPhoto (nutzt PhotoStorageService + FishPhotoRepository.upsert); getPhotoBytes (Ownership-Check FR-025). Acceptance: alle @Transactional-Methoden rollen bei Exception zurück.
- [x] T031 [P] Legacy `FishService.java`/`FishServiceImpl.java` in `sabi-server/src/main/java/de/bluewhale/sabi/services/` mit @Deprecated annotieren und alle 3 Methoden (registerNewFish, removeFish, getUsersFish) an FishStockService delegieren. Acceptance: Bestehende Tests mit FishService kompilieren und laufen weiter.

### REST-Controller und Konfiguration (T032–T033)

- [x] T032 [US1] `FishStockController.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/FishStockController.java` — @RestController @RequestMapping("/api/fish") @PreAuthorize("isAuthenticated()"); Endpoints: GET /{aquariumId}/list → 202 List\<FishStockEntryTo\>; POST / → 201 ResultTo; GET /{fishId} → 202 FishStockEntryTo; PUT /{fishId} → 202 ResultTo; DELETE /{fishId} → 204 / 409 Conflict wenn HAS_DEPARTURE_RECORD; PUT /{fishId}/departure → 202; DELETE /{fishId}/catalogue-link → 202; POST /{fishId}/photo (multipart/form-data) → 204; GET /{fishId}/photo → ResponseEntity\<byte[]\> mit Content-Type/Cache-Control/Content-Disposition; DELETE /{fishId}/photo → 204. Principal aus JWT extrahieren analog zu bestehenden Controllern. Acceptance: Unauthentifizierter Request auf alle Endpoints → 401; Cross-User-Zugriff → 403.
- [x] T033 [US1] `sabi-server/src/main/resources/application.yml` und `application-local.yml` um `sabi.fish.photo.storage-dir` (Prod: /var/sabi-data/fish-photos) + `sabi.fish.photo.max-size-bytes: 5242880` erweitern. Acceptance: PhotoStorageServiceImpl liest Properties per @ConfigurationProperties.

### Frontend (T034–T039)

- [x] T034 [US1] `FishStockService.java` Interface + `FishStockServiceImpl.java` als API-Gateway-Bean erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/FishStockService.java` und `FishStockServiceImpl.java` — Methoden: getFishForTank(aquariumId, token, locale), addFish(entry, token), updateFish(entry, token), recordDeparture(fishId, record, token), deleteFish(fishId, token), removeCatalogueLink(fishId, token), uploadPhoto(fishId, bytes, contentType, token), getPhoto(fishId, token) → byte[]; nutzt RestHelper analog zu TankServiceImpl. Acceptance: HTTP-Fehler (401, 403, 409) werden als lokale Exception weitergereicht.
- [x] T035 [US1] `FishStockView.java` CDI-Bean Controller erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/FishStockView.java` — @Named @ViewScoped @Slf4j; State: List\<FishStockEntryTo\> activeFish, departedFish; FishStockEntryTo selectedFish; boolean departedSectionExpanded = false; @PostConstruct init(): lädt Fischliste für userSession.selectedAquariumId, splitted nach exodusOn == null / != null; Aktionen: onAddFish(), onEditFish(fish), onDeleteFish(fish) — Bestätigungsdialog + FISH_HAS_DEPARTURE_RECORD-Fehler abfangen, onRemoveCatalogueLink(fish). Acceptance: activeFish enthält keine Einträge mit exodusOn != null.
- [x] T036 [US1] `fishStockTab.xhtml` erstellen in `sabi-webclient/src/main/resources/META-INF/resources/secured/fishStockTab.xhtml` — `<h:panelGroup>` mit zwei Sektionen: "Aktuell im Becken" (p:dataTable activeFish, Spalten: Foto-Thumbnail, common_name, scientific_name, addedOn, Aktionen: Bearbeiten/Abgang/Löschen) + "Abgänge" (p:fieldset toggleable collapsed="true", p:dataTable departedFish); leere Liste → fishstock.empty.hint; "Fisch hinzufügen"-Button → onAddFish(); alle Labels aus #{msgs.fishstock.*}; WCAG-konforme Farben. Acceptance: Abgänge-Sektion ist standardmäßig zugeklappt (FR-007).
- [x] T037 [US1] `sabi-webclient/src/main/resources/META-INF/resources/secured/tankView.xhtml` um neuen Tab "Fischbestand" erweitern — `<p:tab title="#{msgs.fishstock.tab.label}">` mit `<ui:include src="fishStockTab.xhtml"/>` neben bestehenden Tabs (Messungen, Seuchen). Acceptance: Tab wird auf der Becken-Detailseite angezeigt; bestehende Tabs werden nicht beeinflusst.
- [x] T038 [US1] `FishStockEntryView.java` CDI-Bean Controller erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/FishStockEntryView.java` — @Named @ViewScoped; State: FishStockEntryTo currentEntry; boolean isEdit; byte[] previewPhoto; Methoden: init(FishStockEntryTo existing), onSave() → addFish / updateFish, onPhotoUpload(FileUploadEvent) → Magic-Bytes-Größen-Validierung client-seitig + Backend-Upload, onDeletePhoto(); Validierung: addedOn darf nicht in Zukunft liegen (FR-003). Acceptance: Pflichtfeld-Fehler werden inline angezeigt ohne Speichern.
- [x] T039 [US1] `fishStockEntryForm.xhtml` als p:dialog-Partial erstellen in `sabi-webclient/src/main/resources/META-INF/resources/secured/fishStockEntryForm.xhtml` — Felder: h:inputText commonName (required, label: fishstock.form.commonname.label), p:datePicker addedOn (required, maxDate=heute, FR-003), h:inputText nickname (optional), h:inputText externalRefUrl (optional, URL-Validator), h:inputTextarea observedBehavior (optional), p:fileUpload Foto (maxFileSize=5242880, accept=image/jpeg image/png image/webp image/gif, mode=advanced, label: fishstock.form.photo.upload.button), h:inputText scientificName (auto-gefüllt, editierbar); Speichern/Abbrechen-Buttons; alle i18n-Labels aus #{msgs.*}; WCAG-konforme Farben für Fehlermeldungen. Acceptance: Formular-Submit ohne commonName oder addedOn → inline Fehlermeldung, kein Netzwerk-Request.

### Tests US1 (T040–T042)

- [x] T040 [P] [US1] `FishStockControllerTest.java` erstellen in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/FishStockControllerTest.java` — MockMvc + @SpringBootTest + TestContainers MySQL; Test-Cases: addFish_validEntry_returns201, addFish_missingCommonName_returns400 (FR-003), addFish_unauthenticated_returns401 (FR-023), getFishForTank_otherUsersAquarium_returns403 (FR-011), deleteFish_withDepartureRecord_returns409 (FR-024), uploadPhoto_exceeds5MB_returns400 (FR-008), getPhoto_otherUsersPhoto_returns403 (FR-025). Acceptance: Alle 7 Tests grün.
- [x] T041 [P] [US1] `FishStockServiceTest.java` erstellen in `sabi-server/src/test/java/de/bluewhale/sabi/services/FishStockServiceTest.java` — Mockito; Test-Cases: addFishToTank_withCatalogueLink_copiesScientificNameSnapshot (FR-009), addFishToTank_withoutCatalogueLink_succeeds, deletePhysically_withDepartureRecord_returnsDepartureBlockedError (FR-024), deletePhysically_withoutDepartureRecord_succeeds, uploadPhoto_invalidMagicBytes_returnsFormatError (FR-008). Acceptance: Alle 5 Tests grün.
- [x] T042 [P] [US1] `TankFishStockRepositoryTest.java` erstellen in `sabi-server/src/test/java/de/bluewhale/sabi/persistence/repositories/TankFishStockRepositoryTest.java` — JPA + TestContainers; Test-Cases: findAllByAquariumId_returnsOnlyActiveEntries (Soft-Delete-Filter via @SQLRestriction), findByIdAndUserId_returnsNullForOtherUsersEntry (FR-011). Acceptance: Soft-Delete-Filter funktioniert.

**Checkpoint US1**: User Story 1 vollständig testbar. Fisch hinzufügen, anzeigen, bearbeiten, Foto hochladen/anzeigen, Eintrag ohne Departure-Record löschen.

---

## Phase 4: User Story 2 — Fish-Abgang erfassen (Priority: P2)

**Ziel**: Ein Benutzer kann für einen aktiven Fischeintrag einen Abgang (Datum + Grund) erfassen. Der Eintrag verschwindet aus "Aktuell im Becken" und erscheint in "Abgänge".

**Independent Test**: Fisch anlegen → Abgang erfassen (Datum = heute, Grund = DECEASED) → Fisch ist nicht mehr in der aktiven Liste → Fisch ist in der "Abgänge"-Sektion mit korrektem Datum + Grund sichtbar.

### Service (T043)

- [x] T043 [US2] `FishStockServiceImpl.recordDeparture()` vervollständigen in `sabi-server/src/main/java/de/bluewhale/sabi/services/FishStockServiceImpl.java` — Validierung: departureDate darf nicht vor addedOn liegen (FR-006); Ownership-Check; entity.setExodusOn(departureDate), entity.setDepartureReason(reason.name()); ResultTo FISH_DEPARTURE_RECORDED zurückgeben. Acceptance: departureDate < addedOn → ResultTo mit DEPARTURE_DATE_BEFORE_ENTRY_DATE Fehler; kein Save.

### Controller (T044)

- [x] T044 [US2] `PUT /api/fish/{fishId}/departure` Endpoint in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/FishStockController.java` vollständig implementieren — @RequestBody @Valid FishDepartureRecordTo; Service-Aufruf; Bei Datums-Fehler → 422 Unprocessable Entity. Acceptance: departureDate im Request-Body fehlt → 400 Bad Request (Bean-Validation).

### Frontend (T045–T046)

- [x] T045 [US2] `FishDepartureView.java` CDI-Bean Controller erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/FishDepartureView.java` — @Named @ViewScoped; State: Long fishId, LocalDate fishAddedOn, FishDepartureRecordTo departureRecord; init(fish), onSave() → FishStockService.recordDeparture() + Seite aktualisieren; Client-seitige Validierung: departureDate >= fishAddedOn (FR-006). Acceptance: Validierungsfehler erscheint inline; kein Backend-Call bei ungültigem Datum.
- [x] T046 [US2] `fishDepartureDialog.xhtml` erstellen in `sabi-webclient/src/main/resources/META-INF/resources/secured/fishDepartureDialog.xhtml` — p:dialog mit: p:datePicker departureDate (required, minDate=fish.addedOn), p:selectOneMenu departureReason (DECEASED/REMOVED_REHOMED/UNKNOWN mit i18n-Labels fishstock.departure.reason.*), Inline-Fehlermeldung bei departureDate < addedOn (fishstock.departure.date.after.error), Speichern/Abbrechen. "Record Departure"-Button in `fishStockTab.xhtml` für jeden aktiven Fischeintrag ergänzen (FishStockView.onRecordDeparture(fish) → Dialog öffnen). Acceptance: DECEASED/REMOVED_REHOMED/UNKNOWN werden korrekt i18n-übersetzt.

### Tests US2 (T047)

- [x] T047 [P] [US2] `FishStockServiceTest.java` um recordDeparture-Tests erweitern: recordDeparture_validatesDateNotBeforeEntryDate (FR-006), recordDeparture_validDate_setsExodusOnAndReason. Acceptance: Beide Tests grün.

**Checkpoint US2**: US1 + US2 funktionieren unabhängig. Abgang erfassen und historische Ansicht werden korrekt angezeigt.

---

## Phase 5: User Story 3 — Katalog-Verlinkung (Priority: P3)

**Ziel**: Beim Anlegen eines Fischeintrags kann der Benutzer aus dem Katalog suchen und einen Eintrag verlinken. Die Auto-Fill-Funktion setzt scientificName + referenceUrl. Das Link kann später entfernt werden.

**Independent Test**: Mindestens ein PUBLIC-Katalogeintrag vorhanden → Add-Fish-Formular öffnen → "clownfish" eintippen → Dropdown erscheint → Eintrag auswählen → scientificName + referenceUrl auto-gefüllt → Speichern → Fischeintrag enthält catalogueId und zeigt wissenschaftlichen Namen.

### Service (T048)

- [x] T048 [US3] `FishCatalogueService.java` Interface + `FishCatalogueServiceImpl.java` in `sabi-server/src/main/java/de/bluewhale/sabi/services/FishCatalogueService.java` und `FishCatalogueServiceImpl.java` erstellen — @Service; `search(query, languageCode, user)`: query muss ≥ 2 Zeichen haben (sonst leere Liste), JPQL-Query über FishCatalogueRepository.searchByQueryAndLang, Ergebnis über FishCatalogueMapper.mapEntity2SearchResult mit lang-Parameter gemapped; `getEntry(id, user)`: PUBLIC oder eigene PENDING sichtbar; `isDuplicateScientificName(name)`: existsByScientificNameAndStatusIn(name, [PENDING, PUBLIC]). Acceptance: search gibt keine fremden PENDING-Einträge zurück (SC-009).

### Controller (T049)

- [x] T049 [US3] `FishCatalogueController.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/FishCatalogueController.java` — @RestController @RequestMapping("/api/fish/catalogue") @PreAuthorize("isAuthenticated()"); GET /search?q={term}&lang={lang} → 202 List\<FishCatalogueSearchResultTo\> (400 wenn q.length() < 2); GET /{id} → 202 FishCatalogueEntryTo. Acceptance: GET /search?q=A (1 Zeichen) → 400.

### Frontend (T050–T052)

- [x] T050 [US3] `FishCatalogueService.java` Interface + `FishCatalogueServiceImpl.java` API-Gateway erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/FishCatalogueService.java` und `FishCatalogueServiceImpl.java` — Methoden: search(query, lang, token) → List\<FishCatalogueSearchResultTo\>, propose(entry, token), updateEntry(entry, token). Acceptance: search-Methode setzt Accept-Language-Header basierend auf aktuellem Locale.
- [x] T051 [US3] `FishStockEntryView.java` um Katalog-Such-Logik erweitern in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/FishStockEntryView.java` — onSearchCatalogue(query) → FishCatalogueService.search(); onSelectCatalogue(entry) → currentEntry.setScientificName(entry.scientificName), currentEntry.setExternalRefUrl(entry.referenceUrl), currentEntry.setFishCatalogueId(entry.id); onRemoveCatalogueLink() → FishStockService.removeCatalogueLink(). Acceptance: Nach onRemoveCatalogueLink: fishCatalogueId = null, scientificName-Feld wird editierbar.
- [x] T052 [US3] `fishStockEntryForm.xhtml` um p:autoComplete-Katalogsuche ergänzen in `sabi-webclient/src/main/resources/META-INF/resources/secured/fishStockEntryForm.xhtml` — p:autoComplete (completeMethod=onSearchCatalogue, minQueryLength=2, itemLabel=commonName + " (" + scientificName + ")", selectListener=onSelectCatalogue); "Keine Ergebnisse"-Meldung (fishstock.form.catalogue.noresults) + Link "Neuen Eintrag vorschlagen" (fishstock.form.catalogue.propose.link); "Katalog-Link entfernen"-Button wenn fishCatalogueId != null (fishstock.form.catalogue.unlink.button). Acceptance: Dropdown erscheint erst ab 2 eingegebenen Zeichen; onSelect füllt Scientific Name und Reference URL aus.

### Tests US3 (T053)

- [x] T053 [P] [US3] `FishCatalogueControllerTest.java` erstellen in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/FishCatalogueControllerTest.java` — Test-Cases: search_minTwoCharsRequired_else400 (FR-020), search_returnsPublicAndOwnPending_notOthersPending (SC-009), search_partialMatch_scientific_and_i18n. Acceptance: Alle 3 Tests grün; fremde PENDING-Einträge erscheinen nicht.

**Checkpoint US3**: US1–US3 funktionieren. Katalogsuche + Auto-Fill verifiziert.

---

## Phase 6: User Story 4 — Neuen Katalogeintrag vorschlagen (Priority: P4)

**Ziel**: Ein authentifizierter Benutzer kann einen neuen Katalogeintrag mit wissenschaftlichem Namen und lokalisierten Feldern vorschlagen. Der Eintrag ist sofort in seiner eigenen Suche sichtbar (status=PENDING), aber für andere Benutzer unsichtbar.

**Independent Test**: Als Benutzer A → neuen Eintrag mit uniquem scientificName vorschlagen → in eigener Suche sichtbar und auswählbar → als Benutzer B suchen → Eintrag NICHT sichtbar → im Admin-Dashboard Eintrag in Pending-Liste sehen.

### Service (T054)

- [x] T054 [US4] `FishCatalogueServiceImpl.proposeEntry()` implementieren in `sabi-server/src/main/java/de/bluewhale/sabi/services/FishCatalogueServiceImpl.java` — isDuplicateScientificName prüfen: falls true → non-blocking Warning CATALOGUE_DUPLICATE_WARNING dem ResultTo beifügen, aber NICHT abbrechen (FR-015); status = PENDING setzen; proposerUserId = user.id; proposalDate = today; FishCatalogueRepository.save(); ResultTo\<FishCatalogueEntryTo\> mit CATALOGUE_ENTRY_PROPOSED zurückgeben. Acceptance: Bei Duplikat-Name wird Eintrag trotzdem gespeichert (non-blocking FR-015); REJECTED-Name löst kein Warning aus (FR-012).

### Controller (T055)

- [x] T055 [US4] `POST /api/fish/catalogue` Endpoint in `FishCatalogueController.java` implementieren — @RequestBody @Valid FishCatalogueEntryTo; Service-Aufruf proposeEntry; 201 Created. Acceptance: 201 auch wenn CATALOGUE_DUPLICATE_WARNING im ResultTo enthalten; 400 wenn scientificName leer.

### Frontend (T056–T058)

- [x] T056 [US4] `FishCatalogueProposalView.java` CDI-Bean Controller erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/FishCatalogueProposalView.java` — @Named @ViewScoped; State: FishCatalogueEntryTo proposal (mit 5 i18n-Einträgen vorbefüllt für DE/EN/ES/FR/IT); boolean duplicateWarningShown; Methoden: init() → proposal vorbereiten, onScientificNameBlur() → isDuplicate-Backend-Call → duplicateWarningShown setzen, onSubmit() → FishCatalogueApiService.propose() → bei ResultTo.WARNING: Warnpanel anzeigen, aber Formular bleibt offen bis User explizit bestätigt oder erneut submittet. Acceptance: REJECTED-Name zeigt kein Warning; PENDING/PUBLIC-Name zeigt Warning-Panel.
- [x] T057 [US4] `fishCatalogueProposalForm.xhtml` erstellen in `sabi-webclient/src/main/resources/META-INF/resources/secured/fishCatalogueProposalForm.xhtml` — h:inputText scientificName (required, onblur=onScientificNameBlur); p:tabView mit 5 Tabs (DE/EN/ES/FR/IT): h:inputText commonName (nullable), h:inputTextarea description (maxlength=2000, Counter anzeigen), h:inputText referenceUrl; Duplikat-Warnpanel (p:messages / p:growl: fishcatalogue.scientificname.duplicate.warning mit param=scientificName); Submit + Abbrechen-Buttons. Acceptance: Formular zeigt Duplikat-Warning, ist aber weiter speicherbar (FR-015); Description > 2000 Zeichen → inline Fehlermeldung.
- [x] T058 [P] [US4] `FishCatalogueApiService` in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/FishCatalogueService.java` und Impl um isDuplicate(scientificName, token) → boolean erweitern. Acceptance: HTTP GET /api/fish/catalogue/search mit scientificName als exakter Query liefert Ergebnis.

### Tests US4 (T059)

- [x] T059 [P] [US4] `FishCatalogueServiceTest.java` erstellen in `sabi-server/src/test/java/de/bluewhale/sabi/services/FishCatalogueServiceTest.java` — Test-Cases: proposeEntry_duplicateScientificName_returnsWarningButProceed (FR-015), proposeEntry_rejectedDuplicateName_noWarning (FR-012), proposeEntry_setsStatusPendingAndProposerId. Acceptance: Alle 3 Tests grün.

**Checkpoint US4**: US4 testbar. Proposal erscheint sofort in eigener Suche, nicht in fremder Suche.

---

## Phase 7: User Story 5 — Admin: Katalog-Proposal genehmigen/ablehnen (Priority: P5)

**Ziel**: Ein Admin-Benutzer kann in einer dedizierten Ansicht alle Pending-Proposals sehen, bearbeiten, genehmigen (→ PUBLIC, für alle sichtbar) oder ablehnen (→ REJECTED, unsichtbar für alle).

**Independent Test**: Als Admin → pending-proposals Liste aufrufen → Proposal genehmigen → als normaler Benutzer suchen → Eintrag jetzt sichtbar. Als Admin → weiteres Proposal ablehnen → Eintrag für alle unsichtbar.

### Service (T060)

- [x] T060 [US5] `FishCatalogueServiceImpl.approveEntry()`, `rejectEntry()`, `listPendingProposals()` implementieren in `sabi-server/src/main/java/de/bluewhale/sabi/services/FishCatalogueServiceImpl.java` — approveEntry: Admin-Check; entry.status = PUBLIC; optionale Feld-Merges aus editedEntryTo; save(); ResultTo CATALOGUE_ENTRY_APPROVED; rejectEntry: Admin-Check; entry.status = REJECTED; save(); ResultTo CATALOGUE_ENTRY_REJECTED; listPendingProposals: FishCatalogueRepository.findAllByStatusOrderByProposalDateAsc("PENDING"); Admin-Check via UserTo.roles. Acceptance: approveEntry → Eintrag sofort in search() aller Benutzer sichtbar; rejectEntry → Eintrag in keiner search() sichtbar.

### Controller und Security (T061–T062)

- [x] T061 [US5] `FishCatalogueAdminController.java` erstellen in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/FishCatalogueAdminController.java` — @RestController @RequestMapping("/api/admin/fish/catalogue") @PreAuthorize("hasRole('ADMIN')"); GET /pending → 202 List\<FishCatalogueEntryTo\>; PUT /{id}/approve + optional @RequestBody FishCatalogueEntryTo → 202; PUT /{id}/reject + @RequestBody Map\<String, String\> reason → 202. Acceptance: Non-Admin → 403 auf allen drei Endpoints.
- [x] T062 [US5] `WebSecurityConfig.java` in `sabi-server/src/main/java/de/bluewhale/sabi/configs/WebSecurityConfig.java` um Regel `antMatchers("/api/admin/**").hasRole("ADMIN")` erweitern. Acceptance: `/api/admin/fish/catalogue/pending` ohne ADMIN-Rolle → 403.

### Frontend (T063–T065)

- [x] T063 [US5] `FishCatalogueAdminService.java` Interface + `FishCatalogueAdminServiceImpl.java` API-Gateway erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/FishCatalogueAdminService.java` und `FishCatalogueAdminServiceImpl.java` — Methoden: getPendingProposals(token), approveEntry(id, edits, token), rejectEntry(id, reason, token). Acceptance: HTTP-Calls gehen gegen `/api/admin/fish/catalogue/*`.
- [x] T064 [US5] `FishCatalogueAdminView.java` CDI-Bean Controller erstellen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/FishCatalogueAdminView.java` — @Named @ViewScoped; State: List\<FishCatalogueEntryTo\> pendingProposals; FishCatalogueEntryTo selectedProposal; String rejectionReason; @PostConstruct lädt pendingProposals sortiert nach proposalDate ASC; Methoden: onOpenProposal(proposal), onApprove() → AdminService.approveEntry(selectedProposal), onReject() → AdminService.rejectEntry(id, reason), alle i18n-Felder editierbar vor Approve. Acceptance: Nach onApprove wird Proposal aus der Liste entfernt.
- [x] T065 [US5] `fishCatalogueAdminView.xhtml` erstellen in `sabi-webclient/src/main/resources/META-INF/resources/secured/admin/fishCatalogueAdminView.xhtml` — p:dataTable (pendingProposals): Spalten scientific_name, proposalDate, proposer (anonymisiert), [Öffnen]-Button; Detail-Dialog: alle FishCatalogueEntryTo-Felder + i18n-Tab-Ansicht (p:tabView DE/EN/ES/FR/IT) editierbar, [Genehmigen]-Button (fishcatalogue.admin.approve.button), [Ablehnen]-Button (fishcatalogue.admin.reject.button) + Textarea für Ablehnungsgrund (fishcatalogue.admin.rejection.reason.label); WCAG-konforme Statuslabels; alle Labels aus #{msgs.fishcatalogue.admin.*}. Acceptance: Admin sieht scientificName, proposalDate und Vorschlagender (anonymisiert) in der Listenspalte.
- [x] T066 [US5] Admin-Menüeintrag "Katalog-Verwaltung" in `sabi-webclient/src/main/resources/META-INF/resources/template/masterLayout.xhtml` hinzufügen — nur sichtbar wenn UserSession.isAdmin(); navigiert zu `secured/admin/fishCatalogueAdminView.xhtml`. Acceptance: Menüpunkt erscheint nur bei ADMIN-Rolle; nicht sichtbar für normale Benutzer.

### Tests US5 (T067)

- [x] T067 [P] [US5] `FishCatalogueAdminControllerTest.java` erstellen in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/FishCatalogueAdminControllerTest.java` — Test-Cases: listPending_nonAdmin_returns403 (FR-021), approve_validProposal_entryBecomesPublic (FR-016), reject_validProposal_entryBecomesRejectedAndInvisible (FR-017), approve_adminEditsBeforeApproving_fieldsSaved (FR-018). Acceptance: Alle 4 Tests grün.

**Checkpoint US5**: US5 testbar. Admin kann Proposals genehmigen/ablehnen; approvedEntry sofort öffentlich sichtbar.

---

## Phase 8: User Story 6 — Katalog-i18n-Felder pflegen (Priority: P6)

**Ziel**: Der Creator eines PENDING-Eintrags (oder ein Admin für PUBLIC) kann jederzeit fehlende oder falsche lokalisierte Felder ergänzen/korrigieren. Änderungen am scientificName re-evaluieren die Duplikat-Prüfung.

**Independent Test**: Bestehenden approved Eintrag als Creator öffnen → Französisch-Tab → commonName und description eintragen → Speichern → Browser auf Französisch umstellen → Eintrag zeigt die französischen Felder.

### Service (T068)

- [x] T068 [US6] `FishCatalogueServiceImpl.updateEntry()` implementieren in `sabi-server/src/main/java/de/bluewhale/sabi/services/FishCatalogueServiceImpl.java` — REJECTED → CATALOGUE_REJECTED_READ_ONLY Exception (FR-019); PENDING: nur Creator darf bearbeiten; PUBLIC: Creator + Admin; scientificName-Änderung → isDuplicateScientificName prüfen → non-blocking Warning wenn Konflikt (FR-015); i18n-Einträge mergen (existierende updaten, neue einfügen über orphanRemoval); save(); ResultTo CATALOGUE_ENTRY_UPDATED. Acceptance: REJECTED-Eintrag bearbeiten → 403/409; PUBLIC-Eintrag von Nicht-Creator → 403.

### Controller (T069)

- [x] T069 [US6] `PUT /api/fish/catalogue/{id}` Endpoint in `FishCatalogueController.java` implementieren — @RequestBody @Valid FishCatalogueEntryTo; Service.updateEntry() aufrufen; 202 ResultTo. Acceptance: Unauthentifizierter Zugriff → 401; Wrong-Owner → 403; REJECTED → 409 Conflict.

### Frontend (T070)

- [x] T070 [US6] `fishCatalogueProposalForm.xhtml` um Edit-Modus erweitern in `sabi-webclient/src/main/resources/META-INF/resources/secured/fishCatalogueProposalForm.xhtml` — rendered="#{fishCatalogueProposalView.isEditMode}": bestehende i18n-Einträge vorausfüllen; description-Zeichen-Counter aktualisieren; Duplikat-Warning erneut anzeigen wenn scientificName geändert wird (onBlur-Call); Description-Länge > 2000 Zeichen → inline Fehlermeldung fishcatalogue.i18n.description.maxlength.error (FR-006 analog für description); Speichern ruft updateEntry auf. FishCatalogueProposalView.onScientificNameChange() ebenfalls auf edit-flow anwenden. Acceptance: Benutzer kann nur Felder bearbeiten, für die er Berechtigung hat (Creator/Admin); Description-Validator feuert client-seitig.

### Tests US6 (T071)

- [x] T071 [P] [US6] `FishCatalogueServiceTest.java` um Update-Tests erweitern: updateEntry_rejectedStatus_throwsCatalogueRejectedReadOnly (FR-019), updateEntry_pendingByCreator_succeeds, updateEntry_publicByNonCreatorNonAdmin_throwsNotYours, updateEntry_changeScientificName_retriggeresDuplicateWarning (FR-015). Acceptance: Alle 4 Tests grün.

**Checkpoint US6**: US6 testbar. i18n-Feld-Update + Anzeige in entsprechender Sprache verifiziert.

---

## Phase 9: DevOps (Docker · Ansible)

**Zweck**: Foto-Storage-Volume in alle Deployment-Artefakte integrieren.

- [x] T072 [P] `devops/sabi_docker_sdk/docker-compose.yml` erweitern — sabi-backend Service: `volumes: - sabi-fish-photos:/var/sabi-data/fish-photos`, `environment: SABI_FISH_PHOTO_STORAGE_DIR: /var/sabi-data/fish-photos`; Volume-Deklaration: `volumes: sabi-fish-photos:`. Acceptance: `docker-compose up` startet ohne Fehler; Volume wird gemountet.
- [x] T073 [P] `devops/sabi_docker_sdk/docker-compose-arm.yml` analog zu T072 für ARM64 erweitern. Acceptance: ARM64-Build funktioniert mit gleichem Volume-Schema.
- [x] T074 `devops/ansible/deploySabiService.yml` um Task "Create fish photo storage directory" erweitern — file: path={{ sabi_fish_photo_dir | default('/var/sabi-data/fish-photos') }} state=directory owner=sabi mode='0750'. Acceptance: Ansible-Dry-Run zeigt Task ohne Error.
- [x] T075 Risiko R-5 adressieren: `TankServiceImpl.java` in `sabi-server/src/main/java/de/bluewhale/sabi/services/TankServiceImpl.java` — bei Aquarium-Löschung (deleteTank) alle Fish-Einträge des Aquariums per `TankFishStockRepository.softDeleteAllByAquariumId(id, LocalDateTime.now())` soft-deleten. Acceptance: Edge Case aus spec.md — gelöschtes Aquarium → Fischeinträge sind per @SQLRestriction unsichtbar; Katalog-Links bleiben intakt.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Zweck**: Qualitätssicherung, Performance-Test, fehlende Tests, OpenAPI-Dokumentation.

- [x] T076 [P] `FishCatalogueRepositoryTest.java` erstellen in `sabi-server/src/test/java/de/bluewhale/sabi/persistence/repositories/FishCatalogueRepositoryTest.java` — TestContainers MySQL; Test-Cases: searchByQuery_returnsPublicAndOwnPendingEntries, searchByQuery_doesNotReturnOtherUsersPendingEntries (SC-009), searchByQuery_partialMatchOnScientificName, searchByQuery_partialMatchOnI18nCommonName, uniqueConstraint_acceptsRejectedDuplicateName (FR-012), uniqueConstraint_rejectsDuplicatePendingOrPublicName. Acceptance: Alle 6 Tests grün.
- [x] T077 [P] Performance-Test SC-003 implementieren (Katalogsuche ≤ 1s bei 500 Einträgen): Testdaten-Factory in `sabi-server/src/test/java/de/bluewhale/sabi/services/FishCataloguePerformanceTest.java` — 500 FishCatalogueEntryEntity + je 5 i18n-Einträge anlegen; Query-Zeit messen; Assertion: durationMs < 1000. Acceptance: Test ist als @Tag("performance") markiert und läuft in CI.
- [x] T078 [P] OpenAPI/Swagger-Annotationen für alle neuen Endpoints hinzufügen in `FishStockController.java`, `FishCatalogueController.java`, `FishCatalogueAdminController.java` — @Operation(summary=..., description=...) + @ApiResponse(responseCode=...) für alle Endpoints. Acceptance: Swagger-UI zeigt alle neuen Endpoints korrekt an.
- [x] T079 [P] Architekturtest ergänzen: `@SQLRestriction` statt `@Where` prüfen — Risiko R-4: ArchUnit-Test oder manuelles Grep verifiziert, dass keine `@Where`-Annotationen mehr in neuen Entities vorhanden. Acceptance: Kein `@Where` in TankFishStockEntity.
- [x] T080 i18n-Vollständigkeitsprüfung: Python-Skript nach `/tmp/i18n_check.py` schreiben, das alle 52 Keys aus plan.md gegen alle 6 Bundle-Dateien in `sabi-webclient/src/main/resources/i18n/` prüft. Acceptance: Ausgabe "All 52 keys present in all 6 files" ohne Lücken.

---

## Phase N: ISO 25010 Quality Gates *(SABI-Konstitution v1.0.0)*

**Zweck**: Alle acht ISO 25010 Qualitätsmerkmale vor dem Merge sicherstellen.

- [x] QGATE-I   Funktionale Eignung: Alle 25 FRs (FR-001–FR-025) aus spec.md abgehakt? Cross-User-Datenlecks (FR-011) via FishStockControllerTest verifiziert? Physisches Löschen nur ohne Departure-Record (FR-024) getestet? Foto-Zugriff ausschließlich über Fischeintrag-API (FR-025) getestet?
- [x] QGATE-II  Leistungseffizienz: Katalogsuche ≤ 1s bei 500 Einträgen (SC-003, T077) gemessen? Foto-Upload 5-MB-Limit enforced (FR-008)? ARM64-Kompatibilität des Docker-Builds verifiziert?
- [x] QGATE-III Kompatibilität: Neue /api/fish/* Endpoints brechen keine bestehenden Endpoints? OpenAPI-Doku aktuell (T078)? Keine ungekennzeichneten Breaking Changes an FishTo/FishCatalogueTo (@Deprecated, T023)?
- [x] QGATE-IV  Benutzbarkeit: Alle 52 i18n-Keys in allen 6 Bundle-Dateien vollständig (T028, T080)? WCAG 2.1 AA Kontrastwerte für alle neuen UI-Elemente verifiziert (T077 WCAG-Check)? Statuslabels PENDING/PUBLIC/REJECTED mit korrekten Kontrastfarben?
- [x] QGATE-V   Zuverlässigkeit: CI grün (mvn test + CodeQL)? Alle @Transactional-Methoden rollen bei Exception zurück (getestet)? Departure-Datum-Validierung client- und server-seitig (FR-006) verifiziert? Foto-Upload-Fehler mit klarer Fehlermeldung abgefangen (FR-008)?
- [x] QGATE-VI  Sicherheit: OWASP Dependency Check grün? Keine Credentials im Code? /api/admin/** auf ADMIN-Rolle eingeschränkt (T062)? Ownership-Checks in FishStockService und FishCatalogueService verifiziert (FR-011, FR-023)?
- [x] QGATE-VII Wartbarkeit: Je mindestens ein Integrationstest für US1–US5 (T040, T047, T053, T059, T067) vorhanden und grün? Flyway-Migrationen V1_5_0_1–5 unveränderlich (keine Altskripte angefasst)?
- [x] QGATE-VIII Übertragbarkeit: Docker-Build AMD64 + ARM64 OK (T072, T073)? Ansible-Playbook mit Fish-Photo-Verzeichnis aktualisiert (T074)? `sabi.fish.photo.storage-dir` als Env-Variable `SABI_FISH_PHOTO_STORAGE_DIR` exponiert?

**Checkpoint Final**: Alle zutreffenden QGATE-Tasks abgehakt → Feature ist merge-bereit.

---

## Abhängigkeiten & Ausführungsreihenfolge

### Phasen-Abhängigkeiten

```
Phase 1 (Setup)
  └─► Phase 2a–2g (Foundational) [alle blockiert durch T001]
        ├─► Phase 3 (US1, P1) [blockiert durch Phase 2 vollständig]
        │     └─► Phase 4 (US2, P2) [US2 braucht FishStockController aus US1]
        │           └─► Phase 5 (US3, P3) [braucht FishCatalogueController]
        │                 └─► Phase 6 (US4, P4) [braucht Katalog-Search aus US3]
        │                       └─► Phase 7 (US5, P5) [braucht Proposal aus US4]
        │                             └─► Phase 8 (US6, P6) [braucht updateEntry]
        ├─► Phase 9 (DevOps) [unabhängig nach Phase 3/T033 für application.yml]
        └─► Phase 10 (Polish) [nach US1–US5]
```

### Intra-Phase-Abhängigkeiten (Phase 2)

- T006 (Migration V1_5_0_5) → nach T003 (V1_5_0_2) + T004 (V1_5_0_3)
- T007 (TankFishStockEntity) → nach T002 (V1_5_0_1)
- T008 (FishCatalogueEntryEntity) → nach T003 (V1_5_0_2)
- T009 (FishCatalogueI18nEntity) → nach T004 (V1_5_0_3)
- T010 (FishPhotoEntity) → nach T005 (V1_5_0_4)
- T011–T014 (Repositories) → nach T007–T010 (Entities)
- T017 (FishStockEntryTo) → nach T015 (DepartureReason)
- T019 (FishCatalogueEntryTo) → nach T016 (FishCatalogueStatus) + T020 (FishCatalogueI18nTo)
- T026 (FishStockMapper) → nach T007, T017 (Entity + DTO)
- T027 (FishCatalogueMapper) → nach T008, T009, T019, T020, T021

### User-Story-Abhängigkeiten

- **US1 (P1)**: startet nach Phase 2 — keine Abhängigkeit zu anderen Stories
- **US2 (P2)**: startet nach US1 (recordDeparture-Endpoint im FishStockController aus T032)
- **US3 (P3)**: startet nach US1 (FishStockEntryView aus T038 wird erweitert)
- **US4 (P4)**: startet nach US3 (FishCatalogueService-Basisinfrastruktur aus T048)
- **US5 (P5)**: startet nach US4 (proposeEntry-Service aus T054 vorhanden)
- **US6 (P6)**: startet nach US4 (FishCatalogueEntryTo + XHTML-Form aus T057 vorhanden)

### Parallele Ausführung innerhalb Phase 2

```bash
# Gleichzeitig startbar (alle [P] in Phase 2):
T002, T003, T004, T005  # 4 unabhängige Flyway-Scripts
T015, T016              # 2 Enums (kein gegenseitige Abhängigkeit)
T020, T021, T023        # DTOs + Deprecated-Markierung
T024, T025              # Exception/Message Codes
T013, T014              # i18n + Photo Repository
T009, T010              # i18n + Photo Entity
```

### Parallele Ausführung innerhalb US1 (Phase 3)

```bash
# Nach T030 (FishStockServiceImpl):
T029 (PhotoStorageService)  # parallel zu T030 startbar (nur Interface nötig)
T031 (Legacy-Deprecation)   # parallel nach T030

# Nach T032 (Controller):
T033 (application.yml)       # parallel
T034 (Webclient API-Gateway) # parallel

# Nach T034–T035 (API-Gateway + FishStockView):
T036 (fishStockTab.xhtml)   # parallel zu T039 (fishStockEntryForm.xhtml)
T037 (tankView.xhtml-Tab)   # nach T036

# Tests parallel zu Implementation:
T040, T041, T042            # alle drei parallel
```

---

## Paralleles Ausführungsbeispiel: User Story 3

```bash
# Sobald US2 abgeschlossen:
Task A: T048 — FishCatalogueServiceImpl.search() implementieren
Task B: T050 — FishCatalogueService API-Gateway (webclient) erstellen
# Task B kann mit T048 parallel starten (Interface-Vertrag klar)

# Nach T048:
Task C: T049 — FishCatalogueController (/api/fish/catalogue/search) erstellen
Task D: T051 — FishStockEntryView Katalog-Such-Logik

# Nach T049 + T050 + T051:
Task E: T052 — fishStockEntryForm.xhtml p:autoComplete ergänzen

# Parallel zum gesamten US3-Code:
Task F: T053 — FishCatalogueControllerTest (kann gegen Interface geschrieben werden)
```

---

## Implementierungsstrategie

### MVP First (User Story 1 + 2 only)

1. Phase 1 + 2 abschließen (Fundament)
2. Phase 3 (US1) implementieren
3. **STOP & VALIDATE**: Fish hinzufügen/anzeigen/löschen vollständig testen
4. Phase 4 (US2) implementieren
5. **STOP & VALIDATE**: Fish-Abgang testen — departed section korrekt?
6. Deploy/Demo als MVP

### Inkrementelle Lieferung

| Sprint | Inhalt | Testbar |
|--------|--------|---------|
| 1 | Phase 1 + Phase 2 (Fundament) | mvn compile grün |
| 2 | Phase 3 (US1 — Add Fish) | Fish CRUD + Photo |
| 3 | Phase 4 (US2 — Departure) | Abgang erfassen |
| 4 | Phase 5 (US3 — Catalogue Link) | Suche + Auto-Fill |
| 5 | Phase 6 (US4 — Propose) | UGC-Workflow |
| 6 | Phase 7 (US5 — Admin Approve) | Governance-Layer |
| 7 | Phase 8 (US6 — i18n Edit) + Phase 9 (DevOps) | i18n-Pflege + Deployment |
| 8 | Phase 10 (Polish + Quality Gates) | Merge-bereit |

### Team-Parallelisierung (2 Entwickler)

- **Dev A**: Phase 2 (DB + Entities + Repos) → Phase 3 Service + Controller → Phase 4+5
- **Dev B**: Phase 2 DTOs + Mapper + i18n → Phase 3 Frontend → Phase 6+7+8
- Sync-Punkt nach Phase 2: beide warten auf Checkpoint

---

## Hinweise

- `[P]`-Tasks = unterschiedliche Dateien, keine offenen Abhängigkeiten → können gleichzeitig gestartet werden
- `[US1–US6]`-Label verknüpft Task mit User Story für Traceability
- Jede User Story ist nach Phase 2 unabhängig implementierbar und testbar
- Commit nach jeder Task oder logischen Gruppe
- Stop an jedem Checkpoint zur unabhängigen Story-Validierung
- Verboten: vage Tasks, Dateikonflikte in parallelen Tasks, Story-übergreifende Abhängigkeiten, die Unabhängigkeit brechen
- Risiken R-1 bis R-7 aus `plan.md` beachten; R-5 (Aquarium-Soft-Delete-Kaskade) explizit in T075 adressiert

