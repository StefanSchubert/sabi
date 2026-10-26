# Quickstart: Validate Email Measurement Reminders (sabi-69)

**Feature**: `.specify/features/sabi-69-email-measurement-reminders/spec.md`
**Date**: 2026-10-24

Runnable validation scenarios that prove the feature works end-to-end. Implementation details
live in `tasks.md` / the implementation phase — this file is a validation/run guide only.

## Prerequisites

- Docker available; `devops/sabi_docker_sdk/docker-compose.yml` runs the stack **including
  fakeSMTP** (`gessnerfl/fake-smtp-server:2.5.0`, port 2525) — no new services needed.
- MariaDB container up; Flyway has applied `V1_8_0_14__addEmailReminderColumns.sql`
  (runs automatically on backend start, or `docker compose run --rm flyway`).
- SMTP env vars point at fakeSMTP for local runs (already the `application.yml` defaults:
  host `localhost`, port `2525`).

## Scenario A — Automated tests (primary proof)

```bash
# From repo root. Runs the new scheduler / service / profile tests (Testcontainers MariaDB).
mvn -pl sabi-server test \
  -Dtest='EmailReminder*,UserProfileController*' | tee /tmp/sabi69-tests.out
```

**Expected outcomes** (map to spec success criteria):
- `SC-001/FR-001`: enabling with overdue units sets next send date to *today*; without
  overdue units to *today + interval*.
- `SC-002/FR-003,FR-005`: advancing the fake clock past the due date and running the
  scheduler dispatches **exactly one** localized email with the overdue list + links.
- `SC-003/FR-004`: forcing a double scheduler run on the same day sends **no second** email
  (unique `user_id + cycle_date` record).
- `FR-002`: interval 0 and 31 are rejected with a validation message; 1–30 accepted.
- `FR-009/FR-010`: interval change recomputes from *now*; disable clears the next send date
  and stops sends.
- Edge case: SMTP failure marks the cycle `FAILED`, increments `attempts`, does **not**
  advance the next send date; after 3 attempts the cycle closes and advances.

## Scenario B — Manual end-to-end with fakeSMTP

1. Start the stack:
   ```bash
   cd devops/sabi_docker_sdk && docker compose up -d | tee /tmp/compose-up.out
   ```
2. Log in to the webclient, open **Profile → Email reminders** (≤ 3 clicks from login).
3. Ensure at least one in-app measurement reminder exists and is **overdue** (last
   measurement older than its interval).
4. Enable email reminders with the default **weekly** preset and save.
5. Trigger one scheduler evaluation (dev-only: call the scheduler bean via a temporary
   `/actuator`-gated test endpoint, or run the integration test from Scenario A against the
   live DB). Do **not** wait for 09:00.
6. Open fakeSMTP's web UI at `http://localhost:2525`.

**Expected outcomes**:
- Exactly one email from `no-reply@sabi-project.net` to the user's address.
- Subject + greeting + table list **only** the overdue units that have an active in-app
  reminder; never-measured units show the localized "no measurement recorded yet" text.
- Two working links: measurement page and profile settings.
- The email is rendered in the user's configured language (switch the profile language and
  re-run to spot-check a second locale).
- Re-running the scheduler the same day produces **no** additional email.

## Scenario C — i18n completeness (release blocker check, SC-005)

```bash
# All new keys present in every bundle (server email bundles + webclient UI bundles).
python3 /tmp/check_i18n_keys.py | tee /tmp/sabi69-i18n.out   # script lists expected keys per file
```

Expected: zero missing keys across the 6 `ReminderEmailMessages*` files and the 6
webclient `messages*.properties` files; no missing-translation warnings at startup.

## Scenario D — GDPR cascade (edge case)

1. With a delivery record present for a user, delete the account via the existing
   self-service unregister flow.
2. Verify `reminder_delivery_record` contains no row for the deleted user and the three new
   `users` columns are gone with the account.

**Expected**: cascade delete via JPA (`orphanRemoval`) and/or DB `ON DELETE CASCADE`; no
orphaned PII rows.

## Scenario E — No regression on existing flows (SC-004)

```bash
mvn -pl sabi-server test | tee /tmp/sabi69-regression.out
```

Expected: existing profile-update, in-app reminder (`/api/measurement/reminder/*`), and
measurement tests pass unchanged; no measurable latency change on existing endpoints
(scheduler runs off-request).

## Where to look

- Delivery records: `SELECT user_id, cycle_date, status, attempts, sent_at FROM reminder_delivery_record;`
- Scheduler log lines: `user_id=` only (no email addresses) — grep the backend log for
  `reminder` to confirm the PII rule.
- fakeSMTP web UI: `http://localhost:2525` (full email source, headers, body).
