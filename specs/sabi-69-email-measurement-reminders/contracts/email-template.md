# Contract: Measurement Reminder Email

**Feature**: sabi-69 — Email Measurement Reminders
**Date**: 2026-10-24

Defines the structure, localization keys, and delivery parameters of the reminder email sent
by `NotificationServiceImpl.sendMeasurementReminderEmail(...)`.

## Delivery parameters (FR-006)

| Parameter | Value / source |
|---|---|
| From | `sabi.email.no-reply-address` (default `no-reply@sabi-project.net`) — **not** `spring.mail.username` |
| To | User's verified email address (`UserEntity.email`) |
| Subject | Localized, key `reminder.email.subject` |
| Format | HTML with plain-text alternative (multipart/alternative via `MimeMessageHelper(..., true)`), **no tracking pixels** (privacy) |
| SMTP | Existing `spring.mail.*` auto-configuration (`SABI_SMTP_*` env vars) |

## Message structure (FR-005)

1. **Subject line** — localized, states the purpose (measurement reminder).
2. **Greeting** — localized, addresses the user by first name/username.
3. **Intro sentence** — explains that one or more measurements are due.
4. **Overdue-units table** — one row per overdue unit (FR-008: only units with an active
   in-app reminder that are currently overdue):

   | Column | Content |
   |---|---|
   | Unit | `unitSign` (e.g. "NO3 (mg/l)") |
   | Last value | `lastValue` formatted with the unit, **or** the localized "no measurement recorded yet" placeholder when never measured |
   | Last measured | `lastMeasuredOn` formatted in the user's locale, **or** the same placeholder |

5. **Measurement-page link** — `{sabi.email.base-url}/secured/measureView.xhtml`, localized
   link label.
6. **Profile-settings link** — `{sabi.email.base-url}/secured/userProfile.xhtml`, localized
   link label (where reminders can be changed or disabled).
7. **Footer** — short note on how to adjust/disable the cadence (points at the profile link).

## Localization keys — `i18n/ReminderEmailMessages` (6 bundles, server-side)

All keys MUST exist in all 6 files (`ReminderEmailMessages.properties` fallback +
`_de/_en/_es/_fr/_it`). Missing keys are a release blocker (SC-005).

| Key | Purpose | Placeholders |
|---|---|---|
| `reminder.email.subject` | Subject line | — |
| `reminder.email.greeting` | Greeting | `{0}` = user name |
| `reminder.email.intro` | Intro sentence (measurements due) | — |
| `reminder.email.unit.header` | Table header: unit | — |
| `reminder.email.value.header` | Table header: last value | — |
| `reminder.email.date.header` | Table header: last measured | — |
| `reminder.email.no_measurement` | Placeholder for never-measured units | — |
| `reminder.email.measurement_link_label` | Link label → measurement page | — |
| `reminder.email.profile_link_label` | Link label → profile settings | — |
| `reminder.email.footer` | Footer / how to change cadence | — |

Date/value formatting uses the user's `Locale` (from `UserEntity.language`) so numbers and
dates render in the user's locale (spec edge case "Timezone/locale").

## Content rules

- **Empty content = no email**: if `ReminderContentProvider.buildContent(...)` returns empty
  (no overdue units), no email is sent and no delivery record is created; the next send date
  still advances (see data-model.md state machine).
- **PII in logs**: the send path logs `user_id=` only, never the email address (Constitution VI).
- **No per-user send time**: the email is sent at the scheduler's fixed daily run; content
  shows measurement dates in the user's locale but does not promise a local send time.

## Out of scope (this iteration)

- Maintenance-window / sponsor emails (future reminder types on the same SPI — FR-011).
- Open/click tracking pixels (privacy, explicitly out of scope).
