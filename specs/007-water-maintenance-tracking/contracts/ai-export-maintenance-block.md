# Contract: AI-JSON Export — `maintenance` Block Extension

**Feature**: `007-water-maintenance-tracking` | **Date**: 2026-10-26
**Supersedes (partially)**: `specs/001-ai-data-export/contracts/export-api.md` for the per-aquarium block and schema version (the spec-001 docs predate later specs and are stale — the code DTOs are the source of truth).

## Endpoint (unchanged)

`GET /api/userprofile/export` — JWT in `Authorization` header, 200 + `application/octet-stream`, `Content-Disposition: attachment; filename="sabi-reef-data-YYYY-MM-DD.json"`. No endpoint, auth, or metadata changes — only the per-aquarium content is extended (spec Assumptions).

## Schema version bump (FR-015, Clarification #4)

| Field | Before | After |
|-------|--------|-------|
| `_meta.sabiSchemaVersion` (constant `ReefDataExportTo.SCHEMA_VERSION`) | `"1.0"` | **`"1.1"`** |

Minor bump for the additive block; C-3 (bump on breaking changes) remains in force. All previously existing fields are unchanged — additive extension only (verified by the T031-style structure test).

## New per-aquarium field: `maintenance`

Added to each object in the top-level `aquariums[]` array, **always present** (FR-016), single object (not a list):

```json
{
  "maintenance": {
    "waterChanges": {
      "regularInterval": { "intervalDays": 21, "percentage": 25 },
      "records": [
        { "date": "2026-10-20", "percentage": 25, "note": null },
        { "date": "2026-10-03", "percentage": 30, "note": "after evaporation top-up" }
      ]
    },
    "activatedCarbon": {
      "enabled": true,
      "mode": "INTERVAL",
      "replacementIntervalDays": 28
    }
  }
}
```

### Field table

| Path | Type | Value rules |
|------|------|-------------|
| `maintenance.waterChanges` | object | always present |
| `maintenance.waterChanges.regularInterval` | object \| null | `{ intervalDays: int 1–365, percentage: int 1–100 }`; `null` when no regular interval is set |
| `maintenance.waterChanges.records` | array | all recorded water changes, **newest first**; empty `[]` when none |
| `maintenance.waterChanges.records[].date` | string | ISO-8601 date-only, `yyyy-MM-dd` (FR-014) |
| `maintenance.waterChanges.records[].percentage` | integer | 1–100, percentage of tank volume |
| `maintenance.waterChanges.records[].note` | string \| null | as entered (max 200 chars), `null` when absent; no normalisation |
| `maintenance.activatedCarbon` | object | always present |
| `maintenance.activatedCarbon.enabled` | boolean | `false` when not used |
| `maintenance.activatedCarbon.mode` | string \| null | `"CONTINUOUS"` \| `"INTERVAL"`; `null` when disabled **or** in continuous mode (FR-013) |
| `maintenance.activatedCarbon.replacementIntervalDays` | integer \| null | 1–365; `null` unless `mode == "INTERVAL"` (FR-013) |

### No `supplements` field (Clarification #3, 2026-10-26)

The `maintenance` block intentionally contains **no** supplement data. Supplement/dosing information remains in the existing top-level `dosings` array of each aquarium block; AI consumers read it there.

### Empty-tank shape (FR-016)

For an aquarium with no maintenance data, the block is still present:

```json
{
  "maintenance": {
    "waterChanges": { "regularInterval": null, "records": [] },
    "activatedCarbon": { "enabled": false, "mode": null, "replacementIntervalDays": null }
  }
}
```

## Style rules (FR-014, consistent with spec 001)

- Human-readable English field names (camelCase), matching all existing export fields.
- Dates ISO-8601; enum values as plain strings (`.name()`), never objects.
- No personal identifying data anywhere in the block (covered by the existing T029 no-PII test).
- Serialization: Lombok `@Data` DTOs in `de.bluewhale.sabi.model` (sabi-boundary), built by `ReefDataExportServiceImpl.buildMaintenanceExport(aquariumId)` with null-guarded `.toString()` date idiom — identical to `buildDosingExports`.

## Consumer example (SC-004)

Paste the export into an AI chatbot and ask "How often and to which percentage is water changed?" — the AI must answer from `maintenance.waterChanges.regularInterval` and/or `records`, cross-referencing automatic vs. manual dosing from the existing `dosings` array and carbon filtering from `activatedCarbon`.
