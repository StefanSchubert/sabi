# Contract: Maintenance REST API

**Feature**: `007-water-maintenance-tracking` | **Date**: 2026-10-26
**Base path**: `/api/tank/{tankId}/maintenance` (built by the webclient gateway as `sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + tankId + "/maintenance…"` — dosing precedent, no new `Endpoint` enum entry)

## Authentication & authorization (all endpoints)

- **Auth**: JWT in `Authorization: Bearer <token>` header (`de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN`), validated by `JWTAuthorizationFilter`. Unauthenticated ⇒ 401/403 via Spring Security (FR-018).
- **Ownership**: every operation resolves the caller (`UserRepository.getByEmail`) and loads the tank with `AquariumRepository.getAquariumEntityByIdAndUser_IdIs(tankId, userId)`. Non-owner or unknown tank ⇒ **403** on all six endpoints (including GET — SC-006). Record-level operations additionally require `findByIdAndAquariumId` (record must belong to the same tank).
- **PII**: logs use `aquarium_id=` only, never the caller's email.
- **Status code convention**: 403 (not 404) for non-owner access, consistent with the dosing controller; 409 on optimistic-lock conflict (PUT); 400 on Bean Validation failure.

## Endpoints

### 1. GET `/api/tank/{tankId}/maintenance`

Returns the full maintenance block for the tank (profile + water change records, newest first).

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
  "waterChanges": [
    { "id": 12, "changedOn": "2026-10-20", "percentage": 25, "note": null,
      "createdOn": "2026-10-20T18:05:00", "updatedOn": "2026-10-20T18:05:00", "optlock": 0 }
  ]
}
```
- **Defaults** (no profile row): `profile` = `{ regularIntervalDays: null, regularPercentage: null, carbonEnabled: false, carbonMode: null, carbonReplacementIntervalDays: null }`; `waterChanges` = `[]`.
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

### 4. POST `/api/tank/{tankId}/maintenance/water-changes`

Creates a water change record (US1). Back-dating (past dates) allowed; future dates rejected.

- **Body**: `WaterChangeTo` — `{ "changedOn": "2026-10-26", "percentage": 25, "note": "optional, max 200 chars" }` (`aquariumId` in the body is ignored; taken from the path).
- **Validation**: `changedOn` required + not in future; `percentage` required, integer 1–100; `note` ≤ 200 chars ⇒ **400** otherwise.
- **201** → created `WaterChangeTo` (with `id`, audit fields, `optlock`) | **403** non-owner.

### 5. PUT `/api/tank/{tankId}/maintenance/water-changes/{recordId}`

Updates an existing record (US1 scenario 6). Same validation as POST. The client must echo `optlock`; mismatch ⇒ **409**.

- **200** → updated `WaterChangeTo` | **403** non-owner / record not in this tank | **409** optimistic lock.

### 6. DELETE `/api/tank/{tankId}/maintenance/water-changes/{recordId}`

Deletes a record (US1 scenario 6).

- **200** (empty body) | **403** non-owner / record not in this tank.

## Error format

Bean Validation failures use the standard Spring `MethodArgumentNotValidException` response (400) — identical to the dosing endpoints. Ownership errors return a bare 403 (dosing controller precedent: `errorOr(...)` maps `Message.CATEGORY.ERROR` → 403).

## OpenAPI

All endpoints carry `@Operation`/`@ApiResponses` annotations (springdoc 3.x), matching the dosing controller's documentation style — constitution III requires current OpenAPI docs.
