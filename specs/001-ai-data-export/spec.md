# Feature Specification: AI Chatbot Data Export

**Feature Branch**: `001-ai-data-export`  
**Created**: 2026-04-04  
**Status**: Draft  
**Input**: User description: "AI Chatbot Data Export: Allow users to download all their reef data (aquarium, measurements, plagues) as a structured JSON file to use as context for AI chatbot consultations (ChatGPT and similar) for reef diagnostics and advice. The user profile download section should include a brief explanation of the intended functionality to encourage new users to try it out."

---

## Overview

### Problem Statement

Reef aquarium keeping is a complex discipline where water chemistry, livestock health, and equipment parameters are closely interrelated. Experienced hobbyists increasingly turn to AI chatbots (ChatGPT, Claude, Gemini, and similar) to analyse their measurement trends and get advice on possible problems. However, manually typing or copy-pasting measurement data from Sabi into a chat window is tedious and error-prone, and the AI has no structured context about the tank parameters, active plagues, or stocking.

### Proposed Solution

Provide a one-click "Download my data" button in the Sabi user profile page that generates and downloads a single, well-structured JSON file containing all of the user's reef data. The user can then attach or paste this file into any AI chatbot conversation to get context-aware diagnostics and advice. A short, friendly explanation on the profile page motivates both new and existing users to try this workflow.

### Business Value

- **Increased engagement**: Users who discover the AI consulting workflow are likely to log data more consistently to get better AI advice.
- **Differentiation**: Sabi becomes the bridge between structured reef data tracking and modern AI-assisted consulting — a unique value proposition highlighted in the Coral magazine.
- **Onboarding**: New users are immediately shown a compelling, concrete use case for why they should log measurements from day one.
- **Data ownership**: Transparent, user-controlled data export builds trust (GDPR spirit: users know exactly what Sabi holds about them).

---

## Scope

### In Scope

- A new "Download reef data for AI chatbot" section in the existing user profile page
- A short explanatory text (2–4 sentences) visible in the profile page section, written in the user's preferred language
- A backend endpoint that assembles the user's complete reef data into a single JSON document and returns it as a file download
- Data included in the export:
  - All aquariums owned by the user (size, water type, description, inception date, active status)
  - All measurements for all tanks (timestamp, parameter, value, unit)
  - All plague records for all tanks (observation timestamp, plague type, status, interval grouping)
  - All fish entries for all tanks (species reference, addition date, observed behaviour)
  - All coral entries for all tanks (species reference, observed behaviour)
  - All treatment records for all tanks
- A metadata block in the JSON (export timestamp, Sabi version, data schema version) to help the AI understand the context
- i18n support: explanatory text in all languages already supported by Sabi (currently DE and EN)
- The download is triggered directly in the browser (file save dialog)
- No personal identifying data (email address, username, password) is included in the export

### Out of Scope (this iteration)

- Selective/partial export (e.g., only one tank, or only the last N months of measurements)
- Direct API integration with any AI provider (data stays on user's device)
- Storing exports server-side or a download history
- CSV or other export formats
- A scheduled / automatic export (e.g., weekly email)
- Public sharing of exported data

---

## User Scenarios & Testing

### User Story 1 - Download Full Reef Data as JSON (Priority: P1)

A logged-in user navigates to their Sabi user profile. In a clearly labelled panel they see a short explanation: "AI chatbots like ChatGPT can analyse your measurement data and advise you about possible problems in your reef. Download your data here and paste it into your favourite AI chat." They click the "Download reef data" button. Their browser immediately starts downloading a JSON file. They open the file and see all their aquariums, measurements, plague records, fish, and corals in a readable, structured format. They paste the JSON content into a ChatGPT conversation and ask: "What does my water chemistry tell you?"

**Why this priority**: This is the entire feature value; without it the feature does not exist. It is also independently demonstrable.

**Independent Test**: Can be fully tested by logging in as a user with at least one tank and at least one measurement, clicking the download button, and verifying the downloaded JSON file contains the expected data structure and values.

**Acceptance Scenarios**:

1. **Given** a logged-in user with at least one aquarium and measurements, **When** they click "Download reef data" on the profile page, **Then** their browser downloads a `sabi-reef-data-YYYY-MM-DD.json` file immediately (no additional confirmation dialog).
2. **Given** a user whose data spans multiple tanks, **When** they download, **Then** all tanks and their respective sub-data are present in the JSON, each tank's data grouped under the tank's entry.
3. **Given** a user with no aquariums, **When** they click "Download reef data", **Then** the file still downloads successfully, containing an empty `aquariums` array and a metadata block — no error is shown.
4. **Given** an unauthenticated request to the export endpoint, **When** the request is received, **Then** the endpoint returns HTTP 401 and no data is returned.

---

### User Story 2 - Onboarding Hint for New Users (Priority: P2)

A newly registered user who has not yet created any aquarium visits their profile page. They see the AI export panel with the explanatory text and a disabled (or visually dimmed) "Download reef data" button alongside a hint: "Add your first aquarium and start logging measurements to unlock this feature." This encourages them to start using Sabi productively.

**Why this priority**: Drives new user activation and communicates the value proposition early, but is secondary to the core download functionality.

**Independent Test**: Can be tested by creating a brand-new account with no tanks, navigating to the profile, and verifying the AI export panel is rendered with the encouraging hint text.

**Acceptance Scenarios**:

1. **Given** a user with no aquariums, **When** they view the profile page, **Then** the AI export panel is visible and shows a call-to-action to create their first aquarium.
2. **Given** a user who already has at least one aquarium, **When** they view the profile page, **Then** the download button is enabled and the hint text is not shown.

---

### User Story 3 - Exported JSON is Useful to an AI Chatbot (Priority: P3)

A user pastes or uploads the downloaded JSON to an AI chatbot. The AI can understand the data without further explanation because the JSON includes human-readable field names, unit labels, and a metadata preamble that describes the content and its context.

**Why this priority**: Quality of the AI interaction depends on the JSON structure; if the structure is cryptic (raw database IDs only), the feature's usefulness is limited.

**Independent Test**: Can be tested by reviewing the JSON export for human-readable field names, the presence of unit descriptions (not just numeric IDs), and the metadata block. A manual "AI readability check" (pasting into ChatGPT and verifying it can answer a basic question about the data) serves as an acceptance test.

**Acceptance Scenarios**:

1. **Given** a downloaded JSON export, **When** inspecting it, **Then** measurement unit IDs are accompanied by human-readable unit names (e.g., `"unitName": "Nitrate (NO₃⁻)"`) so the AI can understand the parameter without needing a Sabi data dictionary.
2. **Given** a downloaded JSON export, **When** inspecting it, **Then** a `_meta` section at the top level contains at least: `exportedAt` (ISO-8601 timestamp), `sabiSchemaVersion`, and a brief `description` string explaining what the file contains.
3. **Given** a downloaded JSON export, **Then** no personal data appears in the file (no email, no username, no password hash).

---

### Edge Cases

- What happens when the user has an extremely large dataset (e.g., thousands of measurements)? The export is synchronous; the button is disabled with a loading indicator for the duration. The export must complete within 3 seconds for typical data volumes (SC-001); if it cannot, the user sees an error message and the button re-enables.
- How does the system handle a concurrent download request (the same user clicking the button twice rapidly)? Each request is independent and processed separately; no deduplication needed.
- What if the backend cannot resolve a unit name for a measurement unit ID (e.g., a unit that was removed from the catalogue)? The export should fall back to including the raw numeric ID with a flag `"unitNameResolved": false` rather than failing entirely.
- What if the user's session token expires between navigating to the profile page and clicking the download button? The normal session-expiry handling applies; the user is redirected to the login page.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The user profile page MUST display a dedicated panel explaining the AI chatbot data export feature, visible to all authenticated users.
- **FR-002**: The panel MUST contain explanatory text in the user's selected language (DE/EN) that describes the intended use with AI chatbots and motivates the user to try the feature.
- **FR-003**: The panel MUST contain a clearly labelled "Download reef data" button.
- **FR-004**: When a user has no aquariums, the download button MUST either be disabled or hidden, and a motivational hint to create the first aquarium MUST be displayed instead.
- **FR-005**: Clicking the "Download reef data" button MUST trigger the download of a JSON file in the user's browser without navigating away from the profile page. During export generation the button MUST be disabled and display a loading indicator; it MUST re-enable automatically once the download has started or an error has occurred.
- **FR-006**: The downloaded filename MUST follow the pattern `sabi-reef-data-YYYY-MM-DD.json` where the date is the export date in the user's local timezone.
- **FR-007**: The JSON export MUST include all aquariums owned by the requesting user, with each aquarium's measurements, plague records, fish, corals, and treatments nested under the respective aquarium entry.
- **FR-008**: The JSON export MUST resolve measurement unit IDs to human-readable unit names in **English** wherever possible (regardless of the user's selected UI language), and MUST include the raw unit ID alongside the name. English names are used to maximise AI chatbot interpretation accuracy.
- **FR-009**: The JSON export MUST include a top-level `_meta` object containing at minimum: `exportedAt` (ISO-8601), `sabiSchemaVersion` (string), and `description` (a short English sentence describing the file's purpose).
- **FR-010**: The JSON export MUST NOT contain any personal identifying data: no email address, no username, no password hash, no OIDC provider links.
- **FR-011**: The backend export endpoint MUST require a valid user session token; unauthenticated requests MUST be rejected with HTTP 401.
- **FR-012**: Only data belonging to the authenticated user MUST be included; cross-user data leakage MUST be prevented by design.
- **FR-013**: The export endpoint MUST produce the response within a time that does not cause the browser to time out under typical data volumes (estimated: up to 5 years of weekly measurements across 3 tanks ≈ ~800 measurement records).
- **FR-014**: Every successful invocation of the export endpoint MUST write an `INFO`-level audit log entry containing: timestamp and an anonymised user reference. No personal data (email, username) may appear in the log entry.

### Non-Functional Requirements (ISO 25010)

| ISO 25010 Merkmal         | Anforderung / Constraint für dieses Feature                              |
|---------------------------|--------------------------------------------------------------------------|
| Funktionale Eignung       | Alle 13 FRs müssen erfüllt sein. Kein Cross-User-Datenleck zulässig. Vollständigkeit der exportierten Daten (alle Entitäten gemäß FR-007) ist prüfbar via Integrations-Test. |
| Leistungseffizienz        | Export-Generierung für geschätzte Maximal-Last (~800 Messwerte, 3 Tanks) muss in unter 3 Sekunden abgeschlossen sein. Kein Streaming oder Pagination erforderlich für diese Datenmenge. |
| Kompatibilität            | Neuer Backend-Endpoint (`GET /api/userprofile/export`) darf keinen Breaking Change an bestehenden Endpoints verursachen. Muss auf ARM64 und AMD64 laufen. |
| Benutzbarkeit             | Erklärungstext in DE und EN (WCAG 2.1 AA). Download-Button max. 2 Klicks vom Profil-Menü entfernt. Dateiname ist ohne weitere Anleitung selbsterklärend. |
| Zuverlässigkeit           | Falls Katalogreferenz (z.B. Unit-Name) nicht auflösbar: Fallback auf rohe ID statt Fehler (FR-008). Kein Datenverlust bei parallelen Requests. |
| Sicherheit                | Authentifizierung zwingend (FR-011). Strenge User-Isolierung (FR-012). Kein PII im Export (FR-010). Keine Secrets im generierten JSON. Jeder Export-Abruf wird als anonymisiertes INFO-Event geloggt (FR-014). |
| Wartbarkeit               | JSON-Schema-Version im `_meta`-Block ermöglicht spätere Schemaänderungen ohne Breaking Change. Kein Flyway-Migration erforderlich (keine DB-Änderung). Test-Coverage: mindestens ein Integrationstest für den Export-Endpoint und ein Test für FR-014 (Audit-Log-Eintrag). |
| Übertragbarkeit           | Kein Docker- oder Ansible-Änderungsbedarf. Feature ist ein reiner Applikations-Change (Backend + Webclient). |

### Key Entities

- **ReefDataExport** (new, transient – never persisted): The assembled JSON document representing a user's complete reef data snapshot. Contains a `_meta` block, and an `aquariums` array where each entry nests its measurements, plague records, fish, corals, and treatments.
- **Aquarium**: Existing entity — exported fields: id (internal reference only), description, waterType, size, sizeUnit, active, inceptionDate.
- **Measurement**: Existing entity — exported fields: measuredOn, measuredValue, unitId, unitName (resolved).
- **PlagueRecord**: Existing entity — exported fields: observedOn, plagueId, plagueStatusId, plagueIntervallId.
- **Fish**: Existing entity — exported fields: fishCatalogueId, addedOn, observedBehavior.
- **Coral**: Existing entity — exported fields: coralCatalougeId, observedBehavior.
- **Treatment**: Existing entity — exported fields: all non-PII fields.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A logged-in user can trigger a complete data download in a single click from the profile page, with the file arriving in the browser within 3 seconds for typical data volumes.
- **SC-002**: 100% of downloaded JSON files pass a structural validation: all expected top-level keys present (`_meta`, `aquariums`), and each aquarium contains all five sub-arrays (`measurements`, `plagueRecords`, `fish`, `corals`, `treatments`).
- **SC-003**: 0% of exported files contain personal identifying data (email, username, password hash) — verified by automated security test scanning the JSON output for known PII patterns.
- **SC-004**: The profile page AI export panel is rendered and accessible on both the DE and EN locale, with all text correctly translated.
- **SC-005**: New users with no aquariums see the motivational hint instead of a broken or confusing UI — verified by a dedicated acceptance test scenario.
- **SC-006**: The export endpoint returns HTTP 401 for every unauthenticated request — verified by automated test.
- **SC-007**: At least 80% of sampled AI-readability checks (manual: paste export into ChatGPT, ask a factual question about the data) yield a correct factual answer from the AI, demonstrating the export is interpretable without a Sabi-specific data dictionary.

---

## Assumptions

- The existing Sabi authentication mechanism (JWT bearer token) is sufficient to secure the new export endpoint; no additional security measure is required.
- The export is generated on-demand and not cached; for the expected data volumes, generation time is within the browser's timeout budget.
- Measurement units are resolvable via the existing unit catalogue available in the backend; the catalogue is complete and up-to-date for the most common parameters.
- The explanatory text in the user profile is short enough (2–4 sentences) that no additional "marketing copy" review process is required; it can be written directly as an i18n message bundle entry.
- The JSON file format is sufficient for all major AI chatbots (ChatGPT, Claude, Gemini); no special formatting for a specific provider is needed.
- Users understand how to copy/paste or upload a file to an AI chatbot; no tutorial is needed within the app (the brief description is sufficient for onboarding).
- No database schema change is required; the export is assembled from existing data at runtime.
- No dedicated rate limiting is applied to the export endpoint; mandatory JWT authentication and Sabi's small authenticated user base provide sufficient protection against accidental or abusive load.
- Treatment data is included for completeness; the exact fields to export follow the existing `TreatmentEntity` structure, including all non-PII fields.
- Corals reference a catalogue ID (`coralCatalougeId`); the catalogue name is resolved analogously to measurement unit names — in English — where possible.

---

## Constraints

| ID | Constraint |
|----|-----------|
| C-1 | No personal identifying data (email, username, password hash, OIDC sub) may appear in the exported JSON |
| C-2 | The export endpoint must be placed under the existing `/api/userprofile` path to remain consistent with the existing user-profile API grouping |
| C-3 | The JSON schema version in `_meta.sabiSchemaVersion` must be incremented whenever the export structure changes in a breaking way |
| C-4 | The feature must not introduce any new external runtime dependency (no new libraries required) |

---

*This specification was created for branch `001-ai-data-export`. Next step: `/speckit.plan` to break this into implementation tasks.*

---

## Clarifications

### Session 2026-04-04

- Q: Should the spec explicitly state this export is NOT a GDPR Art. 15 DSAR? → A: No explicit disclaimer needed — the AI chatbot usage context (reef data only, no PII) makes the distinction self-evident to users and implementors.
- Q: What is the loading/UX state of the download button during export generation? → A: Synchronous — the button is disabled and shows a loading indicator until the browser download starts; it re-enables on completion or error (FR-005 updated).
- Q: In which language should unit names and catalogue references appear in the JSON? → A: Always English — maximises AI chatbot interpretation accuracy regardless of the user's UI language (FR-008 updated; UI texts remain in the user's selected language).
- Q: Should successful export requests be written to the audit log? → A: Yes — one `INFO`-level entry per export (timestamp + anonymised user reference, no PII) consistent with existing audit-log patterns (FR-014 added).
- Q: Should the export endpoint be rate-limited? → A: No — mandatory authentication and Sabi's small authenticated user base make a dedicated rate limit unnecessary; no FR added.

