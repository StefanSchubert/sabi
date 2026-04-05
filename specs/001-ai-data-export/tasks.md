# Tasks: AI Chatbot Data Export (001-ai-data-export)

**Feature**: 001-ai-data-export  
**Created**: 2026-04-04  
**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Contract**: [contracts/export-api.md](contracts/export-api.md)  
**Branch**: `001-ai-data-export`

---

## Summary

| Metric | Value |
|--------|-------|
| Total Tasks | 33 |
| Foundational Tasks (Phase 1–2) | 10 (T001–T010) |
| US1 Tasks (P1 — Backend Export) | 11 (T011–T021) |
| US2 Tasks (P2 — Webclient UI) | 6 (T022–T027) |
| US3 Tasks (P3 — AI Readability) | 4 (T028–T031) |
| Polish Tasks | 2 (T032–T033) |
| Parallelizable tasks [P] | 9 |

**MVP Scope**: Phases 1–3 (T001–T021) deliver a fully working, API-testable backend endpoint. US2 and US3 are additive on top.

---

## Phase 1: Setup

**Goal**: Feature branch ready and local build green before any code changes.

- [x] T001 Checkout feature branch `001-ai-data-export` and verify `mvn clean compile` passes from project root

---

## Phase 2: Foundational — sabi-boundary Transfer Objects

**Goal**: All Transfer Objects (TOs) and the Endpoint enum entry are available as compilation units for sabi-server and sabi-webclient. No functional code yet.

**Independent Test**: `mvn clean compile -pl sabi-boundary` succeeds with zero errors after T010.

- [x] T002 Add `USER_PROFILE_EXPORT("/api/userprofile/export")` constant to the Endpoint enum in `sabi-boundary/src/main/java/de/bluewhale/sabi/api/Endpoint.java`
- [x] T003 [P] Create `ExportMetaTo.java` with fields: `String exportedAt` (ISO-8601 UTC), `String sabiSchemaVersion`, `String description` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/ExportMetaTo.java`
- [x] T004 [P] Create `MeasurementExportTo.java` with fields: `String measuredOn`, `Float measuredValue`, `Integer unitId`, `String unitSign`, `String unitName`, `Boolean unitNameResolved` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/MeasurementExportTo.java`
- [x] T005 [P] Create `PlagueRecordExportTo.java` with fields: `String observedOn`, `Integer plagueId`, `String plagueName`, `Boolean plagueNameResolved`, `Integer plagueStatusId`, `String plagueStatusName`, `Boolean plagueStatusResolved`, `Integer plagueIntervallId` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PlagueRecordExportTo.java`
- [x] T006 [P] Create `FishExportTo.java` with fields: `Long fishCatalogueId`, `String scientificName`, `String addedOn`, `String observedBehavior` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/FishExportTo.java`
- [x] T007 [P] Create `CoralExportTo.java` with fields: `Long coralCatalogueId`, `String scientificName`, `String observedBehavior` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/CoralExportTo.java`
- [x] T008 [P] Create `TreatmentExportTo.java` with fields: `String givenOn`, `Float amount`, `Integer unitId`, `String unitSign`, `String unitName`, `Long remedyId`, `String productName`, `String vendor`, `String description` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/TreatmentExportTo.java`
- [x] T009 Create `AquariumExportTo.java` with fields: `Long id`, `String description`, `String waterType`, `Integer size`, `String sizeUnit`, `Boolean active`, `String inceptionDate`, `List<MeasurementExportTo> measurements`, `List<PlagueRecordExportTo> plagueRecords`, `List<FishExportTo> fish`, `List<CoralExportTo> corals`, `List<TreatmentExportTo> treatments` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/AquariumExportTo.java`
- [x] T010 Create `ReefDataExportTo.java` (top-level document) with fields: `ExportMetaTo _meta`, `List<AquariumExportTo> aquariums`, and public static constant `SCHEMA_VERSION = "1.0"` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/ReefDataExportTo.java`

---

## Phase 3: User Story 1 — Download Full Reef Data as JSON (P1)

**Story Goal**: A logged-in user calls `GET /api/userprofile/export` (from curl, REST client, or the webclient) and receives a complete `sabi-reef-data-YYYY-MM-DD.json` file with all their aquarium data nested inside.

**Independent Test**: `mvn test -pl sabi-server -Dtest=UserProfileControllerTest,ReefDataExportServiceTest`  
— Authenticated request → HTTP 200 + JSON body with `_meta` and `aquariums` keys.  
— Unauthenticated request → HTTP 401, no data.  
— Audit log entry written per invocation (FR-014).

- [x] T011 [US1] Create `ReefDataExportService` interface with single method `ReefDataExportTo buildExportForUser(String userEmail)` in `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportService.java`
- [x] T012 [US1] Create `ReefDataExportServiceImpl` skeleton: resolve user by email to obtain `userId`, load all `AquariumEntity` records for that user, and for each aquarium load measurements, plague records, fish, corals, and treatments using existing repositories; assemble a `ReefDataExportTo` with a populated `_meta` block (`exportedAt`=now UTC, `sabiSchemaVersion`=`ReefDataExportTo.SCHEMA_VERSION`, fixed English `description`) and all sub-arrays (empty lists where no records exist) in `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java`
- [x] T013 [US1] Add measurement unit name resolution to `ReefDataExportServiceImpl`: for each measurement call `LocalizedUnitRepository.findByLanguageAndUnitId("en", unitId)` to populate `unitSign` (from `UnitEntity.name`) and `unitName` (from `LocalizedUnitEntity.description`); set `unitNameResolved=true` on success, `unitNameResolved=false` with raw `unitId` preserved as fallback (FR-008) in `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java`
- [x] T014 [US1] Add plague and plague-status name resolution to `ReefDataExportServiceImpl`: query `LocalizedPlagueRepository` (lang=`"en"`) to populate `plagueName`/`plagueNameResolved`; query `LocalizedPlagueStatusRepository` (lang=`"en"`) to populate `plagueStatusName`/`plagueStatusResolved`; fallback to raw IDs and `*Resolved=false` if not found in `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java`
- [x] T015 [US1] Add fish and coral catalogue resolution to `ReefDataExportServiceImpl`: resolve `fishCatalogueId` → `FishCatalogueEntity.scientificName`; resolve `coralCatalougeId` (note: source-field typo preserved) → `CoralCatalogueEntity.scientificName`; set `scientificName=null` when catalogue entry is not found in `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java`
- [x] T016 [US1] Add treatment remedy resolution to `ReefDataExportServiceImpl`: resolve `remedyId` → `RemedyEntity`; populate `productName` from `RemedyEntity.productname` and `vendor` from `RemedyEntity.vendor`; both null when remedy not found in `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java`
- [x] T017 [US1] Add INFO audit log at the top of `buildExportForUser()` in `ReefDataExportServiceImpl`: `log.info("DATA_EXPORT userId={}", DigestUtils.sha256Hex(userId.toString()))` using `commons-codec` (available via Spring Boot) — no email, no username in log output (FR-014) in `sabi-server/src/main/java/de/bluewhale/sabi/services/ReefDataExportServiceImpl.java`
- [x] T018 [US1] Add `GET /export` endpoint to `UserProfileController`: inject `ReefDataExportService`, call `buildExportForUser(principal.getName())`, serialize `ReefDataExportTo` to JSON bytes via Jackson `ObjectMapper`, return `ResponseEntity<byte[]>` with headers `Content-Type: application/octet-stream` and `Content-Disposition: attachment; filename="sabi-reef-data-<UTC-date>.json"` (HTTP 200); endpoint secured by existing JWT filter (FR-011, FR-012) in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/UserProfileController.java`
- [x] T019 [US1] Add integration test to `UserProfileControllerTest`: authenticated `GET /api/userprofile/export` with valid JWT returns HTTP 200; response body parses as valid JSON; top-level keys `_meta` and `aquariums` are present and non-null in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/UserProfileControllerTest.java`
- [x] T020 [US1] Add integration test to `UserProfileControllerTest`: unauthenticated `GET /api/userprofile/export` (no Authorization header) returns HTTP 401 and no data body (SC-006) in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/UserProfileControllerTest.java`
- [x] T021 [US1] Create `ReefDataExportServiceTest` with unit test: mock repositories to return one measurement with a resolvable unit; call `buildExportForUser()`; capture SLF4J log output and assert exactly one INFO entry matching pattern `DATA_EXPORT userId=<hex-hash>` was written; assert no email string appears in log (FR-014) in `sabi-server/src/test/java/de/bluewhale/sabi/services/ReefDataExportServiceTest.java`

---

## Phase 4: User Story 2 — Onboarding Hint for New Users (P2)

**Story Goal**: Any authenticated user sees the AI export panel on their profile page. New users (no aquariums) see the button disabled with a motivational hint. Users with tanks see the button enabled.

**Independent Test**: Log in as a brand-new test account with zero aquariums, navigate to the User Profile page; verify (a) the AI export panel is visible, (b) the download button is disabled or hidden, (c) the hint text referencing creating the first aquarium is displayed (SC-005, FR-004).

- [x] T022 [US2] Add method signature `byte[] downloadReefDataExport(String jwtBackendAuthToken) throws BusinessException` to the `UserService` interface in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/UserService.java`
- [x] T023 [US2] Implement `downloadReefDataExport()` in `UserServiceImpl`: construct URL as `sabiBackendUrl + Endpoint.USER_PROFILE_EXPORT.getPath()`, perform authenticated `GET` with `Authorization: <jwtBackendAuthToken>` header using the existing HTTP client; return response body as `byte[]`; throw `BusinessException` on non-200 response in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/UserServiceImpl.java`
- [x] T024 [P] [US2] Add 4 German i18n keys to `sabi-webclient/src/main/resources/i18n/messages_de.properties` — write Umlaute as `\uXXXX` escapes (never raw bytes): `userprofile.aiexport.header.h=KI-Chatbot Datenexport`, `userprofile.aiexport.description.t=KI-Chatbots wie ChatGPT k\u00f6nnen deine Messwerte analysieren und dir Hinweise zu m\u00f6glichen Problemen in deinem Riff geben. Lade hier alle deine Daten als JSON-Datei herunter und f\u00fcge sie in ein KI-Chat-Gespr\u00e4ch ein, um fundierte Ratschl\u00e4ge zu erhalten.`, `userprofile.aiexport.download.b=Riff-Daten herunterladen`, `userprofile.aiexport.notanks.hint.t=Leg dein erstes Aquarium an und beginne mit dem Erfassen von Messwerten, um dieses Feature freizuschalten.`
- [x] T025 [P] [US2] Add 4 English i18n keys to `sabi-webclient/src/main/resources/i18n/messages_en.properties`: `userprofile.aiexport.header.h=AI Chatbot Data Export`, `userprofile.aiexport.description.t=AI chatbots like ChatGPT can analyse your measurement data and advise you about possible problems in your reef. Download all your data as a JSON file here and paste it into your favourite AI chat to get context-aware diagnostics.`, `userprofile.aiexport.download.b=Download reef data`, `userprofile.aiexport.notanks.hint.t=Add your first aquarium and start logging measurements to unlock this feature.`
- [x] T026 [US2] Add `downloadReefData()` action method to `UserProfileView`: call `userService.downloadReefDataExport(jwtToken)`, write returned bytes to `FacesContext.getCurrentInstance().getExternalContext().getResponseOutputStream()`, set response headers `Content-Type: application/octet-stream` and `Content-Disposition: attachment; filename="sabi-reef-data-<LocalDate.now()>.json"`, call `FacesContext.getCurrentInstance().responseComplete()` to stop JSF lifecycle (research note 6); the existing `hasTanks` field is reused as-is — no modification needed to `hasTanks` in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/UserProfileView.java`
- [x] T027 [US2] Add AI export `<p:panel>` section to `userProfile.xhtml` below the existing reminder panel: panel header via `#{msg['userprofile.aiexport.header.h']}`; description `<p:outputLabel value="#{msg['userprofile.aiexport.description.t']}"/>`; `<p:commandButton ajax="false" value="#{msg['userprofile.aiexport.download.b']}" action="#{userProfileView.downloadReefData}" disabled="#{!userProfileView.hasTanks}" styleClass="ui-button-primary"/>`; conditionally rendered hint `<p:outputLabel rendered="#{!userProfileView.hasTanks}" value="#{msg['userprofile.aiexport.notanks.hint.t']}"/>`; ensure all text colours meet WCAG 2.1 AA contrast (no `color: lightblue` or pastel values on light background) in `sabi-webclient/src/main/resources/META-INF/resources/secured/userProfile.xhtml`

---

## Phase 5: User Story 3 — AI-Readable Export Quality (P3)

**Story Goal**: A downloaded JSON file is immediately interpretable by an AI chatbot without a Sabi data dictionary — human-readable unit names, scientific species names, a self-describing `_meta` block, and zero personal identifying data.

**Independent Test**: Parse the downloaded JSON file and verify: (a) at least one measurement has `unitNameResolved=true` and non-null `unitName`; (b) `_meta.sabiSchemaVersion == "1.0"` and `_meta.description` is non-blank; (c) no JSON key or value matching `email`, `password`, or `username` appears anywhere in the document (SC-002, SC-003, SC-007).

- [x] T028 [P] [US3] Add test to `ReefDataExportServiceTest`: mock unit catalogue to return an English description for a known `unitId`; call `buildExportForUser()`; assert the returned `MeasurementExportTo` has `unitNameResolved=true`, non-null `unitName`, and the correct `unitSign` abbreviation; add a second case where no catalogue entry exists and assert `unitNameResolved=false` with raw `unitId` preserved in `sabi-server/src/test/java/de/bluewhale/sabi/services/ReefDataExportServiceTest.java`
- [x] T029 [US3] Add controller-level PII-absence test to `UserProfileControllerTest`: serialize the export response body to `String`; assert it does not contain the substrings `"email"`, `"password"`, or `"username"` anywhere in the JSON text (SC-003, FR-010) in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/UserProfileControllerTest.java`
- [x] T030 [US3] Add controller-level `_meta` completeness test to `UserProfileControllerTest`: parse response JSON; assert `_meta.sabiSchemaVersion` equals `"1.0"`, `_meta.exportedAt` matches ISO-8601 UTC pattern (e.g., `\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z`), and `_meta.description` is non-blank (SC-002, FR-009) in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/UserProfileControllerTest.java`
- [x] T031 [US3] Add controller-level structure completeness test to `UserProfileControllerTest` using a test user with at least one tank: parse response; assert each object in the `aquariums` array contains all five sub-array keys `measurements`, `plagueRecords`, `fish`, `corals`, `treatments` (each may be an empty array but the key must be present); covers SC-002 and FR-007 in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/UserProfileControllerTest.java`

---

## Phase 6: Polish & Cross-Cutting Concerns

**Goal**: OpenAPI documentation complete; full multi-module build green.

- [x] T032 Add OpenAPI annotations to the new export method in `UserProfileController`: `@Operation(summary = "Download complete reef data as JSON for AI chatbot consultations.")` and `@ApiResponses({@ApiResponse(responseCode = "200", description = "OK — JSON file download initiated."), @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid token.")})` above the `@GetMapping("/export")` method in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/UserProfileController.java`
- [x] T033 Run `mvn clean verify` from project root; confirm zero test failures and successful build for all three touched modules (`sabi-boundary`, `sabi-server`, `sabi-webclient`)

---

## Dependency Graph

```
T001
 ├─ T002 ──────────────────────────────────── T018 (controller needs enum)
 │                                             T023 (UserServiceImpl needs Endpoint constant)
 │
 ├─ T003 ─┐
 ├─ T004 ─┤
 ├─ T005 ─┤  (all parallel, different files)
 ├─ T006 ─┤
 ├─ T007 ─┤
 └─ T008 ─┘
           └─ T009 ─── T010 ─── T011 ─── T012 ─── T013 ─┐
                                                    T014 ─┤  (sequential: same file)
                                                    T015 ─┤
                                                    T016 ─┤
                                                    T017 ─┘
                                                           └─ T018
                                                                 ├─ T019 ─┐
                                                                 ├─ T020 ─┤  (same file, sequential)
                                                                 └─ T021   │  (different file, parallel with T019/T020)
                                                                           └─ T028 (parallel with T029–T031)
                                                                               T029 ─┐
                                                                               T030 ─┤  (same file, sequential)
                                                                               T031 ─┘

T010, T002 ─── T022 ─── T023 ─── T026 ─── T027
T024, T025 ──────────────────────────────── T027  (parallel to each other, must complete before T027)

T018, T032 ─── T033
```

### User Story Completion Order

| Order | Story | Depends On | Deliverable |
|-------|-------|------------|-------------|
| 1st | **US1** (P1, T011–T021) | Phases 1–2 | Working backend endpoint, testable via HTTP |
| 2nd | **US2** (P2, T022–T027) | US1 endpoint live | Button + panel visible in browser |
| 3rd | **US3** (P3, T028–T031) | US1 service impl (T013–T016) | Quality gate tests pass in CI |

---

## Parallel Execution Examples

### Phase 2 — six leaf TOs can be written simultaneously (after T002):
```
Agent A → T003  ExportMetaTo.java
Agent B → T004  MeasurementExportTo.java
Agent C → T005  PlagueRecordExportTo.java
Agent D → T006  FishExportTo.java
Agent E → T007  CoralExportTo.java
Agent F → T008  TreatmentExportTo.java
          ↓ (all done)
          T009  AquariumExportTo.java
          T010  ReefDataExportTo.java
```

### Phase 4 — i18n files can be written simultaneously:
```
Agent A → T024  messages_de.properties
Agent B → T025  messages_en.properties
          ↓ (both done)
          T027  userProfile.xhtml
```

### Phase 5 — service test parallel with controller tests:
```
Agent A → T028  ReefDataExportServiceTest (unit resolution assertion)
Agent B → T029 → T030 → T031  UserProfileControllerTest (sequential, same file)
```

---

## Implementation Strategy

### MVP First: Phases 1–3 (T001–T021)

The backend export endpoint is independently verifiable without a browser. After Phase 3, a developer can:
```bash
# Manual smoke test using IntelliJ REST client or curl:
curl -H "Authorization: <sabi-jwt>" \
     http://localhost:8080/api/userprofile/export \
     -o sabi-reef-data-test.json
```
This satisfies SC-001, SC-002, SC-003, SC-006 and proves the core feature value.

### Incremental Delivery

| Step | Phases | Visible Change |
|------|--------|----------------|
| 1 | 1+2 | Compile-time only — no runtime change |
| 2 | 3 (US1) | `GET /api/userprofile/export` returns JSON file |
| 3 | 4 (US2) | Button + panel appear in browser profile page |
| 4 | 5+6 | CI test suite green; OpenAPI docs updated |

### Key Implementation Constraints

| Constraint | Rule |
|-----------|------|
| **No Flyway migration** | Do not create any `V*.sql` file. All data assembled at runtime. |
| **Catalogue fallbacks** | T013–T016: on missing catalogue entry, set `*Resolved=false` and keep raw ID. Never throw; never skip the record (FR-008). |
| **i18n encoding** | In `.properties` files, write Umlaute as `\uXXXX` (e.g., `ö` → `\u00f6`). Never raw bytes. `spring.messages.encoding: UTF-8` is set but `\uXXXX` preferred for consistency. |
| **WCAG AA contrast** | All text in the AI export panel (description, hint, button label) must meet WCAG 2.1 AA ≥ 4.5:1. Avoid `color: lightblue`, `color: yellow`, or other pastel values on light backgrounds. |
| **No new dependencies** | Do not add `<dependency>` to any `pom.xml`. All resolution uses existing EclipseLink / Spring Boot / commons-codec / Jackson (C-4). |
| **Audit log** (FR-014) | Use `DigestUtils.sha256Hex(userId.toString())` — never log email or username. |
| **JSON field name `_meta`** | Jackson: annotate field with `@JsonProperty("_meta")` so the leading underscore serializes correctly. |

---

*Generated: 2026-04-04 | Feature: 001-ai-data-export | speckit.tasks output*

