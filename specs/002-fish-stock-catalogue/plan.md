# Implementation Plan: Fish Stock Management & Fish Catalogue

**Feature Branch**: `002-fish-stock-catalogue`  
**Spec**: `specs/002-fish-stock-catalogue/spec.md` (Status: Clarified)  
**Plan erstellt**: 2026-04-05  
**Größenschätzung**: L — 25 FRs, 4 Entities (2 neu, 2 erweitert), 2 neue Controller, 2 neue Services, JSF/PrimeFaces-Frontend, 5 Flyway-Migrations, 6 i18n-Bundle-Dateien

> ⚠️ **Tech-Stack-Korrektur**: Der User-Request nannte "Vaadin" als Frontend-Technologie.
> Der tatsächliche Code sowie die Constitution (§ Technology Stack) bestätigen **JSF 2.3 + PrimeFaces 15.x**
> (CDI-Beans `@Named`, `@RequestScope`, `.xhtml`-Views, `p:*`-Komponenten).
> Dieser Plan verwendet durchgehend die korrekte JSF/PrimeFaces-Terminologie.

---

## Constitution Check

Geprüft gegen `constitution.md v1.0.0` — alle MUSS-Regeln erfüllt:

| Prinzip | Gate-Status | Nachweis im Plan |
|---|---|---|
| I. Funktionale Eignung | ✅ PASS | Alle 25 FRs durch Service-Methoden abgedeckt; Acceptance Criteria aus Spec übernommen |
| II. Leistungseffizienz | ✅ PASS | FR-020: Index + LIKE-Query für ≤500 Einträge; 5-MB-Limit; Performanztest SC-003 definiert |
| III. Kompatibilität | ✅ PASS | `FishTo`/`FishCatalogueTo` als `@Deprecated` erhalten (keine Breaking API-Changes); ARM64 + AMD64 Docker-Volumes |
| IV. Benutzbarkeit | ✅ PASS | 52 i18n-Keys in allen 6 Bundles; WCAG 2.1 AA Kontrastwerte explizit; Inline-Validierung client+server |
| V. Zuverlässigkeit | ✅ PASS | `@Transactional` auf allen schreibenden Services; TestContainers-Tests definiert |
| VI. Sicherheit | ✅ PASS | Auth auf allen Endpunkten (FR-023); Admin-Rolle für `/api/admin/**`; Ownership-Checks in Services |
| VII. Wartbarkeit | ✅ PASS | Flyway V1_5_0_*; 4-Modul-Architektur eingehalten; 12 ADR-level Design-Entscheidungen dokumentiert |
| VIII. Übertragbarkeit | ✅ PASS | Docker-Compose AMD64+ARM64; Ansible-Task; `sabi.fish.photo.storage-dir` konfigurierbar |

**Keine Constitution-Violations.** ✅

---

## Inhaltsverzeichnis

1. [Technische Analyse & Design-Entscheidungen](#1-technische-analyse--design-entscheidungen)
2. [Datenbankschema (Flyway-Migrations)](#2-datenbankschema-flyway-migrations)
3. [Entity-Schicht (sabi-server)](#3-entity-schicht-sabi-server)
4. [DTOs & Boundary (sabi-boundary)](#4-dtos--boundary-sabi-boundary)
5. [Mapper-Schicht (MapStruct)](#5-mapper-schicht-mapstruct)
6. [Service-Schicht (sabi-server)](#6-service-schicht-sabi-server)
7. [REST-API Design (sabi-server)](#7-rest-api-design-sabi-server)
8. [Frontend: Vaadin Views (sabi-webclient)](#8-frontend-vaadin-views-sabi-webclient)
9. [i18n-Keys (alle 6 Bundle-Dateien)](#9-i18n-keys-alle-6-bundle-dateien)
10. [Foto-Speicher-Strategie](#10-foto-speicher-strategie)
11. [Test-Strategie](#11-test-strategie)
12. [Implementierungs-Phasen (geordnet)](#12-implementierungs-phasen-geordnet)

---

## 1. Technische Analyse & Design-Entscheidungen

### 1.1 Bestandsaufnahme der existierenden Fish-Infrastruktur

| Artefakt | Ist-Zustand | Handlungsbedarf |
|---|---|---|
| `fish`-Tabelle (V1_0_0_1) | `fish_catalogue_id` NOT NULL, kein `common_name`, kein `departure_reason`, kein `external_ref_url`, kein `deleted_at` | ALTER TABLE via Flyway V1_5_0_1 |
| `fish_catalogue`-Tabelle | `scientific_name`, `description` (VARCHAR 400), `meerwasserwiki_url` — kein `status`, kein `proposer_user_id`, keine i18n | ALTER TABLE + neue i18n-Tabelle via V1_5_0_2 + V1_5_0_3 |
| `FishEntity.java` | `fishCatalogueId` NOT NULL, kein `commonName`, kein `scientificName`, kein `departureReason`, kein `externalRefUrl` | Klasse erweitern, auf `TankFishStockEntity` umbenennen |
| `FishCatalogueEntity.java` | Kein `status`, kein `proposer`, keine `i18n`-Assoziation | Klasse erweitern |
| `FishService` / `FishServiceImpl` | Nur 3 Methoden: `registerNewFish`, `removeFish`, `getUsersFish` | In neuen `FishStockService` überführen, `FishService` deprecated |
| `FishTo.java` | `fishCatalogueId` quasi-mandatory; kein `commonName`, kein `departureReason` | Erweitern zu `FishStockEntryTo` |
| `FishCatalogueTo.java` | Einfaches flat DTO ohne Status/i18n | Erweitern zu `FishCatalogueEntryTo` |

### 1.2 Bindende Design-Entscheidungen

| ID | Entscheidung | Begründung |
|---|---|---|
| D-01 | `FishEntity` → umbenennen zu **`TankFishStockEntity`**, Tabelle bleibt `fish` | Spec-Naming; kein Tabellen-Rename wegen Flyway-Komplexität |
| D-02 | `fish_catalogue_id` wird **nullable** | Katalog-Link ist laut Spec optional (FR-009) |
| D-03 | `FishCatalogueEntity` bekommt `status` (PENDING/PUBLIC/REJECTED) als **VARCHAR(10)** | Flexibilität; DB-ENUM-Änderungen sind teuer in MySQL |
| D-04 | Existierende `fish_catalogue`-Einträge erhalten bei Migration Status **PUBLIC** | Existierende Daten waren vor UGC-Workflow de-facto öffentlich |
| D-05 | `description` + `meerwasserwiki_url` in `fish_catalogue` werden zu **EN-Einträgen** in `fish_catalogue_i18n` migriert, alte Spalten bleiben bis zur nächsten Major-Version | Zero-Downtime-Migration; alte Felder `@Deprecated` markieren |
| D-06 | Unique-Constraint für `scientific_name` via **MySQL Generated Virtual Column** | MySQL unterstützt keine partiellen Indizes; Virtual Column mit `IF(status IN('PENDING','PUBLIC'), scientific_name, NULL)` + UNIQUE INDEX — NULL-Werte verstoßen nicht gegen UNIQUE |
| D-07 | `departure_reason` als **VARCHAR(30)** gespeichert, App-Enum `DepartureReason` | Erweiterbar ohne DDL-Migration |
| D-08 | Fotos: **Filesystem** + Metadaten-Tabelle `fish_photo`; kein BLOB | C-4; Property `sabi.fish.photo.storage-dir` |
| D-09 | Katalog-Suche: **JPA LIKE-Query** + FullText-Index auf `common_name` in i18n-Tabelle | FR-020 (1s für ≤500 Einträge); bei Skalierungsbedarf auf MATCH AGAINST upgraden |
| D-10 | `deleted_at TIMESTAMP NULL` auf `fish`-Tabelle + `@SQLRestriction("deleted_at IS NULL")` | Aquarium-Cascade-Delete (Edge Case aus Spec); JPA-Level Soft-Delete |
| D-11 | `FishService` bleibt als `@Deprecated` Stub | Bricht keine Tests; Implementierung delegiert an `FishStockService` |
| D-12 | Admin-Endpoints unter `/api/admin/` Prefix | Konsistent mit geplanter Skalierung; Spring Security-Filter greift auf diesen Pfad |

---

## 2. Datenbankschema (Flyway-Migrations)

Neues Verzeichnis: `sabi-database/src/main/resources/db/migration/version1_5_0/`

### Migration V1_5_0_1 — `extendFishTableForStockManagement.sql`

```sql
-- 002-fish-stock-catalogue: TankFishStock — erweitert bestehende fish-Tabelle
-- Fügt Pflichtfeld common_name, optionale Felder, Departure-Semantik und Soft-Delete hinzu

-- 1. Pflichtfelder hinzufügen (vorerst mit DEFAULT für bestehende Datensätze)
ALTER TABLE `fish`
    ADD COLUMN `common_name`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Species common name (free text, mandatory)' AFTER `nickname`,
    ADD COLUMN `scientific_name`   VARCHAR(255) NULL     COMMENT 'Snapshot from catalogue at link time; user-editable' AFTER `common_name`,
    ADD COLUMN `external_ref_url`  VARCHAR(512) NULL     COMMENT 'Optional external reference URL' AFTER `scientific_name`,
    ADD COLUMN `departure_reason`  VARCHAR(30)  NULL     COMMENT 'DECEASED | REMOVED_REHOMED | UNKNOWN' AFTER `exodus_on`,
    ADD COLUMN `deleted_at`        TIMESTAMP    NULL     DEFAULT NULL COMMENT 'Soft-delete; set when parent aquarium is deleted' AFTER `lastmod_on`,
    ADD COLUMN `optlock`           INT UNSIGNED NOT NULL DEFAULT 0 AFTER `deleted_at`,
    MODIFY COLUMN `fish_catalogue_id` BIGINT(20) UNSIGNED NULL COMMENT 'Optional FK to fish_catalogue; NULL = free-text only';

-- 2. common_name aus nickname befüllen (Datenmigration für Bestandsdaten)
UPDATE `fish` SET `common_name` = COALESCE(NULLIF(`nickname`, ''), 'Unknown') WHERE `common_name` = '';

-- 3. DEFAULT entfernen (Pflichtfeld ab jetzt)
ALTER TABLE `fish` MODIFY COLUMN `common_name` VARCHAR(255) NOT NULL;

-- 4. Index für Soft-Delete-Filter
CREATE INDEX `idx_fish_deleted_at` ON `fish` (`deleted_at`);
```

### Migration V1_5_0_2 — `extendFishCatalogueForUGC.sql`

```sql
-- 002-fish-stock-catalogue: UGC-Workflow für fish_catalogue

ALTER TABLE `fish_catalogue`
    ADD COLUMN `status`          VARCHAR(10)  NOT NULL DEFAULT 'PUBLIC'
                                 COMMENT 'PENDING | PUBLIC | REJECTED' AFTER `id`,
    ADD COLUMN `proposer_user_id` BIGINT(20) UNSIGNED NULL
                                 COMMENT 'FK to users.id; NULL for legacy/system entries' AFTER `status`,
    ADD COLUMN `proposal_date`   DATE         NULL
                                 COMMENT 'Date the UGC proposal was submitted' AFTER `proposer_user_id`,
    ADD COLUMN `optlock`         INT UNSIGNED NOT NULL DEFAULT 0,
    MODIFY COLUMN `scientific_name` VARCHAR(255) NULL;

-- Unique-Constraint via Virtual Column (PENDING + PUBLIC Einträge sperren den Namen)
ALTER TABLE `fish_catalogue`
    ADD COLUMN `active_scientific_name` VARCHAR(255)
        GENERATED ALWAYS AS (
            IF(`status` IN ('PENDING', 'PUBLIC'), `scientific_name`, NULL)
        ) VIRTUAL COMMENT 'Used for partial-unique-index on active entries';

CREATE UNIQUE INDEX `uq_fish_catalogue_active_name`
    ON `fish_catalogue` (`active_scientific_name`);

-- Index für schnelle Status-Abfragen
CREATE INDEX `idx_fish_catalogue_status` ON `fish_catalogue` (`status`);
CREATE INDEX `idx_fish_catalogue_proposer` ON `fish_catalogue` (`proposer_user_id`);

-- FK für proposer_user_id (optional; NULL für Legacy-Einträge)
ALTER TABLE `fish_catalogue`
    ADD CONSTRAINT `fk_fish_catalogue_proposer`
        FOREIGN KEY (`proposer_user_id`) REFERENCES `users` (`id`)
        ON DELETE SET NULL;
```

### Migration V1_5_0_3 — `addFishCatalogueI18nTable.sql`

```sql
-- 002-fish-stock-catalogue: Lokalisierte Felder für Katalog-Einträge

CREATE TABLE `fish_catalogue_i18n`
(
    `id`               BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `catalogue_id`     BIGINT(20) UNSIGNED NOT NULL,
    `language_code`    VARCHAR(2)          NOT NULL COMMENT 'de | en | es | fr | it',
    `common_name`      VARCHAR(255)        NULL,
    `description`      TEXT                NULL     COMMENT 'max 2000 chars enforced at app layer',
    `reference_url`    VARCHAR(512)        NULL,
    `created_on`       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`          INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_catalogue_i18n_lang` (`catalogue_id`, `language_code`),
    CONSTRAINT `fk_catalogue_i18n_entry`
        FOREIGN KEY (`catalogue_id`) REFERENCES `fish_catalogue` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Localized fields for fish catalogue entries';

-- Index für Suche nach common_name
CREATE FULLTEXT INDEX `ft_i18n_common_name` ON `fish_catalogue_i18n` (`common_name`);
```

### Migration V1_5_0_4 — `addFishPhotoTable.sql`

```sql
-- 002-fish-stock-catalogue: Metadaten für Fisch-Fotos (Bytes auf Filesystem, C-4)

CREATE TABLE `fish_photo`
(
    `id`           BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `fish_id`      BIGINT(20) UNSIGNED NOT NULL,
    `file_path`    VARCHAR(512)        NOT NULL COMMENT 'Path relative to sabi.fish.photo.storage-dir',
    `content_type` VARCHAR(50)         NOT NULL COMMENT 'image/jpeg | image/png | image/webp | image/gif',
    `file_size`    BIGINT UNSIGNED     NOT NULL COMMENT 'Bytes',
    `upload_date`  DATE                NOT NULL,
    `created_on`   TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`   TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`      INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_fish_photo` (`fish_id`),
    CONSTRAINT `fk_fish_photo_fish`
        FOREIGN KEY (`fish_id`) REFERENCES `fish` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Fish photo metadata; actual bytes on filesystem (C-4, FR-025)';
```

### Migration V1_5_0_5 — `migrateExistingFishCatalogueToI18n.sql`

```sql
-- 002-fish-stock-catalogue: Datenmigration bestehende Katalogdaten → i18n-Tabelle
-- Bestehende description und meerwasserwiki_url werden als EN-Einträge migriert.

INSERT INTO `fish_catalogue_i18n` (`catalogue_id`, `language_code`, `common_name`, `description`, `reference_url`)
SELECT `id`,
       'en',
       NULL,
       NULLIF(`description`, ''),
       NULLIF(`meerwasserwiki_url`, '')
FROM `fish_catalogue`
WHERE `description` IS NOT NULL
   OR `meerwasserwiki_url` IS NOT NULL;

-- Hinweis: Die alten Spalten description und meerwasserwiki_url in fish_catalogue
-- werden in Version 1.6.0 entfernt. Bis dahin sind sie @Deprecated im Code markiert.
```

### Tabellenübersicht nach allen Migrations

```
fish                    (erweitert)
├── id, aquarium_id, user_id
├── common_name (NOT NULL), scientific_name (NULL), nickname (NULL)
├── external_ref_url (NULL), fish_catalogue_id (NULL, FK optional)
├── added_on (entry date), exodus_on (departure date), departure_reason
├── observed_behavior (TEXT)
├── deleted_at (Soft-Delete für Aquarium-Cascade)
├── created_on, lastmod_on, optlock

fish_catalogue          (erweitert)
├── id, scientific_name, status (PENDING/PUBLIC/REJECTED)
├── proposer_user_id (FK users, NULL für Legacy), proposal_date
├── active_scientific_name (VIRTUAL, für Unique-Index)
├── description (VARCHAR 400, DEPRECATED → entfernt in v1.6)
├── meerwasserwiki_url (VARCHAR 120, DEPRECATED → entfernt in v1.6)
├── created_on, lastmod_on, optlock

fish_catalogue_i18n     (neu)
├── id, catalogue_id (FK fish_catalogue), language_code
├── common_name, description (TEXT max 2000), reference_url
├── created_on, lastmod_on, optlock

fish_photo              (neu)
├── id, fish_id (FK fish, UNIQUE)
├── file_path, content_type, file_size, upload_date
├── created_on, lastmod_on, optlock
```

---

## 3. Entity-Schicht (sabi-server)

Paket: `de.bluewhale.sabi.persistence.model`

### 3.1 `TankFishStockEntity` (umbenennt aus `FishEntity`)

```
@Entity
@Table(name = "fish", schema = "sabi")
@SQLRestriction("deleted_at IS NULL")   // Spring Boot 3.x: ersetzt @Where
@Data @EqualsAndHashCode(exclude = {"user", "catalogueEntry"}, callSuper = false)

Felder:
  Long id
  Long aquariumId
  Long fishCatalogueId          // nullable – FK bleibt als Long (kein Join fetch nötig)
  String commonName             // NOT NULL
  String scientificName         // nullable
  String nickname               // nullable
  String externalRefUrl         // nullable
  LocalDate addedOn             // (Umbenennung von addedOn Timestamp → LocalDate)
  LocalDate exodusOn            // nullable
  String departureReason        // nullable, values: DECEASED | REMOVED_REHOMED | UNKNOWN
  String observedBehavior       // nullable, TEXT
  LocalDateTime deletedAt       // nullable, Soft-Delete
  UserEntity user               // @ManyToOne LAZY, JoinColumn user_id

@NamedQuery "TankFishStock.getUsersFish":
  "SELECT f FROM TankFishStockEntity f
   WHERE f.id = :pFishId
   AND :pUserId IN (SELECT a.user.id FROM AquariumEntity a WHERE a.id = f.aquariumId)"
```

**Migration-Hinweis**: `addedOn` in DB bleibt `added_on` (DATETIME), aber JPA-Mapping via `@Column` auf `LocalDate`. Timestamps werden auf Datum gekürzt.

### 3.2 `FishCatalogueEntryEntity` (umbenennt aus `FishCatalogueEntity`)

```
@Entity
@Table(name = "fish_catalogue", schema = "sabi")
@Data @EqualsAndHashCode(callSuper = false)

Felder:
  Long id
  String scientificName                          // NOT NULL nach Migration
  String status                                  // PENDING | PUBLIC | REJECTED
  Long proposerUserId                            // nullable FK
  LocalDate proposalDate                         // nullable

  // DEPRECATED — entfernt in v1.6.0:
  @Deprecated String description
  @Deprecated String meerwasserwikiUrl

  @OneToMany(mappedBy = "catalogueEntry", cascade = ALL, orphanRemoval = true, fetch = LAZY)
  List<FishCatalogueI18nEntity> i18nEntries

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "proposer_user_id", insertable = false, updatable = false)
  UserEntity proposer                            // nur für Admin-Queries
```

### 3.3 `FishCatalogueI18nEntity` (neu)

```
@Entity
@Table(name = "fish_catalogue_i18n", schema = "sabi",
       uniqueConstraints = @UniqueConstraint(columnNames = {"catalogue_id","language_code"}))
@Data @EqualsAndHashCode(callSuper = false)

Felder:
  Long id
  Long catalogueId                    // FK-Spalte (insertable/updatable via JoinColumn)
  String languageCode                 // de | en | es | fr | it
  String commonName                   // nullable
  String description                  // nullable, @Column(columnDefinition = "TEXT")
  String referenceUrl                 // nullable

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "catalogue_id")
  FishCatalogueEntryEntity catalogueEntry
```

### 3.4 `FishPhotoEntity` (neu)

```
@Entity
@Table(name = "fish_photo", schema = "sabi")
@Data @EqualsAndHashCode(callSuper = false)

Felder:
  Long id
  Long fishId                         // FK (scalar)
  String filePath                     // relativer Pfad unter storage-dir
  String contentType                  // image/jpeg | image/png | image/webp | image/gif
  Long fileSize                       // Bytes
  LocalDate uploadDate

  @OneToOne(fetch = LAZY)
  @JoinColumn(name = "fish_id", insertable = false, updatable = false)
  TankFishStockEntity fishEntry
```

---

## 4. DTOs & Boundary (sabi-boundary)

Paket: `de.bluewhale.sabi.model`

### 4.1 Neue Enums

**`DepartureReason.java`**
```java
public enum DepartureReason {
    DECEASED, REMOVED_REHOMED, UNKNOWN
}
```

**`FishCatalogueStatus.java`**
```java
public enum FishCatalogueStatus {
    PENDING, PUBLIC, REJECTED
}
```

### 4.2 `FishStockEntryTo.java` (ersetzt/erweitert `FishTo`)

```
Long id
Long aquariumId
String commonName               // @NotBlank, mandatory
String scientificName           // nullable
String nickname                 // nullable
String externalRefUrl           // nullable, @URL wenn nicht null
LocalDate addedOn               // @NotNull, @PastOrPresent
LocalDate exodusOn              // nullable
DepartureReason departureReason // nullable
String observedBehavior         // nullable
Long fishCatalogueId            // nullable (optional catalogue link)
boolean hasPhoto                // true wenn FishPhotoEntity existiert
```

**Rückwärtskompatibilität**: `FishTo` bleibt als `@Deprecated`-Klasse, die intern auf `FishStockEntryTo` delegiert. Bestehende Tests werden auf `FishStockEntryTo` umgestellt.

### 4.3 `FishCatalogueEntryTo.java` (erweitert `FishCatalogueTo`)

```
Long id
String scientificName           // @NotBlank
FishCatalogueStatus status
Long proposerUserId             // nur für Admin-Kontext befüllt
LocalDate proposalDate
List<FishCatalogueI18nTo> i18nEntries
```

### 4.4 `FishCatalogueI18nTo.java` (neu)

```
Long id                         // nullable für neue Einträge
String languageCode             // de | en | es | fr | it
String commonName               // nullable
String description              // nullable, max 2000 Zeichen (@Size(max=2000))
String referenceUrl             // nullable
```

### 4.5 `FishCatalogueSearchResultTo.java` (neu — leichtgewichtig für Dropdown)

```
Long id
String scientificName
String commonName               // in der angeforderten Sprache (oder Fallback)
String referenceUrl             // in der angeforderten Sprache (oder Fallback)
FishCatalogueStatus status
```

### 4.6 `FishDepartureRecordTo.java` (neu — für Departure-Endpoint)

```
@NotNull LocalDate departureDate
@NotNull DepartureReason departureReason
```

### 4.7 `Endpoint.java` — neue Einträge

```java
FISH_STOCK("/api/fish"),
FISH_CATALOGUE("/api/fish/catalogue"),
FISH_CATALOGUE_ADMIN("/api/admin/fish/catalogue");
```

---

## 5. Mapper-Schicht (MapStruct)

Paket: `de.bluewhale.sabi.mapper`

### 5.1 `FishStockMapper.java`

```
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
interface FishStockMapper {

  @Mappings({
    @Mapping(target = "user", ignore = true),
    @Mapping(target = "deletedAt", ignore = true),
    @Mapping(target = "addedOn",   source = "addedOn"),    // LocalDate → LocalDate
    @Mapping(target = "exodusOn",  source = "exodusOn"),   // LocalDate → LocalDate
  })
  TankFishStockEntity mapTo2Entity(FishStockEntryTo to);

  @Mappings({
    @Mapping(target = "hasPhoto", expression = "java(entity.getPhoto() != null)"),
    // photo-Assoziation: boolean-Flag, keine Bytes im DTO
  })
  FishStockEntryTo mapEntity2To(TankFishStockEntity entity);
}
```

### 5.2 `FishCatalogueMapper.java`

```
@Mapper(componentModel = "spring")
interface FishCatalogueMapper {

  @Mappings({
    @Mapping(target = "proposer",  ignore = true),
    @Mapping(target = "i18nEntries", source = "i18nEntries"),
  })
  FishCatalogueEntryEntity mapTo2Entity(FishCatalogueEntryTo to);

  FishCatalogueEntryTo mapEntity2To(FishCatalogueEntryEntity entity);

  FishCatalogueI18nEntity mapI18nTo2Entity(FishCatalogueI18nTo to);
  FishCatalogueI18nTo     mapI18nEntity2To(FishCatalogueI18nEntity entity);

  // Suche: Compact-Mapping mit Sprach-Fallback
  @Mappings({
    @Mapping(target = "commonName",   expression = "java(resolveI18nName(entity, lang))"),
    @Mapping(target = "referenceUrl", expression = "java(resolveI18nUrl(entity, lang))"),
  })
  FishCatalogueSearchResultTo mapEntity2SearchResult(FishCatalogueEntryEntity entity,
                                                      @Context String lang);
}
```

**`resolveI18nName(entity, lang)`**: Sucht i18nEntries nach `languageCode == lang`, Fallback auf `"en"`, dann ersten vorhandenen.

---

## 6. Service-Schicht (sabi-server)

Paket: `de.bluewhale.sabi.services`

### 6.1 `FishStockService` Interface

```java
public interface FishStockService {

  // FR-002: Fisch anlegen
  @Transactional
  ResultTo<FishStockEntryTo> addFishToTank(FishStockEntryTo entry, UserTo user);

  // FR-004: Fisch bearbeiten
  @Transactional
  ResultTo<FishStockEntryTo> updateFishEntry(FishStockEntryTo entry, UserTo user);

  // FR-005/006: Abgang erfassen
  @Transactional
  ResultTo<FishStockEntryTo> recordDeparture(Long fishId, FishDepartureRecordTo departure, UserTo user);

  // FR-024: Physisch löschen (nur ohne Departure-Record)
  @Transactional
  ResultTo<Void> deletePhysically(Long fishId, UserTo user);

  // FR-007: Fische eines Beckens abrufen
  List<FishStockEntryTo> getFishForTank(Long aquariumId, UserTo user);

  // FR-010: Katalog-Link entfernen
  @Transactional
  ResultTo<FishStockEntryTo> removeCatalogueLink(Long fishId, UserTo user);

  // FR-008: Foto hochladen
  @Transactional
  ResultTo<Void> uploadPhoto(Long fishId, byte[] photoBytes, String contentType, long fileSize, UserTo user);

  // FR-025: Foto-Bytes abrufen
  byte[] getPhotoBytes(Long fishId, UserTo user);
}
```

### 6.2 `FishStockServiceImpl`

**Kernlogik `addFishToTank`**:
1. Aquarium-Ownership validieren (Aquarium gehört dem User)
2. Wenn `fishCatalogueId != null` → Katalog-Eintrag laden, `scientificName` + localisierten `referenceUrl` als Snapshot kopieren (FR-009)
3. `TankFishStockEntity` via Mapper erstellen, `user` setzen
4. Persistieren
5. `ResultTo` mit `CREATE_SUCCEEDED` zurückgeben

**Kernlogik `deletePhysically`**:
1. Entity laden (mit Ownership-Check)
2. Falls `exodusOn != null || departureReason != null` → `ResultTo` mit `FISH_HAS_DEPARTURE_RECORD` Fehler (FR-024)
3. Sonst: physisch löschen

**Kernlogik `uploadPhoto`**:
1. Ownership-Check
2. Inhalt per Magic-Bytes validieren (JPEG: `FF D8 FF`, PNG: `89 50 4E 47`, WebP: `52 49 46 46...57 45 42 50`, GIF: `47 49 46 38`) (FR-008)
3. Dateigröße ≤ 5 MB prüfen
4. Datei in `{storage-dir}/{userId}/{fishId}.{ext}` schreiben
5. `FishPhotoEntity` speichern (upsert: falls vorhanden, überschreiben)

### 6.3 `FishCatalogueService` Interface

```java
public interface FishCatalogueService {

  // FR-020: Suche (PENDING nur für Creator sichtbar)
  List<FishCatalogueSearchResultTo> search(String query, String languageCode, UserTo user);

  // FR-014: Vorschlag einreichen
  @Transactional
  ResultTo<FishCatalogueEntryTo> proposeEntry(FishCatalogueEntryTo entry, UserTo user);

  // FR-019: Eintrag bearbeiten (Creator für PENDING, Creator+Admin für PUBLIC)
  @Transactional
  ResultTo<FishCatalogueEntryTo> updateEntry(FishCatalogueEntryTo entry, UserTo user);

  // Eintrag abrufen (Basis)
  FishCatalogueEntryTo getEntry(Long id, UserTo user);

  // FR-016: Genehmigen (Admin only)
  @Transactional
  ResultTo<FishCatalogueEntryTo> approveEntry(Long id, UserTo admin);

  // FR-017: Ablehnen (Admin only)
  @Transactional
  ResultTo<FishCatalogueEntryTo> rejectEntry(Long id, String reason, UserTo admin);

  // FR-021: Pending-Liste (Admin only)
  List<FishCatalogueEntryTo> listPendingProposals(UserTo admin);

  // FR-015: Duplikat-Prüfung (non-blocking warning)
  boolean isDuplicateScientificName(String scientificName);
}
```

### 6.4 Neue Exception/Message-Code-Enums

**`FishStockExceptionCodes.java`**:
```
FISH_NOT_FOUND
FISH_NOT_YOURS
FISH_HAS_DEPARTURE_RECORD    // FR-024
FISH_PHOTO_TOO_LARGE         // FR-008
FISH_PHOTO_INVALID_FORMAT    // FR-008
AQUARIUM_NOT_YOURS
```

**`FishStockMessageCodes.java`**:
```
FISH_CREATED
FISH_UPDATED
FISH_DEPARTURE_RECORDED
FISH_DELETED
FISH_PHOTO_UPLOADED
CATALOGUE_LINK_REMOVED
```

**`FishCatalogueExceptionCodes.java`**:
```
CATALOGUE_ENTRY_NOT_FOUND
CATALOGUE_ENTRY_NOT_YOURS
CATALOGUE_REJECTED_READ_ONLY  // FR-019
CATALOGUE_ADMIN_REQUIRED      // FR-016/017
```

**`FishCatalogueMessageCodes.java`**:
```
CATALOGUE_ENTRY_PROPOSED
CATALOGUE_ENTRY_APPROVED
CATALOGUE_ENTRY_REJECTED
CATALOGUE_DUPLICATE_WARNING   // non-blocking, FR-015
CATALOGUE_ENTRY_UPDATED
```

---

## 7. REST-API Design (sabi-server)

Alle Endpunkte unter `api/` (C-5). Auth: JWT Bearer Token (FR-023).

### 7.1 `FishStockController` (`/api/fish`)

| Method | Path | Request Body | Response | FR |
|--------|------|--------------|----------|----|
| `GET`  | `/api/fish/{aquariumId}/list` | — | `List<FishStockEntryTo>` (202) | FR-007, FR-011 |
| `POST` | `/api/fish` | `FishStockEntryTo` | `ResultTo<FishStockEntryTo>` (201) | FR-002, FR-003 |
| `GET`  | `/api/fish/{fishId}` | — | `FishStockEntryTo` (202) | FR-011 |
| `PUT`  | `/api/fish/{fishId}` | `FishStockEntryTo` | `ResultTo<FishStockEntryTo>` (202) | FR-004 |
| `DELETE` | `/api/fish/{fishId}` | — | `204` oder `409` (Conflict) | FR-024 |
| `PUT`  | `/api/fish/{fishId}/departure` | `FishDepartureRecordTo` | `ResultTo<FishStockEntryTo>` (202) | FR-005, FR-006 |
| `DELETE` | `/api/fish/{fishId}/catalogue-link` | — | `FishStockEntryTo` (202) | FR-010 |
| `POST` | `/api/fish/{fishId}/photo` | `multipart/form-data` (file) | `204` | FR-008 |
| `GET`  | `/api/fish/{fishId}/photo` | — | `byte[]` + Content-Type-Header (200) | FR-025 |
| `DELETE` | `/api/fish/{fishId}/photo` | — | `204` | FR-004 |

**Sicherheit**: Jeder Endpunkt erfordert gültiges JWT. Ownership wird im Service geprüft → HTTP 403 wenn nicht Eigentümer.

**Foto-Response** (`GET /api/fish/{fishId}/photo`):
```
ResponseEntity<byte[]>
  .header("Content-Type", photoEntity.getContentType())
  .header("Content-Disposition", "inline; filename=\"fish-{fishId}\"")
  .header("Cache-Control", "private, max-age=3600")
```

### 7.2 `FishCatalogueController` (`/api/fish/catalogue`)

| Method | Path | Request Body | Response | FR |
|--------|------|--------------|----------|----|
| `GET`  | `/api/fish/catalogue/search?q={term}&lang={lang}` | — | `List<FishCatalogueSearchResultTo>` (202) | FR-020 |
| `POST` | `/api/fish/catalogue` | `FishCatalogueEntryTo` | `ResultTo<FishCatalogueEntryTo>` (201) | FR-014 |
| `GET`  | `/api/fish/catalogue/{id}` | — | `FishCatalogueEntryTo` (202) | — |
| `PUT`  | `/api/fish/catalogue/{id}` | `FishCatalogueEntryTo` | `ResultTo<FishCatalogueEntryTo>` (202) | FR-018, FR-019 |

**Suchlogik** (`search`):
- `q` muss ≥ 2 Zeichen haben (sonst 400)
- JPQL-Query: Suche auf `fish_catalogue_i18n.common_name LIKE '%q%'` ODER `fish_catalogue.scientific_name LIKE '%q%'`
- Sichtbarkeit: `status = 'PUBLIC'` ODER (`status = 'PENDING'` UND `proposer_user_id = :currentUserId`)

### 7.3 `FishCatalogueAdminController` (`/api/admin/fish/catalogue`)

| Method | Path | Request Body | Response | FR |
|--------|------|--------------|----------|----|
| `GET`  | `/api/admin/fish/catalogue/pending` | — | `List<FishCatalogueEntryTo>` (202) | FR-021 |
| `PUT`  | `/api/admin/fish/catalogue/{id}/approve` | Optional `FishCatalogueEntryTo` (mit Edits) | `ResultTo<FishCatalogueEntryTo>` (202) | FR-016 |
| `PUT`  | `/api/admin/fish/catalogue/{id}/reject` | `{ "reason": "..." }` | `ResultTo<FishCatalogueEntryTo>` (202) | FR-017 |

**Spring Security**: `/api/admin/**` in `WebSecurityConfig` auf Rolle `ADMIN` einschränken.

### 7.4 Sequenzdiagramme

#### A: Fisch hinzufügen mit Katalog-Link

```
Benutzer → FishStockController.addFish(entry)
  → FishStockService.addFishToTank(entry, user)
    → AquariumRepository.findById(entry.aquariumId) → Ownership-Check
    → [entry.fishCatalogueId != null]
        → FishCatalogueRepository.findVisibleEntry(id, userId) → entry
        → catalogueEntry.i18nEntries: resolve(scientificName, refUrl für lang)
        → entry.scientificName = snapshot
        → entry.externalRefUrl = snapshot (wenn noch leer)
    → TankFishStockRepository.save(entity)
  ← ResultTo<FishStockEntryTo>(CREATED)
← 201 Created
```

#### B: Katalog-Proposal-Genehmigungsflow

```
Nutzer → FishCatalogueController.propose(entryTo)
  → FishCatalogueService.proposeEntry(entryTo, user)
    → isDuplicateScientificName(name) → falls true: Warning-Message beilegen (non-blocking)
    → entry.status = PENDING, entry.proposerUserId = user.id
    → FishCatalogueRepository.save(entry)
  ← ResultTo (evtl. mit WARNING CATALOGUE_DUPLICATE_WARNING)
← 201 Created

Admin → FishCatalogueAdminController.approve(id, editedEntryTo)
  → FishCatalogueService.approveEntry(id, editedEntryTo, admin)
    → entry laden, status = PUBLIC
    → editedEntryTo: falls Felder geändert → merge
    → FishCatalogueRepository.save(entry)
  ← ResultTo(CATALOGUE_ENTRY_APPROVED)
← 202 OK
```

#### C: Foto-Upload & -Abruf

```
Upload:
Nutzer → POST /api/fish/{id}/photo (multipart/form-data)
  → FishStockController.uploadPhoto(fishId, file, principal)
    → FishStockService.uploadPhoto(fishId, file.bytes, contentType, size, user)
      → Magic-Bytes-Validierung → falls ungültig: 400 FISH_PHOTO_INVALID_FORMAT
      → Größe > 5 MB → falls ja: 400 FISH_PHOTO_TOO_LARGE
      → Schreibe Bytes nach {storage-dir}/{userId}/{fishId}.{ext}
      → FishPhotoRepository.upsert(fishId, path, contentType, size)
    ← ResultTo(FISH_PHOTO_UPLOADED)
← 204 No Content

Abruf:
Nutzer → GET /api/fish/{id}/photo
  → FishStockController.getPhoto(fishId, principal)
    → FishStockService.getPhotoBytes(fishId, user)
      → Ownership-Check (FR-025)
      → FishPhotoRepository.findByFishId(fishId) → metadata
      → Lese Bytes von {storage-dir}/{metadata.filePath}
    ← byte[]
  ← ResponseEntity<byte[]> mit Content-Type
```

---

## 8. Frontend: Vaadin Views (sabi-webclient)

Paket: `de.bluewhale.sabi.webclient`

### 8.1 Neue API-Gateway-Schicht

**`FishStockService.java`** (Interface):
```
List<FishStockEntryTo> getFishForTank(Long aquariumId, String token, Locale locale)
ResultTo<FishStockEntryTo> addFish(FishStockEntryTo entry, String token)
ResultTo<FishStockEntryTo> updateFish(FishStockEntryTo entry, String token)
ResultTo<FishStockEntryTo> recordDeparture(Long fishId, FishDepartureRecordTo record, String token)
boolean deleteFish(Long fishId, String token)
ResultTo<FishStockEntryTo> removeCatalogueLink(Long fishId, String token)
void uploadPhoto(Long fishId, byte[] bytes, String contentType, String token)
byte[] getPhoto(Long fishId, String token)
```

**`FishStockServiceImpl.java`**: REST-Aufrufe an Backend über `RestHelper` (analog zu `TankServiceImpl`).

**`FishCatalogueService.java`** (Interface):
```
List<FishCatalogueSearchResultTo> search(String query, String lang, String token)
ResultTo<FishCatalogueEntryTo> propose(FishCatalogueEntryTo entry, String token)
ResultTo<FishCatalogueEntryTo> updateEntry(FishCatalogueEntryTo entry, String token)
```

**`FishCatalogueAdminService.java`** (Interface):
```
List<FishCatalogueEntryTo> getPendingProposals(String token)
ResultTo<FishCatalogueEntryTo> approveEntry(Long id, FishCatalogueEntryTo edits, String token)
ResultTo<FishCatalogueEntryTo> rejectEntry(Long id, String reason, String token)
```

### 8.2 Neue Views und Controller

#### `FishStockView.java`

```
@Named @ViewScoped @Slf4j          // JSF CDI-Bean (jakarta.faces.view.ViewScoped)
Controller für: fishStockTab.xhtml (eingebettet in tankDetailView.xhtml via <ui:include>)

State:
  List<FishStockEntryTo> activeFish   // ohne departure
  List<FishStockEntryTo> departedFish // mit departure
  FishStockEntryTo selectedFish
  boolean departedSectionExpanded = false

@PostConstruct init():
  fishList = fishStockService.getFishForTank(userSession.selectedAquariumId, token)
  activeFish = fishList.stream().filter(f -> f.exodusOn == null)...
  departedFish = fishList.stream().filter(f -> f.exodusOn != null)...

Aktionen:
  onAddFish()           → FishStockEntryForm-Dialog öffnen
  onEditFish(fish)      → FishStockEntryForm-Dialog mit fish füllen
  onRecordDeparture(fish) → FishDepartureDialog öffnen
  onDeleteFish(fish)    → Bestätigungsdialog; Fehlerbehandlung für FISH_HAS_DEPARTURE_RECORD
  onRemoveCatalogueLink(fish) → Direct-Call, Liste aktualisieren
```

#### `FishStockEntryForm.java` (Dialog/Partial)

```
State: FishStockEntryTo currentEntry, boolean isEdit

Felder:
  commonName (h:inputText, required)
  addedOn    (p:datePicker, required, past+present validation)
  nickname   (h:inputText, optional)
  externalRefUrl (h:inputText, optional, URL-Validator)
  observedBehavior (h:inputTextarea, optional)
  catalogueSearch  (p:autoComplete mit min 2 Zeichen)
    → onSearchCatalogue(query): ruft FishCatalogueService.search()
    → onSelectCatalogue(entry): auto-fills scientificName + externalRefUrl
  scientificName  (h:inputText, auto-gefüllt oder manuell)
  photoUpload (p:fileUpload, maxSize=5MB, accept=image/jpeg,image/png,image/webp,image/gif)
    → onPhotoUpload(event): validiert, hochgeladen, Vorschau anzeigen
  Wenn kein Katalogeintrag gefunden: Link "Neuen Eintrag vorschlagen" → FishCatalogueProposalForm

Validierung client-seitig:
  addedOn darf nicht in der Zukunft liegen (FR-003)
  Dateigröße ≤ 5 MB (FR-008)
```

#### `FishDepartureDialog.java`

```
State: Long fishId, FishDepartureRecordTo departureRecord

Felder:
  departureDate  (p:datePicker, required)
  departureReason (p:selectOneMenu: DECEASED | REMOVED_REHOMED | UNKNOWN, required)

Validierung:
  departureDate ≥ fish.addedOn (FR-006)
```

#### `FishCatalogueProposalForm.java`

```
State: FishCatalogueEntryTo proposal, boolean duplicateWarningShown

Felder:
  scientificName (required)
  Sprach-Tabs (DE, EN, ES, FR, IT):
    commonName, description (maxlength=2000), referenceUrl

Logik:
  onScientificNameBlur() → Backend-Call isDuplicate → falls true: Warning-Panel anzeigen
  onSubmit() → FishCatalogueService.propose()
```

#### `FishCatalogueAdminView.java` (Admin-Only)

```
@Named @ViewScoped
Route: /admin/fish-catalogue

State: List<FishCatalogueEntryTo> pendingProposals

@PostConstruct: lädt pendingProposals sortiert nach proposalDate ASC

DataTable: scientific_name | proposalDate | proposer (anonymisiert) | [Öffnen]

Detail-Dialog:
  Vollständige FishCatalogueEntryTo anzeigen + alle i18n-Einträge
  Edits-Felder direkt bearbeitbar
  [Genehmigen]-Button → onApprove(): Admin-Service aufrufen
  [Ablehnen]-Button → Rejection-Reason eingeben → onReject()
```

### 8.3 Navigation & Routing

- Aquarium-Detailseite (`tankDetailView.xhtml`) bekommt neuen **Tab "Fischbestand"** neben bestehenden Tabs (Messungen, Seuchen)
- Navigation: `TankListView` → Tank auswählen → `tankDetailView.xhtml?tab=fishstock`
- Neuer Menüpunkt im Admin-Bereich: "Katalog-Verwaltung" → `fishCatalogueAdminView.xhtml`

---

## 9. i18n-Keys (alle 6 Bundle-Dateien)

Alle Keys müssen in `messages.properties` (Fallback/EN), `messages_de.properties`, `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`, `messages_it.properties` vorhanden sein.

| Key | DE | EN |
|-----|----|----|
| `fishstock.tab.label` | Fischbestand | Fish Stock |
| `fishstock.add.button` | Fisch hinzuf\u00fcgen | Add Fish |
| `fishstock.current.section` | Aktuell im Becken | Currently in Tank |
| `fishstock.departed.section` | Abg\u00e4nge | Departed Fish |
| `fishstock.empty.hint` | Noch keine Fische erfasst. Klicke auf "Fisch hinzuf\u00fcgen" um zu beginnen. | No fish recorded yet. Click "Add Fish" to start. |
| `fishstock.form.commonname.label` | Artname | Species Name |
| `fishstock.form.commonname.required` | Artname ist erforderlich | Species name is required |
| `fishstock.form.entrydate.label` | Einzugsdatum | Entry Date |
| `fishstock.form.entrydate.required` | Einzugsdatum ist erforderlich | Entry date is required |
| `fishstock.form.entrydate.future.error` | Das Einzugsdatum darf nicht in der Zukunft liegen | Entry date must not be in the future |
| `fishstock.form.nickname.label` | Spitzname (optional) | Nickname (optional) |
| `fishstock.form.notes.label` | Beobachtungsnotizen | Behaviour Notes |
| `fishstock.form.refurl.label` | Externe Referenz-URL | External Reference URL |
| `fishstock.form.photo.label` | Foto (optional) | Photo (optional) |
| `fishstock.form.photo.upload.button` | Foto hochladen | Upload Photo |
| `fishstock.form.photo.size.error` | Das Foto \u00fcberschreitet 5 MB. Bitte ein kleineres Bild w\u00e4hlen. | Photo exceeds 5 MB. Please choose a smaller image. |
| `fishstock.form.photo.format.error` | Nur JPEG, PNG, WebP und GIF sind erlaubt. | Only JPEG, PNG, WebP, and GIF are allowed. |
| `fishstock.form.catalogue.link.label` | Katalogeintrag verkn\u00fcpfen | Link Catalogue Entry |
| `fishstock.form.catalogue.unlink.button` | Katalog-Link entfernen | Remove Catalogue Link |
| `fishstock.form.catalogue.search.placeholder` | Katalog durchsuchen\u2026 (mind. 2 Zeichen) | Search catalogue\u2026 (min. 2 chars) |
| `fishstock.form.catalogue.noresults` | Keine Katalogeintr\u00e4ge gefunden. | No catalogue entries found. |
| `fishstock.form.catalogue.propose.link` | Neuen Katalogeintrag vorschlagen | Propose new catalogue entry |
| `fishstock.form.scientificname.label` | Wissenschaftlicher Name (aus Katalog) | Scientific Name (from catalogue) |
| `fishstock.departure.title` | Abgang erfassen | Record Departure |
| `fishstock.departure.date.label` | Abgangsdatum | Departure Date |
| `fishstock.departure.date.after.error` | Das Abgangsdatum darf nicht vor dem Einzugsdatum liegen | Departure date must not be before the entry date |
| `fishstock.departure.reason.label` | Abgangsgrund | Departure Reason |
| `fishstock.departure.reason.deceased` | Gestorben | Deceased |
| `fishstock.departure.reason.removed` | Abgegeben / Umgesetzt | Removed / Rehomed |
| `fishstock.departure.reason.unknown` | Unbekannt | Unknown |
| `fishstock.delete.departure.blocked` | Dieser Fischeintrag hat einen Abgangsrekord und kann nicht gel\u00f6scht werden. | This fish entry has a departure record and cannot be deleted. |
| `fishcatalogue.scientificname.label` | Wissenschaftlicher Name | Scientific Name |
| `fishcatalogue.scientificname.required` | Wissenschaftlicher Name ist erforderlich | Scientific name is required |
| `fishcatalogue.scientificname.duplicate.warning` | Ein Katalogeintrag f\u00fcr \u201e{0}\u201c existiert bereits (Status: Ausstehend/\u00d6ffentlich). Sie k\u00f6nnen den Eintrag trotzdem speichern. | A catalogue entry for "{0}" already exists (Status: Pending/Public). You may still save. |
| `fishcatalogue.status.pending` | Ausstehend | Pending |
| `fishcatalogue.status.public` | \u00d6ffentlich | Public |
| `fishcatalogue.status.rejected` | Abgelehnt | Rejected |
| `fishcatalogue.propose.title` | Neuen Katalogeintrag vorschlagen | Propose New Catalogue Entry |
| `fishcatalogue.tab.de` | Deutsch | German |
| `fishcatalogue.tab.en` | Englisch | English |
| `fishcatalogue.tab.es` | Spanisch | Spanish |
| `fishcatalogue.tab.fr` | Franz\u00f6sisch | French |
| `fishcatalogue.tab.it` | Italienisch | Italian |
| `fishcatalogue.i18n.commonname.label` | Allgemeinname | Common Name |
| `fishcatalogue.i18n.description.label` | Beschreibung | Description |
| `fishcatalogue.i18n.description.maxlength.error` | Die Beschreibung darf maximal 2000 Zeichen lang sein | Description must not exceed 2000 characters |
| `fishcatalogue.i18n.refurl.label` | Referenz-URL | Reference URL |
| `fishcatalogue.admin.title` | Katalog-Verwaltung | Catalogue Administration |
| `fishcatalogue.admin.pending.list.title` | Ausstehende Vorschl\u00e4ge | Pending Proposals |
| `fishcatalogue.admin.approve.button` | Genehmigen | Approve |
| `fishcatalogue.admin.reject.button` | Ablehnen | Reject |
| `fishcatalogue.admin.proposer.label` | Vorschlagender | Proposer |
| `fishcatalogue.admin.submissiondate.label` | Eingangsdatum | Submission Date |
| `fishcatalogue.admin.rejection.reason.label` | Ablehnungsgrund (optional) | Rejection Reason (optional) |

**Gesamt: 52 neue i18n-Keys** in je 6 Dateien = 312 Einträge.

> **WCAG-Hinweis**: Alle neuen UI-Elemente müssen WCAG 2.1 AA einhalten. Statuslabel-Farben:
> - PENDING: Hintergrund `#fef3c7` (Amber-100), Text `#92400e` (~7:1) — nie helles Gelb auf Weiß
> - PUBLIC: Hintergrund `#d1fae5` (Green-100), Text `#065f46` (~9:1)
> - REJECTED: Hintergrund `#fee2e2` (Red-100), Text `#991b1b` (~6.5:1)

---

## 10. Foto-Speicher-Strategie

### Konfiguration

**`sabi-server/src/main/resources/application.yml`** (neuer Eintrag):
```yaml
sabi:
  fish:
    photo:
      storage-dir: /var/sabi-data/fish-photos   # prod default
      max-size-bytes: 5242880                    # 5 MB
```

**`PhotoStorageService.java`** (neues Utility-Bean in `sabi-server`):
```
@Service
@ConfigurationProperties("sabi.fish.photo")
interface PhotoStorageService {
  Path store(Long userId, Long fishId, byte[] bytes, String contentType) throws IOException
  byte[] load(String relativePath) throws IOException
  void delete(String relativePath) throws IOException
  String resolveExtension(String contentType)
}
```

Datei-Pfad-Schema: `{storage-dir}/{userId}/{fishId}.{jpg|png|webp|gif}`

### Docker Compose

`devops/sabi_docker_sdk/docker-compose.yml` und `docker-compose-arm.yml`:
```yaml
services:
  sabi-backend:
    volumes:
      - sabi-fish-photos:/var/sabi-data/fish-photos
    environment:
      SABI_FISH_PHOTO_STORAGE_DIR: /var/sabi-data/fish-photos

volumes:
  sabi-fish-photos:
```

### Ansible

`devops/ansible/deploySabiService.yml`:
```yaml
- name: Create fish photo storage directory
  file:
    path: "{{ sabi_fish_photo_dir | default('/var/sabi-data/fish-photos') }}"
    state: directory
    owner: sabi
    mode: '0750'
```

---

## 11. Test-Strategie

Mindestens je ein Integrationstest für User Stories P1–P5 (NFR: Wartbarkeit).

### 11.1 Repository-Tests (JPA / TestContainers)

**`TankFishStockRepositoryTest.java`**:
- `findFishEntitiesByAquariumId_returnsOnlyActiveEntries` (Soft-Delete-Filter via `@SQLRestriction`)
- `findByAquariumId_excludesSoftDeleted`
- `findUsersFish_returnsNullForOtherUsersEntry` (FR-011)

**`FishCatalogueRepositoryTest.java`**:
- `search_returnsPublicAndOwnPendingEntries`
- `search_doesNotReturnOtherUsersPendingEntries` (SC-009)
- `search_partialMatchOnScientificName`
- `search_partialMatchOnI18nCommonName`
- `uniqueConstraint_acceptsRejectedDuplicateName` (FR-012)

### 11.2 Service-Unit-Tests (Mockito)

**`FishStockServiceTest.java`** (P1-P2):
- `addFishToTank_withCatalogueLink_copiesScientificNameSnapshot` (FR-009)
- `addFishToTank_withoutCatalogueLink_succeeds` (US1)
- `recordDeparture_validatesDateNotBeforeEntryDate` (FR-006)
- `deletePhysically_withDepartureRecord_returnsDepartureBlockedError` (FR-024)
- `deletePhysically_withoutDepartureRecord_succeeds` (FR-024)
- `uploadPhoto_exceeds5MB_returnsError` (FR-008)
- `uploadPhoto_invalidMagicBytes_returnsFormatError` (FR-008)

**`FishCatalogueServiceTest.java`** (P4-P5):
- `proposeEntry_duplicateScientificName_returnsWarningButProceed` (FR-015)
- `proposeEntry_rejectedDuplicateName_noWarning` (FR-015)
- `approveEntry_setStatusPublic` (FR-016)
- `rejectEntry_setStatusRejected_invisibleToAll` (FR-017)
- `updateEntry_rejectedStatus_throws` (FR-019)

### 11.3 Controller-Integrationstests (MockMvc + TestContainers)

**`FishStockControllerTest.java`**:
- `addFish_validEntry_returns201` (US1)
- `addFish_missingCommonName_returns400` (FR-003)
- `addFish_unauthenticated_returns401` (FR-023)
- `deleteFish_withDeparture_returns409` (FR-024)
- `getPhoto_otherUsersPhoto_returns403` (FR-025, FR-011)

**`FishCatalogueControllerTest.java`**:
- `search_minTwoChars_requiredElse400` (FR-020)
- `proposeEntry_authenticated_returns201` (FR-014)

**`FishCatalogueAdminControllerTest.java`**:
- `listPending_nonAdmin_returns403` (FR-021)
- `approve_validProposal_entryBecomesPublic` (FR-016)

### 11.4 Performance-Test-Hinweis

SC-003 (Katalogsuche ≤ 1 Sek. bei 500 Einträgen):
- Testdaten-Factory: 500 `FishCatalogueEntryEntity` + je 5 i18n-Einträge anlegen
- Query-Zeit per `@StopWatch` oder JPA-Statistik messen
- Assertion: `durationMs < 1000`

---

## 12. Implementierungs-Phasen (geordnet)

### Phase 1: Datenbankschicht & Entities (sabi-database + sabi-server)
> Unabhängig testbar; keine UI nötig

1. `sabi-database/src/main/resources/db/migration/version1_5_0/` anlegen
2. Migrations V1_5_0_1 bis V1_5_0_5 erstellen
3. `TankFishStockEntity` aus `FishEntity` refaktorieren (neue Felder, `@SQLRestriction`)
4. `FishCatalogueEntryEntity` erweitern (Status, Proposer, i18n-Assoziation)
5. `FishCatalogueI18nEntity` neu erstellen
6. `FishPhotoEntity` neu erstellen
7. Neue Repositories: `TankFishStockRepository`, `FishCatalogueI18nRepository`, `FishPhotoRepository`
8. `FishCatalogueRepository` erweitern (Search-Queries, Pending-Liste)

**Gate**: Repository-Tests laufen durch (TestContainers)

### Phase 2: DTOs, Enums & Mapper (sabi-boundary + sabi-server)
> Abhängig von Phase 1

1. Enums `DepartureReason`, `FishCatalogueStatus` in sabi-boundary
2. `FishStockEntryTo`, `FishCatalogueEntryTo`, `FishCatalogueI18nTo`, `FishCatalogueSearchResultTo`, `FishDepartureRecordTo` erstellen
3. `Endpoint.java` um neue Endpunkte erweitern
4. `FishStockMapper`, `FishCatalogueMapper` (MapStruct)
5. Alte `FishTo` / `FishCatalogueTo` als `@Deprecated` markieren

**Gate**: Keine Kompilierfehler; MapStruct-Generierung erfolgreich

### Phase 3: Service-Schicht (sabi-server)
> Abhängig von Phase 1 + 2

1. `FishStockExceptionCodes`, `FishStockMessageCodes`, `FishCatalogueExceptionCodes`, `FishCatalogueMessageCodes` erstellen
2. `PhotoStorageService` implementieren (Filesystem I/O, Magic-Bytes-Prüfung)
3. `FishStockService` Interface + `FishStockServiceImpl` implementieren
4. `FishCatalogueService` Interface + `FishCatalogueServiceImpl` implementieren
5. Existierenden `FishService` / `FishServiceImpl` auf `@Deprecated` setzen; Delegation an `FishStockService`

**Gate**: Service-Unit-Tests (Mockito) bestehen

### Phase 4: REST-Controller (sabi-server)
> Abhängig von Phase 3

1. `FishStockController` (`/api/fish`) implementieren
2. `FishCatalogueController` (`/api/fish/catalogue`) implementieren
3. `FishCatalogueAdminController` (`/api/admin/fish/catalogue`) implementieren
4. `WebSecurityConfig` um `/api/admin/**` → Rolle ADMIN erweitern
5. `application.yml` um `sabi.fish.photo.*` Properties erweitern

**Gate**: MockMvc-Integrationstests bestehen

### Phase 5: i18n-Bundle-Befüllung
> Kann parallel zu Phase 4 erfolgen

1. 52 Keys in alle 6 Bundle-Dateien eintragen
2. ES, FR, IT: Übersetzungen via maschinengestützter Erst-Übersetzung + manuelle Review
3. Byte-Check-Skript ausführen: nur `\uXXXX`-Escapes, keine Latin-1-Bytes

**Gate**: Vollständigkeitsprüfung (kein Key fehlt in einer Datei)

### Phase 6: Vaadin Frontend (sabi-webclient)
> Abhängig von Phase 4 + 5

1. `FishStockService` Interface + `FishStockServiceImpl` (API-Gateway)
2. `FishCatalogueService` Interface + `FishCatalogueServiceImpl` (API-Gateway)
3. `FishCatalogueAdminService` Interface + Impl
4. `FishStockView` + `fishStockTab.xhtml` (Tab auf Aquarium-Detailseite)
5. `FishStockEntryForm` Dialog
6. `FishDepartureDialog`
7. Aquarium-Detailseite um Fish-Stock-Tab erweitern (`tankDetailView.xhtml`)
8. `FishCatalogueProposalForm`
9. `FishCatalogueAdminView` + `fishCatalogueAdminView.xhtml`
10. Menü-Eintrag "Katalog-Verwaltung" für Admin-Rolle
11. WCAG-Kontrast-Check für alle neuen Farben

**Gate**: Manuelle Smoke-Tests für US1–US5

### Phase 7: Docker & Ansible
> Abhängig von Phase 4

1. `docker-compose.yml` + `docker-compose-arm.yml`: Volume für Fish-Photos
2. `deploySabiService.yml` Ansible: Verzeichnis anlegen, Berechtigungen

**Gate**: Lokaler Docker-Compose-Test; Photo-Upload Ende-zu-Ende

---

## Anhang: Bekannte Risiken & offene Punkte

| # | Risiko / offener Punkt | Empfehlung |
|---|---|---|
| R-1 | MySQL Virtual Columns erfordern MySQL ≥ 5.7.6 | Produktionsversion verifizieren; Notfallplan: App-Layer-Unique-Check ohne DB-Unique-Index |
| R-2 | Bestehende `fish`-Einträge haben `fish_catalogue_id NOT NULL`; Datenmigration könnte Constraint verletzen | V1_5_0_1 setzt `fish_catalogue_id = NULL` erst nach ALTER TABLE MODIFY |
| R-3 | `FishEntity.addedOn` ist derzeit `Timestamp` (Entity) / `LocalDate` (DTO); Typ-Konvertierung im Mapper | MappingUtils.TimestampToLocalDate existiert bereits; auf `LocalDate` vereinheitlichen |
| R-4 | `@SQLRestriction` ist Spring Boot 3.x / Hibernate 6; verifizieren, dass keine Legacy-`@Where` mehr existiert | Architekturtest in `ArchitectureTest.java` ergänzen |
| R-5 | Aquarium-Soft-Delete-Kaskade für Fish-Einträge — existiert noch kein Soft-Delete auf Aquarium | Edge Case aus Spec; `TankService.deleteTank()` muss erweitert werden: alle Fish-Einträge des Aquariums auf `deleted_at = NOW()` setzen |
| R-6 | Photo-Filesystem-Pfad bei ARM64-Docker-Builds | Volume-Mount explizit testen; `sabi.fish.photo.storage-dir` als Env-Variable exponieren |
| R-7 | Katalog-Suche mit `LIKE '%q%'` schlägt fehl bei Umlauten in MySQL utf8 Collation | `utf8_unicode_ci` Collation auf `fish_catalogue_i18n.common_name` sicherstellen |

---

*Plan erstellt für Branch `002-fish-stock-catalogue`. Nächster Schritt: Phase 1 starten.*



