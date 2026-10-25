# Feature Specification: Email Measurement Reminders

**Feature ID**: sabi-69
**Branch**: `sabi-69-email-measurement-reminders`
**Status**: Draft
**Created**: 2026-10-23

## Overview

### Problem Statement

Users who keep aquariums with Sabi are expected to take water-parameter measurements on a regular basis (e.g. nitrate, phosphate, salinity). In practice, users are often too busy and simply forget to measure. The existing in-app reminder (`GET /api/measurement/reminder/list/{language}`) only surfaces overdue measurements **when the user actively opens Sabi** — it cannot pull a busy user back to the application. There is currently no channel that proactively reaches the user outside of Sabi.

### Proposed Solution

Add an **opt-in email reminder** that the user configures in their profile. The user chooses a cadence (weekly, or every *x* days). A server-side scheduler evaluates due reminders once per day and sends a localized email listing the user's overdue measurements, with links back into Sabi.

The implementation is deliberately kept **generic enough** to be reused for future outbound email use cases that the feature request hints at (maintenance-window notices, project-sponsor announcements). Those additional use cases are explicitly **out of scope** for this iteration but must not be architecturally blocked.

### Business Value

- **Higher measurement regularity** → healthier aquariums, better data quality in the community reef report.
- **Reduced churn**: busy users stay engaged instead of drifting away because they forgot to measure.
- **Reusable outbound-email infrastructure** lowers the cost of future notification features.

---

## Scope

### In Scope

- Profile setting: enable/disable email measurement reminders, choose interval (weekly preset or custom 1–30 days)
- Server-side scheduler that evaluates due reminders at least once per day
- Localized reminder email (all 6 message bundles: `de`, `en`, `es`, `fr`, `it` + fallback)
- Flyway migration adding the reminder columns to the user table
- Delivery-status tracking (sent / failed) for idempotency and auditability
- Privacy-policy note disclosing that reminder emails contain a measurement summary

### Out of Scope (this iteration)

- Per-aquarium or per-unit reminder configuration (reminder is **per user**, not per tank)
- SMS / push-notification channels
- Calendar (ICS) integration — explicitly considered and rejected in the feature request in favor of a built-in email trigger
- Maintenance-window emails, sponsor/ad emails (future iterations on top of this infrastructure)
- Per-user scheduling time or timezone (server timezone is used)
- Email open/click tracking pixels (privacy)
- Periodic retention purge of delivery records (they are cascade-deleted with the user account only)

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enable Weekly Email Reminders and Receive Them (Priority: P1)

As a Sabi user, I want to switch on email reminders in my profile so that I get an email whenever it is time to measure again — even if I have not opened Sabi for days.

**Why this priority**: This is the core value of the feature request. Without it, there is no proactive channel at all; every other story depends on this one working end-to-end.

**Independent Test**: Fully testable by creating a user, enabling the reminder with the default weekly interval, advancing the scheduler past the due date (fake clock in tests), and asserting that exactly one email was dispatched to the user's address with the correct content. Delivers the complete user value on its own.

**Acceptance Scenarios**:

1. **Given** a logged-in user with at least one overdue measurement unit, **When** they open their profile and switch on "Email reminders" with the default interval (7 days), **Then** the setting is persisted and the next send date is set to *today* — the first email goes out on the next scheduler run. **Given** a logged-in user with no overdue measurement unit, **When** they switch on "Email reminders" with the default interval (7 days), **Then** the next send date is set to *today + 7 days*.
2. **Given** a user with email reminders enabled whose next send date has been reached, **When** the daily scheduler runs, **Then** exactly one reminder email is sent to the user's verified email address from `no-reply@sabi-project.net`.
3. **Given** a reminder email has been sent, **When** the scheduler runs again on the same day, **Then** no second email is sent for the same user and the same cycle (idempotency).
4. **Given** a reminder email was sent successfully, **When** the user's next send date is evaluated afterwards, **Then** it equals *last sent date + interval*.

---

### User Story 2 - Customize the Interval and Turn Reminders Off (Priority: P2)

As a Sabi user, I want to choose my own cadence (e.g. every 14 days) and turn the reminders off again at any time, so that the feature adapts to my routine instead of nagging me.

**Why this priority**: The feature request explicitly asks for "every week **or x days**". Users who find weekly too frequent would otherwise disable the feature entirely; a custom interval keeps them in. Still, P1 delivers value without this story.

**Independent Test**: Testable independently by changing the interval of an existing setting, verifying the next send date is recomputed from *now*, and by disabling the flag and verifying the scheduler skips that user.

**Acceptance Scenarios**:

1. **Given** a user with email reminders enabled at 7 days, **When** they change the interval to 14 days in their profile, **Then** the next send date is recomputed as *today + 14 days*.
2. **Given** a user with email reminders enabled, **When** they switch the setting off, **Then** no further emails are sent and the next send date is cleared.
3. **Given** a user attempts to save an interval of 0 or 31 days, **When** the form is submitted, **Then** the input is rejected with a validation message (valid range: 1–30 days).

---

### User Story 3 - Reusable Outbound Email Infrastructure (Priority: P3)

As the Sabi application owner, I want the email-sending mechanism to be decoupled from the measurement-reminder logic so that I can later add maintenance-window notices or sponsor announcements without reworking the scheduler.

**Why this priority**: Stated in the feature request's "Additional context" as a future benefit, not a user-facing need of this iteration. It is an architectural constraint rather than a demonstrable user journey, hence P3.

**Independent Test**: Verified by an architecture review: a second reminder *type* can be registered (template + due-date provider) without modifying the scheduler core or the measurement-reminder service.

**Acceptance Scenarios**:

1. **Given** the scheduler core, **When** a new reminder type is introduced (e.g. "maintenance notice") with its own template and trigger logic, **Then** no changes to the existing measurement-reminder service or scheduler loop are required.

---

### Edge Cases

- **SMTP provider unavailable**: the send for that cycle is marked `FAILED` and its attempts counter is incremented; it is retried on the next scheduler run (max 3 attempts per cycle). The next send date is **not** advanced while retries remain. Once the attempts are exhausted, the cycle is closed (record stays `FAILED`) and the next send date advances by the interval. Other users' sends are not blocked by one failing recipient.
- **Bounced / permanently rejected email**: logged with `user_id=` only (no PII), no further retries for that cycle; the next send date advances by the interval (cycle closed). The user is not notified in-band (they cannot receive email by definition).
- **User changes their email address** while a reminder is pending: the next send uses the new, verified address.
- **User deletes their account**: reminder settings and all `ReminderDeliveryRecord` rows are removed with the account (GDPR cascade).
- **Timezone**: the scheduler runs in the server's configured timezone; email content shows measurement dates in the user's locale but does not promise a specific local send time.
- **No overdue measurements**: if the user has no enabled measurement unit that is currently overdue, no email is sent for that cycle (no empty "nothing to do" mail), and the next send date still advances.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow a logged-in user to enable or disable email measurement reminders in their profile. On first enablement, the next send date is set to *today* if the user already has overdue measurement units (first email on the next scheduler run), otherwise to *today + interval*.
- **FR-002**: The system MUST allow the user to choose the reminder interval as an integer number of days between 1 and 30 (inclusive); "weekly" is offered as a preset equal to 7 days; the default on first enablement is 7 days.
- **FR-003**: The system MUST evaluate due reminders exactly once per day at a configurable fixed time (default: 09:00 in the server's configured timezone) via a server-side scheduler.
- **FR-004**: The system MUST send at most one reminder email per user per cycle (idempotent send, keyed by user + cycle date).
- **FR-005**: The reminder email MUST be localized to the user's preferred language using all 6 message bundles (`de`, `en`, `es`, `fr`, `it` + fallback) and MUST contain: a subject line, a greeting, the list of overdue measurement units with their last recorded value and date, a link to the measurement page in Sabi, and a link to the profile settings where reminders can be changed or disabled.
- **FR-006**: The email MUST be sent from the configurable no-reply address (default: `no-reply@sabi-project.net`) and addressed to the user's verified email address.
- **FR-007**: The system MUST record the delivery status (sent / failed) of each send attempt for auditability.
- **FR-008**: The system MUST only include measurement units in the email that are configured as reminders in the user's existing in-app reminder settings and are currently overdue; units without an in-app reminder MUST NOT appear.
- **FR-009**: Changing the interval MUST recompute the next send date from the moment of change (not from the previous cycle's anchor).
- **FR-010**: Disabling reminders MUST stop all future sends immediately.
- **FR-011**: The email infrastructure MUST be structured so that a second reminder type (e.g. maintenance notice) can be added without modifying the scheduler core or the measurement-reminder service (template + trigger registration only).
- **FR-012**: The privacy policy MUST be updated to disclose that reminder emails contain a summary of the user's own measurements.

### Non-Functional Requirements (ISO 25010) *(fill applicable rows)*

| ISO 25010 Merkmal | Requirement / Constraint for this Feature |
|---|---|
| Functional Suitability | All 12 functional requirements (FR-001 – FR-012) must be met without gaps. No duplicate emails per user per cycle; no email for users with nothing overdue. |
| Performance Efficiency | The scheduler batch MUST complete within 5 minutes for up to 10,000 enabled users on the ARM production environment. The email-send path MUST NOT add measurable latency to any existing API endpoint (runs off-request). |
| Compatibility | The feature MUST NOT break the existing profile update flow or the in-app reminder endpoints. Must run on the existing ARM64 production stack; SMTP is already configured via `spring.mail.*` (env vars `SABI_SMTP_HOST/USER/PASSWORD/PORT`) — no new external service is introduced. |
| Usability | Settings MUST be reachable within 3 clicks from login (profile → reminders). UI MUST meet WCAG 2.1 AA (existing style guide). All new UI strings in all 6 language bundles — missing translations are a release blocker. |
| Reliability | Sends MUST be idempotent per user + cycle (safe against duplicate scheduler runs). At-least-once delivery semantics: transient SMTP failures are retried up to 3 times on subsequent scheduler runs; permanently failed sends are logged and skipped. |
| Security | All setting changes MUST follow the standard ownership pattern (principal email → `UserEntity`, load with ownership filter). No PII in logs (`user_id=` only, never email addresses). SMTP credentials remain in environment configuration via the existing `SABI_SMTP_*` variables, never committed to the repository. |
| Maintainability | New user-table columns MUST be added via a Flyway migration (no hardcoded schema prefix). Scheduler logic MUST be unit-testable with an injectable clock; target ≥ 80 % line coverage on the new scheduler + service classes. Local development uses the existing fakeSMTP docker-compose setup. |
| Portability | MUST remain Docker-compatible; no changes to the Ansible playbook required (SMTP env vars already exist). |

### Key Entities

- **UserEntity (modified)**: gains three attributes — `emailReminderEnabled` (boolean, default `false`), `emailReminderIntervalDays` (integer, 1–30, default `7`), `emailReminderNextSendDate` (date; set on enablement and after each successful send; cleared on disablement).
- **MeasurementReminderEmail (transient, not persisted)**: built at send time — `userId`, `languageCode`, list of overdue units (`unitName`, `lastValue`, `lastMeasurementDate`), deep link to the measurement page.
- **ReminderDeliveryRecord (new, persisted)**: `userId`, `cycleDate` (the due date the cycle refers to), `sentAt` (set on success), `status` (`SENT` / `FAILED`), `attempts` (integer 0–3, incremented per failed send attempt within the cycle). Used for idempotency (unique constraint on `userId` + `cycleDate`) and audit. Cascade-deleted with the user account.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can enable email reminders in under 60 seconds (profile → toggle → save), verified by manual user test.
- **SC-002**: 100 % of users with enabled reminders whose next send date is due receive exactly one email within 15 minutes of the scheduled send time — i.e. the configured daily scheduler run time (default 09:00 server timezone) — measured via delivery records in test environment.
- **SC-003**: Zero duplicate emails per user per cycle across repeated scheduler runs (verified by integration test with forced double-run).
- **SC-004**: No existing API endpoint shows measurable latency regression after the scheduler is active (regression test suite passes unchanged).
- **SC-005**: All 6 message bundles contain every new i18n key with zero missing-translation warnings at startup.
- **SC-006**: A second reminder type can be introduced by adding a template and registering a trigger, without modifying the scheduler core or the measurement-reminder service (architecture review).

---

## Assumptions

- Users have a **verified** email address from registration (existing behaviour); OIDC-provisioned accounts are treated as verified per the sabi-150 spec.
- The server's configured timezone is acceptable for daily scheduling; per-user send times/timezones are out of scope.
- **SMTP is already available**: `spring.mail.*` in `application.yml` reads `SABI_SMTP_HOST/USER/PASSWORD/PORT` from the environment; Spring Mail auto-configuration is already in place. Local development uses fakeSMTP via docker-compose (existing recommendation). No new email SaaS is introduced.
- Sender address defaults to `no-reply@sabi-project.net` (configurable); SPF/DKIM/DMARC for that domain are assumed to be in place — verify during the first live send.
- The feature **complements** the existing in-app measurement reminder (`/api/measurement/reminder/*`) and does not replace it.
- Reminder emails are sent in HTML with a plain-text fallback; no tracking pixels are embedded (privacy).
- Overdue units that have never been measured are listed in the email with a localized "no measurement recorded yet" placeholder instead of a last value/date.

---

## Clarifications

### Session 2026-10-24

- Q: How should the scheduler run (FR-003 only said "at least once per day", while SC-002 requires a verifiable scheduled send time)? → A: **Fixed daily time** — default 09:00 in the server's configured timezone, configurable via a property. SC-002 stays valid against that time.
- Q: How are retries modelled given the unique constraint on (userId, cycleDate)? → A: **One record per user + cycle with an `attempts` counter** (0–3). Transient failure → attempts+1, next send date not advanced, retried on the next run. After 3 attempts (or a permanent rejection) the cycle is closed and the next send date advances by the interval.
- Q: Should the first email be delayed by a full interval even if units are already overdue at enablement? → A: **No — send immediately if overdue.** On first enablement with existing overdue units, next send date = today (email on the next scheduler run). Without overdue units, next send date = today + interval. The rule applies to enablement only; changing the interval still recomputes as *today + new interval* (FR-009).
- Q: Is the email interval a check frequency (evaluate every N days, list units overdue at that moment)? → A: **Yes — check-frequency semantics confirmed.** The user-level interval is the evaluation cadence; per-unit `pastdays` from the in-app reminders determine what is overdue. A unit may be up to one interval past its own due date when it first appears in an email.
- Q: What happens to `ReminderDeliveryRecord` rows on account deletion, and how long are they kept? → A: **Cascade delete only.** Delivery records are removed with the account; no periodic retention purge.
