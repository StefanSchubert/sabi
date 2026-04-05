# Research: AI Chatbot Data Export (001-ai-data-export)

**Phase**: 0 — Pre-Implementation Research  
**Date**: 2026-04-04  
**Status**: Complete

---

## 1. Data Assembly Strategy

### Decision: Assemble Export in sabi-server Service Layer

- **Decision**: A new `ReefDataExportService` in `sabi-server` queries all required entities for the authenticated user in a single service call and assembles a `ReefDataExportTo` transfer object. The controller serializes this TO to JSON and returns it as `application/octet-stream` with `Content-Disposition: attachment; filename="sabi-reef-data-YYYY-MM-DD.json"`.
- **Rationale**: All data resides in the MariaDB database accessible to `sabi-server`. The service layer already owns all existing domain services (TankService, MeasurementService, etc.) and their repositories. Assembling in the service layer keeps the controller thin and keeps the same security boundary (JWT filter, Principal) that all other endpoints use.
- **Alternatives considered**:
  - *Assembling in webclient*: Rejected. The webclient would need to make 5+ separate REST calls (tanks, measurements per tank, plagues, fish, corals, treatments) and assemble them client-side — fragile, slow, and increases network chattiness.
  - *Streaming/pagination*: Not required for estimated data volumes (~800 records). Synchronous single-response is sufficient.

---

## 2. Unit Name Resolution

### Decision: Use LocalizedUnitRepository with Language "en"

- **Decision**: For each measurement's `unitId`, query `LocalizedUnitRepository.findByLanguageAndUnitId("en", unitId)` to get the English description. The `UnitEntity.name` field (e.g., `"PO4"`) serves as the unit sign/abbreviation; `LocalizedUnitEntity.description` (language="en") provides the full human-readable name.
- **Rationale**: `LocalizedUnitRepository` already provides `findByLanguageAndUnitId(language, unitId)`. Using `"en"` fulfils the clarified requirement (Q3: always English for AI readability). If no English localization exists for a unit, the export falls back to `UnitEntity.name` (abbreviation) and sets `"unitNameResolved": false`.
- **Alternatives considered**:
  - *Using the user's locale*: Rejected per clarification Q3 — English maximises AI chatbot interpretation accuracy.

---

## 3. Plague and Plague-Status Name Resolution

### Decision: Use LocalizedPlagueRepository + LocalizedPlagueStatusRepository with language "en"

- **Decision**: Analogous to unit resolution. Plague names come from `LocalizedPlagueEntity.commonName` (language="en"); plague status descriptions from `LocalizedPlagueStatusEntity.description` (language="en"). Both repositories already exist. Fallback to raw ID if English entry is absent.
- **Rationale**: Consistent approach — all catalogue references resolved to English for AI readability.

---

## 4. Fish and Coral Catalogue Resolution

### Decision: Include scientificName from FishCatalogueEntity / CoralCatalogueEntity

- **Decision**: For each fish, resolve `fishCatalogueId` to `FishCatalogueEntity.scientificName` (or common name if available). For each coral, resolve `coralCatalougeId` to `CoralCatalogueEntity.scientificName`. If no entry found, include the raw catalogue ID only.
- **Rationale**: Scientific names are language-neutral and universally understood by AI chatbots. They add significant context compared to bare numeric IDs.
- **Note**: `CoralCatalogueEntity` already has `scientificName` and `description` fields (confirmed in codebase). `FishCatalogueEntity` likely has a similar structure — to be confirmed during implementation.

---

## 5. Treatment Remedy Resolution

### Decision: Resolve remedyId to RemedyEntity.productname + vendor

- **Decision**: For each treatment, resolve `remedyId` to `RemedyEntity` fields `productname` and `vendor`. These provide meaningful context for the AI (e.g., "Salifert NO3 test kit by Salifert").
- **Rationale**: The `RemedyEntity` table already has product name and vendor — both are non-PII and highly relevant for AI-assisted reef diagnostics.

---

## 6. File Download in JSF/PrimeFaces Webclient

### Decision: Backend streams JSON; Webclient redirects browser to backend endpoint URL

- **Decision**: The `UserProfileView` controller adds a new method `downloadReefData()` that constructs the authenticated backend URL (`/api/userprofile/export`), sets the appropriate response headers (`Content-Disposition`, `Content-Type: application/json`), and forwards the response stream to the browser using `FacesContext.getCurrentInstance().getExternalContext()`. The PrimeFaces `p:commandButton` uses `ajax="false"` to trigger a standard HTTP GET that initiates the file download without page navigation.
- **Rationale**: JSF provides `ExternalContext.redirect()` and `ExternalContext.responseOutputStream()` for streaming responses. An `ajax="false"` button triggers a full HTTP request cycle which allows `Content-Disposition: attachment` to be interpreted by the browser as a file download. PrimeFaces 15.x supports this pattern.
- **Alternatives considered**:
  - *PrimeFaces `p:fileDownload`*: Requires generating the file content in the webclient layer — rejected since the backend owns the data.
  - *JavaScript `window.open()` / `fetch()`*: Would require passing the JWT token as a query parameter (security risk) or via JavaScript `fetch` with BLOB download — more complex with no benefit.
  - **Selected approach**: The webclient's `UserService` makes an authenticated `GET /api/userprofile/export` call and pipes the response stream to the browser's response. The backend returns `Content-Disposition: attachment` so the browser triggers a file-save dialog.

---

## 7. Loading Indicator (FR-005)

### Decision: Disable button and show PrimeFaces ajaxStatus spinner during synchronous request

- **Decision**: The download button uses `ajax="false"` (full page request → file download). PrimeFaces `p:blockUI` or `p:ajaxStatus` cannot intercept non-ajax requests directly. Instead: the button is disabled via JavaScript `onclick` on the form submit, and re-enabled when the response arrives (browser behaviour). Alternatively, PrimeFaces `p:commandButton` with `onclick="PF('dlgBlock').show()"` shows a modal block overlay for user feedback.
- **Rationale**: For `ajax="false"` requests, the browser's own loading indicator (tab spinner) is visible. Adding a `p:dialog` overlay provides explicit visual feedback. This is a simple, maintainable solution without additional framework dependencies.

---

## 8. Audit Log Pattern (FR-014)

### Decision: SLF4J INFO log at service layer with anonymised user hash

- **Decision**: Use `log.info("DATA_EXPORT userId={}", DigestUtils.sha256Hex(userId.toString()))` at the service layer entry point. `DigestUtils` (from `commons-codec`, already available via Spring Boot) provides a one-way hash. The hash is deterministic per user (useful for forensic correlation) but not reversible to the real user ID or email.
- **Rationale**: Consistent with the sabi-150 security audit log pattern (see sabi-150 spec FR-15). No PII in log (email/username not used — only the internal numeric user ID hash).

---

## 9. sabi-server Version for _meta Block

### Decision: Inject app version from Spring Boot application properties

- **Decision**: Use `@Value("${spring.application.version:unknown}")` or inject from `BuildProperties` (Spring Boot Actuator's build info bean). The `_meta.sabiSchemaVersion` is a static constant `"1.0"` in the new `ReefDataExportTo` class and must be incremented manually when the export schema changes (C-3).
- **Rationale**: `BuildProperties` is auto-populated from `pom.xml` version when `spring-boot-maven-plugin` generates build-info. No additional dependency required (C-4).

---

## 10. No New External Dependencies

All catalogue resolution, data access, and JSON serialization rely on existing dependencies:
- EclipseLink / JPA: existing entity queries
- Jackson (via Spring Boot): JSON serialization
- SLF4J: logging
- commons-codec (via Spring Boot): SHA-256 for anonymised user hash

**Conclusion**: No new external runtime dependency is introduced (C-4 satisfied).

