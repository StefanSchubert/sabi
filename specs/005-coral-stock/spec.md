# Feature Specification: Coral Stock Management & Coral Catalogue

**Feature Branch**: `005-coral-stock`  
**Created**: 2026-04-20  
**Last Revised**: 2026-05-16  
**Status**: Draft  
**Input**: User description: "Analog zur Erfassung von Fischen (wirklich das ganze Handling inkl. der Wissenschaftlichen Referenz, Vorschlag und Freigabe über einen Zentralen Katalog, Aufnahme in AI-Json und im House-Reef-Report etc.) möchte ich nun die Möglichkeit haben Korallen zu erfassen. Diese werden im Meerwasserbereich klassifiziert als LPS und SPS Korallen. Bei den Fischen haben wir pro Fisch die Möglichkeit chronologisch das Wachstum zu erfassen. Bei Korallen ist das etwas differenzierter. Wachstumsparameter können sein: Oberfläche in cm², Größe in cm, Volumen in cm³, Anzahl Äste. Wichtig wäre auch die chronologische Erfassung des Polypenbildes, hier wären Beobachtungen wichtig wie: vital, Gewebeverlust, wirken blasser, wirken schlaffer, deutliches Wachstum."

---

## Overview

### Problem Statement

Sabi users who keep saltwater reef aquariums maintain not only fish but also a diverse coral population — often the centrepiece of a reef tank. Today, Sabi provides no way to document which corals are kept in a tank, how they grow over time, or how their health evolves. This gap means that users cannot correlate coral health with water chemistry events, share their coral specimens with the community, or feed coral data into the existing AI chatbot export for reef diagnostics.

### Proposed Solution

Introduce two tightly coupled sub-features, fully symmetric with the existing Fish Stock & Catalogue pattern (spec `002`):

1. **Tank Coral Stock** — a per-aquarium coral roster where each entry captures: the date the coral was added, an optional personal photo, optional free-text notes, an optional external reference URL, a link to a shared coral catalogue entry, and regular chronological records of:
   - **Growth measurements** using one or more flexible metric types: surface area (cm²), size (cm), volume (cm³), or branch count.
   - **Polyp condition observations** using standardised health states: vital, tissue loss, paler, limper, significant growth.

2. **Coral Catalogue** — a community-maintained, i18n-aware reference catalogue of coral species. Each species carries its scientific name, LPS/SPS classification, care level, water-parameter hints, and localised common names, descriptions, and reference URLs for all five supported languages. The same User-Generated-Content (UGC) proposal-and-approval workflow as the fish catalogue governs quality and prevents duplicates.

### Business Value

- **Complete reef documentation**: Users can track the full story of their reef — chemistry, fish, and corals together in one place.
- **Health trend analysis**: Chronological polyp-condition and growth records enable correlations with water-quality events and are AI-ready.
- **Community coral reference**: A curated, community-built coral catalogue supplements the fish catalogue and deepens the reef knowledge base.
- **AI export enrichment**: Coral data extends the existing AI-JSON export, giving AI chatbots a more complete reef picture for diagnostics and advice.
- **House Reef Report visibility**: Sharing a reef report now conveys the coral population alongside fish, presenting a more realistic snapshot of the tank.

---

## Clarifications

### Session 2026-05-16

- Q: Can an Admin edit a Rejected coral catalogue entry? → A: Yes — Admin can edit Rejected entries; the "read-only" restriction in FR-029 applies only to non-Admin users (creators and regular users).
- Q: Can existing growth history entries and polyp condition entries be edited after creation? → A: Yes — authenticated owners can update the date and measured value of an existing growth history entry, and the date and condition state of an existing polyp condition entry (see FR-039 and FR-040).
- Q: How is the growth history displayed in the UI? → A: Table by default, with a toggle button to switch to a line chart per measurement type (one line per type); see FR-041.
- Q: Is there a departure note field, and what are the departure reason values? → A: Yes — departure note (free text, max 500 chars, optional) AND departure reason as an enum: `DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`; both fields are optional (see FR-005, FR-042).
- Q: Is there a rate limit for coral catalogue proposals? → A: Same rule as the fish catalogue (spec `002`): no dedicated rate limiting is applied to coral catalogue proposal submissions; mandatory JWT authentication (FR-038) provides sufficient protection, consistent with the fish catalogue approach.

---

## Scope

### In Scope

**Tank Coral Stock**

- A new "Coral Stock" tab (or section) on the existing aquarium detail page, alongside the existing "Fish Stock" tab
- Add / edit / remove coral entries per aquarium; **"remove" has two distinct semantics** identical to fish stock: (a) recording a **Departure** (end date + reason) for a genuine departure, retaining the entry in the historical view; (b) **physical deletion** of an entry created in error — only permitted when NO departure record exists
- Per-coral fields: species name (free text or catalogue reference — mandatory), entry date (mandatory), LPS/SPS classification snapshot, optional personal photo, optional free-text notes (unlimited), optional external reference URL
- Optional soft-link to a coral catalogue entry (pre-fills species name, classification, and the localised reference URL)
- Departure record: end date, departure reason (mandatory when recording a departure, one of: `DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`), and an optional free-text departure note (max 500 characters)
- Display of currently-present corals separately from departed corals (historical view); departed section collapsed by default
- **Growth history**: per coral entry, an unlimited ordered list of growth measurements; each measurement record captures: date, measurement type (one of: `SURFACE_AREA_CM2`, `SIZE_CM`, `VOLUME_CM3`, `BRANCH_COUNT`), and the measured value; multiple measurement types can be recorded on the same date; the history is displayed in chronological order descending
- **Polyp condition history**: per coral entry, an unlimited ordered list of polyp condition observations; each record captures: date and condition state (one of: `VITAL`, `TISSUE_LOSS`, `PALE`, `LIMP`, `SIGNIFICANT_GROWTH`); multiple observations can be recorded on the same date; the history is displayed in chronological order descending

**Coral Catalogue**

- A coral catalogue accessible from the coral stock entry form (search & select)
- Catalogue entries contain: scientific name (Latin, unique, mandatory), LPS/SPS classification (mandatory), care level (EASY / MODERATE / DEMANDING), localised common name per language (DE, EN, ES, FR, IT), localised description per language (optional), localised reference URL per language (optional)
- Identical UGC workflow as the fish catalogue:
  - Any authenticated user may propose a new catalogue entry
  - A proposed entry is immediately visible and selectable only by its creator (status: **Private / Pending**)
  - Admin can approve a proposal → status changes to **Public** (visible to all users)
  - Admin can reject a proposal → status changes to **Rejected** (invisible to all); duplicate prevention warns on scientific name collision
- Creator and subsequent editors can add/update localised fields for any supported language
- A dedicated admin view listing all pending coral catalogue proposals for review

**Integration: House Reef Report**

- The public House Reef Report for an aquarium extends to include the currently-present coral stock (species name, classification snapshot, most recent growth measurement per type, most recent polyp condition)
- Coral data is only included if the tank owner has opted the report link in to coral display (same opt-in mechanism used for events in spec `004`)

**Integration: AI-JSON Export**

- The AI data export (spec `001`) is extended: each aquarium's JSON object gains a `corals` array alongside the existing `fish` array
- Each coral entry in the export contains: catalogue reference ID (if linked), scientific name snapshot, common name, classification snapshot, entry date, departure date (if applicable), free-text notes, complete growth history (date, type, value), complete polyp condition history (date, condition)

**UI / i18n**

- All new UI labels, buttons, messages, classification labels, care-level labels, condition-state labels, growth-type labels and status texts in all 6 message bundle files (DE, EN, ES, FR, IT + fallback)
- WCAG 2.1 AA colour-contrast compliance for all new UI elements

### Out of Scope (this iteration)

- Import of coral data from external sources (copyright risk, consistent with fish catalogue decision)
- Bulk import of coral catalogue entries
- Automated deduplication of coral catalogue entries (manual admin review is sufficient)
- Community commenting or rating of coral catalogue entries
- Push notifications to the creator when a proposal is approved/rejected
- Public coral catalogue browsing without login
- Multiple photos per coral entry (single photo upload in v1)
- Versioning or history of coral catalogue entry edits
- Freshwater plant tracking (future feature)
- Invertebrate stock tracking (future feature)
- Automatic water-parameter recommendations derived from coral care level
- Export of coral photos in the AI-JSON (file size concerns; text data only in v1)
- Coral fragmentation / fragging workflow (deferred)

---

## User Scenarios & Testing

### User Story 1 — Add a Coral to My Aquarium (Priority: P1)

A user opens one of their saltwater aquariums and navigates to the new "Coral Stock" tab. They click "Add coral", type the common name "Green Star Polyp", set the entry date to today, select "LPS" as classification, add a note ("Bought from local reefer; placed on a small rock in the flow zone"), and save. The coral now appears in the "Currently in tank" list.

**Why this priority**: Core value of the feature; nothing else depends on anything else until a coral entry exists. Independently deployable as MVP.

**Independent Test**: Log in as a user with at least one aquarium, open the aquarium detail page, navigate to the "Coral Stock" tab, add a coral entry with all mandatory fields plus at least one optional field, save, and verify the entry appears in the "Currently in tank" list with all saved data.

**Acceptance Scenarios**:

1. **Given** a logged-in user with at least one aquarium, **When** they open the aquarium detail page, **Then** a "Coral Stock" tab is visible alongside the existing "Fish Stock" tab.
2. **Given** the Coral Stock tab is open, **When** the user clicks "Add coral", fills in the mandatory fields (species name, entry date) and saves, **Then** the coral appears in the "Currently in tank" section.
3. **Given** a new coral entry form, **When** the user fills in all optional fields (photo, notes, reference URL) and saves, **Then** all fields are persisted and displayed correctly on the coral detail view.
4. **Given** a coral entry form, **When** the user submits without a mandatory field (species name or entry date), **Then** the form shows an inline validation error and does not save.
5. **Given** a coral entry with a reference URL, **When** the user clicks the link, **Then** it opens in a new browser tab.

---

### User Story 2 — Record a Coral Departure (Priority: P2)

A user discovers that one of their SPS corals has bleached completely and is no longer salvageable. They open the Coral Stock tab, find the coral in the "Currently in tank" list, click "Record departure", enter today's date, select reason "DIED" (Died / Deceased), and confirm. The coral moves from the "Currently in tank" list to a collapsible "Departed corals" section, showing the departure date and reason.

**Why this priority**: Without departure tracking, the active coral list becomes inaccurate over time. No dependency on the catalogue sub-feature.

**Independent Test**: Add a coral entry, then record a departure for it; verify it disappears from the active list and appears in the historical section with the correct departure date and reason.

**Acceptance Scenarios**:

1. **Given** an active coral entry (no departure date), **When** the user records a departure with a valid date and reason, **Then** the coral moves to the "Departed corals" section immediately.
2. **Given** a departure form, **When** the user selects a departure date earlier than the entry date, **Then** an inline validation error is shown and the form cannot be saved.
3. **Given** a coral in the "Departed corals" section, **When** the user views it, **Then** the departure date, departure reason, and all original fields are still visible.
4. **Given** a coral with a departure record, **When** the user views the aquarium Coral Stock tab, **Then** the "Departed corals" section is collapsed by default but can be expanded.

---

### User Story 3 — Log a Growth Measurement (Priority: P3)

A user wants to track the growth of their Acropora. They open its coral detail view, click "Add growth measurement", choose measurement type "SIZE_CM", enter value "4.5", set the date to today, and save. Three weeks later they add a second measurement: "6.2 cm". The coral detail view now shows a chronological growth history with both entries. The user then adds a "BRANCH_COUNT" measurement (7 branches), demonstrating that multiple metric types can be tracked independently for the same coral.

**Why this priority**: Growth tracking is a primary differentiator of coral tracking vs. fish tracking. It depends only on P1.

**Independent Test**: Add a coral entry, log three growth measurements (at least two different types, at least two on different dates), verify all records appear in the growth history in date-descending order.

**Acceptance Scenarios**:

1. **Given** a coral stock entry, **When** the user adds a growth measurement with a valid date, type, and positive numeric value, **Then** the measurement appears at the top of the growth history list.
2. **Given** a coral with growth history, **When** the user views the coral detail, **Then** all growth measurements are listed in date-descending order, each showing date, type, and value.
3. **Given** a growth measurement form, **When** the user submits a zero or negative value, **Then** an inline validation error is shown and the record is not saved.
4. **Given** a coral with growth measurements, **When** the user deletes a single growth measurement record, **Then** only that record is removed; all other growth records and the parent coral entry remain intact.
5. **Given** a coral with growth history of multiple measurement types, **When** the user views the growth history, **Then** records of different types (e.g., SIZE_CM and BRANCH_COUNT) are all visible and distinguishable by their type label.
6. **Given** an existing growth measurement record owned by the user, **When** the user edits it and saves a new date and/or value, **Then** the record is updated and the growth history reflects the change immediately; the measurement type remains unchanged.
7. **Given** the growth history view showing a table, **When** the user clicks the chart toggle button, **Then** the view switches to a line chart displaying one line per measurement type; clicking the toggle again restores the table view.

---

### User Story 4 — Log a Polyp Condition Observation (Priority: P4)

A user notices that their Torch coral looks paler than usual after a spike in nitrates. They open the coral detail view, click "Add polyp observation", select condition "PALE", set today's date, and save. A month later, after corrective water changes, they add a new observation: "VITAL". The coral detail now shows a two-entry polyp condition history, giving a clear before-and-after picture.

**Why this priority**: Polyp condition tracking is the second primary differentiator specific to corals. It depends only on P1.

**Independent Test**: Add a coral entry, log two polyp condition observations on different dates with different states, verify both appear in the polyp condition history in date-descending order.

**Acceptance Scenarios**:

1. **Given** a coral stock entry, **When** the user adds a polyp condition observation with a valid date and a recognised condition state, **Then** the observation appears at the top of the polyp condition history list.
2. **Given** a coral with polyp condition history, **When** the user views the coral detail, **Then** all observations are listed date-descending, each showing date and condition label (localised).
3. **Given** a polyp observation form, **When** the user submits without selecting a condition state or without a date, **Then** an inline validation error is shown and the record is not saved.
4. **Given** a coral with polyp condition records, **When** the user deletes a single observation, **Then** only that record is removed; all remaining records and the parent coral entry are intact.
5. **Given** an existing polyp condition observation owned by the user, **When** the user edits it and saves a new date and/or condition state, **Then** the record is updated and the polyp condition history reflects the change immediately.

---

### User Story 5 — Link a Coral Entry to the Coral Catalogue (Priority: P5)

A user adding a new coral (Acropora millepora) types "Acropora" into the catalogue search on the add-coral form, sees a dropdown of matching catalogue entries, selects "Acropora millepora", and the form auto-fills the scientific name, the LPS/SPS classification ("SPS"), care level ("DEMANDING"), and a default reference URL. The user keeps the auto-filled values, adds personal notes, and saves.

**Why this priority**: Catalogue linking improves data quality and speeds up entry. The feature works without it: free-text species name is always the fallback.

**Independent Test**: With at least one public catalogue entry, open the add-coral form, search the catalogue, select an entry, verify auto-fill, save, and confirm the catalogue link is stored.

**Acceptance Scenarios**:

1. **Given** the add-coral form, **When** the user types at least 2 characters in the catalogue search field, **Then** a dropdown of matching entries (by common name or scientific name) appears within 1 second.
2. **Given** a catalogue entry is selected from the dropdown, **When** the form updates, **Then** the scientific name, LPS/SPS classification, and care level are auto-filled from the catalogue entry and the reference URL is pre-filled with the entry's localised URL for the user's language (if available).
3. **Given** a coral entry linked to a catalogue entry, **When** the user saves and views the detail, **Then** the scientific name and classification are shown and the catalogue link is preserved.
4. **Given** a coral entry form, **When** the user does not use the catalogue search, **Then** they can still enter a free-text species name and save without a catalogue link.
5. **Given** a coral linked to a catalogue entry that is later updated, **When** the user views their coral, **Then** the scientific name and classification shown are the values copied from the catalogue at the time of linking and are NOT automatically updated.

---

### User Story 6 — Propose a New Coral Catalogue Entry (Priority: P6)

A user cannot find their coral in the catalogue. They click "Propose new catalogue entry" from the coral stock form, enter the scientific name "Lobophyllia hattai", select "LPS" as classification, set care level to "MODERATE", provide German common name "Gehirnkoralle", English common name "Lobed Brain Coral", add a short German description, paste a reference URL, and submit. The new entry immediately appears in their catalogue search results (marked "Pending approval", visible only to them). They link their coral entry to it and save.

**Why this priority**: The catalogue only grows through user contributions. This is the mechanism for organic growth. Depends on P1–P5.

**Independent Test**: Propose a new catalogue entry with a unique scientific name; verify it appears immediately in the proposer's search results but NOT in a different user's search results; verify it appears in the admin's pending-proposals queue.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they submit a new coral catalogue proposal with at least scientific name, classification, and one localised common name, **Then** the entry is saved with status "Pending" and is immediately searchable by the proposer only.
2. **Given** a catalogue proposal form, **When** the user enters a scientific name that already exists as Pending or Public, **Then** a non-blocking warning is shown but the user may proceed.
3. **Given** a pending catalogue entry, **When** a different non-admin user searches the catalogue, **Then** the pending entry is NOT visible in their results.
4. **Given** a pending catalogue entry, **When** the admin views the pending-proposals list, **Then** the entry appears with all submitted fields including classification and care level.
5. **Given** a coral stock entry linked to a pending catalogue entry, **When** the proposal is later approved, **Then** the coral stock entry remains linked and the snapshot values do not change.

---

### User Story 7 — Admin Approves or Rejects a Coral Catalogue Proposal (Priority: P7)

An admin navigates to "Coral Catalogue Administration", finds a pending proposal for "Lobophyllia hattai", reviews the fields, edits the English description for clarity, and clicks "Approve". The entry is now publicly visible. Later, the admin finds a duplicate proposal and clicks "Reject" with a note. The proposer's entry changes to "Rejected" and becomes invisible.

**Why this priority**: Quality governance layer. Depends on P6.

**Independent Test**: Log in as admin, find a pending proposal, approve it, then log in as a regular user and verify the approved entry now appears in coral catalogue search.

**Acceptance Scenarios**:

1. **Given** an admin user, **When** they navigate to the admin coral catalogue view, **Then** they see all pending proposals with proposer reference, submission date, scientific name, and classification.
2. **Given** an admin reviewing a proposal, **When** they click "Approve", **Then** the entry status changes to "Public" and immediately appears in all users' catalogue search results.
3. **Given** an admin reviewing a proposal, **When** they click "Reject" and optionally provide a reason, **Then** the entry status changes to "Rejected" and is no longer visible to any user (including the proposer).
4. **Given** a non-admin authenticated user, **When** they attempt to access the coral catalogue admin view, **Then** they receive an access-denied response.
5. **Given** an admin approving a proposal, **When** they edit any localised field before approving, **Then** the updated fields are saved as part of the approval action.

---

### User Story 8 — Coral Data in House Reef Report (Priority: P8)

A user has enabled the public House Reef Report share link for their display tank. After enabling the coral opt-in for the report link, visitors to the public report URL see a "Corals" section listing all currently-present corals: species name, LPS/SPS classification, the most recent growth measurement per type, and the most recent polyp condition. The section is clearly labelled and visually separated from the fish section.

**Why this priority**: Extends existing feature; depends on all coral stock being in place (P1–P4) and on the House Reef Report (pre-existing feature).

**Independent Test**: With corals in a tank and the report link opt-in enabled for corals, fetch the public report URL and verify the `corals` array is present and contains the correct snapshot data.

**Acceptance Scenarios**:

1. **Given** a public reef report link with the coral opt-in enabled and at least one currently-present coral, **When** the report is fetched, **Then** the response includes a `corals` array with the currently-present corals.
2. **Given** a public reef report link with the coral opt-in **disabled**, **When** the report is fetched, **Then** the `corals` array is absent (or empty), regardless of how many corals are in the tank.
3. **Given** a coral in the report, **When** the user views the report, **Then** they see: species name, classification, and the most recent value for each tracked growth measurement type.
4. **Given** a coral with polyp condition history in the report, **When** the user views the report, **Then** the most recent polyp condition state is shown next to the coral entry.
5. **Given** a coral that has departed (departure date set), **When** the report is fetched, **Then** that coral does NOT appear in the report.

---

### User Story 9 — Coral Data in AI-JSON Export (Priority: P9)

A user downloads their AI chatbot data export. They open the JSON file and see each aquarium now has a `corals` array alongside the `fish` array. Each coral entry shows scientific name, classification, entry date, notes, complete growth history with typed measurements, and complete polyp condition history. The user pastes the file into ChatGPT and asks: "My Acropora showed tissue loss 3 months ago — what could the cause be given my water parameters?"

**Why this priority**: Extension of pre-existing spec `001`. Depends on P1–P4 being implemented.

**Independent Test**: Download the AI export for a user who has at least one coral with growth history and at least one polyp condition observation; verify the exported JSON contains `corals` with all expected fields.

**Acceptance Scenarios**:

1. **Given** a user with at least one coral in at least one aquarium, **When** they download the AI export, **Then** each aquarium in the JSON has a `corals` array.
2. **Given** a coral with growth history entries, **When** the user downloads the export, **Then** the full growth history (all entries with date, type, value) is present in the coral's `growthHistory` array.
3. **Given** a coral with polyp condition history entries, **When** the user downloads the export, **Then** the full polyp condition history (date, condition) is present in the coral's `polypConditionHistory` array.
4. **Given** a coral that has departed, **When** the user downloads the export, **Then** the coral is still included in the export with its `departedOn` and `departureReason` set — historical records are always exported.
5. **Given** a user with no corals, **When** they download the export, **Then** each aquarium has an empty `corals: []` array — no error is returned.

---

### Edge Cases

- What happens when a user uploads a coral photo larger than the accepted size limit? → Same as fish: the system rejects the upload immediately with a clear error stating the maximum allowed size; no partial upload is stored.
- What happens when a user deletes their aquarium that has coral stock entries? → All coral entries (active and departed), their growth history, and polyp condition history are soft-deleted along with the aquarium; catalogue links remain intact.
- What if two users simultaneously propose catalogue entries with the same scientific name? → Both are accepted as Pending; the admin sees both in the proposals queue and resolves by approving one and rejecting the other as a duplicate.
- What if a user sets the coral entry date after the departure date? → The system shows an inline validation error and prevents saving; enforced both client-side and server-side.
- What if a growth measurement's date is after the departure date of the parent coral? → The system shows an inline validation error and prevents saving the growth record.
- What if a polyp condition observation's date is after the departure date of the parent coral? → The same validation error applies.
- What if the user tries to physically delete a coral entry that already has a departure record? → The system rejects the delete request with an informative error message (e.g., "This coral entry has a departure record and cannot be deleted."); the entry remains unchanged.
- What if the catalogue search returns no results? → The user sees a "No coral catalogue entries found" message and a prominent "Propose new entry" link.
- What if a user clears the catalogue link from an existing coral entry? → The coral entry is saved with the free-text name preserved; the scientific name field becomes user-editable again; the previous catalogue link is removed; the snapshots (classification, reference URL) are retained as user-editable free text.
- What if the coral opt-in flag is toggled off after coral data is already in a public report link? → Next report fetch returns no coral data; the data is not deleted, only excluded from the report response until the opt-in is re-enabled.
- What if a user adds growth measurements of the same type on the same date? → Allowed; both records are stored and displayed (the user may have remeasured or corrected a value; deduplication is deliberately not enforced).

---

## Requirements

### Functional Requirements

**Tank Coral Stock**

- **FR-001**: The system MUST provide a "Coral Stock" tab (or equivalent section) on every aquarium detail page, visible to the aquarium's owner.
- **FR-002**: The system MUST allow an authenticated user to create a coral entry for any of their own aquariums containing: a species name (mandatory, free text), an entry date (mandatory), an LPS/SPS classification (optional free-text at entry if not catalogue-linked), an optional personal photo, optional free-text notes, and an optional external reference URL.
- **FR-003**: The system MUST validate that the entry date is not in the future and that mandatory fields (species name, entry date) are not empty; invalid submissions MUST be rejected with inline error messages.
- **FR-004**: A user MUST be able to edit all fields of a coral entry they own.
- **FR-005**: A user MUST be able to record a departure for an active coral entry by providing a departure date and selecting a departure reason; the departure reason MUST be stored as an enum with exactly the following values: `DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`; additionally, the user MAY provide an optional free-text departure note (max 500 characters) alongside the departure reason (see also FR-042).
- **FR-006**: The system MUST validate that the departure date is not earlier than the entry date; invalid departure submissions MUST be rejected with an inline error message.
- **FR-007**: The aquarium Coral Stock tab MUST display currently-present corals (no departure record) and departed corals in separate, clearly labelled sections; the departed section MUST be collapsed by default.
- **FR-008**: A user MUST be able to upload a single photo for a coral entry; the system MUST reject files that exceed 5 MB or are not in a standard image format (JPEG, PNG, WebP, GIF); on rejection, a clear error message stating the constraint MUST be displayed.
- **FR-009**: The system MUST allow a user to optionally link a coral entry to a coral catalogue entry; linking MUST auto-fill the scientific name, LPS/SPS classification, care level, and the localised reference URL (if available), but MUST allow the user to override these values.
- **FR-010**: The system MUST allow a user to remove the catalogue link from an existing coral entry without deleting the coral entry; upon removal, the free-text species name is retained and the scientific name field becomes user-editable.
- **FR-011**: A user MUST NOT be able to view, edit, or delete coral entries belonging to another user's aquarium.
- **FR-012**: A user MUST be able to physically delete a coral entry they own **if and only if** no departure record exists for that entry; a delete request against an entry that already has a departure record MUST be rejected by the system with an informative error message; entries with a departure record MUST be retained indefinitely as historical records.

**Growth History**

- **FR-013**: The system MUST allow a user to add growth measurement records to any of their coral entries; each record MUST capture: date (mandatory, not in the future), measurement type (mandatory, one of: `SURFACE_AREA_CM2`, `SIZE_CM`, `VOLUME_CM3`, `BRANCH_COUNT`), and a numeric value (mandatory, positive).
- **FR-014**: For measurement type `BRANCH_COUNT`, the value MUST be a whole positive integer; for `SURFACE_AREA_CM2`, `SIZE_CM`, and `VOLUME_CM3`, the value MUST be a positive decimal with up to one decimal place.
- **FR-015**: The system MUST validate that a growth measurement date is not later than the coral entry's departure date (if set); submissions violating this constraint MUST be rejected with an inline error message.
- **FR-016**: The growth history for a coral MUST be displayed in date-descending order; a user MUST be able to delete individual growth measurement records without affecting the parent coral entry or other records; editing of existing growth records is governed by FR-039.
- **FR-017**: Growth history MUST be included in the AI-JSON export for each coral entry (spec `001` extension).

**Polyp Condition History**

- **FR-018**: The system MUST allow a user to add polyp condition observation records to any of their coral entries; each record MUST capture: date (mandatory, not in the future) and condition state (mandatory, one of: `VITAL`, `TISSUE_LOSS`, `PALE`, `LIMP`, `SIGNIFICANT_GROWTH`).
- **FR-019**: The system MUST validate that a polyp condition observation date is not later than the coral entry's departure date (if set); invalid submissions MUST be rejected with an inline error message.
- **FR-020**: The polyp condition history for a coral MUST be displayed in date-descending order; a user MUST be able to delete individual condition records without affecting the parent coral entry or other records; editing of existing condition records is governed by FR-040.
- **FR-021**: Polyp condition history MUST be included in the AI-JSON export for each coral entry (spec `001` extension).

**Coral Catalogue**

- **FR-022**: The coral catalogue MUST store each species as a unique entry identified by its scientific (Latin) name; the scientific name MUST be unique among all catalogue entries with status **Pending** or **Public**; entries with status **Rejected** do NOT participate in the uniqueness constraint.
- **FR-023**: Each catalogue entry MUST carry: a scientific name (mandatory, unique), LPS/SPS classification (mandatory: `LPS` or `SPS`), care level (mandatory: `EASY`, `MODERATE`, or `DEMANDING`), a localised common name for at least one language (mandatory at creation), an optional localised description per language (max 2000 characters), and an optional localised reference URL per language — for each of the five supported languages (DE, EN, ES, FR, IT).
- **FR-024**: Any authenticated user MUST be able to propose a new coral catalogue entry; upon submission, the entry is assigned status **Pending** and is immediately searchable and selectable **only** by its creator.
- **FR-025**: The system MUST warn the user when they attempt to propose a catalogue entry with a scientific name that already exists with status **Pending** or **Public**; this non-blocking warning MUST also appear when the creator of a Pending entry edits and changes the scientific name to one that already exists as Pending or Public; the warning MUST NOT block submission.
- **FR-026**: An authenticated user with the **Admin** role MUST be able to approve a pending coral catalogue entry; approved entries receive status **Public** and MUST immediately become searchable by all authenticated users.
- **FR-027**: An Admin MUST be able to reject a pending coral catalogue entry, optionally providing a rejection reason; rejected entries MUST become invisible to all users including the creator.
- **FR-028**: An Admin MUST be able to edit any field of a catalogue entry (any status) before or after approval.
- **FR-029**: The creator of a Pending catalogue entry MUST be able to edit all fields of that entry, including the scientific name; when the scientific name is changed, the system MUST re-evaluate the uniqueness constraint and show the same non-blocking duplicate warning (FR-025) if the new name conflicts. Public entries MUST be editable by their creator and by Admins; Rejected entries MUST be read-only for non-Admin users (creators and regular users); an Admin CAN edit any field of a Rejected entry (consistent with FR-028).
- **FR-030**: The catalogue search (used from the coral stock entry form) MUST search by partial match on both scientific name and all available localised common names; results MUST return within 1 second for up to 500 catalogue entries.
- **FR-031**: The system MUST provide an admin view listing all Pending coral catalogue proposals, sortable by submission date; each list item MUST show: scientific name, classification, proposer reference (anonymised), submission date, and a link to the full proposal details.

**House Reef Report Integration**

- **FR-032**: The public House Reef Report response MUST support an optional `corals` array containing all currently-present corals for the aquarium, gated by a per-report-link coral opt-in flag.
- **FR-033**: Each coral entry in the House Reef Report MUST include: species name, LPS/SPS classification, the most recent growth measurement value per tracked type (or null if none), and the most recent polyp condition state (or null if none).
- **FR-034**: Departed corals (departure date set) MUST NOT appear in the House Reef Report regardless of the opt-in state.

**AI-JSON Export Integration**

- **FR-035**: The AI export endpoint MUST include a `corals` array in each aquarium's data block; the array MUST be empty (`[]`) if no coral entries exist for that aquarium.
- **FR-036**: Each coral entry in the AI export MUST include: catalogue reference ID (if linked), scientific name snapshot, common name, LPS/SPS classification snapshot, entry date, departure date, departure reason, and departure note (all three if applicable), free-text notes, and the complete growth history (all records with date, type, value) and polyp condition history (all records with date, condition).

**General**

- **FR-037**: All new UI labels, button texts, status labels, condition-state labels, growth-type labels, classification labels, care-level labels, and user-facing messages MUST be present in all six message bundle files (DE, EN, ES, FR, IT + fallback `messages.properties`); missing keys are a release blocker.
- **FR-038**: All new backend operations that modify coral stock or catalogue data MUST be protected by the existing authentication mechanism; unauthenticated requests MUST be rejected with HTTP 401.

**Growth & Polyp Condition — Editing**

- **FR-039**: An authenticated user MUST be able to edit an existing growth measurement record that they own; the editable fields are the measurement date and the measured value; the measurement type MUST NOT be changeable after creation (it is immutable). All existing validations (date not in future, value positive, date not after departure date) apply on update as well.
- **FR-040**: An authenticated user MUST be able to edit an existing polyp condition observation record that they own; the editable fields are the observation date and the condition state. All existing validations (date not in future, date not after departure date) apply on update as well.

**Growth History — Display**

- **FR-041**: The growth history view for a coral entry MUST display records in a data table by default; a visible toggle button MUST allow the user to switch to a line-chart view that renders one line per measurement type (e.g., a separate line for SIZE_CM and for BRANCH_COUNT); clicking the toggle again MUST restore the table view. Both views MUST respect the date-descending order defined in FR-016.

**Departure Note**

- **FR-042**: When recording or editing a departure, the system MUST enforce that any provided departure note does not exceed 500 characters; submissions that violate this limit MUST be rejected with an inline validation error. An empty or absent departure note is valid.

---

### Non-Functional Requirements (ISO 25010)

| ISO 25010 Quality             | Requirement / Constraint for this Feature                                                                                                                                                                                                  |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Functional Suitability        | All 42 FRs must be satisfied. No cross-user data leaks (FR-011). Completeness of coral entry fields (FR-002, FR-023) verifiable via integration tests. Physical deletion only without departure record (FR-012).                             |
| Performance Efficiency        | Coral catalogue search: results within 1 second for up to 500 entries (FR-030). Growth history and polyp condition history rendered without perceptible delay for up to 200 records each. Photo upload: max 5 MB enforced (FR-008).           |
| Compatibility                 | New API endpoints introduce no breaking changes to existing endpoints. Multi-platform: ARM64 and AMD64 Docker images. Existing aquarium detail page extended, not replaced. AI export JSON schema version incremented, not broken.            |
| Usability                     | i18n in all 5 languages + fallback (FR-037). WCAG 2.1 AA contrast for all new elements. Coral catalogue linking reachable within 3 clicks from the Coral Stock tab. Growth and polyp condition sections accessible inline on the coral detail view. Growth history supports table and line-chart views with a toggle (FR-041). |
| Reliability                   | Photo-upload errors caught with clear user messages (FR-008). Departure date and growth measurement date validations enforced client-side and server-side (FR-006, FR-015, FR-019). Transactional saves for coral entry + linked histories. |
| Security                      | Authentication required for all write operations (FR-038). Strict user isolation: no access to another user's coral entries (FR-011). Admin-only access for catalogue approval (FR-026, FR-027). No dedicated rate limiting is applied to coral catalogue proposal submissions; mandatory JWT authentication (FR-038) provides sufficient protection, consistent with the fish catalogue approach (spec `002`). |
| Maintainability               | Flyway migrations required for new tables: `coral_stock`, `coral_catalogue_entry`, `coral_catalogue_i18n`, `coral_growth_history`, `coral_polyp_condition`. At least one integration test per P1–P7 user story.                              |
| Portability                   | Coral photo storage on the same configurable filesystem volume as fish photos. No new external service dependencies. ARM/AMD64 Docker images remain compatible.                                                                              |

---

### Key Entities

- **TankCoralStock**: Represents a single coral living (or having lived) in a specific aquarium. Key attributes: aquarium reference, species name (free text), scientific name (free text or resolved from catalogue), LPS/SPS classification snapshot, entry date, departure date (optional), departure reason (optional, enum: `DIED` | `SOLD` | `GIVEN_AWAY` | `MOVED_TO_OTHER_TANK` | `OTHER`), departure note (optional, free text, max 500 characters), free-text notes (optional), external reference URL (optional), photo reference (optional), coral-catalogue-entry reference (optional, nullable), deleted_at (soft-delete). A record without a departure date is considered currently active.

- **CoralGrowthHistory**: Records a single growth measurement for a `TankCoralStock` entry. Key attributes: coral-stock reference, measurement date, measurement type (enum: `SURFACE_AREA_CM2` / `SIZE_CM` / `VOLUME_CM3` / `BRANCH_COUNT`), numeric value (`BRANCH_COUNT` integer; others decimal). Multiple records per coral per date are permitted.

- **CoralPolypCondition**: Records a single polyp health observation for a `TankCoralStock` entry. Key attributes: coral-stock reference, observation date, condition state (enum: `VITAL` / `TISSUE_LOSS` / `PALE` / `LIMP` / `SIGNIFICANT_GROWTH`). Multiple observations per coral per date are permitted.

- **CoralCatalogueEntry**: A community-maintained reference record for a coral species. Key attributes: scientific name (unique, Latin), LPS/SPS classification (enum: `LPS` / `SPS`), care level (enum: `EASY` / `MODERATE` / `DEMANDING`), approval status (Pending / Public / Rejected), proposer reference, proposal date. One-to-many relationship with `CoralCatalogueI18n`.

- **CoralCatalogueI18n**: The language-specific portion of a coral catalogue entry. Attributes: language code (de / en / es / fr / it), common name (optional), description (optional, max 2000 characters), reference URL (optional). At most one record per language code per catalogue entry.

- **CoralPhoto**: A stored image file associated with a `TankCoralStock` record. Key attributes: file reference/path, content type, upload date. Stored on the server filesystem in the same configurable volume as fish photos; not stored as a database BLOB.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: A user can add a complete coral entry (all optional fields filled) to an aquarium in under 3 minutes from opening the Coral Stock tab.
- **SC-002**: A user can record a coral departure in under 1 minute from opening the Coral Stock tab.
- **SC-003**: A user can log a growth measurement or polyp condition observation for an existing coral in under 30 seconds from opening the coral detail view.
- **SC-004**: Coral catalogue search returns matching entries within 1 second for a catalogue of up to 500 public entries — verified by a performance test scenario.
- **SC-005**: 100% of submitted coral entries and catalogue proposals are accessible only to their owner (and admins for proposals) — verified by cross-user access tests.
- **SC-006**: An admin can process (approve or reject) a pending coral catalogue proposal in under 2 minutes from opening the admin view.
- **SC-007**: All new user-facing text is correctly displayed in all 5 supported languages — verified by a checklist covering all new i18n keys across all 6 bundle files, including: coral stock labels, catalogue labels, growth type labels (SURFACE_AREA_CM2, SIZE_CM, VOLUME_CM3, BRANCH_COUNT), polyp condition labels (VITAL, TISSUE_LOSS, PALE, LIMP, SIGNIFICANT_GROWTH), care level labels (EASY, MODERATE, DEMANDING), classification labels (LPS, SPS), and all validation/error messages.
- **SC-008**: Photo uploads of valid files (≤ 5 MB, supported formats) succeed 100% of the time; uploads exceeding the limit or with unsupported formats are rejected with a clear error message 100% of the time — verified by integration tests.
- **SC-009**: A newly proposed catalogue entry is searchable by its creator immediately after submission (< 2 seconds) but NOT visible in any other non-admin user's search results — verified by two-user acceptance test.
- **SC-010**: The AI export for a user with corals includes a correct, complete `corals` array (all entries with full growth and polyp condition histories) — verified by JSON schema validation and a data-completeness integration test.
- **SC-011**: The public House Reef Report for a tank with the coral opt-in enabled shows all currently-present corals with their latest growth snapshot — verified by a report-fetch integration test comparing JSON output to the stored coral data.

---

## Assumptions

- The existing Sabi authentication and authorisation mechanism (JWT bearer token + role-based access) is sufficient to enforce user isolation (FR-011) and admin-only access (FR-026/027); no new auth infrastructure is required.
- Coral photos are stored on the same server filesystem volume as fish photos (configurable directory, not database BLOBs); the existing deployment setup requires only a unified volume mount, not a separate coral-specific mount.
- A single photo per coral entry is sufficient for v1; multi-photo support is deferred.
- The departure-reason list is fixed for v1 as the following enum: `DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`; stored as a database enum column; additional values can be added in a later iteration.
- Multiple growth measurements of the same type on the same date are permitted (no deduplication); the user may have remeasured or is correcting a value.
- Multiple polyp condition observations on the same date are permitted; no deduplication.
- The `BRANCH_COUNT` measurement type stores an integer value; the remaining three types store decimal values with up to one decimal place — this distinction is enforced at the application layer before persistence.
- The LPS/SPS classification in a coral catalogue entry is an exhaustive two-value enum for v1; if a "Soft Coral" category is required in the future, the enum is extended.
- The care level field (EASY / MODERATE / DEMANDING) in the coral catalogue is an application-managed enum with localised display labels; it is not user-definable in v1.
- The scientific name uniqueness constraint covers only entries with status **Pending** or **Public**, enforced at both application layer (non-blocking duplicate warning) and database level (partial unique index); **Rejected** entries are excluded.
- Rejected catalogue entries are retained in the database for audit purposes but are never shown in the UI; a future purge policy can be added later.
- Catalogue changes (scientific name, classification, localised names) do NOT propagate automatically to coral stock entries already linked; all values are copied into the `TankCoralStock` record at link time and may be overridden independently by the user.
- No email notification is sent to catalogue-entry creators when their proposal is approved or rejected in v1.
- The House Reef Report coral opt-in per report link is a single boolean flag added to the existing `PublicReportLink` configuration; implementing a per-field granularity (e.g., only show classifications but not growth data) is out of scope for v1.
- The AI export JSON schema version is incremented to reflect the addition of the `corals` array; backward compatibility with existing exported files is documented in the schema version metadata but not technically enforced.
- Image format validation for coral photos is performed server-side by inspecting file magic bytes, not solely the filename extension — consistent with fish photo handling (spec `002` assumption).
- Coral stock pages are restricted to saltwater (marine) aquariums only; the freshwater aquarium detail page does not display the Coral Stock tab. The aquarium's water-type flag (already in the data model) governs this restriction.

---

## Constraints

| ID  | Constraint                                                                                                                                                                      |
|-----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C-1 | No import of coral data from external wikis (CoralDB, Wikipedia, Reeflex, etc.) is permitted due to copyright concerns — consistent with the fish catalogue decision (spec `002` C-1). |
| C-2 | Sabi is NOT intended to become a full coral encyclopaedia; catalogue entries are lightweight reference pointers. Descriptions are capped at 2000 characters per language.         |
| C-3 | Scientific names must be stored as-is (case-sensitive Latin binomial nomenclature); no automatic normalisation or spell-check against external taxonomic databases.               |
| C-4 | Coral photos must NOT be stored as database BLOBs; they must be stored on the same configurable filesystem volume as fish photos.                                                 |
| C-5 | All new API endpoints must be consistent with the existing Sabi REST API conventions and placed under the `/api/` path.                                                           |
| C-6 | The feature must not introduce any new external runtime service dependencies (no external storage, no CDN, no third-party image processing).                                       |
| C-7 | No standalone photo direct-link endpoint. Coral photo bytes are served exclusively as part of the coral-entry API endpoint, subject to the same authentication and ownership checks as the coral entry itself. |
| C-8 | The Coral Stock tab MUST only be visible for saltwater (marine) aquariums; freshwater aquariums do not display coral stock capabilities. This is controlled by the aquarium's existing water-type attribute. |
| C-9 | The `BRANCH_COUNT` measurement type stores a non-negative integer; fractional branch counts MUST be rejected at the application boundary with a validation error.                  |

---

*This specification was created for branch `005-coral-stock`. Next step: `/speckit.plan` to break this into implementation tasks.*

