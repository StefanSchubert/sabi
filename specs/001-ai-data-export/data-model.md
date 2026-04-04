# Data Model: AI Chatbot Data Export (001-ai-data-export)

**Phase**: 1 — Design  
**Date**: 2026-04-04  
**Module**: `sabi-boundary` (new TOs) + `sabi-server` (service + controller)  
**Status**: Complete

---

## Overview

No new database tables or Flyway migrations are required. The export assembles data from **existing** entities at runtime into a new set of transient Transfer Objects (TOs) that are serialized to JSON and returned as a file download. All new TOs live in `sabi-boundary`.

---

## New Transfer Objects (sabi-boundary)

### ReefDataExportTo (top-level document)

```
de.bluewhale.sabi.model.ReefDataExportTo
```

| Field | Type | Description |
|-------|------|-------------|
| `_meta` | `ExportMetaTo` | Metadata block (schema version, timestamp, description) |
| `aquariums` | `List<AquariumExportTo>` | All tanks owned by the user, each with nested sub-data |

**Schema version constant**: `SCHEMA_VERSION = "1.0"` — increment on breaking changes (C-3).

---

### ExportMetaTo

```
de.bluewhale.sabi.model.ExportMetaTo
```

| Field | Type | Description |
|-------|------|-------------|
| `exportedAt` | `String` | ISO-8601 UTC timestamp of export generation |
| `sabiSchemaVersion` | `String` | Export schema version (currently `"1.0"`) |
| `description` | `String` | Fixed English sentence: `"Sabi reef data export — all aquariums, measurements, plague records, fish, corals, and treatments for use with AI chatbot consultations."` |

---

### AquariumExportTo

```
de.bluewhale.sabi.model.AquariumExportTo
```

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `id` | `Long` | `AquariumEntity.id` | Internal reference only (links sub-records); not PII |
| `description` | `String` | `AquariumEntity.description` | User-defined tank name |
| `waterType` | `String` | `AquariumEntity.waterType.name()` | e.g., `"SALT_WATER"` |
| `size` | `Integer` | `AquariumEntity.size` | Tank volume |
| `sizeUnit` | `String` | `AquariumEntity.sizeUnit.name()` | e.g., `"LITER"` |
| `active` | `Boolean` | `AquariumEntity.active` | Whether tank is currently active |
| `inceptionDate` | `String` | `AquariumEntity.inceptionDate` | ISO date string (YYYY-MM-DD); null if not set |
| `measurements` | `List<MeasurementExportTo>` | — | Nested measurement records |
| `plagueRecords` | `List<PlagueRecordExportTo>` | — | Nested plague records |
| `fish` | `List<FishExportTo>` | — | Nested fish entries |
| `corals` | `List<CoralExportTo>` | — | Nested coral entries |
| `treatments` | `List<TreatmentExportTo>` | — | Nested treatment records |

---

### MeasurementExportTo

```
de.bluewhale.sabi.model.MeasurementExportTo
```

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `measuredOn` | `String` | `MeasurementEntity.measuredOn` | ISO-8601 datetime |
| `measuredValue` | `Float` | `MeasurementEntity.measuredValue` | Numeric value |
| `unitId` | `Integer` | `MeasurementEntity.unitId` | Raw unit ID (always included) |
| `unitSign` | `String` | `UnitEntity.name` | Abbreviation, e.g., `"PO4"` |
| `unitName` | `String` | `LocalizedUnitEntity.description` (lang=`"en"`) | Human-readable name; null if not resolvable |
| `unitNameResolved` | `Boolean` | — | `true` if `unitName` was resolved; `false` as fallback flag |

---

### PlagueRecordExportTo

```
de.bluewhale.sabi.model.PlagueRecordExportTo
```

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `observedOn` | `String` | `PlagueRecordEntity.observedOn` | ISO-8601 datetime |
| `plagueId` | `Integer` | `PlagueRecordEntity.plagueId` | Raw plague catalogue ID |
| `plagueName` | `String` | `LocalizedPlagueEntity.commonName` (lang=`"en"`) | English common name; null if not resolvable |
| `plagueNameResolved` | `Boolean` | — | Fallback flag |
| `plagueStatusId` | `Integer` | `PlagueRecordEntity.plagueStatusId` | Raw plague status ID |
| `plagueStatusName` | `String` | `LocalizedPlagueStatusEntity.description` (lang=`"en"`) | English status name; null if not resolvable |
| `plagueStatusResolved` | `Boolean` | — | Fallback flag |
| `plagueIntervallId` | `Integer` | `PlagueRecordEntity.plagueIntervallId` | Groups related records for same plague occurrence |

---

### FishExportTo

```
de.bluewhale.sabi.model.FishExportTo
```

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `fishCatalogueId` | `Long` | `FishEntity.fishCatalogueId` | Raw catalogue ID |
| `scientificName` | `String` | `FishCatalogueEntity.scientificName` | Scientific name; null if not resolvable |
| `addedOn` | `String` | `FishEntity.addedOn` | ISO-8601 datetime |
| `observedBehavior` | `String` | `FishEntity.observedBehavior` | User notes; may be null |

---

### CoralExportTo

```
de.bluewhale.sabi.model.CoralExportTo
```

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `coralCatalogueId` | `Long` | `CoralEntity.coralCatalougeId` | Raw catalogue ID (note: typo in source is preserved) |
| `scientificName` | `String` | `CoralCatalogueEntity.scientificName` | Scientific name; null if not resolvable |
| `observedBehavior` | `String` | `CoralEntity.observedBehavior` | User notes; may be null |

---

### TreatmentExportTo

```
de.bluewhale.sabi.model.TreatmentExportTo
```

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `givenOn` | `String` | `TreatmentEntity.givenOn` | ISO-8601 datetime |
| `amount` | `Float` | `TreatmentEntity.amount` | Dosage amount |
| `unitId` | `Integer` | `TreatmentEntity.unitId` | Raw unit ID |
| `unitSign` | `String` | `UnitEntity.name` | Abbreviation |
| `unitName` | `String` | `LocalizedUnitEntity.description` (lang=`"en"`) | Human-readable; null if not resolvable |
| `remedyId` | `Long` | `TreatmentEntity.remedyId` | Raw remedy catalogue ID |
| `productName` | `String` | `RemedyEntity.productname` | Product name; null if not resolvable |
| `vendor` | `String` | `RemedyEntity.vendor` | Vendor name; null if not resolvable |
| `description` | `String` | `TreatmentEntity.description` | User notes; may be null |

---

## New Service (sabi-server)

### ReefDataExportService

```
de.bluewhale.sabi.services.ReefDataExportService  (interface)
de.bluewhale.sabi.services.ReefDataExportServiceImpl  (implementation)
```

**Single public method**:

```java
ReefDataExportTo buildExportForUser(String userEmail);
```

Internally:
1. Resolve user by email → get `userId`
2. Load all `AquariumEntity` for user
3. For each aquarium: load measurements, plague records, fish, corals, treatments
4. Resolve all catalogue references (units, plagues, fish, corals, remedies) using English locale
5. Assemble and return `ReefDataExportTo` (never persisted)
6. Write INFO audit log entry: `log.info("DATA_EXPORT userId={}", sha256(userId))`

---

## New Endpoint (sabi-server)

`GET /api/userprofile/export` — added to existing `UserProfileController`

| Aspect | Value |
|--------|-------|
| HTTP Method | `GET` |
| Path | `/api/userprofile/export` |
| Auth | JWT Bearer token required (standard `AUTH_TOKEN` header) |
| Response Content-Type | `application/octet-stream` |
| Response Header | `Content-Disposition: attachment; filename="sabi-reef-data-{YYYY-MM-DD}.json"` |
| Success | HTTP 200 + JSON body |
| Unauthorized | HTTP 401 |
| Server Error | HTTP 500 (unexpected failure) |

---

## New Endpoint Entry (sabi-boundary)

`Endpoint` enum: add `USER_PROFILE_EXPORT("/api/userprofile/export")`

---

## Webclient Changes (sabi-webclient)

### UserService interface — new method

```java
byte[] downloadReefDataExport(String jwtBackendAuthToken) throws BusinessException;
```

### UserServiceImpl — implementation

Performs `GET /api/userprofile/export` with auth header, returns response body as `byte[]`.

### UserProfileView — new action method

```java
public void downloadReefData()
```

Calls `UserService.downloadReefDataExport(...)`, streams the bytes to the browser via `FacesContext.getCurrentInstance().getExternalContext()` with appropriate `Content-Disposition` and `Content-Type` headers.

### userProfile.xhtml — new panel section

New `<p:panel>` below the existing reminder section with:
- Explanatory text via i18n key `userprofile.aiexport.description.t` (DE/EN)
- `<p:commandButton>` calling `downloadReefData()` with `ajax="false"`
- Disabled state + hint text when `!userProfileView.hasTanks` (reuses existing `hasTanks` field)

---

## i18n Message Keys

New keys required in `messages_de.properties` and `messages_en.properties`:

| Key | DE | EN |
|-----|----|----|
| `userprofile.aiexport.header.h` | `KI-Chatbot Datenexport` | `AI Chatbot Data Export` |
| `userprofile.aiexport.description.t` | (2–3 Sätze DE) | (2–3 sentences EN) |
| `userprofile.aiexport.download.b` | `Riff-Daten herunterladen` | `Download reef data` |
| `userprofile.aiexport.notanks.hint.t` | (Hint text DE) | (Hint text EN) |

---

## JSON Example Structure

```json
{
  "_meta": {
    "exportedAt": "2026-04-04T21:00:00Z",
    "sabiSchemaVersion": "1.0",
    "description": "Sabi reef data export — all aquariums, measurements, plague records, fish, corals, and treatments for use with AI chatbot consultations."
  },
  "aquariums": [
    {
      "id": 42,
      "description": "My main reef tank",
      "waterType": "SALT_WATER",
      "size": 300,
      "sizeUnit": "LITER",
      "active": true,
      "inceptionDate": "2021-03-15",
      "measurements": [
        {
          "measuredOn": "2026-04-01T18:30:00",
          "measuredValue": 0.05,
          "unitId": 7,
          "unitSign": "PO4",
          "unitName": "Phosphate (PO₄³⁻)",
          "unitNameResolved": true
        }
      ],
      "plagueRecords": [
        {
          "observedOn": "2026-03-20T10:00:00",
          "plagueId": 3,
          "plagueName": "Aiptasia",
          "plagueNameResolved": true,
          "plagueStatusId": 1,
          "plagueStatusName": "Active",
          "plagueStatusResolved": true,
          "plagueIntervallId": 1
        }
      ],
      "fish": [],
      "corals": [],
      "treatments": []
    }
  ]
}
```

