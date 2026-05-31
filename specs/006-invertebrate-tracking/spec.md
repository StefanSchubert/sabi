# Feature Specification: Invertebrate Stock Management & Invertebrate Catalogue

**Feature Branch**: `006-invertebrate-tracking`  
**Created**: 2026-05-30  
**Status**: Draft  
**Input**: User description: "Als nächstes möchte ich in Sabi die Erfassung von Wirbellosen (Invertebraten) ermöglichen. Wichtig: UX/UI müssen so umgesetzt werden, wie wir das bei den Fischen und bei den Corallen umgesetzt haben. Der Benutzer soll die gewohnte Navigation haben das gleiche Pattern bzgl. der Umsetzung der CRUD Funktionalität haben. Analog zu Fischen und Korallen, benötige ich auch wieder die Möglichkeit Katalogeinträge zu referenzieren und vorzuschlagen, inkl. der Admin-Funktionalität. Unter Wirbellose verstehen wir bei Sabi 'alles ohne Rückgrat' und der Benutzer soll bei der Anlage die spezifische Kategorie angeben. Unterteilung: Krebstiere, Weichtiere, Stachelhäuter, Würmer. Zusätzlich soll der User die Möglichkeit haben eine weitere funktionale Klassifikation anzugeben (Mehrfachnennung ist möglich): Mobil vs. sessil, Reinigend (Cleanup Crew) vs. neutral vs. belastend, Tagaktiv / nachtaktiv, Sensitivität gegenüber Wasserwerten (mit Mehrfachauswahl aus unit/localized_unit Tabelle). Zusätzlich sollen die Wirbellosen auch im bestehenden JSON AI Export aus dem Benutzerprofil aufgeführt werden sowie im bestehenden ReefReport."

---

## Overview

### Problem Statement

Sabi users who keep saltwater reef aquariums typically maintain not only fish and corals but also a rich population of invertebrates — shrimp, snails, sea urchins, starfish, tube worms, and more. Today, Sabi provides no way to document which invertebrates live in a tank, categorise them by type, describe their ecological role, or factor their water-sensitivity requirements into tank management. This gap means users cannot correlate invertebrate well-being with water chemistry events, build a community invertebrate reference, or include invertebrate data in the AI-chatbot export and House Reef Report.

### Proposed Solution

Introduce two tightly coupled sub-features, fully symmetric with the existing Fish Stock & Catalogue pattern (spec `002`) and Coral Stock & Catalogue pattern (spec `005`):

1. **Tank Invertebrate Stock** — a per-aquarium invertebrate roster where each entry captures: the date the invertebrate was added, a mandatory taxonomic category, an optional personal photo, optional free-text notes, an optional external reference URL, an optional link to a shared invertebrate catalogue entry, an optional departure record, and a set of optional functional classification flags that describe the animal's ecological role and behaviour.

2. **Invertebrate Catalogue** — a community-maintained, i18n-aware reference catalogue of invertebrate species. Each species carries its scientific name, taxonomic category, care level, water-parameter sensitivity hints, and localised common names, descriptions, and reference URLs for all five supported languages. The identical UGC proposal-and-approval workflow as the fish and coral catalogues governs quality and prevents duplicates.

### Business Value

- **Complete reef documentation**: Users can track the full occupancy of their reef — chemistry, fish, corals, and invertebrates — in one place.
- **Cleanup-Crew awareness**: Functional classification (cleanup crew role, mobility, activity pattern) enables users and the AI export to reason about biological maintenance capacity.
- **Water-sensitivity reference**: Linking invertebrate entries to specific measurement units (nitrate, salinity, etc.) makes sensitivity constraints searchable and AI-exportable.
- **Community invertebrate reference**: A curated, community-built catalogue supplements the fish and coral catalogues.
- **AI export enrichment**: Invertebrate data extends the existing AI-JSON export (spec `001`), giving AI chatbots a more complete reef picture for diagnostics and advice.
- **House Reef Report visibility**: Sharing a reef report now conveys the invertebrate population alongside fish and corals.

---

## Scope

### In Scope

**Tank Invertebrate Stock**

- A new "Invertebrate Stock" tab (or section) on the existing aquarium detail page, alongside the existing "Fish Stock" and "Coral Stock" tabs.
- Add / edit / remove invertebrate entries per aquarium; **"remove" has two distinct semantics** identical to fish and coral stock: (a) recording a **Departure** (end date + reason) for a genuine departure, retaining the entry in the historical view; (b) **physical deletion** of an entry created in error — only permitted when NO departure record exists.
- Per-invertebrate mandatory fields: species name (free text or catalogue reference), entry date, taxonomic category (one of: `CRUSTACEAN`, `MOLLUSC`, `ECHINODERM`, `WORM`).
- Per-invertebrate optional fields: personal photo, free-text notes (unlimited), external reference URL, catalogue reference.
- Departure record: end date (mandatory), departure reason (mandatory, one of: `DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`), optional free-text departure note (max 500 characters).
- Display of currently-present invertebrates separately from departed invertebrates (historical view); departed section collapsed by default.
- **Functional classification** (multi-select, all optional per entry):
  - **Mobility**: `MOBILE` or `SESSILE` (single choice within this dimension, optional)
  - **Ecological Role**: `CLEANUP_CREW`, `NEUTRAL`, or `DETRIMENTAL` (single choice within this dimension, optional)
  - **Activity Pattern**: `DIURNAL`, `NOCTURNAL`, or `BOTH` (single choice within this dimension, optional)
  - **Water Sensitivity**: zero or more references to measurement units from the existing `unit` / `localized_unit` tables (multi-select); each selected unit indicates that this invertebrate is sensitive to that water parameter.

**Invertebrate Catalogue**

- An invertebrate catalogue accessible from the invertebrate stock entry form (search & select).
- Catalogue entries contain: scientific name (Latin, unique, mandatory), taxonomic category (mandatory: `CRUSTACEAN`, `MOLLUSC`, `ECHINODERM`, or `WORM`), care level (`EASY`, `MODERATE`, or `DEMANDING`), localised common name per language (DE, EN, ES, FR, IT), localised description per language (optional), localised reference URL per language (optional).
- Identical UGC workflow as fish and coral catalogues:
  - Any authenticated user may propose a new catalogue entry.
  - A proposed entry is immediately visible and selectable only by its creator (status: **Pending**).
  - Admin can approve a proposal → status changes to **Public** (visible to all users).
  - Admin can reject a proposal → status changes to **Rejected** (invisible to all); duplicate prevention warns on scientific name collision.
- Creator and subsequent editors can add/update localised fields for any supported language.
- A dedicated admin view listing all pending invertebrate catalogue proposals for review.

**Integration: House Reef Report**

- The public House Reef Report for an aquarium extends to include the currently-present invertebrate stock (species name, category, functional classification flags, sensitivity units), gated by a per-report-link invertebrate opt-in flag (same mechanism as coral opt-in in spec `005`).

**Integration: AI-JSON Export**

- The AI data export (spec `001`) is extended: each aquarium's JSON object gains an `invertebrates` array alongside the existing `fish` and `corals` arrays.
- Each invertebrate entry in the export contains: catalogue reference ID (if linked), scientific name snapshot, common name, taxonomic category, entry date, departure date and reason (if applicable), departure note (if applicable), free-text notes, mobility classification, ecological role classification, activity pattern classification, and the list of sensitive water-parameter unit IDs with their localised names.

**UI / i18n**

- All new UI labels, buttons, messages, category labels, care-level labels, functional-classification labels, and status texts in all 6 message bundle files (DE, EN, ES, FR, IT + fallback).
- WCAG 2.1 AA colour-contrast compliance for all new UI elements.

### Out of Scope (this iteration)

- Import of invertebrate data from external sources (copyright risk, consistent with fish and coral catalogue decisions).
- Bulk import of invertebrate catalogue entries.
- Automated deduplication of catalogue entries.
- Community commenting or rating of catalogue entries.
- Push notifications to the creator when a proposal is approved or rejected.
- Public invertebrate catalogue browsing without login.
- Multiple photos per invertebrate entry (single photo upload in v1).
- Versioning or history of catalogue entry edits.
- Chronological growth or health history for invertebrates (in scope for corals via spec `005`; not requested for invertebrates in v1).
- Freshwater invertebrate tracking.
- Automatic water-parameter recommendations derived from sensitivity flags.
- Export of invertebrate photos in the AI-JSON (file size concerns; text data only in v1).

---

## User Scenarios & Testing

### User Story 1 — Add an Invertebrate to My Aquarium (Priority: P1)

A user opens one of their saltwater aquariums and navigates to the new "Invertebrate Stock" tab. They click "Add invertebrate", type the name "Scarlet Skunk Cleaner Shrimp", set the entry date to today, select category "Crustacean", optionally add a note ("Two specimens, placed near the rock arch"), and save. The entry now appears in the "Currently in tank" list.

**Why this priority**: Core value of the feature; nothing else depends on anything until an invertebrate entry exists. Independently deployable as MVP.

**Independent Test**: Log in as a user with at least one aquarium, open the aquarium detail page, navigate to the "Invertebrate Stock" tab, add an invertebrate entry with all mandatory fields plus at least one optional field, save, and verify the entry appears in the "Currently in tank" list with all saved data.

**Acceptance Scenarios**:

1. **Given** a logged-in user with at least one aquarium, **When** they open the aquarium detail page, **Then** an "Invertebrate Stock" tab is visible alongside the existing "Fish Stock" and "Coral Stock" tabs.
2. **Given** the Invertebrate Stock tab is open, **When** the user clicks "Add invertebrate", fills in the mandatory fields (species name, entry date, category) and saves, **Then** the invertebrate appears in the "Currently in tank" section.
3. **Given** a new invertebrate entry form, **When** the user fills in all optional fields (photo, notes, reference URL) and saves, **Then** all fields are persisted and displayed correctly on the invertebrate detail view.
4. **Given** an invertebrate entry form, **When** the user submits without a mandatory field (species name, entry date, or category), **Then** the form shows an inline validation error and does not save.
5. **Given** an invertebrate entry with a reference URL, **When** the user clicks the link, **Then** it opens in a new browser tab.

---

### User Story 2 — Set Functional Classifications for an Invertebrate (Priority: P2)

A user adds a Turbo snail and wants to document its role in the tank. During the add (or edit) flow, they select: Mobility = "Sessile is not applicable — Mobil", Ecological Role = "Cleanup Crew", Activity Pattern = "Nocturnal", and Water Sensitivity = [Nitrate, Salinity] (selected from the existing unit list). They save, and the detail view shows all four classification flags clearly.

**Why this priority**: Functional classification is the primary differentiator that sets invertebrate tracking apart from fish and coral. It can be tested immediately after P1 without any further dependencies.

**Independent Test**: Create an invertebrate entry; set all four functional classification dimensions; save; verify all selections are persisted and displayed on the detail view.

**Acceptance Scenarios**:

1. **Given** the add/edit form for an invertebrate, **When** the user sets Mobility, Ecological Role, and Activity Pattern, **Then** each choice is stored individually and displayed on the detail page with its localised label.
2. **Given** the Water Sensitivity selector, **When** the user opens it, **Then** a multi-select list of measurement units (from the existing `unit`/`localized_unit` tables) is displayed in the user's language.
3. **Given** a saved invertebrate with two water-sensitivity units selected, **When** the user views the detail page, **Then** both unit names are displayed in the user's language.
4. **Given** an invertebrate entry with no functional classifications set, **When** the user views the detail page, **Then** the classification section displays "Not specified" (or equivalent localised text) for each unset dimension — no error is shown.
5. **Given** an existing invertebrate entry, **When** the user edits and changes the functional classifications and saves, **Then** the updated classifications are reflected immediately.

---

### User Story 3 — Record an Invertebrate Departure (Priority: P3)

A user's hermit crab has died. They open the Invertebrate Stock tab, find the entry, click "Record departure", enter today's date, select reason "Died", optionally add a note ("Found empty shell — cause unknown"), and confirm. The entry moves from the "Currently in tank" list to a collapsible "Departed" section.

**Why this priority**: Without departure tracking, the active invertebrate list becomes inaccurate over time. No dependency on the catalogue sub-feature.

**Independent Test**: Add an invertebrate entry, record a departure for it, verify it disappears from the active list and appears in the historical section with the correct departure date and reason.

**Acceptance Scenarios**:

1. **Given** an active invertebrate entry (no departure date), **When** the user records a departure with a valid date and reason, **Then** the invertebrate moves to the "Departed" section immediately.
2. **Given** a departure form, **When** the user selects a departure date earlier than the entry date, **Then** an inline validation error is shown and the form cannot be saved.
3. **Given** an invertebrate in the "Departed" section, **When** the user views it, **Then** the departure date, reason, departure note, and all original fields are visible.
4. **Given** a departed invertebrate in the "Departed" section, **When** the user views the tab, **Then** the "Departed" section is collapsed by default but can be expanded.
5. **Given** a departure note longer than 500 characters, **When** the user submits, **Then** an inline validation error is shown and the form cannot be saved.

---

### User Story 4 — Link an Invertebrate Entry to the Catalogue (Priority: P4)

A user types "Lysmata" in the catalogue search on the add-invertebrate form, sees matching catalogue entries including "Lysmata amboinensis", selects it, and the form auto-fills the scientific name, category ("Crustacean"), care level ("Easy"), and a reference URL for their language. They save.

**Why this priority**: Catalogue linking improves data quality and speeds up entry. The feature works without it; free-text species name is always the fallback.

**Independent Test**: With at least one public catalogue entry, open the add-invertebrate form, search the catalogue, select an entry, verify auto-fill, save, and confirm the catalogue link is stored.

**Acceptance Scenarios**:

1. **Given** the add-invertebrate form, **When** the user types at least 2 characters in the catalogue search field, **Then** a dropdown of matching entries (by common name or scientific name) appears within 1 second.
2. **Given** a catalogue entry is selected from the dropdown, **When** the form updates, **Then** the scientific name, category, and care level are auto-filled; the reference URL is pre-filled with the localised URL for the user's language (if available).
3. **Given** a saved invertebrate linked to a catalogue entry, **When** the user views the detail, **Then** the catalogue link is preserved and the scientific name and category are shown.
4. **Given** a catalogue entry that auto-fills the taxonomic category, **When** the user attempts to change the category on the form, **Then** the change is permitted (override allowed, consistent with fish and coral patterns).
5. **Given** a coral entry form without catalogue search, **When** the user does not use the catalogue search, **Then** they can still enter a free-text species name and save without a catalogue link.

---

### User Story 5 — Propose a New Invertebrate Catalogue Entry (Priority: P5)

A user cannot find their Banded Coral Shrimp in the catalogue. They click "Propose new catalogue entry", enter scientific name "Stenopus hispidus", select category "Crustacean", care level "Moderate", provide German common name "Korallenwächter" and English common name "Banded Coral Shrimp", and submit. The new entry immediately appears in their catalogue search results (marked "Pending approval"). They link their invertebrate entry to it and save.

**Why this priority**: The catalogue only grows through user contributions. Depends on P1–P4.

**Independent Test**: Propose a new catalogue entry with a unique scientific name; verify it appears immediately in the proposer's search results but NOT in a different user's search results; verify it appears in the admin's pending-proposals queue.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they submit a new invertebrate catalogue proposal with at least scientific name, category, and one localised common name, **Then** the entry is saved with status "Pending" and is immediately searchable by the proposer only.
2. **Given** a catalogue proposal form, **When** the user enters a scientific name that already exists as Pending or Public, **Then** a non-blocking warning is shown but the user may proceed.
3. **Given** a pending catalogue entry, **When** a different non-admin user searches the catalogue, **Then** the pending entry is NOT visible in their results.
4. **Given** a pending catalogue entry, **When** the admin views the pending-proposals list, **Then** the entry appears with all submitted fields including category and care level.
5. **Given** an invertebrate stock entry linked to a pending catalogue entry, **When** the proposal is later approved, **Then** the stock entry remains linked and snapshot values do not change.

---

### User Story 6 — Admin Approves or Rejects an Invertebrate Catalogue Proposal (Priority: P6)

An admin navigates to "Invertebrate Catalogue Administration", finds a pending proposal for "Stenopus hispidus", reviews the fields, edits the English description for clarity, and clicks "Approve". The entry is now publicly visible. Later, the admin finds a duplicate proposal and clicks "Reject". The proposer's entry becomes invisible.

**Why this priority**: Quality governance layer. Depends on P5.

**Independent Test**: Log in as admin, find a pending invertebrate catalogue proposal, approve it, then log in as a regular user and verify the approved entry now appears in invertebrate catalogue search.

**Acceptance Scenarios**:

1. **Given** an admin user, **When** they navigate to the admin invertebrate catalogue view, **Then** they see all pending proposals with proposer reference, submission date, scientific name, and category.
2. **Given** an admin reviewing a proposal, **When** they click "Approve", **Then** the entry status changes to "Public" and immediately appears in all users' catalogue search results.
3. **Given** an admin reviewing a proposal, **When** they click "Reject", **Then** the entry status changes to "Rejected" and is no longer visible to any user (including the proposer).
4. **Given** a non-admin authenticated user, **When** they attempt to access the invertebrate catalogue admin view, **Then** they receive an access-denied response.
5. **Given** an admin approving a proposal, **When** they edit any localised field before approving, **Then** the updated fields are saved as part of the approval action.

---

### User Story 7 — Invertebrate Data in House Reef Report (Priority: P7)

A user has enabled the public House Reef Report share link. After enabling the invertebrate opt-in, visitors to the public report URL see an "Invertebrates" section listing all currently-present invertebrates: species name, taxonomic category, functional classification flags, and any water-sensitivity units. The section is clearly labelled and visually separated from the fish and coral sections.

**Why this priority**: Extends existing feature; depends on P1–P3 being in place and on the House Reef Report (pre-existing feature).

**Independent Test**: With invertebrates in a tank and the opt-in enabled for the report link, fetch the public report URL and verify the `invertebrates` array is present and contains the correct snapshot data.

**Acceptance Scenarios**:

1. **Given** a public reef report link with the invertebrate opt-in enabled and at least one currently-present invertebrate, **When** the report is fetched, **Then** the response includes an `invertebrates` array with the currently-present invertebrates.
2. **Given** a public reef report link with the invertebrate opt-in **disabled**, **When** the report is fetched, **Then** the `invertebrates` array is absent (or empty), regardless of how many invertebrates are in the tank.
3. **Given** an invertebrate in the report, **When** the user views the report, **Then** species name, category, functional classification flags, and water-sensitivity unit names are shown.
4. **Given** an invertebrate that has departed, **When** the report is fetched, **Then** that invertebrate does NOT appear in the report.

---

### User Story 8 — Invertebrate Data in AI-JSON Export (Priority: P8)

A user downloads their AI chatbot data export. They open the JSON file and see each aquarium now has an `invertebrates` array alongside `fish` and `corals`. Each entry shows scientific name, category, entry date, notes, all functional classification flags, and the list of water-sensitivity unit names. The user pastes the file into ChatGPT and asks: "My cleaner shrimp seem stressed — what could be the cause given my current water parameters?"

**Why this priority**: Extension of pre-existing spec `001`. Depends on P1–P2 being implemented.

**Independent Test**: Download the AI export for a user who has at least one invertebrate with at least one functional classification and at least one water-sensitivity unit set; verify the exported JSON contains `invertebrates` with all expected fields.

**Acceptance Scenarios**:

1. **Given** a user with at least one invertebrate in at least one aquarium, **When** they download the AI export, **Then** each aquarium in the JSON has an `invertebrates` array.
2. **Given** an invertebrate with functional classifications set, **When** the user downloads the export, **Then** the export contains the mobility, ecological role, activity pattern, and water-sensitivity fields with their correct values.
3. **Given** an invertebrate that has departed, **When** the user downloads the export, **Then** the invertebrate is still included with its `departedOn`, `departureReason`, and `departureNote` set — historical records are always exported.
4. **Given** a user with no invertebrates, **When** they download the export, **Then** each aquarium has an empty `invertebrates: []` array — no error is returned.

---

### Edge Cases

- What happens when a user uploads an invertebrate photo larger than the accepted size limit? → Same as fish and coral: the system rejects the upload immediately with a clear error stating the maximum allowed size; no partial upload is stored.
- What happens when a user deletes their aquarium that has invertebrate stock entries? → All invertebrate entries (active and departed) are soft-deleted along with the aquarium; catalogue links remain intact.
- What if two users simultaneously propose catalogue entries with the same scientific name? → Both are accepted as Pending; the admin sees both in the proposals queue and resolves by approving one and rejecting the other as a duplicate.
- What if a user sets the invertebrate entry date after the departure date? → The system shows an inline validation error and prevents saving; enforced both client-side and server-side.
- What if the user tries to physically delete an invertebrate entry that already has a departure record? → The system rejects the delete request with an informative error message; the entry remains unchanged.
- What if the catalogue search returns no results? → The user sees a "No invertebrate catalogue entries found" message and a prominent "Propose new entry" link.
- What if a user sets no functional classifications at all? → Valid; all classification dimensions are optional; the entry is saved without classifications and the detail view shows "Not specified" for each dimension.
- What if a user selects all available water-sensitivity units? → Allowed; there is no upper limit on the number of sensitive-unit references per invertebrate entry.
- What if the invertebrate opt-in flag is toggled off after invertebrate data is already in a public report link? → Next report fetch returns no invertebrate data; the data is not deleted, only excluded from the report response until the opt-in is re-enabled.
- What if a user clears the catalogue link from an existing invertebrate entry? → The entry is saved with the free-text name preserved; the species name field becomes user-editable again; the previous catalogue snapshot (category, reference URL) is retained as user-editable free text.

---

## Requirements

### Functional Requirements

**Tank Invertebrate Stock**

- **FR-001**: The system MUST provide an "Invertebrate Stock" tab (or equivalent section) on every aquarium detail page, visible to the aquarium's owner.
- **FR-002**: The system MUST allow an authenticated user to create an invertebrate entry for any of their own aquariums containing: a species name (mandatory, free text), an entry date (mandatory, not in the future), a taxonomic category (mandatory, one of: `CRUSTACEAN`, `MOLLUSC`, `ECHINODERM`, `WORM`), an optional personal photo, optional free-text notes (unlimited), and an optional external reference URL.
- **FR-003**: The system MUST validate that the entry date is not in the future and that all mandatory fields (species name, entry date, category) are not empty; invalid submissions MUST be rejected with inline error messages.
- **FR-004**: A user MUST be able to edit all fields of an invertebrate entry they own.
- **FR-005**: A user MUST be able to record a departure for an active invertebrate entry by providing a departure date and selecting a departure reason; the departure reason MUST be stored as an enum with exactly the following values: `DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`; the user MAY also provide an optional free-text departure note (max 500 characters).
- **FR-006**: The system MUST validate that the departure date is not earlier than the entry date; invalid departure submissions MUST be rejected with an inline error message.
- **FR-007**: The aquarium Invertebrate Stock tab MUST display currently-present invertebrates (no departure record) and departed invertebrates in separate, clearly labelled sections; the departed section MUST be collapsed by default.
- **FR-008**: A user MUST be able to upload a single photo for an invertebrate entry; the system MUST reject files that exceed 5 MB or are not in a standard image format (JPEG, PNG, WebP, GIF); on rejection, a clear error message stating the constraint MUST be displayed.
- **FR-009**: The system MUST allow a user to optionally link an invertebrate entry to an invertebrate catalogue entry; linking MUST auto-fill the scientific name, taxonomic category, care level, and the localised reference URL (if available), but MUST allow the user to override these values.
- **FR-010**: The system MUST allow a user to remove the catalogue link from an existing invertebrate entry without deleting the entry; upon removal, the free-text species name is retained and the species name field becomes user-editable.
- **FR-011**: A user MUST NOT be able to view, edit, or delete invertebrate entries belonging to another user's aquarium.
- **FR-012**: A user MUST be able to physically delete an invertebrate entry they own **if and only if** no departure record exists for that entry; a delete request against an entry that already has a departure record MUST be rejected with an informative error message; entries with a departure record MUST be retained indefinitely as historical records.
- **FR-013**: When recording or editing a departure, the system MUST enforce that any provided departure note does not exceed 500 characters; submissions that violate this limit MUST be rejected with an inline validation error.

**Functional Classification**

- **FR-014**: The system MUST allow a user to optionally assign a **Mobility** classification to an invertebrate entry; the value MUST be one of: `MOBILE`, `SESSILE`; the field is optional — an invertebrate entry MAY be saved without it.
- **FR-015**: The system MUST allow a user to optionally assign an **Ecological Role** classification; the value MUST be one of: `CLEANUP_CREW`, `NEUTRAL`, `DETRIMENTAL`; the field is optional.
- **FR-016**: The system MUST allow a user to optionally assign an **Activity Pattern** classification; the value MUST be one of: `DIURNAL`, `NOCTURNAL`, `BOTH`; the field is optional.
- **FR-017**: The system MUST allow a user to optionally select one or more **Water Sensitivity** references; each reference MUST correspond to an existing entry in the `unit` table (identified by its unit ID); there is no upper limit on the number of sensitive-unit references per invertebrate entry; the field is optional (zero references is valid).
- **FR-018**: The Water Sensitivity multi-select control MUST display unit names in the current user's language using the existing `localized_unit` table; units MUST be selectable by their localised name.
- **FR-019**: All functional classification fields (FR-014 to FR-017) MUST be persistable and editable independently at any time after initial creation of the invertebrate entry.

**Invertebrate Catalogue**

- **FR-020**: The invertebrate catalogue MUST store each species as a unique entry identified by its scientific (Latin) name; the scientific name MUST be unique among all catalogue entries with status **Pending** or **Public**; entries with status **Rejected** do NOT participate in the uniqueness constraint.
- **FR-021**: Each catalogue entry MUST carry: a scientific name (mandatory, unique), taxonomic category (mandatory: `CRUSTACEAN`, `MOLLUSC`, `ECHINODERM`, or `WORM`), care level (mandatory: `EASY`, `MODERATE`, or `DEMANDING`), a localised common name for at least one language (mandatory at creation), an optional localised description per language (max 2000 characters), and an optional localised reference URL per language — for each of the five supported languages (DE, EN, ES, FR, IT).
- **FR-022**: Any authenticated user MUST be able to propose a new invertebrate catalogue entry; upon submission, the entry is assigned status **Pending** and is immediately searchable and selectable **only** by its creator.
- **FR-023**: The system MUST warn the user when they attempt to propose a catalogue entry with a scientific name that already exists with status **Pending** or **Public**; this non-blocking warning MUST also appear when the creator of a Pending entry edits and changes the scientific name to a conflicting one; the warning MUST NOT block submission.
- **FR-024**: An authenticated user with the **Admin** role MUST be able to approve a pending invertebrate catalogue entry; approved entries receive status **Public** and MUST immediately become searchable by all authenticated users.
- **FR-025**: An Admin MUST be able to reject a pending invertebrate catalogue entry, optionally providing a rejection reason; rejected entries MUST become invisible to all users including the creator.
- **FR-026**: An Admin MUST be able to edit any field of a catalogue entry (any status) before or after approval.
- **FR-027**: The creator of a Pending catalogue entry MUST be able to edit all fields of that entry; Public entries MUST be editable by their creator and by Admins; Rejected entries MUST be read-only for non-Admin users; an Admin CAN edit any field of a Rejected entry.
- **FR-028**: The catalogue search (used from the invertebrate stock entry form) MUST search by partial match on both scientific name and all available localised common names; results MUST return within 1 second for up to 500 catalogue entries.
- **FR-029**: The system MUST provide an admin view listing all Pending invertebrate catalogue proposals, sortable by submission date; each list item MUST show: scientific name, category, proposer reference (anonymised), submission date, and a link to the full proposal details.

**House Reef Report Integration**

- **FR-030**: The public House Reef Report response MUST support an optional `invertebrates` array containing all currently-present invertebrates for the aquarium, gated by a per-report-link invertebrate opt-in flag (consistent with the coral opt-in pattern from spec `005`).
- **FR-031**: Each invertebrate entry in the House Reef Report MUST include: species name, taxonomic category, mobility classification (if set), ecological role classification (if set), activity pattern classification (if set), and the list of water-sensitivity unit names (localised, if set).
- **FR-032**: Departed invertebrates (departure date set) MUST NOT appear in the House Reef Report regardless of the opt-in state.

**AI-JSON Export Integration**

- **FR-033**: The AI export endpoint MUST include an `invertebrates` array in each aquarium's data block; the array MUST be empty (`[]`) if no invertebrate entries exist for that aquarium.
- **FR-034**: Each invertebrate entry in the AI export MUST include: catalogue reference ID (if linked), scientific name snapshot, common name, taxonomic category, entry date, departure date, departure reason, departure note (all three if applicable), free-text notes, mobility, ecological role, activity pattern, and the list of sensitive water-parameter unit IDs together with their English unit names.

**General**

- **FR-035**: All new UI labels, button texts, status labels, category labels, functional-classification labels, care-level labels, and user-facing messages MUST be present in all six message bundle files (DE, EN, ES, FR, IT + fallback `messages.properties`); missing keys are a release blocker.
- **FR-036**: All new backend operations that modify invertebrate stock or catalogue data MUST be protected by the existing authentication mechanism; unauthenticated requests MUST be rejected with HTTP 401.

---

### Non-Functional Requirements (ISO 25010)

| ISO 25010 Quality          | Requirement / Constraint for this Feature                                                                                                                                                                                                          |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Functional Suitability     | All 36 FRs must be satisfied. No cross-user data leaks (FR-011). Completeness of entry fields (FR-002, FR-021) verifiable via integration tests. Physical deletion only without departure record (FR-012). Water-sensitivity multi-select bounded by existing unit table.  |
| Performance Efficiency     | Invertebrate catalogue search: results within 1 second for up to 500 entries (FR-028). Unit multi-select populates within 1 second. Photo upload: max 5 MB enforced (FR-008).                                                                       |
| Compatibility              | New API endpoints introduce no breaking changes to existing endpoints. Multi-platform: ARM64 and AMD64 Docker images. Existing aquarium detail page extended, not replaced. AI export JSON schema version incremented, not broken.                   |
| Usability                  | i18n in all 5 languages + fallback (FR-035). WCAG 2.1 AA contrast for all new elements. Invertebrate catalogue linking reachable within 3 clicks from the Invertebrate Stock tab. Functional classification controls accessible inline on the entry form. Water-sensitivity unit names shown in user's language (FR-018). |
| Reliability                | Photo-upload errors caught with clear user messages (FR-008). Departure date validation enforced client-side and server-side (FR-006). Transactional saves for invertebrate entry + functional classifications + sensitivity unit references.         |
| Security                   | Authentication required for all write operations (FR-036). Strict user isolation: no access to another user's invertebrate entries (FR-011). Admin-only access for catalogue approval (FR-024, FR-025). No dedicated rate limiting beyond JWT authentication. |
| Maintainability            | Flyway migrations required for new tables: `invertebrate_stock`, `invertebrate_catalogue_entry`, `invertebrate_catalogue_i18n`, `invertebrate_water_sensitivity`. At least one integration test per P1–P6 user story.                               |
| Portability                | Invertebrate photo storage on the same configurable filesystem volume as fish and coral photos. No new external service dependencies. ARM/AMD64 Docker images remain compatible.                                                                     |

---

### Key Entities

- **TankInvertebrateStock**: Represents a single invertebrate living (or having lived) in a specific aquarium. Key attributes: aquarium reference, species name (free text), scientific name (free text or resolved from catalogue), taxonomic category (enum: `CRUSTACEAN` | `MOLLUSC` | `ECHINODERM` | `WORM`), entry date, departure date (optional), departure reason (optional, enum: `DIED` | `SOLD` | `GIVEN_AWAY` | `MOVED_TO_OTHER_TANK` | `OTHER`), departure note (optional, free text, max 500 characters), free-text notes (optional), external reference URL (optional), photo reference (optional), catalogue-entry reference (optional, nullable), mobility (optional, enum: `MOBILE` | `SESSILE`), ecological role (optional, enum: `CLEANUP_CREW` | `NEUTRAL` | `DETRIMENTAL`), activity pattern (optional, enum: `DIURNAL` | `NOCTURNAL` | `BOTH`), deleted_at (soft-delete).

- **InvertebrateWaterSensitivity**: A join record linking a `TankInvertebrateStock` entry to a `unit` table entry. Key attributes: invertebrate-stock reference, unit ID (foreign key to existing `unit` table). Zero or more records may exist per invertebrate entry.

- **InvertebrateCatalogueEntry**: A community reference record for a single invertebrate species. Key attributes: scientific name (unique among Pending/Public), taxonomic category (enum), care level (enum: `EASY` | `MODERATE` | `DEMANDING`), status (enum: `PENDING` | `PUBLIC` | `REJECTED`), creator reference, creation date.

- **InvertebrateCatalogueI18n**: Localised content for a single language variant of a catalogue entry. Key attributes: catalogue-entry reference, language code (DE/EN/ES/FR/IT), localised common name, optional localised description (max 2000 characters), optional localised reference URL.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can add a new invertebrate entry with all mandatory fields and at least one functional classification in under 2 minutes from opening the Invertebrate Stock tab.
- **SC-002**: Invertebrate catalogue search returns matching results within 1 second for a catalogue of up to 500 entries on the target hardware.
- **SC-003**: The AI-JSON export for a user with 10 invertebrates across 2 aquariums completes within the same time bounds as the existing export with equivalent fish and coral entries — no perceptible regression.
- **SC-004**: All UI elements for the new feature pass WCAG 2.1 AA contrast checks, ensuring accessibility parity with the existing fish and coral stock UIs.
- **SC-005**: 100% of existing fish and coral stock integration tests continue to pass after the invertebrate feature is introduced — no regressions in related features.
- **SC-006**: All 36 functional requirements have at least one passing automated integration test before the feature is released.

---

## Assumptions

- The taxonomic categories (`CRUSTACEAN`, `MOLLUSC`, `ECHINODERM`, `WORM`) are sufficient for all saltwater reef invertebrate species tracked in Sabi v1; freshwater invertebrates are out of scope.
- The functional classification dimensions (Mobility, Ecological Role, Activity Pattern) are all optional and single-choice within their dimension; no combination constraint is enforced (e.g., a sessile animal can be classified as Cleanup Crew).
- Water sensitivity references are IDs drawn directly from the existing `unit` / `localized_unit` tables already used by the measurement feature; no new unit types are introduced by this feature.
- A single photo per invertebrate entry is sufficient in v1 (consistent with fish and coral stock in their respective first iterations).
- The departure reason enum values (`DIED`, `SOLD`, `GIVEN_AWAY`, `MOVED_TO_OTHER_TANK`, `OTHER`) are identical to those used for fish and coral departures, ensuring a consistent UX across all stock types.
- The UX navigation pattern follows exactly the established fish and coral pattern: the Invertebrate Stock tab lives on the aquarium detail page; the catalogue is accessed from the add/edit form; the admin view is reachable from the existing admin navigation.
- The invertebrate opt-in for the House Reef Report uses the same per-report-link flag mechanism introduced for corals in spec `005`; no new opt-in infrastructure is required.
- No rate limiting is applied to invertebrate catalogue proposal submissions; mandatory JWT authentication provides sufficient protection, consistent with fish (spec `002`) and coral (spec `005`) catalogue approaches.
- The AI-JSON export schema version is incremented to reflect the addition of the `invertebrates` array; existing consumers must treat the new field as optional to remain backward-compatible.
