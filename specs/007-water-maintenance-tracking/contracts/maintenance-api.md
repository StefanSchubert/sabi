# Contract: Maintenance REST API

**Feature**: `007-water-maintenance-tracking` | **Date**: 2026-10-26 (reworked 2026-11-06)
**Base path**: `/api/tank/{tankId}/maintenance` (built by the webclient gateway as `sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + tankId + "/maintenance…"` — dosing precedent, no new `Endpoint` enum entry)

> **Rework 2026-11-06**: the record endpoints were generalized from `/water-changes` to
> `/events` and now cover both `WATER_CHANGE` and `CARBON_REPLACEMENT` events
> (`MaintenanceEventTo` with a `maintenanceType` discriminator). The profile endpoints
> (interval, activated carbon) are unchanged.

## Authentication & authorization (all endpoints)

- **Auth**: JWT in `Authorization: Bearer <token>` header (`de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN`), validated by `JWTAuthorizationFilter`. Unauthenticated ⇒ 401/403 via Spring Security (FR-018).
- **Ownership**: every tank-scoped operation resolves the caller (`UserRepository.getByEmail`) and loads the tank with `AquariumRepository.getAquariumEntityByIdAndUser_IdIs(tankId, userId)`. Non-owner or unknown tank ⇒ **403** on all six tank-scoped endpoints (including GET — SC-006). Record-level operations additionally require `findByIdAndAquariumId` (record must belong to the same tank). The user-scoped suggestions endpoint (#7) has no tank ownership check.
- **PII**: logs use `aquarium_id=` only, never the caller's email.
- **Status code convention**: 403 (not 404) for non-owner access, consistent with the dosing controller; 409 on optimistic-lock conflict (PUT); 400 on Bean Validation failure.

## Endpoints

### 1. GET `/api/tank/{tankId}/maintenance`

Returns the full maintenance block for the tank (profile + all recorded events, newest first).

- **200** → `MaintenanceTo` (JSON):
```json
{
  "profile": {
    "regularIntervalDays": 21,
    "regularPercentage": 25,
    "carbonEnabled": true,
    "carbonMode": "INTERVAL",
    "carbonReplacementIntervalDays": 28
  },
  "events": [
    { "id": 12, "maintenanceType": "WATER_CHANGE", "changedOn": "2026-10-20", "percentage": 25, "note": null,
      "createdOn": "2026-10-20T18:05:00", "updatedOn": "2026-10-20T18:05:00", "optlock": 0 },
    { "id": 13, "maintenanceType": "CARBON_REPLACEMENT", "changedOn": "2026-10-18", "percentage": null, "note": "media change",
      "createdOn": "2026-10-18T09:00:00", "updatedOn": "2026-10-18T09:00:00", "optlock": 0 }
  ]
}
```
- **Defaults** (no profile row): `profile` = `{ regularIntervalDays: null, regularPercentage: null, carbonEnabled: false, carbonMode: null, carbonReplacementIntervalDays: null }`; `events` = `[]`.
- **403** non-owner.

### 2. PUT `/api/tank/{tankId}/maintenance/water-change-interval`

Upserts or clears the regular water exchange interval (US1). Idempotent upsert: creates the profile row if missing.

- **Body**: `WaterChangeIntervalTo` — `{ "intervalDays": 21, "percentage": 25 }` or `{ "intervalDays": null, "percentage": null }` (clear).
- **Validation**: both-or-neither; `intervalDays` 1–365; `percentage` 1–100 ⇒ **400** otherwise.
- **200** → updated `MaintenanceTo.profile` | **403** non-owner.

### 3. PUT `/api/tank/{tankId}/maintenance/activated-carbon`

Upserts the activated carbon setting (US3). Idempotent upsert: creates the profile row if missing.

- **Body**: `ActivatedCarbonTo` — `{ "enabled": true, "mode": "INTERVAL", "replacementIntervalDays": 28 }`.
- **Validation**: `enabled` required; enabled ⇒ `mode` required; `mode == "INTERVAL"` ⇒ `replacementIntervalDays` 1–365 required ⇒ **400** otherwise.
- **Behaviour**: `enabled == false` ⇒ `mode` and `replacementIntervalDays` are stored as null (FR-011).
- **200** → updated `MaintenanceTo.profile` | **403** non-owner.

### 4. POST `/api/tank/{tankId}/maintenance/events`

Creates a maintenance event — water change or carbon replacement (US1 + rework 2026-11-06). Back-dating (past dates) allowed; future dates rejected.

- **Body**: `MaintenanceEventTo` —
  - water change: `{ "maintenanceType": "WATER_CHANGE", "changedOn": "2026-10-26", "percentage": 25, "note": "optional, max 200 chars" }`
  - carbon replacement: `{ "maintenanceType": "CARBON_REPLACEMENT", "changedOn": "2026-10-26", "note": "optional, max 200 chars" }`
  - (`aquariumId` in the body is ignored; taken from the path.)
- **Validation** (TO-level `@AssertTrue`, 400 via `RestJSR303ExceptionHandler`): `maintenanceType` required; `changedOn` required + not in future; `percentage` integer 1–100 **required for `WATER_CHANGE`**, **must be absent for `CARBON_REPLACEMENT`**; `note` ≤ 200 chars.
- **201** → created `MaintenanceEventTo` (with `id`, audit fields, `optlock`) | **403** non-owner.

### 5. PUT `/api/tank/{tankId}/maintenance/events/{eventId}`

Updates an existing event (US1 scenario 6). Same validation as POST. The client must echo `optlock`; mismatch ⇒ **409**.

- **The `maintenanceType` of a record is fixed at creation** — the merge ignores any type in the payload; updating a `WATER_CHANGE` whose merged percentage ends up null is rejected (403).
- **200** → updated `MaintenanceEventTo` | **403** non-owner / record not in this tank | **409** optimistic lock.

### 6. DELETE `/api/tank/{tankId}/maintenance/events/{eventId}`

Deletes an event (US1 scenario 6).

- **200** (empty body) | **403** non-owner / record not in this tank.

### 7. GET `/api/tank/dosing/suggestions` (rework 2026-11-06, issue 4)

Distinct product names, categories and dosing methods the user has already recorded in any of their **active** tanks — backs the autocomplete of the dosing form. Lives in `DosingController` (same `api/tank` base path, no `{tankId}` segment).

- **200** → `DosingSuggestionsTo` (JSON):
```json
{ "productNames": ["iodine", "magnesium"], "categories": ["trace element"], "dosingMethods": ["drop"] }
```
- Lists are case-insensitive deduplicated, sorted alphabetically; blank values ignored; empty lists when the user has no dosing records.
- No ownership check on a specific tank (user-scoped read); unauthenticated ⇒ 401/403 via Spring Security.

## Error format

Bean Validation failures use the standard Spring `MethodArgumentNotValidException` response (400) — identical to the dosing endpoints. Ownership errors return a bare 403 (dosing controller precedent: `errorOr(...)` maps `Message.CATEGORY.ERROR` → 403).

## OpenAPI

All endpoints carry `@Operation`/`@ApiResponses` annotations (springdoc 3.x), matching the dosing controller's documentation style — constitution III requires current OpenAPI docs.
