# Implementation Plan: AI Chatbot Data Export (001-ai-data-export)

**Branch**: `001-ai-data-export` | **Date**: 2026-04-04 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/001-ai-data-export/spec.md`

---

## Summary

Nutzer können auf der Profil-Seite mit einem Klick alle ihre Riff-Daten (Aquarien, Messwerte, Seuchen, Fische, Korallen, Behandlungen) als strukturierte JSON-Datei herunterladen und in KI-Chatbots (ChatGPT, Claude usw.) einzuspeisen. Ein neuer `GET /api/userprofile/export`-Endpoint im `sabi-server` assembliert die Daten mit englischen Katalogreferenzen aus der bestehenden Datenbank. Kein DB-Schema-Change, keine neuen externen Abhängigkeiten.

---

## Technical Context

**Language/Version**: Java 25, Spring Boot 4  
**Primary Dependencies**:
- EclipseLink / JPA (existing) — entity queries
- Jackson / Spring MVC (existing) — JSON serialization + `application/octet-stream` response
- SLF4J (existing) — INFO audit log
- `commons-codec` via Spring Boot (existing) — SHA-256 for anonymised user hash (FR-014)

**Storage**: MariaDB 10.x + EclipseLink JPA — **no new Flyway migration required**  
**Testing**: JUnit 5 + Testcontainers (MariaDB) + Mockito — existing infrastructure  
**Target Platform**: ARM (Raspberry Pi, production) + AMD64 (development/Docker)  
**Project Type**: Web Service (REST Backend) + JSF/PrimeFaces Webclient  
**Performance Goals**: Export response ≤ 3 s for ~800 measurement records (SC-001); Constitution § II (≤ 2 s) is the general guideline — the export is accepted as a slightly heavier operation within the same order of magnitude  
**Constraints**: No PII in export (C-1); endpoint at `/api/userprofile/export` (C-2); schema version in `_meta` (C-3); no new external runtime dependency (C-4)  
**Scale/Scope**: Single user, all their data, on-demand, synchronous

---

## Constitution Check

*GATE: Passed — basis for Phase 0. Re-checked after Phase 1 design (see below).*

| # | ISO 25010 Prinzip | Gate-Frage | Status |
|---|-------------------|------------|--------|
| I | Funktionale Eignung | 3 User Stories, 14 FRs, 7 SCs, Acceptance Scenarios vorhanden? | ✅ |
| II | Leistungseffizienz | ARM-Impact bewertet: Datenmenge ~800 Records; synchrone DB-Abfrage, kein N+1 (Service lädt pro Aquarium eager); 3 s Budget akzeptiert | ✅ |
| III | Kompatibilität | Bestehende `/api/userprofile` Endpoints unverändert; neuer `/export`-Subpfad additiv; OpenAPI via springdoc-v2 ergänzt; ARM+AMD64 | ✅ |
| IV | Benutzbarkeit | i18n DE/EN; WCAG 2.1 AA (Button mit sinnvollem Label + disabled-State); max. 2 Klicks vom Profil-Menü | ✅ |
| V | Zuverlässigkeit | Fallback auf rohe IDs bei unauflösbaren Katalogreferenzen (FR-008); keine Transaktionsschreiboperation → kein Datenverlustrisiko | ✅ |
| VI | Sicherheit | JWT-Auth zwingend (FR-011); User-Isolierung by design (FR-012); kein PII im Export (FR-010); anonymisiertes Audit-Log (FR-014); Secrets nicht im Code | ✅ |
| VII | Wartbarkeit | Schema-Version im `_meta`-Block; keine Flyway-Migration; JUnit-Tests geplant (min. 2: Export-Endpoint + Audit-Log); Code in bestehenden Modulen | ✅ |
| VIII | Übertragbarkeit | Kein Docker-/Ansible-Änderungsbedarf; reiner App-Change auf bestehender Infrastruktur | ✅ |

**Violations requiring justification**: keine

---

## Project Structure

### Documentation (this feature)

```text
specs/001-ai-data-export/
├── plan.md              ← dieses Dokument
├── research.md          ← Phase 0 Output
├── data-model.md        ← Phase 1 Output
├── quickstart.md        ← Phase 1 Output
├── contracts/
│   └── export-api.md   ← Phase 1 Output
└── tasks.md             ← Phase 2 Output (/speckit.tasks — NOT created by /speckit.plan)
```

### Source Code

```text
sabi-boundary/
└── src/main/java/de/bluewhale/sabi/
    ├── api/
    │   └── Endpoint.java                       [MODIFY] Add USER_PROFILE_EXPORT
    └── model/
        ├── ReefDataExportTo.java               [NEW]
        ├── ExportMetaTo.java                   [NEW]
        ├── AquariumExportTo.java               [NEW]
        ├── MeasurementExportTo.java            [NEW]
        ├── PlagueRecordExportTo.java           [NEW]
        ├── FishExportTo.java                   [NEW]
        ├── CoralExportTo.java                  [NEW]
        └── TreatmentExportTo.java              [NEW]

sabi-server/
└── src/main/java/de/bluewhale/sabi/
    ├── services/
    │   ├── ReefDataExportService.java          [NEW] interface
    │   └── ReefDataExportServiceImpl.java      [NEW] implementation
    └── rest/controller/
        └── UserProfileController.java          [MODIFY] Add GET /export endpoint

sabi-server/
└── src/test/java/de/bluewhale/sabi/rest/controller/
    └── UserProfileControllerTest.java          [MODIFY or NEW] Add export endpoint tests

sabi-webclient/
└── src/main/java/de/bluewhale/sabi/webclient/
    ├── apigateway/
    │   ├── UserService.java                    [MODIFY] Add downloadReefDataExport method
    │   └── UserServiceImpl.java                [MODIFY] Implement REST call
    └── controller/
        └── UserProfileView.java                [MODIFY] Add downloadReefData() action + hasTanks reuse

sabi-webclient/
└── src/main/resources/
    ├── META-INF/resources/secured/
    │   └── userProfile.xhtml                  [MODIFY] Add AI export panel + button
    └── i18n/
        ├── messages_de.properties              [MODIFY] Add 4 new keys
        └── messages_en.properties              [MODIFY] Add 4 new keys
```

**Structure Decision**: Existing multi-module web-service architecture (`sabi-boundary` → `sabi-server` + `sabi-webclient`). All changes are additive within existing modules. No new module required.

---

## Complexity Tracking

No Constitution violations — this section intentionally left empty.
