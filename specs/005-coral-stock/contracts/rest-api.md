# REST API Contracts: 005-coral-stock

**Phase 1 output for `/speckit.plan`**  
**Date**: 2026-05-22

All new endpoints are under `/api/` (C-5) and protected by Bearer JWT (FR-038).  
Auth header: `Authorization: Bearer <token>` (read from `HttpHeader.AUTH_TOKEN`).

---

## CoralStockController — `api/coral`

### GET `/api/coral/{aquariumId}/list`
List all coral entries (active + departed) for an aquarium.

| | |
|---|---|
| **Auth** | JWT required |
| **Path** | `aquariumId` : Long |
| **Response 202** | `List<CoralStockEntryTo>` |
| **Response 403** | Aquarium does not belong to caller |

### POST `/api/coral/`
Create a new coral stock entry.

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralStockEntryTo` (JSON, `@Valid`) |
| **Response 201** | `ResultTo<CoralStockEntryTo>` |
| **Response 400** | Validation error |
| **Response 403** | Aquarium not owned by caller |

**Service-layer guards**: aquarium ownership check; `waterType == MARINE` (C-8).

### GET `/api/coral/{coralId}`
Get a single coral entry.

| | |
|---|---|
| **Auth** | JWT required |
| **Response 202** | `CoralStockEntryTo` |
| **Response 403** | Not caller's coral |

### PUT `/api/coral/{coralId}`
Update an existing coral entry.

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralStockEntryTo` (JSON, `@Valid`) |
| **Response 202** | `ResultTo<CoralStockEntryTo>` |
| **Response 400** | Validation error |
| **Response 403** | Not caller's coral |

### DELETE `/api/coral/{coralId}`
Physically delete a coral entry (only allowed when no departure record exists — FR-012).

| | |
|---|---|
| **Auth** | JWT required |
| **Response 204** | Deleted |
| **Response 403** | Not caller's coral |
| **Response 409** | Entry has a departure record → cannot delete |

### PUT `/api/coral/{coralId}/departure`
Record a coral departure.

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralDepartureRecordTo` (JSON, `@Valid`) |
| **Response 202** | `ResultTo<CoralStockEntryTo>` |
| **Response 400** | Missing required fields |
| **Response 403** | Not caller's coral |
| **Response 422** | `departureDate` before `addedOn` |

### DELETE `/api/coral/{coralId}/catalogue-link`
Remove the catalogue link from a coral entry.

| | |
|---|---|
| **Auth** | JWT required |
| **Response 202** | `ResultTo<CoralStockEntryTo>` |
| **Response 403** | Not caller's coral |

### POST `/api/coral/{coralId}/photo` (multipart/form-data)
Upload a photo for a coral entry.

| | |
|---|---|
| **Auth** | JWT required |
| **Param** | `file`: `MultipartFile` (MANDATORY: use `MultipartFile`, not `byte[]`) |
| **Constraints** | Max 5 MB; JPEG/PNG/WebP/GIF; magic-byte validation |
| **Response 204** | Uploaded |
| **Response 400** | Invalid format or size exceeded |

### GET `/api/coral/{coralId}/photo`
Download the photo for a coral entry (ownership-gated).

| | |
|---|---|
| **Auth** | JWT required |
| **Response 200** | `byte[]` with `Content-Type: image/jpeg` |
| **Response 404** | No photo |
| **Response 403** | Not caller's coral |

### DELETE `/api/coral/{coralId}/photo`
Delete the photo.

| | |
|---|---|
| **Auth** | JWT required |
| **Response 204** | Deleted |

---

## Growth History — sub-resource of CoralStockController

### GET `/api/coral/{coralId}/growth`
List growth measurements (newest first).

| | |
|---|---|
| **Auth** | JWT required |
| **Response 200** | `List<CoralGrowthHistoryTo>` |
| **Response 403** | Not caller's coral |

### POST `/api/coral/{coralId}/growth`
Add a new growth measurement.

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralGrowthHistoryTo` (JSON, `@Valid`) |
| **Response 201** | `ResultTo<CoralGrowthHistoryTo>` |
| **Response 400** | Validation error (value ≤ 0, date in future, BRANCH_COUNT not integer) |
| **Response 403** | Not caller's coral |
| **Response 422** | Date after departure date |

### PUT `/api/coral/{coralId}/growth/{recordId}`
Edit an existing growth record (date and value only; type is immutable — FR-039).

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralGrowthHistoryTo` (type field ignored) |
| **Response 202** | `ResultTo<CoralGrowthHistoryTo>` |
| **Response 403** | Not caller's coral |
| **Response 422** | Date after departure date |

### DELETE `/api/coral/{coralId}/growth/{recordId}`
Delete a single growth record.

| | |
|---|---|
| **Auth** | JWT required |
| **Response 204** | Deleted |
| **Response 403** | Not caller's coral |

---

## Polyp Condition History — sub-resource of CoralStockController

### GET `/api/coral/{coralId}/polyp`
List polyp condition observations (newest first).

| | |
|---|---|
| **Auth** | JWT required |
| **Response 200** | `List<CoralPolypConditionTo>` |

### POST `/api/coral/{coralId}/polyp`
Add a new polyp condition observation.

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralPolypConditionTo` (JSON, `@Valid`) |
| **Response 201** | `ResultTo<CoralPolypConditionTo>` |
| **Response 400** | Missing fields |
| **Response 422** | Date after departure date |

### PUT `/api/coral/{coralId}/polyp/{recordId}`
Edit an existing polyp condition record (FR-040).

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralPolypConditionTo` |
| **Response 202** | `ResultTo<CoralPolypConditionTo>` |

### DELETE `/api/coral/{coralId}/polyp/{recordId}`
Delete a single polyp observation.

| | |
|---|---|
| **Auth** | JWT required |
| **Response 204** | Deleted |

---

## CoralCatalogueController — `api/coral/catalogue`

### GET `/api/coral/catalogue`
List all PUBLIC entries + caller's own PENDING entries.

| | |
|---|---|
| **Auth** | JWT required |
| **Params** | `lang` (default `en`) |
| **Response 200** | `List<CoralCatalogueSearchResultTo>` |

### GET `/api/coral/catalogue/search`
Search catalogue by scientific name or common name (partial match, min 2 chars — FR-030).

| | |
|---|---|
| **Auth** | JWT required |
| **Params** | `q` (min 2 chars), `lang` (default `en`) |
| **Response 202** | `List<CoralCatalogueSearchResultTo>` |
| **Response 400** | Query too short |

### POST `/api/coral/catalogue`
Propose a new catalogue entry (FR-024).

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralCatalogueEntryTo` (JSON, `@Valid`) |
| **Response 201** | `ResultTo<CoralCatalogueEntryTo>` with status=PENDING |
| **Response 400** | Validation error |
| **Response 409** | Scientific name already exists as PENDING or PUBLIC (non-blocking warning in spec; returns 201 with warning message field) |

### GET `/api/coral/catalogue/{id}`
Get a single catalogue entry (PUBLIC or own PENDING only).

| | |
|---|---|
| **Auth** | JWT required |
| **Response 200** | `CoralCatalogueEntryTo` |
| **Response 403** | Entry is PENDING and not owned by caller |

### PUT `/api/coral/catalogue/{id}`
Update a catalogue entry (creator for PENDING/PUBLIC; admin for any — FR-029).

| | |
|---|---|
| **Auth** | JWT required |
| **Body** | `CoralCatalogueEntryTo` (JSON, `@Valid`) |
| **Response 202** | `ResultTo<CoralCatalogueEntryTo>` |
| **Response 403** | Not creator and not admin |

---

## CoralCatalogueAdminController — `api/admin/coral/catalogue`

All endpoints require `ADMIN` role (verified in service layer via `isAdmin()` check, consistent with `FishCatalogueAdminController`).

### GET `/api/admin/coral/catalogue/pending`
List all PENDING proposals (FR-031).

| | |
|---|---|
| **Auth** | JWT + ADMIN role |
| **Response 200** | `List<CoralCatalogueEntryTo>` sorted by `proposalDate` descending |

### PUT `/api/admin/coral/catalogue/{id}/approve`
Approve a PENDING proposal → status becomes PUBLIC (FR-026).

| | |
|---|---|
| **Auth** | JWT + ADMIN role |
| **Response 202** | `ResultTo<CoralCatalogueEntryTo>` |
| **Response 403** | Not admin |

### PUT `/api/admin/coral/catalogue/{id}/reject`
Reject a PENDING proposal → status becomes REJECTED (FR-027).

| | |
|---|---|
| **Auth** | JWT + ADMIN role |
| **Body** | Optional rejection reason (String) |
| **Response 202** | `ResultTo<CoralCatalogueEntryTo>` |

### PUT `/api/admin/coral/catalogue/{id}`
Edit any field of any catalogue entry regardless of status (FR-028).

| | |
|---|---|
| **Auth** | JWT + ADMIN role |
| **Body** | `CoralCatalogueEntryTo` |
| **Response 202** | `ResultTo<CoralCatalogueEntryTo>` |

---

## Modified Existing Endpoints

### PublicReportLinkController (MODIFIED)
- `POST /api/report-link` and `PUT /api/report-link/{id}` — add `includeCorals` boolean field handling
- No breaking change to existing callers (defaults to `false`)

### PublicReportController (MODIFIED)
- `GET /api/public/report/{token}` — when `includeCorals == true` on the link, populate `coralInhabitants` in `PublicReefReportTo`

### ReefDataExportController (MODIFIED)
- AI export endpoint — populate `corals` array in each `AquariumExportTo` (FR-035, FR-036)

