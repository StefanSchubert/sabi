# Feature Specification: Fish Stock Management & Fish Catalogue

**Feature Branch**: `002-fish-stock-catalogue`  
**Created**: 2026-04-05  
**Status**: Clarified  
**Input**: User description: "Fish Stock Management & Fish Catalogue: Allow users to track the fish population of their aquariums including entry date, individual photo, observed behaviour notes, external reference URL, and an optional link to a shared fish catalogue. Fish can be marked as departed with an end date and reason. The shared fish catalogue is user-generated: any tank owner can propose a new entry, which is immediately usable by the proposer; public visibility requires admin approval to prevent duplicates and ensure legal compliance. Catalogue entries carry a scientific (Latin) name as unique identifier, localised common names, localised descriptions, and localised reference URLs for all five supported languages."

---

## Overview

### Problem Statement

Sabi users currently track water-chemistry measurements and plague records for their aquariums but have no structured way to document which fish live in a tank and what has been observed about their behaviour. This gap makes it impossible for users to correlate livestock history with water-quality events, to share fish-specific observations within the community, or to leverage that data in future BI/AI-assisted analyses. In addition, maintaining a useful reference catalogue for common reef and freshwater fish is a community effort that must be governed carefully to avoid duplicates, copyright issues, and low-quality entries.

### Proposed Solution

Introduce two tightly coupled sub-features:

1. **Tank Fish Stock** — a per-aquarium fish roster where each entry captures: the date the fish joined the tank, an optional personal photo, individual observed-behaviour notes, an optional external reference URL (e.g., Meerwasserwiki), a link to a shared fish catalogue entry, and an optional departure record (date + reason) when the fish dies or is removed.

2. **Fish Catalogue** — a community-maintained, i18n-aware reference catalogue of fish species. Each species is identified by its unique scientific (Latin) name and carries localised common names, descriptions, and reference URLs for all five supported languages. Catalogue entries are user-proposed and immediately usable by their creator; admin approval is required before an entry becomes publicly visible to all users.

### Business Value

- **Holistic tank records**: Users can finally tell the complete story of a tank — not just chemistry numbers but also which animals lived in it and when.
- **Community knowledge base**: A curated, user-contributed fish catalogue grows organically without imposing copyright risk or scope-creep into a full wiki.
- **BI / AI readiness**: Structured behaviour observations lay the groundwork for future trend analysis and AI-assisted health diagnostics (aligned with the AI data export feature `001`).
- **User retention**: Richer data entry means users have more reasons to open Sabi regularly and record observations.

---

## Clarifications

### Session 2026-04-05

- Q: Unique-Constraint für wissenschaftlichen Namen — gilt er auch für Einträge mit Status "Rejected"? → A: Nein. Der Unique-Constraint gilt nur für Pending + Public; Rejected-Einträge sperren den Namen NICHT dauerhaft.
- Q: Wissenschaftlicher Name — Live-Referenz vs. gespeicherte Kopie — wie verhält sich ein Fischeintrag nach Katalog-Änderungen? → A: Gespeicherte Kopie: Name wird beim Verknüpfen in den Fischeintrag kopiert; User kann ihn überschreiben; Katalog-Änderungen propagieren NICHT automatisch zu bereits verknüpften Fischeinträgen.
- Q: Semantik von „Remove" bei Fischeinträgen — wird ein Fischeintrag soft-gelöscht, physisch gelöscht, oder ist eine Departure-Semantik gemeint? → A: Departure-Record für echte Abgänge (historisch erhalten); physisches Löschen NUR für Einträge, die noch KEINEN Departure-Record haben (z. B. Fehleingaben). Einträge mit Departure-Record sind nicht physisch löschbar.
- Q: Kann der Creator eines Pending-Katalog-Eintrags den wissenschaftlichen Namen nachträglich ändern? → A: Ja; beim Ändern wird der Unique-Constraint erneut geprüft; existiert der neue Name bereits als Pending/Public, erscheint dieselbe non-blocking Warnung wie bei der Neuerstellung (analog FR-015); Creator kann trotzdem speichern.
- Q: Foto-Zugriff — eigenständiger Foto-URL-Endpunkt oder Auslieferung über Fischeintrag-API? → A: Kein eigenständiger Foto-Direktlink-Endpunkt; Foto-Bytes werden ausschließlich über den Fischeintrag-API-Endpunkt ausgeliefert und erben vollständig dessen Auth- und Ownership-Checks (FR-011, FR-023, FR-025, C-7).

---

## Scope

### In Scope

**Tank Fish Stock**

- A new "Fish Stock" tab (or section) on the existing aquarium detail page
- Add / edit / remove fish entries per aquarium; **"remove" has two distinct semantics**: (a) recording a **Departure** (end date + reason) for a genuine departure — the entry is retained in the historical view; (b) **physical deletion** of an entry that was created in error — permitted only when NO departure record exists for that entry
- Per-fish fields: species name (free text or catalogue reference), entry date (date the fish was added to the tank), optional personal photo upload, optional observed-behaviour notes (free text, unlimited length), optional external reference URL
- Optional soft-link to a fish catalogue entry (catalogue entry provides pre-filled names and default reference URL)
- Departure record: optional end date + departure reason (options at minimum: "Deceased", "Removed / Rehomed", "Unknown"); a fish without a departure record is considered currently present
- Display of currently present fish separately from departed fish (historical view)

**Fish Catalogue**

- A fish catalogue accessible from the fish stock entry form (search & select)
- Catalogue entries contain: scientific name (Latin, unique, mandatory), localised common name per language (DE, EN, ES, FR, IT), localised description per language (optional), localised reference URL per language (optional)
- User-Generated-Content workflow:
  - Any authenticated user may propose a new catalogue entry
  - A proposed entry is immediately visible and selectable only by its creator (status: **Private / Pending**)
  - Admin role can approve a proposal → status changes to **Public** (visible to all users)
  - Admin role can reject a proposal → status changes to **Rejected** (invisible to all, creator notified)
  - Duplicate prevention: system warns when scientific name already exists
- i18n maintenance: creator and subsequent editors can add/update localised fields for any supported language
- A dedicated admin view listing all pending catalogue proposals for review

**UI / i18n**

- All new UI labels, buttons, messages, and status texts in all 6 message bundle files (DE, EN, ES, FR, IT + fallback)
- WCAG 2.1 AA colour-contrast compliance for all new UI elements

### Out of Scope (this iteration)

- Import of fish data from external wikis (copyright risk; sabi-10 decision)
- Bulk import of catalogue entries (CSV, JSON, scraping)
- Coral or invertebrate stock management (separate future feature)
- Automated deduplication of catalogue entries (manual admin review is sufficient)
- Community commenting or rating of catalogue entries
- Push notifications to the creator when a catalogue proposal is approved/rejected
- Public catalogue browsing without being logged in
- Image editing or cropping within Sabi (upload only)
- Multiple photos per fish entry (single photo upload in v1)
- Versioning / history of catalogue entry edits

---

## User Scenarios & Testing

### User Story 1 — Add a Fish to My Aquarium (Priority: P1)

A user opens the detail page of one of their aquariums and navigates to the new "Fish Stock" tab. They click "Add fish", fill in the common name, set the entry date to today, optionally paste a link to a reference page on Meerwasserwiki, write a short note about the fish's behaviour ("Very territorial, chases the clownfish away from the anemone"), and save. The fish now appears in the "Currently in tank" list on the Fish Stock tab.

**Why this priority**: This is the core value of the entire feature. Without the ability to create and view fish entries, nothing else can be built on top. It is independently deployable as an MVP.

**Independent Test**: Can be fully tested by logging in as a user with at least one aquarium, adding one fish entry with all fields (including behaviour note and URL), saving, and verifying it appears in the fish list with all saved data intact.

**Acceptance Scenarios**:

1. **Given** a logged-in user with at least one aquarium, **When** they open the aquarium detail page, **Then** a "Fish Stock" tab is visible.
2. **Given** the Fish Stock tab is open, **When** the user clicks "Add fish" and fills in the mandatory fields (common name, entry date) and saves, **Then** the fish appears in the "Currently in tank" section.
3. **Given** a new fish entry form, **When** the user fills in all optional fields (photo, behaviour notes, reference URL) and saves, **Then** all fields are persisted and displayed correctly on the fish detail view.
4. **Given** a fish entry form, **When** the user submits without a mandatory field (common name or entry date), **Then** the form shows an inline validation error and does not save.
5. **Given** a fish entry with a reference URL, **When** the user clicks the URL link, **Then** it opens in a new browser tab.

---

### User Story 2 — Record a Fish Departure (Priority: P2)

A user's flame angelfish died overnight. They open the Fish Stock tab, find the fish in the "Currently in tank" list, click "Record departure", enter today's date, select "Deceased" as the reason, and confirm. The fish moves from the "Currently in tank" list to a collapsible "Departed fish" section showing the departure date and reason.

**Why this priority**: Without departure tracking, the "Currently in tank" list becomes inaccurate over time. This is essential data hygiene and has zero dependencies on the catalogue sub-feature.

**Independent Test**: Can be tested by adding a fish entry, then recording a departure for it, and verifying it disappears from the active list and appears in the historical/departed section with the correct departure date and reason.

**Acceptance Scenarios**:

1. **Given** an active fish entry (no departure date), **When** the user records a departure with a valid date and reason, **Then** the fish moves to the "Departed fish" section immediately.
2. **Given** a departure form, **When** the user selects a departure date earlier than the entry date, **Then** an inline validation error is shown and the form cannot be saved.
3. **Given** a fish in the "Departed fish" section, **When** the user views it, **Then** the departure date, departure reason, and all original fields (name, entry date, behaviour notes, photo) are still visible.
4. **Given** a fish with a departure record, **When** the user views the aquarium's fish stock tab, **Then** the "Departed fish" section is collapsed by default but can be expanded.

---

### User Story 3 — Link a Fish Entry to the Fish Catalogue (Priority: P3)

A user is adding a new fish (Amphiprion ocellaris — common clownfish) and wants to link it to the existing catalogue entry to get the pre-filled scientific name and a default reference URL. They type "clownfish" in the catalogue search field on the add-fish form, see a dropdown of matching catalogue entries, select "Amphiprion ocellaris", and the form auto-fills the scientific name and a default reference URL. The user keeps the auto-filled values, adds their personal behaviour note, and saves.

**Why this priority**: Catalogue linking speeds up data entry significantly for common species and ensures consistent scientific naming, but the feature works without it (free-text name is always an alternative).

**Independent Test**: Can be tested independently once at least one public catalogue entry exists: open the add-fish form, search the catalogue, select an entry, verify auto-fill, save, and confirm the link is stored.

**Acceptance Scenarios**:

1. **Given** the add-fish form, **When** the user types at least 2 characters in the catalogue search field, **Then** a dropdown of matching catalogue entries (by common name or scientific name) appears within 1 second.
2. **Given** a catalogue entry is selected from the dropdown, **When** the form updates, **Then** the scientific name field is auto-filled from the catalogue entry and the reference URL field is pre-filled with the catalogue's localised URL for the user's current language (if available).
3. **Given** a fish entry linked to a catalogue entry, **When** the user saves and then views the fish detail, **Then** the scientific name is shown alongside the common name, and the catalogue link is preserved.
4. **Given** a fish entry form, **When** the user does not use the catalogue search, **Then** they can still enter a free-text common name and save without a catalogue link.
5. **Given** a fish linked to a catalogue entry that is later updated by its author, **When** the user views their fish, **Then** the scientific name shown is the value that was copied from the catalogue at the time of linking and is NOT automatically updated when the catalogue entry changes later.

---

### User Story 4 — Propose a New Fish Catalogue Entry (Priority: P4)

A user cannot find their rare fish ("Synchiropus splendidus" — mandarinfish) in the catalogue. They click "Propose new catalogue entry" from the fish stock form, enter the scientific name "Synchiropus splendidus", provide the German common name "Mandarinfisch", English common name "Mandarinfish", add a German description, paste a reference URL for the German Meerwasserwiki, and submit. They immediately see the new entry appear in their catalogue search results (visible only to them, marked "Pending approval"). They link their fish to this entry and save.

**Why this priority**: The catalogue is only valuable if it can grow. The UGC workflow is the mechanism that makes this possible. It depends on P1–P3 being in place.

**Independent Test**: Can be tested by proposing a new catalogue entry with a unique scientific name, verifying it immediately appears in the proposer's search results but not in the search results of a different user account, and confirming the entry is visible in the admin's pending-proposals queue.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they submit a new catalogue proposal with at least a valid scientific name and one localised common name, **Then** the entry is saved with status "Pending" and is immediately searchable by the proposer only.
2. **Given** a catalogue proposal form, **When** the user enters a scientific name that already exists in the catalogue with status **Pending** or **Public**, **Then** a warning message is shown ("A catalogue entry for [name] already exists") but the user may still proceed.
3. **Given** a pending catalogue entry, **When** a different non-admin user searches the catalogue, **Then** the pending entry is NOT visible in their search results.
4. **Given** a pending catalogue entry, **When** the admin views the pending-proposals list, **Then** the entry appears with all submitted fields.
5. **Given** a fish stock entry linked to a pending catalogue entry, **When** the proposal is later approved by an admin, **Then** the fish stock entry remains linked and the scientific name display does not change.

---

### User Story 5 — Admin Approves or Rejects a Catalogue Proposal (Priority: P5)

An admin logs in and navigates to the "Fish Catalogue Administration" view. They see a list of pending proposals. They open a proposal for "Synchiropus splendidus", review the submitted fields, edit the English description to improve clarity, and click "Approve". The entry is now publicly visible to all users. Later, the admin finds a duplicate proposal ("Chromis viridis" vs. an existing "Chromis viridis" entry) and clicks "Reject" with the note "Duplicate of existing entry #42". The proposer's entry changes to "Rejected" status.

**Why this priority**: Without admin approval, the catalogue quality degrades rapidly. This is the governance layer that makes the UGC workflow safe for all users. Depends on P4.

**Independent Test**: Can be tested by logging in as an admin account, finding a pending proposal, approving it, then logging in as a regular user and verifying the approved entry now appears in the catalogue search.

**Acceptance Scenarios**:

1. **Given** an admin user, **When** they navigate to the admin catalogue view, **Then** they see a list of all pending proposals with the proposer, submission date, and scientific name.
2. **Given** an admin reviewing a proposal, **When** they click "Approve", **Then** the entry status changes to "Public" and it immediately appears in all users' catalogue search results.
3. **Given** an admin reviewing a proposal, **When** they click "Reject" and optionally provide a reason, **Then** the entry status changes to "Rejected" and it is no longer visible to any user (including the proposer).
4. **Given** a non-admin authenticated user, **When** they attempt to access the catalogue admin view, **Then** they receive an access-denied response.
5. **Given** an admin approving a proposal, **When** they edit any localised field before approving, **Then** the updated fields are saved as part of the approval action.

---

### User Story 6 — Maintain i18n Fields of a Catalogue Entry (Priority: P6)

A German-speaking user who proposed a catalogue entry returns to it and notices the French description is empty. They open the catalogue entry editor, switch to the French language tab, fill in a French common name and description, and save. The French fields are now available for French-speaking users when they link their fish to this entry and view the reference URL.

**Why this priority**: i18n completeness is important for Sabi's international user base but is not a blocker for the core stock-management workflow. Incremental catalogue enrichment is expected and acceptable.

**Independent Test**: Can be tested by opening an existing approved catalogue entry as its creator (or as admin), adding a localised field for a previously-empty language, saving, then switching the UI to that language and verifying the field is shown.

**Acceptance Scenarios**:

1. **Given** a catalogue entry with missing localised fields, **When** the creator (or an admin) opens the edit form and fills in a localised common name for a previously-empty language, **Then** the field is saved and displayed to users with that UI language preference.
2. **Given** a catalogue entry with a localised reference URL per language, **When** a user with a specific language preference links their fish to the entry, **Then** the reference URL shown defaults to the catalogue entry's URL for that language (if present) or falls back to the English URL, then to any available URL.
3. **Given** a catalogue entry editor, **When** the user provides a localised description longer than 2000 characters, **Then** an inline validation message informs the user of the limit.

---

### Edge Cases

- What happens when a user uploads a photo larger than the accepted size limit? → The system rejects the upload immediately with a clear user-facing error message stating the maximum allowed size; no partial upload is stored.
- What happens when a user deletes their aquarium that has fish stock entries? → All fish entries (active and departed) linked to that aquarium are soft-deleted along with the aquarium; catalogue links remain intact (catalogue entries are not affected).
- What if two users simultaneously propose catalogue entries with the same scientific name? → Both are accepted as pending entries; the admin sees both in the proposals queue and can approve one and reject the other as a duplicate.
- What if a user tries to set the fish entry date to a date after the departure date? → The system shows an inline validation error and prevents saving; the constraint must be enforced both client-side and server-side.
- What if a catalogue entry's scientific name contains special characters (e.g., "Pseudocheilinus hexataenia")? → The system must store and search the name with full Unicode support; no character normalisation that changes meaning.
- What happens when the fish catalogue search returns no results? → The user sees a "No catalogue entries found" message and a prominent "Propose new entry" link.
- What if a user clears the catalogue link from an existing fish entry? → The fish entry is saved with the free-text name preserved; the scientific name field becomes editable again; the previous catalogue link is removed.
- What if a user tries to physically delete a fish entry that already has a departure record? → The system rejects the delete request with a clear error message (e.g., "This fish entry has a departure record and cannot be deleted. Use the departure record to document the end of the fish's stay."); the entry remains unchanged.
- What if someone attempts to access the photo bytes of a fish entry outside the fish-entry API (e.g., via a guessed direct URL)? → Not possible by design — no standalone photo direct-link endpoint exists (FR-025, C-7); the only way to retrieve photo bytes is through the fish-entry API endpoint, which enforces authentication and ownership checks for every request.

---

## Requirements *(mandatory)*

### Functional Requirements

**Tank Fish Stock**

- **FR-001**: The system MUST provide a "Fish Stock" tab (or equivalent section) on every aquarium detail page, visible to the aquarium's owner.
- **FR-002**: The system MUST allow an authenticated user to create a fish entry for any of their own aquariums containing: a common name (mandatory, free text), an entry date (mandatory), an optional personal photo, optional observed-behaviour notes (free text), and an optional external reference URL.
- **FR-003**: The system MUST validate that the entry date is not in the future and that mandatory fields (common name, entry date) are not empty; invalid submissions MUST be rejected with inline error messages.
- **FR-004**: A user MUST be able to edit all fields of a fish entry they own.
- **FR-005**: A user MUST be able to record a departure for an active fish entry by providing a departure date and selecting a departure reason; valid departure reasons MUST include at minimum: "Deceased", "Removed / Rehomed", "Unknown".
- **FR-006**: The system MUST validate that the departure date is not earlier than the entry date; invalid departure submissions MUST be rejected with an inline error message.
- **FR-007**: The aquarium Fish Stock tab MUST display currently-present fish (no departure record) and departed fish (with departure record) in separate, clearly labelled sections; the departed-fish section MUST be collapsed by default.
- **FR-008**: A user MUST be able to upload a single photo for a fish entry; the system MUST reject files that exceed 5 MB or are not in a standard image format (JPEG, PNG, WebP, GIF); on rejection, a clear error message stating the constraint MUST be displayed.
- **FR-009**: The system MUST allow a user to optionally link a fish entry to a fish catalogue entry; linking MUST auto-fill the scientific name and the localised reference URL (if available) but MUST allow the user to override these values.
- **FR-010**: The system MUST allow a user to remove the catalogue link from an existing fish entry without deleting the fish entry; upon removal, the free-text name is retained and the scientific name field becomes user-editable.
- **FR-011**: A user MUST NOT be able to view, edit, or delete fish entries belonging to another user's aquarium.

**Fish Catalogue**

- **FR-012**: The fish catalogue MUST store each species as a unique entry identified by its scientific (Latin) name; the scientific name MUST be unique among all catalogue entries with status **Pending** or **Public**; entries with status **Rejected** do NOT participate in the uniqueness constraint and do NOT permanently block the scientific name from being re-used in a new proposal.
- **FR-013**: Each catalogue entry MUST carry: a scientific name (mandatory, unique), a localised common name for at least one language (mandatory at creation), an optional localised description per language, and an optional localised reference URL per language — for each of the five supported languages (DE, EN, ES, FR, IT).
- **FR-014**: Any authenticated user MUST be able to propose a new catalogue entry; upon submission, the entry is assigned status **Pending** and is immediately searchable and selectable **only** by its creator.
- **FR-015**: The system MUST warn the user when they attempt to propose a catalogue entry with a scientific name that already exists with status **Pending** or **Public**; the same non-blocking warning MUST also be shown when the creator of a Pending entry edits and changes the scientific name to one that already exists with status **Pending** or **Public**; entries with status **Rejected** do NOT trigger the duplicate warning in either case; the warning MUST be non-blocking (the user may proceed).
- **FR-016**: An authenticated user with the **Admin** role MUST be able to approve a pending catalogue entry; approved entries receive status **Public** and MUST immediately become searchable by all authenticated users.
- **FR-017**: An Admin MUST be able to reject a pending catalogue entry, optionally providing a rejection reason; rejected entries MUST become invisible to all users including the creator.
- **FR-018**: An Admin MUST be able to edit any field of a catalogue entry (any status) before or after approval.
- **FR-019**: The creator of a Pending catalogue entry MUST be able to edit all fields of that entry, including the scientific name; when the scientific name is changed, the system MUST re-evaluate the uniqueness constraint and display the same non-blocking duplicate warning defined in FR-015 if the new name conflicts with an existing Pending or Public entry. Public entries MUST be editable by their creator and by Admins; Rejected entries MUST be read-only.
- **FR-020**: The catalogue search (used from the fish stock entry form) MUST search by partial match on both scientific name and all available localised common names; results MUST return within 1 second for up to 500 catalogue entries.
- **FR-021**: The system MUST provide an admin view listing all Pending catalogue proposals, sortable by submission date; each list item MUST show: scientific name, proposer reference (anonymised), submission date, and a link to the full proposal details.

**General**

- **FR-022**: All new UI labels, button texts, status labels, and user-facing messages MUST be present in all six message bundle files (DE, EN, ES, FR, IT + fallback `messages.properties`); missing keys are a release blocker.
- **FR-023**: All new backend operations that modify fish stock or catalogue data MUST be protected by the existing authentication mechanism; unauthenticated requests MUST be rejected with HTTP 401.
- **FR-024**: A user MUST be able to physically delete a fish entry they own **if and only if** no departure record exists for that entry; a delete request against an entry that already has a departure record MUST be rejected by the system with an informative error message; entries with a departure record MUST be retained indefinitely as historical records and are not physically deletable by the owner.
- **FR-025**: Fish photo bytes MUST be served exclusively through the fish-entry API endpoint that owns the photo; no standalone photo direct-link endpoint exists. Photo retrieval is subject to the same authentication and ownership checks as the fish entry itself (FR-011, FR-023); any attempt to access photo bytes outside of the fish-entry API is not possible by design.

### Non-Functional Requirements (ISO 25010)

| ISO 25010 Merkmal         | Anforderung / Constraint für dieses Feature                                                                                                                                                  |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Funktionale Eignung       | Alle 25 FRs müssen erfüllt sein. Keine Cross-User-Datenlecks (FR-011). Vollständigkeit der Fischbesatz-Felder (FR-002) und Katalog-Felder (FR-013) ist via Integrationstest prüfbar. Physisches Löschen nur ohne Departure-Record (FR-024). Foto-Zugriff ausschließlich über Fischeintrag-API (FR-025).         |
| Leistungseffizienz        | Katalogssuche: Ergebnisse innerhalb 1 Sekunde bei bis zu 500 Einträgen (FR-020). Foto-Upload: max. 5 MB-Limit verhindert übermäßigen Speicherverbrauch. ARM64-Kompatibilität erforderlich.   |
| Kompatibilität            | Neue API-Endpoints dürfen keine Breaking Changes an bestehenden Endpoints verursachen. Multi-Plattform: ARM64 und AMD64. Bestehende Aquarium-Detailseite wird erweitert, nicht ersetzt.       |
| Benutzbarkeit             | i18n in allen 5 Sprachen + Fallback (FR-022). WCAG 2.1 AA Kontrast für alle neuen UI-Elemente. Katalog-Verlinkung max. 3 Klicks vom Fischbesatz-Tab entfernt. Inline-Fehlermel­dungen.      |
| Zuverlässigkeit           | Foto-Upload-Fehler (zu groß, falsches Format) werden mit klarer Fehlermeldung abgefangen (FR-008). Departure-Datum-Validierung client- und serverseitig (FR-006). Transaktionssichere Saves. |
| Sicherheit                | Authentifizierung für alle schreibenden Operationen zwingend (FR-023). Strikte User-Isolierung: kein Zugriff auf fremde Fischeinträge (FR-011). Admin-Only für Katalog-Approval (FR-016/17). |
| Wartbarkeit               | Flyway-Datenbankmigrationen erforderlich (neue Tabellen: fish_stock, fish_catalogue_entry, fish_catalogue_i18n). Test-Coverage: mind. je ein Integrationstest für P1–P5 User Stories.        |
| Übertragbarkeit           | Foto-Speicher muss in Docker-Compose und Ansible-Deployment konfigurierbar sein (Volume/Pfad). Kein neuer externer Service-Dependency. ARM/AMD64 Docker-Images bleiben kompatibel.           |

### Key Entities

- **TankFishStock**: Represents a single fish living (or having lived) in a specific aquarium. Key attributes: aquarium reference, common name (free text), scientific name (free text or resolved from catalogue), entry date, departure date (optional), departure reason (optional), observed-behaviour notes (optional, free text), external reference URL (optional), photo reference (optional), fish-catalogue-entry reference (optional, nullable). A `TankFishStock` record without a departure date is considered currently active.

- **FishCatalogueEntry**: A community-maintained reference record for a fish species. Key attributes: scientific name (unique, Latin), approval status (Pending / Public / Rejected), proposer reference, proposal date. One-to-many relationship with `FishCatalogueI18n`.

- **FishCatalogueI18n**: The language-specific portion of a catalogue entry. Attributes: language code (de / en / es / fr / it), common name (optional), description (optional, max 2000 characters), reference URL (optional). Each entry has at most one `FishCatalogueI18n` record per language code.

- **FishPhoto**: A stored image file associated with a `TankFishStock` record. Key attributes: file reference/path, content type, upload date. Stored on the server filesystem or a configured storage volume; not stored in the database as a BLOB.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can add a complete fish entry (with all optional fields filled) to an aquarium in under 3 minutes from opening the Fish Stock tab.
- **SC-002**: A user can record a fish departure (marking a fish as deceased or removed) in under 1 minute from opening the Fish Stock tab.
- **SC-003**: Catalogue search returns matching entries within 1 second for a catalogue of up to 500 public entries — verified by a performance test scenario.
- **SC-004**: 100% of submitted fish entries and catalogue proposals are accessible only to their owner (and admins for proposals) — verified by cross-user access tests.
- **SC-005**: An admin can process (approve or reject) a pending catalogue proposal in under 2 minutes from opening the admin view.
- **SC-006**: All new user-facing text is correctly displayed in all 5 supported languages — verified by a checklist covering all 22 new i18n keys (or however many are introduced) across all 6 bundle files.
- **SC-007**: The Fish Stock tab renders correctly and all WCAG 2.1 AA colour-contrast requirements are met for all new UI elements — verified by automated contrast-check tooling and manual review.
- **SC-008**: Photo uploads of valid files (≤ 5 MB, supported formats) succeed 100% of the time; uploads exceeding the limit or with unsupported formats are rejected with a clear error message 100% of the time — verified by integration tests.
- **SC-009**: A newly proposed catalogue entry is searchable by its creator immediately after submission (< 2 seconds) but is NOT visible in the search results of any other non-admin user — verified by two-user acceptance test scenario.

---

## Assumptions

- The existing Sabi authentication and authorisation mechanism (JWT bearer token + role-based access) is sufficient to enforce user isolation (FR-011) and admin-only access (FR-016/17); no new auth infrastructure is required.
- Fish photos are stored on the server filesystem in a configurable directory (not as database BLOBs); the existing Ansible deployment and Docker Compose setup can be extended with a volume mount for this directory without major changes.
- A single photo per fish entry is sufficient for v1; multi-photo support is deferred.
- The departure-reason list ("Deceased", "Removed / Rehomed", "Unknown") is fixed for v1 and stored as an enum; it can be extended in a later iteration if users request additional reasons.
- The existing database migration tooling (Flyway) will handle the new schema additions; no manual DDL is required in production.
- The scientific name uniqueness constraint covers only entries with status **Pending** or **Public** and is enforced at the application layer as well as at the database level (partial unique index filtered to Pending/Public status); entries with status **Rejected** are excluded from this constraint. The application-level duplicate warning (FR-015) fires before the database constraint is hit.
- Catalogue entries in "Rejected" status are retained in the database for admin audit purposes but are never shown in the UI; a future purge/archive policy can be added later.
- The localised reference URL in a catalogue entry is intended as a suggestion; users may override it in their individual fish stock entry (FR-009). There is no automatic sync between the catalogue URL and the fish stock entry URL after initial auto-fill.
- Catalogue changes (scientific name, localised names, descriptions, reference URLs) do NOT propagate automatically to fish stock entries that are already linked to that catalogue entry; all values are copied at link time into the `TankFishStock` record and may be overridden independently by the user. The catalogue link (foreign-key reference) is retained so admins can trace the original source, but any field values diverge independently after the initial copy.
- No email notification is sent to catalogue-entry creators when their proposal is approved or rejected in v1; this can be added in a future iteration.
- Coral and invertebrate stock management (using the same or a similar pattern) is explicitly out of scope for this iteration; the data model should, however, be designed to accommodate a future `TankCoralStock` entity in a symmetric way.
- Image format validation (JPEG, PNG, WebP, GIF) is performed server-side by inspecting the file magic bytes, not solely by the uploaded filename extension.
- The behaviour-notes field has no enforced maximum length in v1; a practical soft limit may be imposed by the UI editor (e.g., 5000 characters) but is not a hard constraint.
- All catalogue entries visible to regular users (status: Public) are read-only for non-creators; editing is restricted to the original proposer and admins.

---

## Constraints

| ID  | Constraint                                                                                                                                                              |
|-----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C-1 | No import of content from external wikis (Meerwasserwiki, Wikipedia, FishBase, etc.) is permitted due to copyright concerns (sabi-10 decision).                        |
| C-2 | Sabi is NOT intended to become a full fish wiki; catalogue entries are lightweight reference pointers, not encyclopaedic articles. Descriptions are capped at 2000 characters per language. |
| C-3 | Scientific names must be stored as-is (case-sensitive Latin binomial nomenclature); no automatic normalisation or spell-check against external taxonomic databases.     |
| C-4 | Fish photos must NOT be stored as database BLOBs; they must be stored on a configurable filesystem volume.                                                             |
| C-5 | All new API endpoints must be consistent with the existing Sabi REST API conventions and placed under the `/api/` path.                                                |
| C-6 | The feature must not introduce any new external runtime service dependencies (no external storage, no CDN, no third-party image processing).                            |
| C-7 | No standalone photo direct-link endpoint. Fish photo bytes are served exclusively as part of the fish-entry API endpoint and are subject to the same authentication and ownership checks; there is no publicly accessible URL for photo bytes independent of the fish entry. |

---

*This specification was created for branch `002-fish-stock-catalogue`. Next step: `/speckit.plan` to break this into implementation tasks.*
