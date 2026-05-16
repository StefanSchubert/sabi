# Feature Specification: Aquarium Event Logbook

**Feature Branch**: `004-aquarium-events`  
**Created**: 2026-06-05  
**Status**: Draft  
**Input**: User description (DE): "Als Reefer möchte ich die Möglichkeit haben auch unstrukturierte Ereignisse jeder Art, also 'Events' im allgemeinen zu erfassen, die ggf. eine Auswirkung auf die Riff-Biologie haben könnten. Da Events zum Aquarium gehören, denke ich sie mir als eine Art Logbuch auf der Aquarium Detail-Seite vor. Wenn ich einen neuen Eintrag mache, sollte dieser über ein Datum (Pflichtfeld), eine Angabe zur Dauer in Stunden (Optionales Feld) und einem Beschreibungsfeld (Pflichtfeld, Mehrzeiliger Text möglich). Bestehen. Ich sollte chronologisch absteigend eine Liste der letzten Events angezeigt bekommen und, für den Fall dass ich mich mal vertippt habe, soll auch die Möglichkeit bestehen Events zu editieren, oder gar ein Event ganz zu löschen. Zusätzlich möchte ich im UserProfil, an der Stelle wo der Public Link für die Seite houseReefReport.xhtml generiert wird ein Optionsfeld, welches ich ankreuzen kann, wenn ich möchte dass die Events ebenfalls in houseReefReport gelistet werden sollen. Ist das Feld aktiv, sollte der Report eine Sektion für die Events enthalten. Im Housereef Reef-Report sollen aber nur Events dargestellt werden, die nicht älter als 1 Jahr sind."

---

## Overview

### Problem Statement

Reef aquarium keepers regularly encounter extraordinary events — power outages, sudden temperature spikes, coral fragging sessions, medication treatments, major water changes, equipment failures, or pest introductions — that fall outside the structured measurement and plague tracking Sabi already provides. Without a general-purpose logbook, these contextually important events are lost, making it very difficult to later correlate a water-chemistry anomaly or a livestock problem with what actually happened. Experienced reefers know from hard experience that an unexplained coral die-off is often only explainable months later when they remember "that was the week the heater controller failed."

### Proposed Solution

Add a free-form **Aquarium Event Logbook** that lets authenticated users record any notable event against a specific aquarium directly from the aquarium detail view (`tankView.xhtml`). Each event captures a mandatory date, an optional duration in hours, and a mandatory free-text description. The logbook is displayed chronologically descending (most recent first), and existing entries can be edited or deleted.

In addition, a new opt-in checkbox on the user profile page — placed alongside the existing public HouseReef Report link panel — lets users choose to include their event logbook in the public `houseReefReport.xhtml`. When active, the report renders a dedicated events section showing only events from the past twelve months.

### Business Value

- **Holistic reef history**: Users can finally record the full narrative of a tank — not just numeric measurements but also free-form context, making future retrospective analysis far more useful.
- **Correlation enabler**: Linking unusual measurement trends or plague outbreaks to a logged event (e.g., "added new coral frag from shop X") dramatically improves root-cause analysis, both manually and in future AI-assisted diagnostics (aligned with feature `001`).
- **Community transparency**: The optional inclusion in the public HouseReef Report allows experienced reefers to share noteworthy events as part of their public aquarium profile, increasing the community value of the report feature.
- **Low barrier to entry**: A free-text logbook with minimal mandatory fields (date + description) requires no data-modelling knowledge from the user and covers virtually any real-world event.

---

## Scope

### In Scope

**Aquarium Event Logbook (per-tank, authenticated users)**

- A new "Events" section (panel) rendered on the existing aquarium detail page (`tankView.xhtml`), per aquarium owned by the authenticated user
- Create a new event with:
  - **Date** (mandatory): the calendar date on which the event occurred, selectable via a date picker consistent with the existing date pickers in `measureView.xhtml`
  - **Duration in hours** (optional): a positive decimal numeric field capturing how long the event lasted (e.g., `2.5`); must be greater than zero if provided
  - **Description** (mandatory): free-text multi-line field, unlimited length, with line breaks preserved
- Display a chronologically descending list of all events for the aquarium, showing date, duration (if set), and description
- Edit an existing event (all three fields modifiable)
- Delete an existing event
- i18n support: all labels, buttons, messages, and placeholder texts in all 6 supported resource bundles (DE, EN, ES, FR, IT + fallback `messages.properties`)

**User Profile — Opt-in for Events in HouseReef Report**

- On the existing `userProfile.xhtml`, inside the "Public HouseReef Report links" panel, a per-link boolean checkbox "Include events in report" (i18n key) rendered only for tanks that already have an active report link
- The checkbox state is persisted alongside the existing `PublicReportLinkTo` record as a new `includeEvents` field (default `false`)
- When checked and the report is accessed, the backend assembles the event list for the linked aquarium filtered to the past 12 months

**Public HouseReef Report — Events Section**

- `houseReefReport.xhtml` renders a dedicated "Events" section only when `includeEvents = true` for the accessed report link
- Events are filtered server-side to the past 12 months (rolling window: event date ≥ today minus 365 days) at report assembly time
- The event list in the public report is **read-only**; no create, edit, or delete controls are exposed
- All existing report sections (tank parameters, fish, vital data charts) remain unchanged

### Out of Scope (this iteration)

- Categorising or tagging events (e.g., "equipment", "livestock", "chemistry") — free text is sufficient for v1
- Attaching photos or files to events
- Searching or filtering events by keyword or date range within the authenticated logbook view
- Exporting events as part of the AI data export (`001`), though this is a natural future extension
- Event notifications or reminders
- Admin-level visibility of other users' events
- Pagination of the event list (all events for a tank are shown in one list; expected volume is low)

---

## User Scenarios & Testing

### User Story 1 — Log a Notable Reef Event from the Aquarium Detail Page (Priority: P1)

A logged-in reefer notices their skimmer overflowed and caused a three-hour water loss. They navigate to their aquarium detail page (the "Tanks" view). They see an "Events" panel below the tank parameters. They click "Add event", fill in today's date, enter `3` in the duration field, and type "Skimmer overflowed — approx. 3 hours until noticed; topped off with fresh saltwater mix" in the description. They save. It immediately appears at the top of the event list on the same page.

**Why this priority**: This is the core value of the feature. Without the ability to create and see events, the entire logbook concept does not exist. It is independently demonstrable.

**Independent Test**: Can be fully tested by logging in as a user with at least one aquarium, opening the tank view, creating an event with all mandatory fields, and verifying it appears at the top of the list — without requiring any other user story.

**Acceptance Scenarios**:

1. **Given** a logged-in user on the aquarium detail page, **When** they enter a valid date and description and save, **Then** the new event appears at the top of the event list for that aquarium.
2. **Given** a user who leaves the description field empty, **When** they attempt to save, **Then** a field-level validation error is displayed and no event is created.
3. **Given** a user who leaves the date field empty, **When** they attempt to save, **Then** a field-level validation error is displayed and no event is created.
4. **Given** a user who enters a duration of `2.5`, **When** they save a valid event, **Then** the event is stored and `2.5 h` is displayed in the list entry.
5. **Given** a user who leaves the duration field empty, **When** they save a valid event, **Then** the event is saved with no duration and the list entry shows only date and description.
6. **Given** a user who types a multi-line description with explicit line breaks, **When** the event is displayed in the list, **Then** the line breaks are visually preserved.

---

### User Story 2 — Edit or Delete an Existing Event (Priority: P2)

A reefer logged an event yesterday but made a typo in the description. They open the aquarium detail page, find the event in the list, and click the edit icon. The entry form is populated with the existing values. They correct the description and save. The updated event appears in-place in the list. Later they decide the event was too trivial to keep and delete it using the delete button; after confirmation the event is gone.

**Why this priority**: Editing and deletion complete the basic CRUD lifecycle required to maintain data quality. User Story 1 (create + read) is independently valuable as a day-one MVP; edit/delete is the natural next priority.

**Independent Test**: Can be tested independently after User Story 1 by using the edit and delete actions on a previously created event, verifying the changes are persisted and reflected in the list.

**Acceptance Scenarios**:

1. **Given** an existing event, **When** the user clicks the edit button, **Then** the entry form is populated with the event's current values.
2. **Given** an edit form populated with current values, **When** the user modifies the description and saves, **Then** the updated description is persisted and displayed in the list.
3. **Given** an edit form, **When** the user clears the mandatory description field and saves, **Then** a validation error is shown and the original data is unchanged.
4. **Given** an edit form, **When** the user clears the optional duration and saves, **Then** the event is updated with no duration value and no error is shown.
5. **Given** an existing event, **When** the user clicks the delete button and confirms, **Then** the event is permanently removed and no longer appears in the list.

---

### User Story 3 — Include Events in the Public HouseReef Report (Priority: P3)

A reefer wants their public aquarium showcase to include a logbook of noteworthy events. In user profile → "Public HouseReef Report links", for the tank that has an active report link, they check "Include events in report." A visitor who opens the public report URL sees a new "Events" section below the vital data charts, listing events from the past 12 months (date, duration if any, description). An event older than 12 months is not shown even though it exists in the tank's logbook.

**Why this priority**: This is an additive capability on top of the existing public report and depends on User Stories 1 and 2, making it naturally the last story to implement.

**Independent Test**: Can be tested by enabling the `includeEvents` flag for a tank that has at least one event within the past 12 months and one event older than 12 months, accessing the public report URL, and verifying only the recent event appears.

**Acceptance Scenarios**:

1. **Given** a report link with `includeEvents = false`, **When** a visitor opens the public report, **Then** no events section is rendered.
2. **Given** a report link with `includeEvents = true` and events within the past 12 months, **When** a visitor opens the public report, **Then** a dedicated "Events" section lists those events sorted newest first.
3. **Given** events both within and older than 12 months for the linked aquarium, **When** a visitor opens the public report with `includeEvents = true`, **Then** only events within the past 12 months are shown; older events are silently excluded.
4. **Given** a report link with `includeEvents = true` but no events recorded, **When** a visitor opens the public report, **Then** the events section is rendered with an "no events recorded" placeholder message.
5. **Given** an unauthenticated visitor accessing the public report URL, **When** the report is rendered with `includeEvents = true`, **Then** the events section is visible to the visitor without requiring a Sabi login, consistent with all other public report sections.

---

### Edge Cases

- What happens if a user deletes all events for a tank that has `includeEvents = true`? The events section in the public report renders with a "no events recorded" placeholder — the section header remains so visitors know events are intended to be shared.
- What if the event list for a tank grows large (e.g., 200+ entries)? All events are loaded for the authenticated view in v1 (no pagination); expected typical volume is well under 100 entries per tank per year. Pagination can be added in a future iteration without a spec change.
- What if a user enters a negative or zero duration? The duration field must validate as a positive decimal greater than zero; a non-positive value triggers a validation error and the event is not saved.
- What if a user attempts to edit or delete an event belonging to another user (e.g., via a crafted API call)? The backend enforces ownership on every write operation; such a request receives HTTP 403.
- What if the report link token has expired at the moment a visitor accesses it? The existing expired-link handling (`linkExpired` flag in `PublicReefReportTo`) applies unchanged; the events section is never reached.
- What if a user generates a new report link for a tank (which replaces the previous one)? The `includeEvents` flag of the previous link is not automatically carried over to the new link; the new link starts with `includeEvents = false`. The user must re-enable the option if they want events in the new report link.

---

## Requirements

### Functional Requirements

- **FR-001**: The aquarium detail page (`tankView.xhtml`) MUST display an "Events" panel for each aquarium owned by the authenticated user, containing both the creation form and the event list.
- **FR-002**: The creation form MUST contain three fields: date (mandatory, date picker), duration in hours (optional, positive decimal input), and description (mandatory, multi-line text area).
- **FR-003**: The system MUST reject a save attempt when the date field is empty, displaying a field-level validation error message.
- **FR-004**: The system MUST reject a save attempt when the description field is empty, displaying a field-level validation error message.
- **FR-005**: The system MUST reject a save attempt when the duration field is present but contains a value that is zero or negative, displaying a field-level validation error message.
- **FR-006**: The events list for an aquarium MUST be displayed in chronologically descending order (most recent first), with each entry showing at minimum: event date, duration (if set), and description with line breaks preserved.
- **FR-007**: Each event entry in the list MUST offer an edit action that repopulates the form with the event's current field values for in-place editing.
- **FR-008**: Each event entry in the list MUST offer a delete action using a PrimeFaces `p:confirmDialog` (with a unique `widgetVar` per event row, e.g., `confirmDeleteEvent_#{event.id}`). The delete button triggers the dialog via `oncomplete="PF('confirmDeleteEvent_#{event.id}').show()"` with `ajax="true"`; a "Confirm" button inside the dialog calls `aquariumEventView.deleteEvent(event.id)`. This permanently removes the event. The i18n keys `aquariumevent.delete.confirm.header` and `aquariumevent.delete.confirm.message` MUST be present in all 6 message bundles (covered by FR-010).
- **FR-009**: All event CRUD backend operations MUST enforce aquarium ownership: the authenticated user MUST own the aquarium associated with the event; requests that fail this check MUST be rejected with HTTP 403.
- **FR-010**: All new UI texts (labels, buttons, validation messages, placeholders, section headers) MUST be supplied in all 6 message bundle files: `messages.properties`, `messages_de.properties`, `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`, `messages_it.properties`.
- **FR-011**: The user profile page (`userProfile.xhtml`) MUST render a boolean "Include events in report" `p:selectBooleanCheckbox` within each active per-tank report link row (inside the existing `reportLinkRow_#{tank.id}` form), alongside a dedicated `p:commandButton` (`msg['common.save.b']`, `ajax="false"`) calling `userProfileView.saveIncludeEvents(tank.id)` to persist the flag.
- **FR-012**: The `includeEvents` boolean flag MUST be persisted as part of the public report link record (extending `PublicReportLinkTo` and its backing persistence entity with a new `include_events` column); it MUST default to `false`.
- **FR-013**: When the user saves the `includeEvents` checkbox state via the profile page, the updated value MUST be persisted immediately and take effect on the next report request without requiring a server restart or cache invalidation.
- **FR-014**: When assembling the public report and `includeEvents` is `true`, the backend MUST include only events whose `eventDate` is no earlier than 365 days before the current date at assembly time; this filter MUST be applied server-side before any event data is sent to the client.
- **FR-015**: The public report page (`houseReefReport.xhtml`) MUST render an "Events" section if and only if `includeEvents` is `true` for the accessed report link; when `includeEvents` is `false` the section MUST NOT appear.
- **FR-016**: The events section in the public report MUST be strictly read-only; no create, edit, or delete controls shall be rendered.
- **FR-017**: All event CRUD endpoints MUST require a valid user session; unauthenticated requests MUST be rejected with HTTP 401, consistent with other secured API endpoints.
- **FR-018**: A Flyway migration (MariaDB syntax) MUST create the `aquarium_event` table with columns: `id` (BIGINT UNSIGNED PK AUTO_INCREMENT), `aquarium_id` (BIGINT UNSIGNED FK → aquarium, NOT NULL), `event_date` (DATE, NOT NULL), `duration_hours` (DECIMAL(6,2), nullable, must be > 0 when present), `description` (TEXT, NOT NULL), `created_on` (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP), `lastmod_on` (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP), `optlock` (INT UNSIGNED NOT NULL DEFAULT 0). `AquariumEventEntity` MUST extend `Auditable`; the `updated_on` and `version` column names mentioned in the original draft are replaced by `lastmod_on` and `optlock` respectively to match the `Auditable` superclass.
- **FR-019**: A Flyway migration (MariaDB syntax) MUST add a boolean column `include_events TINYINT(1) NOT NULL DEFAULT 0` to the existing `public_report_link` table.
- **FR-020**: All event CRUD logic MUST be encapsulated in a new `AquariumEventController` class (`@RestController @RequestMapping("api/tank")`) with four endpoints: `GET /{tankId}/events`, `POST /{tankId}/events` (→ HTTP 201), `PUT /{tankId}/events/{eventId}` (→ HTTP 200), `DELETE /{tankId}/events/{eventId}` (→ HTTP 200). No event endpoints shall be added to `TankController`.
- **FR-021**: `UserProfileView` MUST expose a `saveIncludeEvents(Long tankId)` action method that persists the updated `includeEvents` flag for the report link associated with the given tank, called by an explicit "Save" (`msg['common.save.b']`) button added to the `reportLinkRow_#{tank.id}` form in `userProfile.xhtml`.

### Non-Functional Requirements (ISO 25010)

| ISO 25010 Merkmal         | Anforderung / Constraint für dieses Feature                                                                                                                                                             |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Funktionale Eignung       | Alle 19 FRs müssen erfüllt sein. Ownership-Check auf allen Event-Endpunkten zwingend (FR-009, FR-017). Der 12-Monats-Filter (FR-014) ist per Integrationstest mit präzisen Testdaten (–6 Monate, –13 Monate) prüfbar. |
| Leistungseffizienz        | Laden der Event-Liste für ein Aquarium (erwartete Maximalgröße: ~200 Einträge) in unter 1 Sekunde. Für den Public Report: Event-Assembly darf die bestehende Report-Antwortzeit nicht um mehr als 200 ms erhöhen. |
| Kompatibilität            | Zwei neue Flyway-Migrationen (FR-018, FR-019) ohne Breaking Change an bestehenden Endpunkten. Neuer Event-CRUD-Endpunkt unter `/api/tank/{tankId}/events`. Muss auf ARM64 und AMD64 laufen.              |
| Benutzbarkeit             | Alle neuen Texte i18n in DE/EN/ES/FR/IT + Fallback (FR-010). WCAG 2.1 AA Kontrast für alle neuen UI-Elemente. Date-Picker konsistent mit `measureView.xhtml`. Maximal 2 Klicks vom Tank-View bis zum Save eines neuen Events. |
| Zuverlässigkeit           | Optimistisches Locking auf `aquarium_event`-Tabelle (FR-018). Bei gleichzeitiger Bearbeitung desselben Events: konflikterkennung und benutzerfreundliche Fehlermeldung. Transaktionssicherheit beim Delete. |
| Sicherheit                | Authentifizierung zwingend für alle Event-Writes (FR-009, FR-017). Strikte Owner-Isolierung: kein Cross-User-Zugriff auf Events. Events im Public Report nur über bestehenden Token-Mechanismus erreichbar; kein separater öffentlicher Event-Endpunkt. |
| Wartbarkeit               | Flyway-Migrationen unter `version1_6_0`, MariaDB-Syntax (Backtick-Identifier, `BIGINT UNSIGNED`, `TINYINT(1)` für Boolean). `AquariumEventEntity` erweitert `Auditable` (→ `lastmod_on`, `optlock`). Mindestens ein Integrationstest für Event-CRUD und ein Test für den 12-Monats-Filter. Kein neues externes Dependency. `AquariumEventTo` als neues DTO in `sabi-boundary`. Neuer `AquariumEventController` (FR-020). |
| Übertragbarkeit           | Kein Docker- oder Ansible-Änderungsbedarf. Rein applikationsseitiger Change (sabi-server + sabi-webclient + sabi-boundary + sabi-database).                                                              |

### Key Entities

- **AquariumEvent** (new, persisted): A single logbook entry for an aquarium. Attributes: `id` (internal PK), `aquariumId` (FK), `eventDate` (mandatory date), `durationHours` (optional positive decimal, stored as `DECIMAL(6,2)`), `description` (mandatory unlimited text), `createdOn`, `lastmodOn` (formerly referred to as `updatedOn`), `optlock` (optimistic lock, formerly referred to as `version`). One aquarium has zero-to-many events. Entity class MUST extend `Auditable`.
- **AquariumEventTo** (new, boundary DTO in `sabi-boundary`): Transfer object mirroring `AquariumEvent` fields; used by the authenticated CRUD API and the public report assembly.
- **PublicReportLinkTo** (existing, extended): Gains a new `includeEvents` boolean field (default `false`). When `true`, the report assembly includes recent events.
- **PublicReefReportTo** (existing, extended): Gains a new optional `recentEvents` field (`List<AquariumEventTo>`, `null` when `includeEvents` is `false`).
- **Aquarium** (existing): No structural change; events are associated via the `aquariumId` FK on `AquariumEvent`.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: A logged-in reefer can create, view, edit, and delete events for any of their aquariums entirely within the aquarium detail page (no separate navigation required), verified by an end-to-end acceptance scenario covering all four CRUD operations.
- **SC-002**: The event list for an aquarium with up to 200 entries loads in under 1 second, verified by a load test against the staged environment.
- **SC-003**: 100% of event write requests (create, update, delete) from a different authenticated user targeting another user's aquarium events are rejected with HTTP 403, verified by an automated security test.
- **SC-004**: All new UI keys are present and non-empty in all 6 message bundle files, verified by an automated i18n completeness check consistent with the pattern established in feature `002`.
- **SC-005**: When `includeEvents = true` and the aquarium has an event dated –13 months ago, 0% of such out-of-window events appear in the public report, verified by a dedicated integration test using a controlled set of events with dates of –6 months and –13 months.
- **SC-006**: When `includeEvents = false`, no events section is rendered in the public report HTML output, verified by automated UI test.
- **SC-007**: Both Flyway migrations (`aquarium_event` table and `include_events` column) apply cleanly on a fresh MariaDB schema and on a database migrated from the v1.5.x baseline, verified by the CI migration test suite. (Note: SC-007 previously referenced "PostgreSQL" in error — the application uses MariaDB as confirmed by `application.yml` driver configuration `org.mariadb.jdbc.Driver`.)

---

## Assumptions

- Events belong exclusively to a single aquarium; cross-tank events (e.g., a room power outage affecting all tanks) are recorded as duplicate per-tank entries at the user's discretion.
- Duration is stored as a decimal number of hours (e.g., `2.5` = two and a half hours); no separate minute field is needed.
- The description stores plain text; HTML or Markdown rendering is out of scope for v1. Line breaks are preserved visually by the `p:inputTextarea` component.
- The event date picker captures a calendar date without a time-of-day component, consistent with the pattern in `measureView.xhtml` (`p:datePicker` with `showTime="false"`).
- The "12 months" filter is a rolling 365-day window relative to the moment the report is fetched; no fixed calendar-year boundary is used.
- The `includeEvents` checkbox is rendered per active report link row; if no report link exists for a tank, no checkbox is shown for that tank.
- When a user generates a new report link (replacing the previous one), the `includeEvents` flag resets to `false` for the new link; users must re-enable it explicitly.
- Physical deletion of events is unconditionally permitted; there are no domain-level constraints preventing deletion (unlike fish stock entries, which carry departure record constraints).
- The Flyway migration for `aquarium_event` is versioned under a new `version1_6_0` directory, which is the next logical version after the existing `version1_5_0` migrations.
- The existing `PublicReportController` and its service layer handle the public report assembly; events will be integrated into the same assembly chain used for fish inhabitants and measurements.
- WCAG 2.1 AA colour contrast is maintained by reusing the existing Sabi CSS palette (`#075985`, `#0369a1`, `#1e293b`, `#64748b`) for all new UI elements, consistent with patterns in `houseReefReport.xhtml` and `tankView.xhtml`.

---

## Constraints

| ID  | Constraint                                                                                                                                                                            |
|-----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C-1 | Event data (all fields including description) MUST NOT be accessible to other authenticated users via any API endpoint; strict per-user ownership isolation applies.                  |
| C-2 | Events for the public report MUST be fetched and assembled server-side as part of `PublicReefReportTo`; no dedicated public event API endpoint shall be created.                     |
| C-3 | The 12-month cutoff for public report events MUST be applied server-side; client-side filtering alone is insufficient.                                                                |
| C-4 | No new external runtime library or service dependency may be introduced by this feature.                                                                                             |
| C-5 | The `aquarium_event` table MUST include an optimistic locking `version` column, consistent with other Sabi entity tables.                                                             |
| C-6 | The `includeEvents` flag MUST default to `false` for all new and existing report links; no existing report is affected unless the owner explicitly opts in.                           |

---

## Clarifications

### Session 2026-05-14

- Q: What column names must the `aquarium_event` Flyway migration use for the audit/lock columns — `updated_on`/`version` as stated in FR-018, or `lastmod_on`/`optlock` as used by the `Auditable` base class? → A: `lastmod_on` and `optlock`; `AquariumEventEntity` MUST extend `Auditable`

  **Applied to FR-018 and Data Model:** The `Auditable` superclass (used by every persisted entity in the project, including `PublicReportLinkEntity`) maps `created_on`, `lastmod_on`, and `optlock` (`integer DEFAULT 0 UNSIGNED`). `AquariumEventEntity` MUST extend `Auditable`. Consequently FR-018 is corrected: the Flyway migration MUST declare columns `lastmod_on` (not `updated_on`) and `optlock` (not `version`) with the same types and defaults as in `V1_5_0_11__addPublicReportLink.sql`. The DTO field may be named `updatedOn` in Java (mapped from `lastmodOn`), but the DB column MUST be `lastmod_on`. FR-018 is amended accordingly. The `version` column name mentioned in FR-018 is replaced by `optlock INT UNSIGNED NOT NULL DEFAULT 0`.

- Q: Should CRUD endpoints for `/api/tank/{tankId}/events` live in a new `AquariumEventController` class or be added to the existing `TankController`? → A: New dedicated `AquariumEventController`

  **Applied to Functional Requirements and Constraints:** A new `AquariumEventController` class annotated `@RestController @RequestMapping("api/tank")` MUST be created in `sabi-server` under the same `rest.controller` package. The four CRUD endpoint paths are: `GET /{tankId}/events` (list), `POST /{tankId}/events` (create → HTTP 201), `PUT /{tankId}/events/{eventId}` (update → HTTP 200), `DELETE /{tankId}/events/{eventId}` (delete → HTTP 200). Ownership is verified by calling `tankService.getTank(tankId, principal.getName())` before any operation, returning HTTP 403 on mismatch — consistent with the pattern in `MeasurementController.listUsersTankMeasurements`. Added as **FR-020**: The event CRUD logic MUST be encapsulated in a new `AquariumEventController` class; no event endpoints SHALL be added to `TankController`.

- Q: How should the delete confirmation be implemented in `tankView.xhtml` given that no existing delete operation in the app uses a confirmation dialog? → A: PrimeFaces `p:confirmDialog` per event row, triggered via `oncomplete`

  **Applied to FR-008 and User Story 2:** No existing XHTML view (including `tankView.xhtml`) currently shows a confirmation dialog before a delete; the pattern is to call the action directly. To honour the "user confirmation step" stated in FR-008 without conflicting with existing UI patterns, a PrimeFaces `p:confirmDialog` (with a unique `widgetVar` derived from the event ID) MUST be used. The delete `p:commandButton` in each event row triggers it via `oncomplete="PF('confirmDeleteEvent_#{event.id}').show()"` with `ajax="true"`. A "Confirm" button inside the dialog calls the actual `aquariumEventView.deleteEvent(event.id)` action. This is the first in-page confirmation dialog in the project and establishes the pattern for future delete confirmations. The i18n keys `aquariumevent.delete.confirm.header` and `aquariumevent.delete.confirm.message` MUST be added to all 6 message bundles (covered by FR-010).

- Q: How does the user save the `includeEvents` checkbox change in `userProfile.xhtml` — auto-save on change, or an explicit save button? → A: Explicit save button within the existing `reportLinkRow_#{tank.id}` form

  **Applied to FR-011 and FR-013:** The existing `reportLinkRow_#{tank.id}` form in `userProfile.xhtml` already wraps the per-tank report link row and contains the "Link widerrufen" delete button. The `includeEvents` `p:selectBooleanCheckbox` MUST be added to this same form, accompanied by a dedicated `p:commandButton` using `msg['common.save.b']` (reusing the existing i18n key) that calls `userProfileView.saveIncludeEvents(tank.id)` with `ajax="false"`. No auto-save on checkbox interaction; an explicit save button prevents accidental toggling and is consistent with the locale-switcher save pattern already present in `userProfile.xhtml`. Added as **FR-021**: `UserProfileView` MUST expose a `saveIncludeEvents(Long tankId)` action method that persists the updated `includeEvents` flag for the report link associated with the given tank.

- Q: SC-007 references "PostgreSQL" but `application.yml` configures MariaDB (`org.mariadb.jdbc.Driver`, `jdbc:mariadb://`). Which dialect must be used for new Flyway migrations? → A: MariaDB syntax, consistent with all existing migrations

  **Applied to SC-007 and FR-018/FR-019:** The production and development database is MariaDB, not PostgreSQL. All existing migrations under `version1_5_0` use MariaDB/MySQL syntax: backtick-quoted identifiers, `BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT`, `TINYINT(1)` for booleans, `DEFAULT CHARSET = utf8`, and SQL `COMMENT` clauses. The two new Flyway migrations (FR-018: `aquarium_event` table; FR-019: `include_events` column) MUST follow the same syntax. SC-007 is corrected: CI migration tests run against MariaDB, not PostgreSQL. The `include_events` column (FR-019) MUST be declared as `TINYINT(1) NOT NULL DEFAULT 0` (the MariaDB boolean convention used throughout the schema).

---

*This specification was created for branch `004-aquarium-events`. Next step: `/speckit.plan` to break this into implementation tasks.*
