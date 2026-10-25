# Completion Report — sabi-69 Email Measurement Reminders

**Branch**: `feature/069-email-measurement-reminders` | **Datum**: 2026-10-25
**Status**: ✅ Alle 39 Tasks (T001–T039) abgeschlossen und verifiziert — Feature ist merge-bereit.

---

## 1. Ergebnis-Überblick

| Phase | Tasks | Status |
|-------|-------|--------|
| 1 — Setup | T001–T002 | ✅ SBOM geprüft, `spring-boot-starter-mail` vorhanden, Flyway-Version V1_8_0_14 frei |
| 2 — Foundational | T003–T010 | ✅ Migration, Entities, DTOs, Repositories, Config, `@EnableScheduling`, Clock-Bean |
| 3 — US-1 (MVP) | T011–T021 | ✅ Scheduler + Service + E-Mail-Pipeline, 4 Test-Klassen (Fake-Clock, gestubbt `JavaMailSender`) |
| 4 — US-2 | T022–T028 | ✅ Intervall-Validierung (1–30, HTTP 409), Webclient-UI-Panel, GDPR-Disclosure |
| 5 — US-3 | T029–T030 | ✅ SPI `ReminderContentProvider` + Extensibility-Test (zweiter Provider ohne Core-Änderung) |
| 6 — Quality Gates | T031–T039 | ✅ Alle 8 ISO-25010-QGATEs + Quickstart-Szenarien A/B/D grün |

**Tests**: Vollregression `mvn -pl sabi-server test` → **185 grün, 0 F / 0 E** (2 Skips pre-existing).
**Coverage**: `EmailReminderServiceImpl` **95.0 %** (57/60 Lines), `ReminderScheduler` **89.4 %** (76/85) — beide ≥ 80 %.
**E2E (Live-Docker-Stack)**: Login → Profil-Enablement → Scheduler-Lauf → lokalisierte E-Mail in fakeSMTP (EN + DE), Idempotenz, GDPR-Cascade — alles verifiziert.

---

## 2. Quickstart-Szenarien (T039)

| Szenario | Ergebnis |
|----------|----------|
| **A** — Automatisierte Tests | ✅ 185 grün (Regression) + `ReminderSchedulerTest`, `EmailReminderServiceImplTest`, `ReminderDeliveryRecordRepositoryTest`, `NotificationServiceReminderEmailTest`, `ReminderSchedulerExtensibilityTest` |
| **B** — fakeSMTP-E2E inkl. Locale-Spot-Check | ✅ API-Flow: Login (202 + Token), GET zeigt neue Felder, PUT Enable → `next_send_date = today` (FR-001), Old-Client-PUT → 200. Scheduler-Lauf: exakt 1 E-Mail, Record `SENT`, `next_send_date` vorgerückt, zweiter Lauf → 0 due (Idempotenz). E-Mail: `multipart/alternative` (HTML+Text), From = No-Reply, Subject/Greeting/Links korrekt. **Locale de**: Subject „Zeit für deine nächsten Messungen“, Tabellensprache, `25,5` (deutsches Dezimalkomma), Datum `25.09.2026` |
| **D** — GDPR-Cascade auf Live-DB | ✅ `reminder_delivery_record` hat FK `fk_reminder_delivery_user … ON DELETE CASCADE` (SHOW CREATE TABLE verifiziert); nach `DELETE FROM users` → 0 Delivery-Records. JPA-Ebene zusätzlich via T013-Test (orphanRemoval) |

---

## 3. Wichtige Findings & Abweichungen

### 3.1 EclipseLink Shared Cache — Test-Artefakt, kein Produktionsrisiko
Die Auswahl-Query des Schedulers läuft ohne Transaktion (Spring Data JPA 4 Read-Methoden sind nicht `@Transactional`); das per-ID-Re-Fetchen in der Per-User-Transaktion kann vom **EclipseLink-Second-Level-Cache** mit einem veralteten `UserEntity` bedient werden, wenn Änderungen **außerhalb von JPA** (reines SQL) gesetzt wurden.
- **In der E2E beobachtet**: nach einem raw-SQL-Reset (`next_send_date`) lief der Scheduler mit dem gecachten Datum (2026-11-01 statt 2026-10-25).
- **Produktionsrisiko: gering.** Alle Schreibzugriffe laufen über JPA (Profil-Update, Scheduler selbst), die den Shared Cache konsistent halten. Nur manuelle SQL-Interventions (Test/DBA) umgehen ihn.
- **Workaround in Tests**: Backend-Neustart nach raw-SQL-Änderungen (leert den In-Memory-Cache).
- Die `findOverdueUnits`-Lese nutzt bewusst eine abgeleitete Query (`UserMeasurementReminderRepository.findByUserId`) statt der Lazy-Collection — SQL wird immer ausgeführt, unabhängig vom Cache-Zustand.

### 3.2 „°C-Bug“ in der E2E-E-Mail = Seed-Data-Fehler, kein App-Bug
Die erste E2E-E-Mail zeigte für °C „No measurement recorded yet“. Ursache: die Seed-Row in `measurement` wurde **ohne `user_id`** per SQL angelegt (NULL), während die App-Query `findTopByUserAndUnitIdOrderByMeasuredOnDesc(user, unitId)` korrekt nach `user_id = ?` filtert. Nach `UPDATE measurement SET user_id=…` zeigte die E-Mail korrekt `25.5`. Zusätzlich: `aquarium.size_unit` erwartet den **Enum-Namen** (`LITER`), nicht `'l'` — ebenfalls Seed-Fehler (führte zu `EclipseLink-116 DescriptorException`, sauber pro User abgefangen, Batch lief weiter → R8-Isolation bestätigt).

### 3.3 fakeSMTP-API-Rückgabe
`GET /api/emails` liefert Mails **newest-first** — bei der Auswertung immer auf `receivedOn`/`id` prüfen, nicht auf die Array-Position.

### 3.4 JaCoCo 0.8.11 vs. Java 25 (Tooling-Falle)
JaCoCo **0.8.11** überspringt Java-25-Klassen (Major Version 69) **stumm** → leere `.exec`-Datei / 0 % Coverage. Lösung: **JaCoCo 0.8.14** (Agent-Runtime-Jar + Maven-Plugin). Zusätzlich: Der `pom.xml` hat ein hartkodiertes Surefire-`argLine` (spring-instrument), das `jacoco:prepare-agent` überschreibt → für den Coverage-Lauf temporär gepatcht, danach wiederhergestellt (git-diff-verifiziert: clean).

### 3.5 Env-Var-Namen für Spring-Binding
`sabi.reminder.scheduler-cron` bindet über **`SABI_REMINDER_SCHEDULER-CRON`** (Punkte → Unterstriche, **Bindestrich bleibt erhalten**). `SABI_REMINDER_SCHEDULERCN` wird **nicht** gebunden (empirisch verifiziert: invalides Cron im korrekten Namen bricht den Startup, der andere Name wird ignoriert).

### 3.6 Old-Client-PUT-Semantik (bewusst so, kein Breaking Change)
`emailReminderEnabled` ist primitive `boolean` (Default `false`) → alte Clients, die das Feld weglassen, schalten Erinnerungen **stumm ab**. Das ist konsistent mit dem bestehenden `darkModeEnabled`-Pattern und wird als gewollt dokumentiert (Antwort = 200 OK, kein Fehler).

### 3.7 Kein Self-Service-Lösch-Endpunkt vorhanden
Im Codebase existiert **kein** Account-Deletion-Endpunkt für Nutzer (nur `unregisterUserAndClearPersonalData` als Fallback im Registrierungs-Flow). Scenario D wurde daher auf DB-Ebene (FK-Cascade) + JPA-Ebene (orphanRemoval, T013) verifiziert — Abweichung von der Quickstart-Annahme dokumentiert.

### 3.8 Colima-Bind-Mount-Quirk (Umwelt-spezifisch)
Bind-Mounts in Colima zeigen nur einen **Teilauszug** des Host-Dateibaums (selbst Repo-Root-Mounts nur partiell) → der Compose-`flyway`-Service ist in dieser Umgebung nicht nutzbar. Workaround: `docker create` des Flyway-Containers mit vollständigen CLI-Args, Migrations per `docker cp` hinein, `docker start -ai`. (MariaDB-Image 12.2.2 hat kein `mysql`-Binary — Client ist `/usr/bin/mariadb`; `docker exec` mit `-i` für Heredoc-stdin.)

### 3.9 Spring Boot 4.1: Nested `@Configuration` in Test-Klassen
Ein nested static `@Configuration` in einer `@SpringBootTest`-Klasse **hijackt den Context-Root** → für den zweiten SPI-Provider-Bean in `ReminderSchedulerExtensibilityTest` stattdessen `@MockitoBean(name=…)` verwendet.

### 3.10 i18n-Status
- **Server-Bundles**: alle 6 `ReminderEmailMessages*`-Dateien identisch (10 Keys), UTF-8/`\uXXXX`, Byte-Check bestanden.
- **Webclient-Bundles**: alle 8 sabi-69-Keys in allen 6 Dateien; git-diff = exakt 11 Zeilen pro Bundle.
- **Pre-existing** (nicht sabi-69): einzelne raw non-ASCII-Zeichen und fehlende Keys in anderen Sektionen der Webclient-Bundles (z. B. `oidc.welcome.*`) — bewusst nicht in diesem Feature angefasst.
- **Save-Bestätigung (T025)**: bestehender Key `userprofile.updateconfirmation.t` wiederverwendet — kein neuer Key nötig.

---

## 4. Deployment-Notizen (T038)

- `devops/sabi_docker_sdk/server_redeploy.sh --boundary` → **BUILD SUCCESS**, `Started SabiServiceApplication in 5.995 s`, Container `sabi-as` running.
- **Keine** Ansible-Playbook-Änderungen erforderlich (neue Env-Vars `SABI_EMAIL_NO_REPLY_ADDRESS`, `SABI_EMAIL_BASE_URL` haben produktionsfähige Defaults; SMTP über bestehende `SABI_SMTP_*`).
- **Neue Env-Vars für Produktion (optional)**: `SABI_EMAIL_NO_REPLY_ADDRESS` (Default `no-reply@sabi-project.net`), `SABI_EMAIL_BASE_URL` (Default `https://sabi-project.net`).
- **SPF/DKIM/DMARC** für `no-reply@sabi-project.net` vor dem ersten Live-Versand prüfen (Spec-Annahme).

---

## 5. Dateisummary (uncommitted, Working Tree)

**Neu**: Migration `V1_8_0_14`, `ReminderDeliveryRecordEntity`, `ReminderDeliveryRecordRepository`, `ClockConfig`, `EmailReminderService(Impl)`, `MeasurementReminderEmail`, `scheduler/` (Scheduler, SPI, Measurement-Provider), 6 Server-i18n-Bundles, 5 Test-Klassen.
**Geändert**: `UserEntity`, `UserProfileTo` (boundary), `UserRepository`, `UserMeasurementReminderRepository`, `NotificationService(Impl)`, `UserServiceImpl`, `SabiServiceApplication` (`@EnableScheduling`), `application.yml`, Webclient (`UserProfileView`, `userProfile.xhtml`, `gdpr.xhtml`, 6 i18n-Bundles), Test-Suites.
