# API Contract: Reef Data Export Endpoint (001-ai-data-export)

**Phase**: 1 — Design  
**Date**: 2026-04-04  
**Module**: `sabi-server` (`UserProfileController`) + `sabi-boundary` (`Endpoint` enum)  
**Status**: Complete

---

## Overview

A single new `GET` endpoint is added to the existing `UserProfileController`. It returns the authenticated user's complete reef data as a downloadable JSON file. The endpoint is secured by the existing JWT filter — no new security configuration is required.

---

## Endpoint: GET /api/userprofile/export

### Summary

Assemble and download the authenticated user's complete reef data (aquariums, measurements, plague records, fish, corals, treatments) as a structured JSON file suitable for use with AI chatbot consultations.

### Security

- **Sabi JWT required** — standard `Authorization` header (same as all existing authenticated endpoints)
- `Principal` is injected by Spring Security after JWT validation; user identity is derived from `principal.getName()` (email)
- No new security configuration needed

### Request

| Property | Value |
|----------|-------|
| Method | `GET` |
| Path | `/api/userprofile/export` |
| Headers | `Authorization: <sabi-jwt-token>` |
| Body | None |
| Query Parameters | None |

### Response — Success (HTTP 200)

| Property | Value |
|----------|-------|
| HTTP Status | `200 OK` |
| Content-Type | `application/octet-stream` |
| Content-Disposition | `attachment; filename="sabi-reef-data-YYYY-MM-DD.json"` (server-side date, UTC) |
| Body | JSON document (see schema below) |

### Response — Error Cases

| Status | Condition |
|--------|-----------|
| `401 Unauthorized` | Missing or invalid JWT token |
| `500 Internal Server Error` | Unexpected error during data assembly |

---

## Response Body JSON Schema

```json
{
  "_meta": {
    "exportedAt": "string (ISO-8601 UTC)",
    "sabiSchemaVersion": "string (e.g. \"1.0\")",
    "description": "string (fixed English sentence)"
  },
  "aquariums": [
    {
      "id": "number",
      "description": "string",
      "waterType": "string (enum name, e.g. SALT_WATER)",
      "size": "number | null",
      "sizeUnit": "string (enum name, e.g. LITER) | null",
      "active": "boolean",
      "inceptionDate": "string (YYYY-MM-DD) | null",
      "measurements": [
        {
          "measuredOn": "string (ISO-8601 datetime)",
          "measuredValue": "number",
          "unitId": "number",
          "unitSign": "string",
          "unitName": "string | null",
          "unitNameResolved": "boolean"
        }
      ],
      "plagueRecords": [
        {
          "observedOn": "string (ISO-8601 datetime)",
          "plagueId": "number",
          "plagueName": "string | null",
          "plagueNameResolved": "boolean",
          "plagueStatusId": "number",
          "plagueStatusName": "string | null",
          "plagueStatusResolved": "boolean",
          "plagueIntervallId": "number"
        }
      ],
      "fish": [
        {
          "fishCatalogueId": "number",
          "scientificName": "string | null",
          "addedOn": "string (ISO-8601 datetime)",
          "observedBehavior": "string | null"
        }
      ],
      "corals": [
        {
          "coralCatalogueId": "number",
          "scientificName": "string | null",
          "observedBehavior": "string | null"
        }
      ],
      "treatments": [
        {
          "givenOn": "string (ISO-8601 datetime)",
          "amount": "number",
          "unitId": "number",
          "unitSign": "string",
          "unitName": "string | null",
          "remedyId": "number",
          "productName": "string | null",
          "vendor": "string | null",
          "description": "string | null"
        }
      ]
    }
  ]
}
```

**Schema version**: `1.0` — increment in `ReefDataExportTo.SCHEMA_VERSION` for breaking changes (C-3).

---

## Audit Log Event (FR-014)

| Event | Log Level | Fields |
|-------|-----------|--------|
| Successful export | `INFO` | Timestamp (implicit via SLF4J), `userId=<sha256-hash>` |

No PII (email, username) in log output.

---

## Endpoint Enum Change (sabi-boundary)

Add to `de.bluewhale.sabi.api.Endpoint`:

```java
USER_PROFILE_EXPORT("/api/userprofile/export"),
```

---

## OpenAPI Annotation Target

Controller: `de.bluewhale.sabi.rest.controller.UserProfileController`  
New method: `getReefDataExport(String token, Principal principal)`  
Annotations:

```java
@Operation(summary = "Download complete reef data as JSON for AI chatbot consultations.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "OK — JSON file download initiated."),
    @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid token.")
})
@GetMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
```

SpringDoc will expose this under the existing `/v3/api-docs` endpoint automatically.

---

## Webclient → Backend Integration Detail

The `UserServiceImpl.downloadReefDataExport(token)` method:

1. Constructs URL: `sabiBackendUrl + Endpoint.USER_PROFILE_EXPORT.getPath()`
2. Performs authenticated `GET` with `Authorization` header
3. Returns response body as `byte[]`
4. `UserProfileView.downloadReefData()` receives the bytes and writes them to `FacesContext.getExternalContext().getResponseOutputStream()` with headers:
   - `Content-Type: application/octet-stream`
   - `Content-Disposition: attachment; filename="sabi-reef-data-<date>.json"`
5. Calls `FacesContext.responseComplete()` to prevent JSF lifecycle from continuing

---

*Contract complete. Proceed to `/speckit.tasks` for implementation task breakdown.*

