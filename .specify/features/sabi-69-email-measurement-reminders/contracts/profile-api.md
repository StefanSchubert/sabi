# Contract: Profile API Extension (PUT /sabi/api/userprofile)

**Feature**: sabi-69 — Email Measurement Reminders
**Date**: 2026-10-24

The existing endpoint `PUT /api/userprofile` (controller `UserProfileController`, context
path `/sabi`) is **extended, not changed**: two optional fields are added to the request and
response body. Old clients that omit them keep working (backward compatibility, Constitution
III). OpenAPI documentation via springdoc updates automatically from the annotated DTO.

## Request body — `UserProfileTo` (sabi-boundary)

| Field | Type | Required | Default | Validation |
|---|---|---|---|---|
| `language` | string (ISO-639-1) | yes | `"en"` | unchanged |
| `country` | string (ISO-3166-1 alpha-2) | yes | `"US"` | unchanged |
| `measurementReminderTos` | list (existing) | no | `[]` | unchanged |
| `darkModeEnabled` | boolean | no | `false` | unchanged |
| **`emailReminderEnabled`** | boolean | no | `false` | new (FR-001) |
| **`emailReminderIntervalDays`** | integer \| null | no | `7` (when enabled) | new: **1–30 inclusive** whenever `emailReminderEnabled == true`; `null` = "unchanged/absent" |

### Example request (enable weekly reminders)

```json
{
  "language": "de",
  "country": "DE",
  "measurementReminderTos": [
    { "unitId": 3, "unitName": "NO3 (mg/l)", "pastDays": 7, "active": true }
  ],
  "darkModeEnabled": false,
  "emailReminderEnabled": true,
  "emailReminderIntervalDays": 7
}
```

## Server-side state machine (applied in `UserServiceImpl.updateProfile`)

| Transition | Effect on `emailReminderNextSendDate` |
|---|---|
| enable (off → on) with ≥ 1 overdue unit | `today` — first email on the next scheduler run (FR-001) |
| enable (off → on) without overdue units | `today + intervalDays` |
| interval change while enabled | `today + newIntervalDays` (recomputed from now, FR-009) |
| disable (on → off) | `NULL` — no further sends (FR-010) |
| interval out of range 1–30 while enabled | **reject the whole update** (see errors) |

## Response

`200 OK` — returns the updated `UserProfileTo` (existing behavior), now including the two
new fields.

## Errors

| Condition | HTTP | Behavior |
|---|---|---|
| `emailReminderEnabled == true` and `emailReminderIntervalDays` ∉ [1, 30] | `409 CONFLICT` (existing error channel of this endpoint) | Profile update rejected; webclient shows the localized range message (`userprofile.emailreminder.interval.error.range`) |
| Unknown user / token issues | unchanged (`401`/`409` as today) | — |

## Client-side validation (webclient, `userProfile.xhtml`)

- Interval input: PrimeFaces `p:inputNumber` with `min="1" max="30"` (client-side rejection
  before submit) — mirrors the server rule (Constitution IV: client **and** server validation).
- "Weekly" preset = selecting 7 days (FR-002).
- Save goes through the existing `UserProfileView.save()` → single `PUT` round-trip.

## Non-goals

- No new endpoint, no new auth flow — the standard ownership pattern
  (principal email → `UserEntity` via `userRepository.getByEmail`) already applies inside
  `updateProfile`.
- No per-aquarium or per-unit email settings (out of scope per spec).
