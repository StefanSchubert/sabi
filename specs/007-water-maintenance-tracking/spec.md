# Feature Specification: Water Maintenance Tracking & AI Export Integration

**Feature Branch**: `007-water-maintenance-tracking`  
**Created**: 2026-10-25  
**Status**: Draft  
**Input**: User description: "The third time I have an Euphyllia now. This time it looked that I could keep here. But unfortunately she is retreating since 3 weeks and her coral tissue melted meanwhile so much that the skeleton becomes visible. Again I fear it is too late to rescue that coral. I switched from manual to automatic dosing of Ca and KH and my water parameter seemed to be OK, so what could be the cause?! I also feed the coral, with phytoplankton to make it grow, but this remedy doesn't show any effect. I observed that other corals like my caulestrae and some dish anemones are spreading very well, also kenia trees. Due to too much work at my job I became a lazy reefer of my nano-reef (80 Liters). Meanwhile I believe that because I doesn't cycle sea-water and just add water and (Ca/KH) Supplements, that the concentration of nettle toxin has become too much and causes the retreat of the Euphylia. I started to do coal based filtering to reduce the nettle toxin (which amount can't be measured through water tests). Describe the solution you'd like: To confirm the possible cause we would require to collect the following parameters for the tanks: How often do you change water and to which percentage? E.g. the user shall be able to enter a fixed exchange interval, or record single cycle in case he does it rather arbitrary. Do you rely completely on water exchange or do you add supplements (which ones, how often). Do you do any coal filtering (if so steady, or as interval? how often). The so collected data need to be added in the AI JSON export function."

---

## Overview

### Problem Statement

Reef keepers using Sabi regularly face livestock problems that water chemistry alone cannot explain. A typical case: a Euphyllia coral retreats and loses tissue over several weeks while all measured parameters (calcium, alkalinity, nitrate, …) look fine. In such cases the root cause often lies in **maintenance practices** that Sabi does not capture today: how often and to which percentage water is changed, which supplements are dosed (and whether manually or automatically), and whether activated carbon filtering is running.

When users consult an AI chatbot with their Sabi data export (spec `001`), the AI has no context about these practices and cannot evaluate plausible hypotheses such as "accumulation of nettle toxin due to insufficient water exchange". Users themselves also lose track of their own ad-hoc routines over time, especially when they are short on time — making it impossible to answer the very questions an AI (or an experienced hobbyist) would ask during a diagnosis.

### Proposed Solution

Add a per-aquarium **Maintenance** page to Sabi in which users document:

1. **Water changes** — an optional regular exchange interval (interval in days + percentage) describing the intended routine, and/or individual recorded water change events (date + percentage + optional note) capturing what actually happened.
2. **Supplements** — no new supplement list: the Maintenance page provides a hint/cross-link to the existing dosing feature, where supplements are already documented (see Clarifications 2026-10-26).
3. **Activated carbon filtering** — whether it is used, whether it runs continuously or as an interval, and the replacement frequency.

The collected data is added to the existing AI-JSON export (spec `001`) as a per-aquarium `maintenance` block, so AI chatbots receive the complete maintenance picture alongside measurements and livestock for diagnostics and advice.

### Business Value

- **Better AI-assisted diagnosis**: The export finally answers the questions ("how often do you change water?", "what are you dosing?") that AI chatbots and experienced hobbyists ask first when parameters look fine but livestock is struggling.
- **Self-awareness for users**: Documenting routines makes ad-hoc habits visible — a lazy reefer can see at a glance that water exchange has slipped, which is often the actual problem.
- **Data completeness for the open-science goal**: Maintenance practices are a first-class variable in reef health; capturing them makes Sabi's dataset more valuable for community analysis.
- **Low effort, high value**: All inputs are quick to record (a water change takes one date and one percentage), matching the target audience of hobbyists without technical background.

---

## Scope

### In Scope

- A new standalone per-aquarium Maintenance page (`maintenanceView.xhtml` with tank selector, header-menu entry) following the established dosing-view pattern.
- **Water changes**: optional regular exchange interval per tank (interval in days + percentage) and individual water change records (date, percentage of tank volume, optional note), including back-dating for history.
- **Supplements**: no new supplement list — a hint/cross-link section on the Maintenance page pointing to the existing dosing feature, where supplements are documented.
- **Activated carbon filtering**: per-tank setting (enabled; mode continuous/interval; replacement interval in days for interval mode).
- **AI-JSON export integration**: a `maintenance` block per aquarium in the existing export (spec `001`), with an incremented schema version.
- i18n: all new UI labels and messages in all six message bundle files (DE, EN, ES, FR, IT + fallback).

### Out of Scope (this iteration)

- Reminders or notifications for water changes, supplement dosing, or carbon replacement.
- Tracking of other maintenance activities (filter pad cleaning, equipment servicing, refugium management).
- A separate supplement data model or list — supplements are documented in the existing Dosing feature (see Clarifications 2026-10-26).
- Absolute water change volume in liters (percentage of tank volume only).
- Integration into the public House Reef Report.
- Automatic derivation of maintenance recommendations or health scores from the collected data.

---

## Clarifications

### Session 2026-10-26

| # | Question | Answer |
|---|----------|--------|
| 1 | Where does the Maintenance section live in the UI? (The spec said "aquarium detail page", but Sabi has no central tabbed detail page — there is the tank list `tankView.xhtml` and standalone per-feature views with a tank selector.) | **Standalone page**: new `maintenanceView.xhtml` with tank selector + 3 sub-sections (Water Changes, Supplements cross-link, Activated Carbon), following the established dosing-view pattern; reachable via the header menu. |
| 2 | How do supplements relate to the existing Dosing feature (`aquarium_dosing`, migration V1_8_0_12), which already records per-tank dosing events (product, amount, manual/automatic, interval)? | **No separate supplement feature or table.** The Maintenance page instead shows a hint/cross-link section pointing to the existing Dosing functionality. FR-008/FR-009 removed; FR-007 rewritten as cross-link requirement. |
| 3 | What goes into `maintenance.supplements` in the AI export (FR-013)? | **Field removed** from the `maintenance` block entirely. Dosing data remains in the existing top-level `dosings` array of each aquarium block; consumers read supplements from there. |
| 4 | How is `sabiSchemaVersion` bumped, given FR-015 (increment) vs. spec 001 C-3 (increment only on breaking changes)? | **Minor bump `1.0` → `1.1`** for the additive `maintenance` block. C-3's mandate (bump on breaking changes) remains in force for future major changes. |

---

## User Scenarios & Testing

### User Story 1 — Record Water Changes and Define a Regular Interval (Priority: P1)

A user opens one of their aquariums, navigates to the new "Maintenance" section, and records a water change: date (defaults to today), 25 %, optional note ("topped up after evaporation — no, actually a real exchange"). They also set the regular interval "25 % every 3 weeks". The section now shows the regular interval and a chronological list of recorded water changes, newest first.

**Why this priority**: This is the core data the AI needs to answer "how often do you change water and to which percentage?" — the first question in the user's feature request. Everything else (supplements, carbon) builds on the same section, but this story alone already delivers a viable MVP.

**Independent Test**: Log in as a user with at least one aquarium, open the Maintenance section, set a regular interval, record two water changes (one with today's date, one back-dated), edit one, and verify the list displays everything correctly in chronological order.

**Acceptance Scenarios**:

1. **Given** a logged-in user with at least one aquarium, **When** they open the Maintenance page (header menu), **Then** the page is visible with tank selector and all three sub-sections.
2. **Given** the Maintenance section, **When** the user records a water change with a valid date and percentage (1–100), **Then** the record appears in the chronological list (newest first) showing date, percentage, and note.
3. **Given** a water change form, **When** the user submits a percentage outside 1–100 or a date in the future, **Then** an inline validation error is shown and nothing is saved.
4. **Given** the Maintenance section, **When** the user sets a regular interval (e.g., 21 days, 25 %) and saves, **Then** the section displays it as the tank's regular water exchange routine.
5. **Given** a set regular interval, **When** the user clears it, **Then** the section shows "no regular interval" and all individual records remain untouched.
6. **Given** an existing water change record, **When** the user edits or deletes it, **Then** the list reflects the change immediately.
7. **Given** a tank, **When** the user records a water change with a past date (back-dating history), **Then** the record is accepted and sorted chronologically among the others.

---

### User Story 2 — Cross-Link to the Existing Dosing Feature (Priority: P2)

A user opens the Maintenance page for their tank and sees a short "Supplements & dosing" section explaining that supplement usage is documented in the existing Dosing feature. A clearly labelled link/button takes them to the Dosing view (with the same tank pre-selected where supported), where they can record e.g. "Calcium" (automatic, daily) or "Phytoplankton feed" (manual, 2× per week). No new supplement data model is introduced.

**Why this priority**: Answers the second question ("do you rely completely on water exchange or do you add supplements — which ones, how often?") without duplicating the dosing feature that already exists in Sabi (see Clarifications 2026-10-26). The manual-vs-automatic distinction is diagnostic-relevant (automatic dosing implies stable parameters). No dependency on P1 beyond the shared page.

**Independent Test**: Open the Maintenance page for a tank, verify the supplements section shows the hint text and the link to the Dosing feature; follow the link and verify the Dosing view opens (with the tank pre-selected where supported); record a dosing entry there and verify it appears in the Dosing list.

**Acceptance Scenarios**:

1. **Given** the Maintenance page for a tank, **When** the user views it, **Then** a "Supplements & dosing" section is visible with localised hint text explaining that supplements are documented in the Dosing feature.
2. **Given** the "Supplements & dosing" section, **When** the user clicks the link/button, **Then** the Dosing view opens (with the same tank pre-selected where supported).
3. **Given** a tank with existing dosing records, **When** the user opens the Dosing view via the Maintenance cross-link, **Then** the tank's dosing records are available as before (no data model change).

---

### User Story 3 — Configure Activated Carbon Filtration (Priority: P3)

A user starts using activated carbon to reduce nettle toxin. They enable "Activated carbon" for their tank, select mode "Interval" and a replacement every 28 days. Six weeks later they switch the mode to "Continuous". The section always shows the current setting at a glance.

**Why this priority**: Answers the third question ("do you do any coal filtering — steady or interval, how often?"). It is a simple per-tank setting; lowest data volume of the three inputs, hence P3.

**Independent Test**: In a tank's Maintenance section, enable activated carbon in interval mode with a replacement interval, save and verify display; switch to continuous mode and verify the replacement interval is no longer required or shown; disable carbon and verify the section shows "not used".

**Acceptance Scenarios**:

1. **Given** the Maintenance section, **When** the user enables activated carbon, selects mode "Interval" and a replacement interval of 28 days, and saves, **Then** the setting is displayed (enabled, interval mode, 28 days).
2. **Given** activated carbon enabled in interval mode, **When** the user saves without a replacement interval, **Then** an inline validation error is shown and nothing is saved.
3. **Given** activated carbon enabled in continuous mode, **When** the user saves, **Then** no replacement interval is required.
4. **Given** an existing carbon setting, **When** the user disables activated carbon, **Then** mode and replacement interval are cleared from the display.
5. **Given** a tank with carbon disabled, **When** the user views the section, **Then** it shows "not used" (or equivalent localised text).

---

### User Story 4 — Maintenance Data in the AI-JSON Export (Priority: P4)

A user downloads their AI chatbot data export. Each aquarium in the JSON now contains a `maintenance` block with the regular water exchange interval, all recorded water changes, and the activated carbon setting. The user pastes the file into ChatGPT and asks: "My Euphyllia is retreating and losing tissue, but my parameters look fine — what could be the cause given my maintenance routine?" The AI references the user's 25 %-every-3-weeks water exchange, the automatic Ca/KH dosing (from the existing `dosings` array), and the newly added carbon filtering in its answer.

**Why this priority**: This is the explicit purpose of the whole feature — confirming suspected causes via AI consultation. It depends on P1–P3 producing data, but is independently testable once any maintenance data exists.

**Independent Test**: With at least one tank containing a regular interval, two water change records, and an activated carbon setting, download the AI export and verify the `maintenance` block contains all values exactly as entered.

**Acceptance Scenarios**:

1. **Given** a user with maintenance data for at least one aquarium, **When** they download the AI export, **Then** each aquarium's data block contains a `maintenance` object with `regularInterval`, `events[]` (water changes + carbon replacements) and `activatedCarbon`.
2. **Given** an aquarium with no maintenance data, **When** the user downloads the export, **Then** the `maintenance` block is still present with an empty records array, a null regular interval, and carbon disabled — no error is returned.
3. **Given** the downloaded export, **When** inspecting the `maintenance` block, **Then** all field names are human-readable English (consistent with the existing export style), dates are ISO-8601, and no personal data appears.
4. **Given** a recorded water change, **When** inspecting the export, **Then** it contains the date and the percentage of tank volume (and the note, if provided).
5. **Given** the exported JSON compared to a pre-feature export of the same data, **Then** `sabiSchemaVersion` is incremented and all previously existing fields are unchanged (additive extension only).

---

### Edge Cases

- **Percentage out of range**: A water change percentage below 1 or above 100 is rejected with an inline validation error (client- and server-side).
- **Future-dated water change**: Rejected with an inline validation error; back-dating (past dates) is explicitly allowed for history.
- **Large history**: A user back-dates 50+ water change records — the list remains usable (sorted newest first) and the export includes all records without degrading generation time beyond the existing budget.
- **Regular interval AND individual records**: Both may coexist; both are exported, so the AI can compare intended routine vs. actual practice (e.g., "planned 25 % every 3 weeks, but only 2 changes in the last 8 weeks").
- **Tank deletion**: Deleting an aquarium removes all its maintenance data (water change records, settings) consistently with other per-tank data.
- **Cross-user access**: A user attempting to read or modify maintenance data of another user's tank is rejected (ownership check), consistent with the existing security pattern.
- **Text length limits**: Water change notes > 200 characters are rejected with inline validation errors. (Supplement names/frequencies: no longer part of this feature — see Clarifications 2026-10-26.)
- **Carbon interval out of range**: Replacement intervals below 1 or above 365 days are rejected with inline validation errors.
- **Multiple tanks**: Each tank has fully independent maintenance data; nothing is shared or inherited between tanks.
- **User with no aquariums**: The AI export behaves exactly as before (empty `aquariums` array); the feature introduces no new failure mode.

---

## Requirements *(mandatory)*

### Functional Requirements

**Water Changes**

- **FR-001**: The system MUST provide a per-aquarium Maintenance page (standalone `maintenanceView.xhtml` with tank selector), visible to the aquarium's owner.
- **FR-002**: A user MUST be able to record individual water change events for their own aquariums with: a date (mandatory, not in the future), a percentage of tank volume (mandatory, integer 1–100), and an optional free-text note (max 200 characters).
- **FR-003**: A user MUST be able to define, change, or clear a regular water exchange interval per aquarium: an interval in days (mandatory when set, 1–365) and an exchange percentage (mandatory when set, 1–100).
- **FR-004**: A user MUST be able to edit and delete individual water change records they created.
- **FR-005**: The Maintenance section MUST display the regular interval (if set) and a chronological list of recorded water changes (newest first), each showing date, percentage, and note.
- **FR-006**: Water change records MUST be accepted for past dates (back-dating history); the system MUST NOT auto-generate water change records from the regular interval.

**Supplements (cross-link only — see Clarifications 2026-10-26)**

- **FR-007**: The Maintenance page MUST provide a "Supplements & dosing" section with localised hint text explaining that supplement usage is documented in the existing Dosing feature, and a link/button that navigates to the Dosing view with the same tank pre-selected where supported.
- ~~**FR-008**: A user MUST be able to edit and delete supplement entries.~~ — **REMOVED** (supplement CRUD superseded by the existing Dosing feature).
- ~~**FR-009**: The Maintenance section MUST display the current supplement list.~~ — **REMOVED** (no supplement list in this feature; dosing records remain in the Dosing view).

**Activated Carbon Filtering**

- **FR-010**: A user MUST be able to configure per-aquarium activated carbon usage: enabled (boolean); when enabled, a mode (mandatory, one of `CONTINUOUS` | `INTERVAL`) and a replacement interval in days (mandatory only for `INTERVAL` mode, 1–365).
- **FR-011**: When activated carbon is disabled, the mode and replacement interval MUST be cleared (or ignored); the section MUST display "not used" (or equivalent localised text).
- **FR-012**: The Maintenance section MUST display the current carbon setting (enabled state, mode, replacement interval where applicable).

**AI-JSON Export Integration**

- **FR-013**: The AI export MUST include a `maintenance` object in each aquarium's data block containing:
  - `regularInterval`: an object `{intervalDays, percentage}` or `null` when no regular interval is set;
  - `events`: an array of `{type, date, percentage, note}` (`type` is `WATER_CHANGE` or `CARBON_REPLACEMENT`; `percentage` is `null` for carbon replacements), empty array when none;
  - `activatedCarbon`: an object `{enabled, mode, replacementIntervalDays}` where `mode` and `replacementIntervalDays` are `null` when disabled or in continuous mode.
  - No `supplements` field: supplement/dosing data is already exported in the existing top-level `dosings` array (see Clarifications 2026-10-26).
- **FR-014**: The export MUST use human-readable English field names consistent with the existing export style, ISO-8601 dates, and MUST NOT contain any personal identifying data.
- **FR-015**: The export's `sabiSchemaVersion` MUST be incremented to reflect the new `maintenance` block; all previously existing export fields MUST remain unchanged (additive, backward-compatible extension).
- **FR-016**: For aquariums with no maintenance data, the `maintenance` block MUST still be present (empty records array, null regular interval, carbon disabled) so consumers can rely on a stable structure.

**General**

- **FR-017**: All new UI labels, button texts, mode/method labels, empty-state messages, and validation error messages MUST be present in all six message bundle files (DE, EN, ES, FR, IT + fallback `messages.properties`); missing keys are a release blocker.
- **FR-018**: All maintenance data operations MUST be protected by the existing authentication mechanism (unauthenticated requests rejected with HTTP 401) and MUST enforce aquarium ownership: a user can only view or modify maintenance data of their own aquariums.
- **FR-019**: Deleting an aquarium MUST remove all of its maintenance data (water change records, settings) consistently with the existing per-tank data handling.

### Non-Functional Requirements (ISO 25010)

| ISO 25010 Quality          | Requirement / Constraint for this Feature                                                                                                                                                                                                                   |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Functional Suitability     | All 17 active FRs must be satisfied (FR-008/FR-009 removed per Clarifications 2026-10-26). No cross-user data leaks (FR-018). Completeness of the exported `maintenance` block (FR-013, FR-016) verifiable via integration tests. |
| Performance Efficiency     | Maintenance section loads within the existing page-response budget (< 2 s per constitution). Export generation must not regress beyond the existing time budget with up to ~500 water change records per tank.                                      |
| Compatibility              | AI export extension is additive only; `sabiSchemaVersion` incremented, existing fields unchanged (FR-015). Multi-platform: ARM64 and AMD64 Docker images. New standalone Maintenance page added; existing views unchanged.                                              |
| Usability                  | i18n in all 5 languages + fallback (FR-017). WCAG 2.1 AA contrast for all new elements. Recording a water change reachable within 3 interactions (open section, enter date + percentage, save). Localised labels for all enum values.                     |
| Reliability                | Validation enforced client- and server-side (percentage ranges, date not in future, interval ranges, text length limits). Transactional saves; no partial state on failure. Tank deletion cascades maintenance data consistently (FR-019).                |
| Security                   | Authentication required for all write operations (FR-018). Strict user isolation: no access to another user's maintenance data (FR-018). No PII in the export (FR-014). No dedicated rate limiting beyond JWT authentication, consistent with existing endpoints. |
| Maintainability            | Schema changes via Flyway migration scripts only (new tables for maintenance profile and water change records); existing migrations immutable. At least one integration test per P1–P4 user story.                                                    |
| Portability                | No new external runtime dependencies. Photo/file storage not involved. ARM/AMD64 Docker images remain compatible; no Ansible or deployment changes required.                                                                                                  |

### Key Entities

- **TankMaintenanceProfile**: The per-aquarium maintenance configuration (one profile per aquarium). Key attributes: regular water exchange interval (`intervalDays`, `percentage` — both optional), activated carbon setting (`enabled`, `mode` ∈ {`CONTINUOUS`, `INTERVAL`}, `replacementIntervalDays`).
- **WaterChangeRecord**: A single recorded water change event. Key attributes: aquarium reference, date (date-only, no time component), percentage of tank volume (1–100), optional note (max 200 characters).
- ~~**TankSupplement**~~ — **REMOVED**: no supplement entity; supplements are documented in the existing Dosing feature (see Clarifications 2026-10-26).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can record a water change in under 30 seconds (at most 3 interactions) from opening the Maintenance section.
- **SC-002**: 100 % of AI exports for aquariums with maintenance data contain the complete `maintenance` block with values matching what was entered — verified by automated export tests.
- **SC-003**: AI export generation time does not measurably regress (stays within the existing 3-second budget for typical data volumes) for a tank with up to 500 water change records.
- **SC-004**: In a manual AI-readability check (paste the export into an AI chatbot and ask "How often and to which percentage is water changed?"), the AI answers correctly in 100 % of trials for tanks with a set regular interval or at least three recorded water changes.
- **SC-005**: All new UI strings are present in all six message bundle files — verified by an i18n completeness check with zero missing keys.
- **SC-006**: Cross-user access attempts to another user's maintenance data are rejected in 100 % of automated security tests.

---

## Assumptions

- Maintenance data is scoped per aquarium (not a user-wide setting), consistent with all other Sabi tank data.
- Water change percentage is relative to the tank's volume; absolute volume in liters is not tracked in v1.
- Water change records carry date-only precision (no time component); the date defaults to "today" in the form.
- Supplements are documented exclusively via the existing Dosing feature; this feature adds no supplement data model (see Clarifications 2026-10-26).
- No reminders or notifications are included; users manage their routines themselves.
- The regular water exchange interval is informational (the intended routine); the system does not derive or schedule anything from it.
- The existing AI export endpoint and its metadata block are reused; only the per-aquarium content is extended.
- Free-text fields (water change notes) are stored exactly as entered; no normalisation or translation is applied.
- The `maintenance` block structure in FR-013 is the contract for AI consumption; field names are English to maximise chatbot interpretation accuracy (consistent with spec `001` clarifications).

---

*This specification was created for branch `007-water-maintenance-tracking`. Next step: `/speckit.plan` to break this into implementation tasks.*
