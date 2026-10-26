# Data Model: Email Measurement Reminders (sabi-69)

**Feature**: `.specify/features/sabi-69-email-measurement-reminders/spec.md`
**Date**: 2026-10-24

Entities are mapped to the existing JPA/EclipseLink model in `sabi-server`
(`de.bluewhale.sabi.persistence.model`). All table names/columns follow the existing
snake_case convention of schema `sabi`. Flyway migration:
`sabi-database/src/main/resources/db/migration/version1_8_0/V1_8_0_14__addEmailReminderColumns.sql`
(no hardcoded schema prefix, per AGENTS.md).

---

## 1. UserEntity (modified) — table `users`

Three new attributes, all on the existing entity
(`sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/UserEntity.java`):

| Field (Java) | Column | Type | Constraints | Default | Notes |
|---|---|---|---|---|---|
| `emailReminderEnabled` | `email_reminder_enabled` | `boolean` | NOT NULL | `false` | Opt-in flag (FR-001) |
| `emailReminderIntervalDays` | `email_reminder_interval_days` | `Integer` (int) | NOT NULL, 1–30 | `7` | Cadence in days; "weekly" preset = 7 (FR-002). Range enforced in application code (client + server validation); DB-level CHECK optional |
| `emailReminderNextSendDate` | `email_reminder_next_send_date` | `LocalDate` (DATE) | NULL allowed | `NULL` | Set on enablement / after each closed cycle; cleared on disable (FR-010). NULL while disabled |

New collection on `UserEntity` (GDPR cascade, see R12 in research.md):

```java
@OneToMany(fetch = FetchType.LAZY, mappedBy = "user",
           cascade = CascadeType.REMOVE, orphanRemoval = true)
private List<ReminderDeliveryRecordEntity> reminderDeliveryRecords = new ArrayList<>();
```

Also added to the `@EqualsAndHashCode(exclude = {...})` list (existing pattern for
collections).

**Validation rules**:
- `emailReminderIntervalDays` ∈ [1, 30] whenever `emailReminderEnabled == true`
  (server-side rejection of the whole profile update otherwise, FR-002 / US2 scenario 3).
- `emailReminderNextSendDate` is never set while `emailReminderEnabled == false`.

**Index**: composite index on `(email_reminder_enabled, email_reminder_next_send_date)` to
serve the scheduler's daily due-user query (Performance gate, Constitution II).

## 2. ReminderDeliveryRecordEntity (new) — table `reminder_delivery_record`

Persisted per user + cycle for idempotency (FR-004) and auditability (FR-007).

| Field (Java) | Column | Type | Constraints | Notes |
|---|---|---|---|---|
| `id` | `id` | `Long` | PK, AUTO_INCREMENT | Identity strategy (existing pattern) |
| `user` | `user_id` | `Long` FK → `users.id` | NOT NULL, ON DELETE CASCADE | Owner-side `@ManyToOne(fetch = LAZY)` |
| `cycleDate` | `cycle_date` | `LocalDate` (DATE) | NOT NULL | The due date the cycle refers to (= user's `emailReminderNextSendDate` at evaluation, see R5) |
| `status` | `status` | `String` (VARCHAR 10) | NOT NULL, ∈ {`SENT`, `FAILED`} | Enum stored as string (existing schema style) |
| `attempts` | `attempts` | `int` (INT) | NOT NULL, 0–3 | Incremented per failed send attempt within the cycle (spec clarification 2026-10-24) |
| `sentAt` | `sent_at` | `LocalDateTime` (DATETIME) | NULL until success | Set when the email was actually dispatched |
| `createdOn` / `lastmodOn` / `optlock` | `created_on` / `lastmod_on` / `optlock` | — | via `Auditable` base class | Existing audit columns (NOT NULL / nullable / int default 0) |

**Unique constraint**: `UNIQUE (user_id, cycle_date)` — the idempotency key (FR-004, SC-003).
Duplicate scheduler runs find the existing record and skip.

**Relationships**:
- `UserEntity 1 ── * ReminderDeliveryRecordEntity` (cascade REMOVE + orphanRemoval on the
  `UserEntity` side; DB-level `ON DELETE CASCADE` as defense in depth).
- No relation to measurement entities — the record is about *delivery*, not content.

**State transitions (per cycle)**:

```
                    ┌──────────────┐   send OK        ┌──────────────┐
   due user ──────► │ (no record)  │ ───────────────► │   SENT       │ nextSendDate =
                    └──────┬───────┘                  └──────────────┘ cycleDate + interval
                           │ send fails (transient)
                           ▼
                    ┌──────────────┐  attempts < 3   (retry on next run,
                    │   FAILED     │ ──────────────►  nextSendDate unchanged)
                    └──────┬───────┘
                           │ attempts >= 3  OR  permanent rejection (bounce)
                           ▼
                    cycle closed: status stays FAILED,
                    nextSendDate = cycleDate + interval

   due user with no overdue units ──► no record (no send attempt),
                                       nextSendDate = cycleDate + interval
```

## 3. MeasurementReminderEmail (transient, NOT persisted)

Server-internal record built at send time in `EmailReminderServiceImpl` (package
`de.bluewhale.sabi.services`), consumed by the new `NotificationServiceImpl` method:

| Field | Type | Notes |
|---|---|---|
| `userId` | `Long` | For logging (`user_id=` only — PII rule) |
| `languageCode` | `String` | User's ISO-639-1 language (`UserEntity.language`) → bundle selection |
| `overdueUnits` | `List<OverdueUnit>` | Only active in-app-reminder units that are overdue (FR-008) |
| `measurementPageUrl` | `String` | `{sabi.email.base-url}/secured/measureView.xhtml` |
| `profilePageUrl` | `String` | `{sabi.email.base-url}/secured/userProfile.xhtml` |

Nested `OverdueUnit`:

| Field | Type | Notes |
|---|---|---|
| `unitSign` | `String` | e.g. "NO3 (mg/l)" — from the unit catalogue (`UnitTo.unitSign`) |
| `lastValue` | `Float` (nullable) | Last `MeasurementEntity.measuredValue`; NULL when never measured |
| `lastMeasuredOn` | `LocalDateTime` (nullable) | Last `MeasurementEntity.measuredOn`; NULL when never measured → localized "no measurement recorded yet" placeholder (spec assumption) |

## 4. ReminderContentProvider (SPI, FR-011)

```java
public interface ReminderContentProvider {
    /** Stable type identifier, e.g. "MEASUREMENT". */
    String type();

    /** Build the email payload for this user/cycle. Empty = nothing to send
     *  (cycle closes without an email, next send date advances). */
    Optional<MeasurementReminderEmail> buildContent(UserEntity user, LocalDate cycleDate);
}
```

The first (and only in this iteration) implementation: `MeasurementReminderContentProvider`,
which applies the overdue rule from FR-008 (active in-app reminder + last measurement older
than `pastdays`, or never measured). The scheduler core iterates all registered providers;
adding a future type (maintenance notice) = new bean + new template, no core changes (SC-006).

## 5. Flyway migration `V1_8_0_14__addEmailReminderColumns.sql` (shape)

```sql
-- users: reminder settings
ALTER TABLE users ADD COLUMN email_reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN email_reminder_interval_days INT NOT NULL DEFAULT 7;
ALTER TABLE users ADD COLUMN email_reminder_next_send_date DATE NULL;
CREATE INDEX idx_users_email_reminder_due
    ON users (email_reminder_enabled, email_reminder_next_send_date);

-- delivery records
CREATE TABLE reminder_delivery_record (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    cycle_date   DATE NOT NULL,
    status       VARCHAR(10) NOT NULL,
    attempts     INT NOT NULL DEFAULT 0,
    sent_at      DATETIME NULL,
    created_on   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmod_on   TIMESTAMP NULL,
    optlock      INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_reminder_delivery_user_cycle UNIQUE (user_id, cycle_date),
    CONSTRAINT fk_reminder_delivery_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

(Exact column definitions follow the conventions of existing migrations, e.g.
`V1_8_0_3__addInvertebrateStockTable.sql`; no schema prefix anywhere.)

## 6. i18n key sets (release blocker: all 6 bundles)

**Server — `i18n/ReminderEmailMessages`** (new, 6 files): subject, greeting, intro, table
headers (unit / last value / date), "no measurement recorded yet" placeholder, link labels
(measurement page, profile settings), footer. → see [contracts/email-template.md](./contracts/email-template.md).

**Webclient — `i18n/messages*.properties`** (existing, 6 files): panel header/description,
toggle label, interval label, "weekly" preset label, range-validation error, save
confirmation. Naming follows the existing `userprofile.*` prefix convention.

**GDPR — `gdpr.xhtml` + webclient i18n**: one new disclosure paragraph stating that reminder
emails contain a summary of the user's own measurements (FR-012), in all 6 bundles.
