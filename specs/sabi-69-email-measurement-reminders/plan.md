# Implementation Plan: Email Measurement Reminders

**Branch**: `feature/sabi-69-email-measurement-reminders` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `.specify/features/sabi-69-email-measurement-reminders/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

> **Setup deviation**: `setup-plan.sh --json` was rejected because the branch name
> (`feature/sabi-69-email-measurement-reminders`) does not match the spec-kit convention
> (`NNN-feature-name`). Path resolution was performed manually against the feature directory
> declared in the spec header. All artifacts live next to `spec.md` under
> `.specify/features/sabi-69-email-measurement-reminders/`.

## Summary

Users can opt in to **email measurement reminders** in their profile (weekly preset or custom
1–30 day interval). A new server-side scheduler (`@Scheduled`, fixed daily time, default 09:00
server timezone) evaluates due reminders once per day and sends a localized HTML email (all 6
message bundles) listing the user's overdue measurement units — with last value/date or a
"no measurement recorded yet" placeholder — plus deep links back into Sabi. Delivery is
idempotent per user + cycle via a new `reminder_delivery_record` table (unique key
`user_id + cycle_date`, `attempts` counter 0–3), transient SMTP failures are retried on
subsequent scheduler runs, and the whole pipeline is built on a generic
`ReminderContentProvider` SPI so future reminder types (maintenance notices, sponsor emails)
can be added without touching the scheduler core or the measurement-reminder service.

Technical approach: extend the existing profile flow (`UserProfileTo` →
`UserServiceImpl.updateProfile` → `UserProfileView`/`userProfile.xhtml`) with the two new
settings, reuse the existing Spring Mail infrastructure (`NotificationServiceImpl`,
`JavaMailSender`, `spring.mail.*` env vars, fakeSMTP for local dev), add one Flyway migration
(`V1_8_0_14`) for the three new `users` columns and the delivery-record table, and add a
server-side i18n bundle `i18n/ReminderEmailMessages` (6 files) mirroring the existing
`RegistrationMessages` pattern.

## Technical Context

**Language/Version**: Java 25 (LTS) — verified in `sabi-server/pom.xml` (`<java.version>25</java.version>`)
**Primary Dependencies**: Spring Boot 4.1.1 (`spring-boot-starter-parent`), `spring-boot-starter-mail` (already a dependency, used by `NotificationServiceImpl`), EclipseLink 5.0.1 (JPA provider — not Hibernate), MariaDB JDBC driver, Flyway
**Storage**: MariaDB 10.x, schema `sabi` — modified `users` table (3 new columns) + new `reminder_delivery_record` table; transient email payload built at send time (not persisted)
**Testing**: JUnit 5, `@SpringBootTest` + Testcontainers (`MariaDBContainer`, see `UserServiceTest`), injectable `java.time.Clock` for scheduler tests, fakeSMTP (docker-compose, port 2525) for delivery verification; EclipseLink L1-cache rules from DEVELOPERS_MANUAL.md apply
**Target Platform**: ARM64 Raspberry Pi (production), x86/AMD64 Docker Compose (development)
**Project Type**: web-service (Spring Boot REST backend `sabi-server` + JSF/PrimeFaces web client `sabi-webclient`, shared DTOs in `sabi-boundary`)
**Performance Goals**: scheduler batch completes within 5 minutes for up to 10,000 enabled users on ARM; zero measurable latency impact on existing API endpoints (runs off-request)
**Constraints**: no PII in logs (`user_id=` only), standard ownership pattern for all setting changes, interval validation 1–30 days client- and server-side, all new UI + email strings in all 6 language bundles (missing translations are a release blocker), WCAG 2.1 AA per UI_StyleGuide.md, SMTP credentials only via existing `SABI_SMTP_*` env vars
**Scale/Scope**: ~10k users max for the batch; 3 new user columns, 1 new table (unique key `user_id + cycle_date`), 1 scheduled job, 2 modules touched for code (`sabi-server`, `sabi-webclient`) + `sabi-boundary` DTO + `sabi-database` migration

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | ISO 25010 Principle                               | Gate Question                                                       | Status |
|---|---------------------------------------------------|---------------------------------------------------------------------|--------|
| I | Funktionale Eignung (Functional Suitability)      | Issue/User-Story + Akzeptanzkriterien vorhanden?                    | ✅ — spec.md contains 3 user stories (P1–P3) with acceptance scenarios, edge cases, FR-001…FR-012, SC-001…SC-006 |
| II | Leistungseffizienz (Performance Efficiency)      | Ressourcen-Impact auf ARM/Raspberry Pi bewertet?                    | ✅ — scheduler runs off-request once daily; single indexed batch query (`email_reminder_enabled = TRUE AND email_reminder_next_send_date <= ?`); per-user work is 1 content build + 1 SMTP send; no N+1 (overdue units fetched per user in one query); no impact on request path |
| III | Kompatibilität (Compatibility)                 | API-Rückwärtskompatibilität und OpenAPI-Docs sichergestellt?         | ✅ — `PUT /api/userprofile` only gains optional fields with defaults (old clients unaffected); OpenAPI via springdoc updates automatically; no breaking changes |
| IV | Benutzbarkeit (Usability)                        | i18n (DE/EN) + Validierung + WCAG 2.1 AA berücksichtigt?            | ✅ — all new UI strings in 6 bundles (de/en/es/fr/it + fallback); interval validated client-side (`p:inputNumber` min/max) and server-side (1–30, rejection with localized message); panel added to existing profile page (≤ 3 clicks from login); PrimeFaces components per UI_StyleGuide.md |
| V | Zuverlässigkeit (Reliability)                    | Tests grün, Fehlerbehandlung + Transaktionssicherheit geplant?      | ✅ — idempotency via DB unique constraint; per-user transaction isolation (one failing recipient does not block others); retry semantics with `attempts` counter; JUnit coverage target ≥ 80 % on scheduler + service |
| VI | Sicherheit (Security)                           | Auth/Authz, keine Secrets im Code, OWASP-Scan geplant?              | ✅ — profile changes go through the standard ownership pattern (principal email → `UserEntity`); scheduler is server-side with no user context; logs use `user_id=` only; SMTP credentials stay in `SABI_SMTP_*` env vars; no new secrets |
| VII | Wartbarkeit (Maintainability)                  | Modulare Architektur + Flyway + JUnit-Tests eingeplant?              | ✅ — code placed per module boundaries (`sabi-boundary` DTO, `sabi-server` service/scheduler/i18n, `sabi-webclient` UI, `sabi-database` migration `V1_8_0_14`, no schema prefix, immutable existing scripts); JUnit tests planned for scheduler, service, and profile update |
| VIII | Übertragbarkeit (Portability)                 | ARM + AMD64 kompatibel, Docker/Ansible-Deployment geprüft?          | ✅ — no platform-specific dependencies; `spring-boot-starter-mail` already ships in the existing ARM images; fakeSMTP already in `devops/sabi_docker_sdk/docker-compose.yml`; no Ansible playbook changes required (SMTP env vars exist) |

**Re-check after Phase 1 design**: all gates remain ✅ — the design (delivery-record table,
`ReminderContentProvider` SPI, clock injection, per-user transactions) adds no new violations.

**Violations requiring justification** (see Complexity Tracking section): none.

## Project Structure

### Documentation (this feature)

```text
.specify/features/sabi-69-email-measurement-reminders/
├── spec.md              # Feature specification (input)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── profile-api.md       # PUT /api/userprofile extension contract
│   └── email-template.md    # Reminder email structure + i18n key contract
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
sabi-boundary/src/main/java/de/bluewhale/sabi/model/
└── UserProfileTo.java                          # MODIFIED: + emailReminderEnabled, + emailReminderIntervalDays

sabi-server/src/main/java/de/bluewhale/sabi/
├── SabiServiceApplication.java                 # MODIFIED: + @EnableScheduling
├── persistence/model/
│   ├── UserEntity.java                         # MODIFIED: + emailReminderEnabled, + emailReminderIntervalDays,
│   │                                           #          + emailReminderNextSendDate, + delivery-records collection
│   └── ReminderDeliveryRecordEntity.java       # NEW: persisted delivery record (idempotency + audit)
├── persistence/repository/
│   └── ReminderDeliveryRecordRepository.java   # NEW: find by (userId, cycleDate), due-user query support
├── services/
│   ├── UserServiceImpl.java                    # MODIFIED: updateProfile() applies reminder settings + validation
│   ├── EmailReminderService.java               # NEW (interface): applyProfileChange(), overdue-unit query
│   ├── EmailReminderServiceImpl.java           # NEW: next-send-date state machine, overdue computation
│   └── NotificationServiceImpl.java            # MODIFIED: + sendMeasurementReminderEmail(payload, locale)
├── scheduler/
│   ├── ReminderScheduler.java                  # NEW: @Scheduled cron job, iterates providers, per-user transactions
│   └── ReminderContentProvider.java            # NEW (SPI): type() + buildContent(user, cycleDate) — FR-011
└── (server-internal transient DTO)
    └── MeasurementReminderEmail.java           # NEW: record built at send time (userId, languageCode, overdue units, links)

sabi-server/src/main/resources/
├── application.yml                             # MODIFIED: + sabi.reminder.scheduler-cron, + sabi.email.* properties
└── i18n/ReminderEmailMessages*.properties      # NEW: 6 bundles (de/en/es/fr/it + fallback)

sabi-database/src/main/resources/db/migration/version1_8_0/
└── V1_8_0_14__addEmailReminderColumns.sql      # NEW: users columns + reminder_delivery_record table

sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/
└── UserProfileView.java                        # MODIFIED: + emailReminder fields, load/save handling

sabi-webclient/src/main/resources/
├── META-INF/resources/secured/userProfile.xhtml # MODIFIED: + "Email reminders" panel (toggle + interval)
├── META-INF/resources/gdpr.xhtml               # MODIFIED: + disclosure paragraph (FR-012)
└── i18n/messages*.properties                   # MODIFIED: + new UI keys in all 6 bundles

sabi-server/src/test/java/de/bluewhale/sabi/    # NEW tests: scheduler (fake clock + double-run),
                                                # service state machine, profile update validation,
                                                # NotificationService email content (6 locales)
```

**Structure Decision**: Follows the existing modular architecture mandated by Constitution
Principle VII (`sabi-boundary | sabi-server | sabi-webclient | sabi-database`). The feature
touches all four modules exactly where the existing code already lives: DTOs in
`sabi-boundary` (shared REST contract), persistence/services/scheduler/i18n in `sabi-server`,
UI + webclient i18n in `sabi-webclient`, schema evolution in `sabi-database`. No new module,
no new external service — Spring Mail and fakeSMTP are already part of the stack.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations — this section is intentionally empty.
