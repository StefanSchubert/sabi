# Research: Email Measurement Reminders (sabi-69)

**Feature**: `.specify/features/sabi-69-email-measurement-reminders/spec.md`
**Date**: 2026-10-24

All NEEDS CLARIFICATION items from the Technical Context were resolved by inspecting the
existing codebase (paths verified). No external research was required — every decision builds
on infrastructure that already exists in Sabi.

---

## R1. Scheduler mechanism (FR-003: fixed daily time, configurable)

**Decision**: Spring `@Scheduled(cron = "${sabi.reminder.scheduler-cron:0 0 9 * * *}")` on a
dedicated `ReminderScheduler` bean; `@EnableScheduling` added to `SabiServiceApplication`.
The cron expression is a property, so the daily time (default 09:00 server timezone) is
configurable without code changes.

**Rationale**: Sabi runs as a single Spring Boot instance per deployment (Docker Compose, one
`sabi-backend` container). A single daily job with no catch-up/missed-run semantics beyond
"run now, pick up all due users" fits `@Scheduled` perfectly. The due-user query
(`email_reminder_enabled = TRUE AND email_reminder_next_send_date <= runDate`) is naturally
idempotent across runs, so a missed day self-heals on the next run.

**Alternatives considered**:
- *Quartz (clustering, persistent triggers)*: overkill — no multi-instance deployment, no
  sub-daily granularity needed; adds a new dependency and a Quartz schema to maintain.
- *Fixed-rate (`fixedRate`)*: wrong semantics — the spec requires evaluation at a fixed time
  of day, not every N ms; also drifts over long uptimes.

## R2. Extensibility for future reminder types (FR-011 / SC-006)

**Decision**: Introduce a small SPI — `ReminderContentProvider` interface with
`String type()` and `Optional<MeasurementReminderEmail-like payload> buildContent(UserEntity, LocalDate cycleDate)`.
The scheduler core is provider-agnostic: it selects due users, manages the delivery record,
handles retry/advance semantics, and delegates only *content building* to the registered
provider(s). Spring injects `List<ReminderContentProvider>`; a new reminder type = one new
bean + one template, zero changes to scheduler core or measurement-reminder service.

**Rationale**: The spec's acceptance criterion is explicit: a second type must be addable
"without modifying the scheduler core or the measurement-reminder service (template + trigger
registration only)". The provider SPI is the minimal structure that satisfies this. Keeping
idempotency/retry/next-send-date logic in the core (keyed by `user_id + cycle_date`) means
future types inherit correct delivery semantics for free.

**Alternatives considered**:
- *Enum-based type registry*: still requires touching the core to register a new value — fails FR-011.
- *Fully generic "reminder type" table (type column in delivery records)*: premature — the spec
  explicitly defers multi-type storage; one user-level setting exists. Revisit when a second
  type is actually built.

## R3. Email i18n — where the reminder email text lives

**Decision**: New server-side bundle `i18n/ReminderEmailMessages` with the standard 6 files
(`.properties` fallback + `_de/_en/_es/_fr/_it`), loaded via `ResourceBundle.getBundle(...)`
exactly like the existing `i18n/RegistrationMessages` used by `NotificationServiceImpl`.

**Rationale**: The server must render emails independently of the webclient; the
`RegistrationMessages` pattern is the established precedent (same module, same loading code).
The webclient `i18n/messages*.properties` bundles are for the JSF UI only and must not be
mixed with server-side email text.

**Alternatives considered**:
- *Reuse webclient `messages*.properties`*: wrong module boundary — `sabi-server` does not
  depend on `sabi-webclient`.
- *Inline strings in code*: violates Constitution IV (i18n) and the AGENTS.md release-blocker
  rule for missing translations.

## R4. Sender address (FR-006: configurable no-reply, default `no-reply@sabi-project.net`)

**Decision**: New property `sabi.email.no-reply-address` (default `no-reply@sabi-project.net`)
injected in the new send method. A second property `sabi.email.base-url`
(default `https://sabi-project.net`) builds the deep links into Sabi.

**Rationale**: The existing code uses `spring.mail.username` as the From address — that is the
SMTP *authentication* user, not a product-level sender identity. Coupling the reminder's From
address to SMTP credentials would break when the auth user and the public sender diverge. A
dedicated property keeps FR-006's "configurable" requirement explicit.

**Alternatives considered**:
- *Reuse `spring.mail.username`*: works today but couples identity to credentials; rejected.

## R5. Cycle-date semantics and idempotency (FR-004, SC-003)

**Decision**: `cycleDate` = the user's `emailReminderNextSendDate` (the due date) at the time
the scheduler evaluates the user. The delivery record is keyed by the unique constraint
`(user_id, cycle_date)`. Scheduler re-runs on the same day find the existing record and skip
(status `SENT`) — this is what makes double-runs safe (SC-003).

**Rationale**: Using the due date (not "today") as the cycle key is stable across retry runs
(next send date is *not* advanced while retries remain, per the spec clarification of
2026-10-24) and robust if the server is down for days: the cycle still refers to the original
due date, and after closure `nextSendDate = cycleDate + interval` keeps the cadence
predictable.

**Alternatives considered**:
- *cycleDate = run date (today)*: breaks retry idempotency — a retry the next day would create
  a second record for the same logical cycle. Rejected.

## R6. "No overdue measurements" cycles (spec edge case)

**Decision**: If `buildContent()` returns empty at send time, **no delivery record is
created** (no send attempt happened) and `nextSendDate` advances by the interval. If a
previously `FAILED` record exists for the cycle (transient failure, then the user's overdue
units were all measured), the record is left in place as audit evidence and the next send
date advances.

**Rationale**: FR-007 requires recording "each send attempt" — with nothing to send there is
no attempt, so no record. Leaving a stale `FAILED` row untouched preserves the audit trail
without inventing a third status value (spec defines exactly `SENT`/`FAILED`).

**Alternatives considered**:
- *Third status `SKIPPED`*: deviates from the spec's entity definition; rejected.
- *Delete the FAILED row*: loses audit information; rejected.

## R7. Retry and permanent-failure handling (spec edge cases)

**Decision**: One record per user + cycle with `attempts` counter (0–3), per the spec
clarification of 2026-10-24.
- Transient `MailSendException` (SMTP unavailable, timeout) → `attempts += 1`, status
  `FAILED`, next send date **not** advanced; retried on the next scheduler run.
- `attempts >= 3` or permanent rejection (bounce / `MessageNotSavedException`) → cycle closed:
  status stays `FAILED`, next send date advances by the interval.
- Logs carry `user_id=` only (PII rule), e.g. `log.warn("Reminder email failed for user_id={} cycle={} attempt={}", ...)`.

**Rationale**: Directly implements the clarified retry model; per-user transaction isolation
(see R8) guarantees one failing recipient cannot block other users' sends.

**Alternatives considered**:
- *In-run retry loop (3 immediate attempts)*: the spec explicitly says "retried on the next
  scheduler run"; in-run retries would hold the batch open for SMTP outages. Rejected.

## R8. Transaction boundaries in the batch

**Decision**: The scheduler selects due users in one read query, then processes each user in
its **own transaction** (`@Transactional` service method per user). A failure for one user
rolls back only that user's state and the loop continues.

**Rationale**: Spec edge case: "Other users' sends are not blocked by one failing recipient."
A single batch transaction would roll back all progress on any failure and violate the
5-minute/10k-user performance goal during partial SMTP outages.

**Alternatives considered**:
- *Single batch transaction*: simple but fails the isolation requirement; rejected.

## R9. Clock injection for testability (Maintainability, SC-002/SC-003)

**Decision**: A `Clock` bean (`Clock.systemDefaultZone()` in production config) is injected
into the scheduler and `EmailReminderServiceImpl`. Tests use a fixed/advancing `Clock` to
deterministically cross due dates, and force double-runs for the idempotency test (SC-003).

**Rationale**: Constitution VII requires the scheduler to be "unit-testable with an injectable
clock" (spec NFR Maintainability). `java.time.Clock` is the standard JDK mechanism; no
additional library needed.

**Alternatives considered**:
- *Mockito `mockStatic(LocalDateTime::now)`*: brittle, hides time assumptions in call sites;
  rejected in favor of explicit clock injection.

## R10. Overdue-unit determination (FR-008)

**Decision**: Reuse the existing in-app reminder semantics: a unit is overdue iff the user
has an **active** `UserMeasurementReminderEntity` for it and either (a) the last
`MeasurementEntity.measuredOn + pastdays` is before now, or (b) no measurement exists at all
(spec assumption: never-measured units are listed with the "no measurement recorded yet"
placeholder). Units without an active in-app reminder are never included. The computation
lives in `EmailReminderServiceImpl` as a new method — the existing
`MeasurementServiceImpl.getMeasurementRemindersForUser()` (in-app endpoint) is **not**
modified, preserving the Compatibility gate.

**Rationale**: FR-008 ties email content to the existing in-app reminder settings; duplicating
the rule in a new method (instead of refactoring the old endpoint) avoids any regression risk
on the live in-app reminder API.

**Alternatives considered**:
- *Refactor `MeasurementServiceImpl` to share the overdue query*: cleaner long-term, but
  touches a live endpoint for this feature; deferred as optional follow-up.

## R11. Profile DTO extension and validation (FR-001, FR-002, FR-009, FR-010)

**Decision**: `UserProfileTo` (sabi-boundary) gains `boolean emailReminderEnabled` and
`Integer emailReminderIntervalDays` (nullable — absent on old clients, treated as "unchanged").
`UserServiceImpl.updateProfile()` applies the state machine:
- enable (was off, now on): `nextSendDate = today` if overdue units exist, else `today + interval`;
- interval change while enabled: `nextSendDate = today + newInterval` (FR-009);
- disable: `nextSendDate = null`, no further sends (FR-010);
- interval outside 1–30 while enabled → reject the whole profile update with a localized
  error (server-side validation mirrors the client-side `p:inputNumber` min/max).

**Rationale**: Extending the existing profile round-trip keeps a single save path (one
`PUT /api/userprofile`), matches the webclient's current `UserProfileView.save()` flow, and
keeps the API backward compatible (new fields optional with defaults).

**Alternatives considered**:
- *Dedicated endpoint `PUT /api/userprofile/email-reminder`*: extra surface, extra ownership
  check code; the profile endpoint already implements the standard pattern. Rejected.

## R12. Delivery-record ownership and GDPR cascade (spec edge case)

**Decision**: `ReminderDeliveryRecordEntity` with `@ManyToOne user`, owned via a
`@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)`
collection on `UserEntity` — the same pattern as `oidcProviderLinks`. The Flyway migration
additionally declares `FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE` as a
defense-in-depth guarantee for `unregisterUserAndClearPersonalData()` (which calls
`userRepository.delete(userEntity)`).

**Rationale**: The spec requires cascade deletion with the account; JPA cascade handles the
managed-entity path, the DB-level `ON DELETE CASCADE` covers any future raw-SQL deletion.
Both mechanisms are already used elsewhere in the schema (e.g. `aquarium` FKs).

**Alternatives considered**:
- *JPA cascade only*: acceptable, but a single `DELETE FROM users` in an ad-hoc maintenance
  script would orphan PII rows; the DB constraint removes that risk at zero cost.

## R13. Local development / validation (fakeSMTP)

**Decision**: No new dev infrastructure — `devops/sabi_docker_sdk/docker-compose.yml` already
runs `gessnerfl/fake-smtp-server:2.5.0` on port 2525 with the same credentials as
`application.yml` defaults. Manual validation reads sent mail via fakeSMTP's web UI
(`http://localhost:2525`); automated validation uses the Testcontainers-based integration
tests with a stubbed `JavaMailSender`.

**Rationale**: Spec assumption: "Local development uses the existing fakeSMTP docker-compose
setup." Verified present in the compose file (lines 67–73).

**Alternatives considered**: none needed — infrastructure exists.

## R14. Deep links in the email (FR-005)

**Decision**: Two links, built from `sabi.email.base-url`:
- measurement page: `{base}/secured/measureView.xhtml` (`PageRegister.MEASUREMENT_VIEW_PAGE`)
- profile settings: `{base}/secured/userProfile.xhtml` (`PageRegister.USER_PROFILE_VIEW_PAGE`)

**Rationale**: Both pages exist and are the canonical destinations ("link to the measurement
page" / "link to the profile settings where reminders can be changed or disabled"). Context
path `/sabi` is part of the public base URL in production; the property default already points
at the public domain.

**Alternatives considered**: none — destinations are unambiguous per spec.

---

## Open items (none blocking)

- SPF/DKIM/DMARC verification for `no-reply@sabi-project.net` happens during the first live
  send (spec assumption) — not a code task.
- Optional follow-up (out of scope): share the overdue-unit query between
  `MeasurementServiceImpl` and `EmailReminderServiceImpl` (see R10).
