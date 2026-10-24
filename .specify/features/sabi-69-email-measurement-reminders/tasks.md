# Tasks: Email Measurement Reminders (sabi-69)

**Feature ID**: sabi-69 | **Branch**: `feature/069-email-measurement-reminders`
**Generated**: 2026-10-24 | **Based on**: spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md
**Stack**: Spring Boot 4.1.1, Java 25, Spring Mail, JSF/PrimeFaces, MariaDB, EclipseLink JPA, Flyway

> **Hinweis Migration-Nummer**: plan.md/quickstart.md benennen die Migration
> `V1_8_0_14__addEmailReminderColumns.sql`, im Repo ist aktuell
> `V1_8_0_9__addAiptasiaPlagueType.sql` die neueste. Flyway verlangt nur
> eindeutige Versionen — T002 prüft, ob V1_8_0_10..13 von anderen Branches belegt
> sind, und benennt ggf. auf die nächste freie Nummer um.

---

## Implementation Strategy

**MVP = Phase 3 komplett** (Backend-Pipeline voll funktionsfähig: Enable → Scheduler →
exakt eine lokalisierte E-Mail pro Zyklus). Danach Phase 4 (US-2 Intervall + Disable +
Webclient-UI), Phase 5 (US-3 SPI-Erweiterbarkeit), Phase 6 (Quality Gates).
Jede Phase ist unabhängig testbar.

**Dependency order** (darf NICHT übersprungen werden):
```
Phase 1 (Setup)
  └─► Phase 2 (Flyway + Entities + DTOs + Config)
        └─► Phase 3 (US-1: Scheduler + Service + Email → MVP)
              ├─► Phase 4 (US-2: Validierung + Webclient-UI)
              └─► Phase 5 (US-3: SPI-Erweiterbarkeit)
Phase 6 (Polish & Quality Gates) — erst wenn alle vorherigen Phasen grün sind
```

---

## Phase 1 — Setup

**Goal**: Prerequisites verifizieren; keine neuen Dependencies erwartet (`spring-boot-starter-mail` bereits vorhanden).
**Independent test**: `mvn package -pl sabi-boundary,sabi-server` baut ohne Fehler; fakeSMTP erreichbar auf `localhost:2525`.

- [ ] T001 [P] Prerequisites verifizieren (keine neuen Dependencies): SBOM gemäß AGENTS.md prüfen (`sabi-server/target/bom.json`, bei Stale neu generieren) und `spring-boot-starter-mail` bestätigen; fakeSMTP-Service (`gessnerfl/fake-smtp-server:2.5.0`, Port 2525) in `devops/sabi_docker_sdk/docker-compose.yml` und `spring.mail.*`-Defaults in `sabi-server/src/main/resources/application.yml` (Host `localhost`, Port `2525`) verifizieren
- [ ] T002 [P] Nächste freie Flyway-Version bestimmen: aktuell neueste Migration ist `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_9__addAiptasiaPlagueType.sql`; der Plan benennt `V1_8_0_14__addEmailReminderColumns.sql` — prüfen, ob V1_8_0_10..13 nicht von anderen Branches belegt sind; falls doch, Migration auf die nächste freie Nummer umbenennen (Flyway verlangt nur Eindeutigkeit)

---

## Phase 2 — Foundational

**Goal**: Datenbank-Schema, JPA-Entities, Boundary-DTOs und Konfiguration, auf die alle nachfolgenden Phasen aufbauen.
**Independent test**: Flyway-Migration läuft fehlerfrei auf lokalem Docker-MariaDB; `sabi-boundary` und `sabi-server` kompilieren.

- [ ] T003 Flyway-Migration anlegen: `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_14__addEmailReminderColumns.sql` — gemäß data-model.md §5: 3 neue Spalten in `users` (`email_reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE`, `email_reminder_interval_days INT NOT NULL DEFAULT 7`, `email_reminder_next_send_date DATE NULL`), Composite-Index `idx_users_email_reminder_due (email_reminder_enabled, email_reminder_next_send_date)`, neue Tabelle `reminder_delivery_record` (`id` PK AUTO_INCREMENT, `user_id` FK → `users.id` ON DELETE CASCADE, `cycle_date DATE NOT NULL`, `status VARCHAR(10) NOT NULL`, `attempts INT NOT NULL DEFAULT 0`, `sent_at DATETIME NULL`, Audit-Spalten) mit UNIQUE-Constraint `(user_id, cycle_date)`; **kein Schema-Prefix** (AGENTS.md); benötigt T002
- [ ] T004 [P] Neue JPA-Entity anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/ReminderDeliveryRecordEntity.java` — `@Entity`, `@Table(name = "reminder_delivery_record", schema = "sabi")`, erbt von `Auditable`; Felder: `Long id` (PK, autoincrement), `UserEntity user` (`@ManyToOne(fetch = LAZY)`, FK `user_id`), `LocalDate cycleDate`, `String status` (`SENT`/`FAILED`), `int attempts`, `LocalDateTime sentAt` gemäß data-model.md §2
- [ ] T005 `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/UserEntity.java` modifizieren — Felder `boolean emailReminderEnabled` (Spalte `email_reminder_enabled`), `Integer emailReminderIntervalDays` (Spalte `email_reminder_interval_days`, Default 7), `LocalDate emailReminderNextSendDate` (Spalte `email_reminder_next_send_date`, nullable) + Collection `List<ReminderDeliveryRecordEntity> reminderDeliveryRecords` mit `@OneToMany(fetch = LAZY, mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)`; Collection in `@EqualsAndHashCode(exclude = {...})` aufnehmen; benötigt T004
- [ ] T006 [P] `sabi-boundary/src/main/java/de/bluewhale/sabi/model/UserProfileTo.java` modifizieren — `boolean emailReminderEnabled = false` und `Integer emailReminderIntervalDays` (nullable; `null` = "unchanged/absent" für alte Clients) mit Gettern/Settern im bestehenden Stil ergänzen gemäß contracts/profile-api.md
- [ ] T007 Neues Repository anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/ReminderDeliveryRecordRepository.java` — `extends JpaRepository<ReminderDeliveryRecordEntity, Long>`; Finder `Optional<ReminderDeliveryRecordEntity> findByUserIdAndCycleDate(Long userId, LocalDate cycleDate)`; **zusätzlich** Due-User-Finder in `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/UserRepository.java` ergänzen: `List<UserEntity> findByEmailReminderEnabledTrueAndEmailReminderNextSendDateLessThanEqual(LocalDate runDate)` (bedient den täglichen Scheduler-Batch, Constitution II); benötigt T004, T005
- [ ] T008 [P] `sabi-server/src/main/resources/application.yml` modifizieren — unter der bestehenden `sabi:`-Sektion ergänzen: `reminder.scheduler-cron: "0 0 9 * * *"` (FR-003, Default 09:00 Server-Timezone) sowie `email.no-reply-address: ${SABI_EMAIL_NO_REPLY_ADDRESS:no-reply@sabi-project.net}` und `email.base-url: ${SABI_EMAIL_BASE_URL:https://sabi-project.net}` (R4, R14)
- [ ] T009 [P] `sabi-server/src/main/java/de/bluewhale/sabi/SabiServiceApplication.java` modifizieren — `@EnableScheduling` ergänzen (R1; in der Codebase noch nicht vorhanden)
- [ ] T010 [P] Neue Config-Klasse anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/configs/ClockConfig.java` — `@Bean Clock clock() { return Clock.systemDefaultZone(); }` für Injektion in Scheduler + Service (R9, Testbarkeit mit Fake-Clock)

**Checkpoint**: Foundation ready — User-Story-Implementierung kann beginnen.

---

## Phase 3 — US-1: Wöchentliche Email-Remerinder aktivieren und erhalten (P1) 🎯 MVP [Backend]

**User Story**: US-1 (Nutzer aktiviert Email-Remerinder im Profil und erhält pro Zyklus exakt eine lokalisierte E-Mail mit Overdue-Liste + Links)
**Goal**: Komplette Backend-Pipeline: Profil-Enablement setzt das nächste Sendedatum (State Machine), der tägliche Scheduler evaluiert fällige Nutzer, exakt eine lokalisierte E-Mail pro User + Zyklus wird versendet (idempotent, mit Retry-Semantik).
**Independent test**: T011–T014 grün (Fake-Clock, gestubbt `JavaMailSender`) — quickstart.md Scenario A.

### Tests für US-1 (zuerst schreiben, müssen vor Implementierung FAIL) ⚠️

- [ ] T011 [P] [US1] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/scheduler/ReminderSchedulerTest.java` — `@SpringBootTest` + `@Testcontainers` MariaDB (analog `UserServiceTest`), Fake-/Advancing-`Clock` injizieren, `JavaMailSender` stubben; Szenarien: (a) fälliger Nutzer mit Overdue-Units → exakt eine E-Mail, Record `SENT`, `nextSendDate = cycleDate + interval` (SC-002), (b) Scheduler-Lauf am selben Tag erneut → keine zweite E-Mail, unique `user_id + cycle_date` (SC-003), (c) SMTP-Fehler → Status `FAILED`, `attempts = 1`, `nextSendDate` **nicht** vorgerückt; nach 3 Attempts schließt sich der Zyklus und rückt vor (Spec-Edge-Case), (d) ein fehlschlagender Empfänger blockiert andere Nutzer nicht
- [ ] T012 [P] [US1] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/services/EmailReminderServiceImplTest.java` — State Machine: (a) Enable mit ≥ 1 Overdue-Unit → `nextSendDate = today` (FR-001, SC-001), (b) Enable ohne Overdue-Units → `today + interval`
- [ ] T013 [P] [US1] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/persistence/repositories/ReminderDeliveryRecordRepositoryTest.java` — `@Testcontainers` (analog `OidcProviderLinkRepositoryTest`): (a) Unique-Constraint `(user_id, cycle_date)` lehnt Duplikate ab, (b) Löschen des Users kaskadiert auf Delivery-Records (GDPR — quickstart.md Scenario D); Test-User via `UserRepository` in `@BeforeEach` anlegen (DEVELOPERS_MANUAL.md)
- [ ] T014 [P] [US1] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/services/NotificationServiceReminderEmailTest.java` — Email-Inhalt für alle 6 Locales (de/en/es/fr/it + Fallback): Subject, Greeting `{0}`, Intro, Tabellen-Header, Zeilen mit `lastValue`/`lastMeasuredOn`, "no measurement recorded yet"-Platzhalter für nie gemessene Units, beide Deep-Links; From = No-Reply-Adresse; multipart/alternative HTML + Text gemäß contracts/email-template.md

### Implementation für US-1

- [ ] T015 [P] [US1] Neue Server-i18n-Bundles anlegen: `sabi-server/src/main/resources/i18n/ReminderEmailMessages.properties` + `_de/_en/_es/_fr/_it` (6 Dateien) — 10 Keys gemäß contracts/email-template.md (`reminder.email.subject`, `.greeting`, `.intro`, `.unit.header`, `.value.header`, `.date.header`, `.no_measurement`, `.measurement_link_label`, `.profile_link_label`, `.footer`); Encoding gemäß AGENTS.md (UTF-8 / `\uXXXX`, Byte-Check nach dem Schreiben)
- [ ] T016 [P] [US1] Neues transientes DTO anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/services/MeasurementReminderEmail.java` — Java-Record gemäß data-model.md §3: `Long userId`, `String languageCode`, `List<OverdueUnit> overdueUnits` (nested Record: `String unitSign`, `Float lastValue` nullable, `LocalDateTime lastMeasuredOn` nullable), `String measurementPageUrl`, `String profilePageUrl`; **wird nicht persistiert**
- [ ] T017 [US1] Neuen Service anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/services/EmailReminderService.java` (Interface) + `sabi-server/src/main/java/de/bluewhale/sabi/services/EmailReminderServiceImpl.java` — (a) `applyProfileChange(UserEntity, boolean enabled, Integer intervalDays)` State Machine gemäß contracts/profile-api.md (Enable mit Overdue → `today`; Enable ohne → `today + interval`; Intervallwechsel bei aktiviert → `today + newInterval` (FR-009); Disable → `NULL` (FR-010)), (b) Overdue-Regel gemäß FR-008: aktive `UserMeasurementReminderEntity` + (letzte `MeasurementEntity.measuredOn` älter als `pastdays` **oder** keine Messung vorhanden); `Clock` injizieren; bestehende In-App-Reminder-Endpunkte **nicht** modifizieren (R10); benötigt T005, T016
- [ ] T018 [US1] `sabi-server/src/main/java/de/bluewhale/sabi/services/NotificationServiceImpl.java` modifizieren — `sendMeasurementReminderEmail(MeasurementReminderEmail payload, Locale locale)` ergänzen: `ResourceBundle.getBundle("i18n/ReminderEmailMessages", locale)` (analog `sendValidationMail`), `MimeMessageHelper(..., true)` multipart/alternative, From = `${sabi.email.no-reply-address}` (R4), To = verifizierte Email des Users, Datums-/Wertformatierung in der User-Locale; Log nur `user_id=` (PII-Regel); benötigt T015, T016
- [ ] T019 [P] [US1] Neue SPI + erste Implementierung anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/scheduler/ReminderContentProvider.java` — `String type()`, `Optional<MeasurementReminderEmail> buildContent(UserEntity user, LocalDate cycleDate)` (data-model.md §4, FR-011) + `sabi-server/src/main/java/de/bluewhale/sabi/scheduler/MeasurementReminderContentProvider.java` (`@Component`, Type `"MEASUREMENT"`) — delegiert an `EmailReminderService`; leerer Optional bei keinen Overdue-Units (R6); Links aus `sabi.email.base-url` + `/secured/measureView.xhtml` bzw. `/secured/userProfile.xhtml` (R14); benötigt T016, T017
- [ ] T020 [US1] Neuen Scheduler anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/scheduler/ReminderScheduler.java` — `@Component` mit `@Scheduled(cron = "${sabi.reminder.scheduler-cron}")`; fällige Nutzer via `UserRepository`-Finder laden (T007); injizierte `List<ReminderContentProvider>` iterieren; pro Nutzer in eigener Transaktion verarbeiten (R8): bestehender Record mit Status `SENT` → skip; leeres Content → kein Record, `nextSendDate = cycleDate + interval` vorrücken (R6); Send OK → Record `SENT` + `sentAt`, vorrücken; transientes `MailSendException` → `attempts += 1`, Status `FAILED`, **kein** Vorrücken; `attempts >= 3` oder permanente Ablehnung (Bounce) → Zyklus schließen + vorrücken (R7); Log nur `user_id=`; benötigt T018, T019, T007
- [ ] T021 [US1] `sabi-server/src/main/java/de/bluewhale/sabi/services/UserServiceImpl.java` modifizieren — in `updateProfile()` die Email-Reminder-State-Machine via `EmailReminderService` anwenden (Standard-Ownership-Pattern bereits vorhanden: Principal-Email → `UserEntity`); benötigt T017

**Checkpoint**: US-1 voll funktionsfähig und unabhängig testbar (MVP) — quickstart.md Scenario A grün.

---

## Phase 4 — US-2: Intervall anpassen und Remerinder abschalten (P2) [Backend-Validierung + Webclient-UI]

**User Story**: US-2 (Nutzer wählt eigenes Intervall 1–30 Tage und kann Remerinder jederzeit abschalten)
**Goal**: Server- + Client-Seiten-Intervallvalidierung (0/31 wird abgelehnt), Intervallwechsel rechnet von jetzt neu, Disable stoppt das Senden; neues "Email reminders"-Panel im Profil-UI (≤ 3 Klicks vom Login).
**Independent test**: T022/T023 grün + manueller UI-Check (quickstart.md Scenario B, Schritt 4).

### Tests für US-2 (zuerst schreiben, müssen vor Implementierung FAIL) ⚠️

- [ ] T022 [P] [US2] `sabi-server/src/test/java/de/bluewhale/sabi/services/EmailReminderServiceImplTest.java` erweitern: Intervall 0 und 31 → abgelehnt mit lokalisiertem Fehler; 1 und 30 → akzeptiert (FR-002, US-2 Szenario 3); benötigt T012
- [ ] T023 [P] [US2] `sabi-server/src/test/java/de/bluewhale/sabi/services/EmailReminderServiceImplTest.java` erweitern: Intervallwechsel bei aktiviert → `nextSendDate = today + newInterval` (FR-009); Disable → `nextSendDate = null` und Scheduler überspringt den Nutzer (FR-010, US-2 Szenario 2); benötigt T012

### Implementation für US-2

- [ ] T024 [US2] `sabi-server/src/main/java/de/bluewhale/sabi/services/UserServiceImpl.java` modifizieren — Server-Validierung in `updateProfile()`: `emailReminderEnabled == true` und Intervall außerhalb 1–30 → **gesamtes** Update über den bestehenden Fehlerkanal des Endpunkts (HTTP 409) mit lokalisiertem Message `userprofile.emailreminder.interval.error.range` verwerfen gemäß contracts/profile-api.md; benötigt T021
- [ ] T025 [P] [US2] Webclient-i18n — neue Keys in allen 6 Dateien `sabi-webclient/src/main/resources/i18n/messages{,_de,_en,_es,_fr,_it}.properties` ergänzen: `userprofile.emailreminder.header.h`, `.description.t`, `.toggle.l`, `.interval.l`, `.weekly.preset`, `.interval.error.range` + Save-Bestätigung (data-model.md §6, bestehende `userprofile.*`-Prefix-Konvention); Byte-Check gemäß AGENTS.md
- [ ] T026 [US2] `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/UserProfileView.java` modifizieren — neue Felder beim Seiten-Load in das View-Model laden und im bestehenden `save()`-PUT-Roundtrip übergeben (ein Endpunkt, kein neuer Endpoint gemäß contracts/profile-api.md); benötigt T006, T025
- [ ] T027 [US2] `sabi-webclient/src/main/resources/META-INF/resources/secured/userProfile.xhtml` modifizieren — neues "Email reminders"-Panel: `p:selectBooleanCheckbox`-Toggle + `p:inputNumber` mit `min="1" max="30"` (Client-Validierung) + "weekly"-Preset (= 7 Tage, FR-002); Labels via `#{msg[...]}`; Styling gemäß UI_StyleGuide.md (WCAG 2.1 AA, keine verbotenen Farb-Kombos); ≤ 3 Klicks vom Login erreichbar; benötigt T025, T026
- [ ] T028 [P] [US2] `sabi-webclient/src/main/resources/META-INF/resources/gdpr.xhtml` modifizieren + i18n-Keys in allen 6 Webclient-Bundles — FR-012 Disclosure-Absatz: Reminder-E-Mails enthalten eine Zusammenfassung der eigenen Messungen des Users

**Checkpoint**: US-1 und US-2 funktionieren beide unabhängig.

---

## Phase 5 — US-3: Wiederverwendbare Outbound-Email-Infrastruktur (P3)

**User Story**: US-3 (Application Owner — ein zweiter Reminder-Typ lässt sich ohne Scheduler-Umbau hinzufügen)
**Goal**: Ein zweiter `ReminderContentProvider`-Bean lässt sich registrieren (Template + Trigger nur) ohne Scheduler-Core oder Measurement-Reminder-Service zu modifizieren (FR-011, SC-006).
**Independent test**: T029 grün — zweiter Provider-Bean registriert, Scheduler nimmt ihn auf, Core-Klassen unverändert.

- [ ] T029 [US3] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/scheduler/ReminderSchedulerExtensibilityTest.java` — zweiten `ReminderContentProvider`-Test-Bean (Type `"MAINTENANCE"`, Stub-Content) im Spring-Kontext registrieren; asserten, dass der Scheduler ihn verarbeitet, ohne `ReminderScheduler` oder `MeasurementReminderContentProvider` zu modifizieren (SC-006, FR-011); benötigt T020
- [ ] T030 [P] [US3] SPI-Erweiterungspunkt dokumentieren — Javadoc an `ReminderContentProvider` + kurzer "How to add a new reminder type"-Hinweis (neuer Bean der das SPI implementiert + Template-Keys; keine Core-Änderungen)

**Checkpoint**: Alle User Stories sind unabhängig funktionsfähig.

---

## Phase 6 — Polish & Quality Gates (ISO 25010)

**Goal**: Alle 8 ISO-25010-Merkmale verifiziert; quickstart-Szenarien A–E grün; Merge-bereit.

- [ ] T031 [P] QGATE-I Funktionale Eignung: alle Akzeptanzszenarien aus spec.md auf grüne Tests mappen; SC-001..SC-006 abhaken
- [ ] T032 [P] QGATE-II Leistungseffizienz: Scheduler läuft off-request (keine messbare Latenz auf bestehenden Endpunkten, SC-004); `EXPLAIN` des Due-User-Queries → nutzt `idx_users_email_reminder_due`; Batch für 10k Nutzer < 5 min (Schätzung oder Load-Test)
- [ ] T033 [P] QGATE-III Kompatibilität: springdoc zeigt die zwei neuen optionalen Felder auf `PUT /api/userprofile`; alte Clients (Felder weggelassen) funktionieren weiterhin — abgedeckt durch bestehende Profil-Tests + T022/T023; keine Breaking Changes
- [ ] T034 [P] QGATE-IV Benutzbarkeit: i18n-Vollständigkeits-Check über alle 6 `ReminderEmailMessages*` + alle 6 Webclient-`messages*.properties`-Dateien (quickstart.md Scenario C, null fehlende Keys); WCAG 2.1 AA-Check am neuen Panel (Kontrast, Labels, Tastatur)
- [ ] T035 QGATE-V Zuverlässigkeit: komplette Regression `mvn -pl sabi-server test` grün (quickstart.md Scenario E); neues Package `de.bluewhale.sabi.scheduler` in `sabi-server/src/test/java/de/bluewhale/sabi/DeveloperTestSuite.java` registrieren (und `IntegrationTestSuite.java`, falls Scheduler-Tests mit Tag `IntegrationTest` laufen)
- [ ] T036 [P] QGATE-VI Sicherheit: PII-Audit — neue Code-Log-Statements greppen; nur `user_id=`, niemals Email-Adressen; keine SMTP-Credentials im Code (nur `SABI_SMTP_*` Env-Vars); Ownership-Pattern in `updateProfile` verifiziert
- [ ] T037 QGATE-VII Wartbarkeit: Line-Coverage ≥ 80 % auf `ReminderScheduler` + `EmailReminderServiceImpl`; Flyway-Migration verifiziert: `docker compose run --rm flyway` wendet die neue Version erfolgreich an; bestehende Migrations-Skripte unverändert
- [ ] T038 QGATE-VIII Übertragbarkeit: Docker-Build AMD64+ARM OK (`devops/sabi_docker_sdk/server_redeploy.sh`); keine Ansible-Playbook-Änderungen erforderlich
- [ ] T039 Quickstart-Validierung end-to-end ausführen: Scenario A (automatisierte Tests), B (manueller fakeSMTP-E2E via `http://localhost:2525`, inkl. Locale-Spot-Check), D (GDPR-Cascade auf Live-DB)

**Checkpoint**: Alle zutreffenden QGATE-Tasks abgehakt → Feature ist merge-bereit.

---

## Dependencies Summary

| Task | Requires |
|------|----------|
| T003 | T002 (Versionsnummer) |
| T004 | — (Schema gemäß data-model.md §2) |
| T005 | T004 |
| T006 | — |
| T007 | T004, T005 |
| T008 | — |
| T009 | — |
| T010 | — |
| T011 | T020, T007 (Scheduler + Due-User-Query) |
| T012 | T017 |
| T013 | T004, T007 |
| T014 | T018, T015 |
| T015 | — |
| T016 | — |
| T017 | T005, T016, T010 (Clock) |
| T018 | T015, T016, T008 (No-Reply-Property) |
| T019 | T016, T017, T008 (Base-URL-Property) |
| T020 | T017, T018, T019, T007, T009 |
| T021 | T017, T006 |
| T022 | T012, T024 |
| T023 | T012, T017 |
| T024 | T021 |
| T025 | — |
| T026 | T006, T025 |
| T027 | T025, T026 |
| T028 | — (nur i18n + xhtml) |
| T029 | T020 |
| T030 | T019 |
| T031–T034 | alle vorherigen Phasen (Phasen-Level) |
| T035 | T011–T014, T029 (Test-Registrierung) |
| T036 | T018, T020 |
| T037 | T003, T011–T014 |
| T038 | alle Code-Phasen |
| T039 | alles |

---

## Parallel Execution Opportunities

Die folgenden Gruppen können **gleichzeitig** von verschiedenen Entwicklern bearbeitet werden:

**Group A** (Phase 2, nach Phase 1):
- T004 + T006 + T008 + T009 + T010 (komplett unabhängig)
- T003 nach T002; T005 nach T004; T007 nach T004 + T005

**Group B** (Phase 3):
- T011–T014 (Tests, untereinander parallel)
- T015 + T016 (parallel)
- T017 nach T005/T016; T018 nach T015/T016; T019 nach T017; T020 nach T018 + T019

**Group C** (Phase 4 + Phase 5 parallel):
- T022 + T023 (Tests) und T025 (i18n) untereinander parallel
- T026 → T027 sequentiell; T028 parallel zu allem
- Phase 5 (T029, T030) kann parallel zu Phase 4 laufen — verschiedene Dateien

**Group D** (Phase 6, alle parallel):
- T031–T038 haben keine gegenseitigen Abhängigkeiten; T039 zuletzt

---

## Task Count Summary

| Phase | Name | Tasks | User Story |
|-------|------|-------|------------|
| 1 | Setup | 2 | — |
| 2 | Foundational | 8 | — |
| 3 | US-1 Backend (Enable + Receive, MVP) | 11 | US-1 |
| 4 | US-2 Backend-Validierung + Webclient-UI | 7 | US-2 |
| 5 | US-3 SPI-Erweiterbarkeit | 2 | US-3 |
| 6 | Polish & Quality Gates (ISO 25010) | 9 | — |
| **Total** | | **39** | |

**Parallel opportunities identified**: 4 Gruppen (A–D)
**Suggested MVP scope**: Phase 1 + Phase 2 + Phase 3 (Backend vollständig, testbar via `ReminderSchedulerTest` + REST-Client)

---

## File Change Reference

| File | Change | Phase | Task |
|------|--------|-------|------|
| `sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_14__addEmailReminderColumns.sql` | NEW | 2 | T003 |
| `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/ReminderDeliveryRecordEntity.java` | NEW | 2 | T004 |
| `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/UserEntity.java` | MOD | 2 | T005 |
| `sabi-boundary/src/main/java/de/bluewhale/sabi/model/UserProfileTo.java` | MOD | 2 | T006 |
| `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/ReminderDeliveryRecordRepository.java` | NEW | 2 | T007 |
| `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/UserRepository.java` | MOD | 2 | T007 |
| `sabi-server/src/main/resources/application.yml` | MOD | 2 | T008 |
| `sabi-server/src/main/java/de/bluewhale/sabi/SabiServiceApplication.java` | MOD | 2 | T009 |
| `sabi-server/src/main/java/de/bluewhale/sabi/configs/ClockConfig.java` | NEW | 2 | T010 |
| `sabi-server/src/test/java/de/bluewhale/sabi/scheduler/ReminderSchedulerTest.java` | NEW | 3 | T011 |
| `sabi-server/src/test/java/de/bluewhale/sabi/services/EmailReminderServiceImplTest.java` | NEW | 3+4 | T012, T022, T023 |
| `sabi-server/src/test/java/de/bluewhale/sabi/persistence/repositories/ReminderDeliveryRecordRepositoryTest.java` | NEW | 3 | T013 |
| `sabi-server/src/test/java/de/bluewhale/sabi/services/NotificationServiceReminderEmailTest.java` | NEW | 3 | T014 |
| `sabi-server/src/main/resources/i18n/ReminderEmailMessages*.properties` (6 Dateien) | NEW | 3 | T015 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/MeasurementReminderEmail.java` | NEW | 3 | T016 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/EmailReminderService.java` | NEW | 3 | T017 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/EmailReminderServiceImpl.java` | NEW | 3 | T017 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/NotificationServiceImpl.java` | MOD | 3 | T018 |
| `sabi-server/src/main/java/de/bluewhale/sabi/scheduler/ReminderContentProvider.java` | NEW | 3 | T019 |
| `sabi-server/src/main/java/de/bluewhale/sabi/scheduler/MeasurementReminderContentProvider.java` | NEW | 3 | T019 |
| `sabi-server/src/main/java/de/bluewhale/sabi/scheduler/ReminderScheduler.java` | NEW | 3 | T020 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/UserServiceImpl.java` | MOD | 3+4 | T021, T024 |
| `sabi-webclient/src/main/resources/i18n/messages*.properties` (6 Dateien) | MOD | 4 | T025, T028 |
| `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/UserProfileView.java` | MOD | 4 | T026 |
| `sabi-webclient/src/main/resources/META-INF/resources/secured/userProfile.xhtml` | MOD | 4 | T027 |
| `sabi-webclient/src/main/resources/META-INF/resources/gdpr.xhtml` | MOD | 4 | T028 |
| `sabi-server/src/test/java/de/bluewhale/sabi/scheduler/ReminderSchedulerExtensibilityTest.java` | NEW | 5 | T029 |
| `sabi-server/src/test/java/de/bluewhale/sabi/DeveloperTestSuite.java` (+ ggf. `IntegrationTestSuite.java`) | MOD | 6 | T035 |
