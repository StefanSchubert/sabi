# Quickstart Validation: Water Maintenance Tracking

**Feature**: `007-water-maintenance-tracking` | **Date**: 2026-10-26

Runnable validation scenarios proving the feature works end-to-end. Implementation details live in `tasks.md` and the implementation phase; this file is the run/verify guide.

## Prerequisites

- Local dev stack up: `cd devops/sabi_docker_sdk && docker compose up -d` (MariaDB + backend + webclient).
- After the new Flyway migration: run `bash server_redeploy.sh --flyway` (script applies pending migrations, see AGENTS.md redeploy checklist).
- Test user `sabi@bluewhale.de` / `clibanarius` with tanks "Nano-Reef" (id 1) and "Freshwater" (id 2) — same fixtures the E2E suite uses.
- Webclient reachable at `http://localhost:8088` (Playwright baseURL).

## Scenario 1 — Water changes & regular interval (US1, P1)

1. Log in at `http://localhost:8088/login.xhtml`, open the hamburger menu → **Maintenance** (new entry, water-drop icon).
2. Select "Nano-Reef" in the tank selector → all three sub-sections render (Water Changes, Supplements & dosing, Activated Carbon).
3. **Regular interval**: enter 21 days / 25 %, save → section shows the routine. Clear it → "no regular interval", records untouched (US1-5).
4. **Record a water change**: date defaults to today, 25 %, optional note → save → appears in the list (newest first) with date, percentage, note (US1-2).
5. **Back-date**: add a record with a past date → accepted, sorted chronologically (US1-7).
6. **Validation**: percentage 0 / 101 → inline error, nothing saved; future date → inline error (US1-3); note > 200 chars → inline error.
7. **Edit & delete**: pencil on a record → change percentage → save; trash → confirm removal from the list (US1-6).

## Scenario 2 — Supplements cross-link (US2, P2)

1. On the Maintenance page for "Nano-Reef", verify the **Supplements & dosing** section shows localised hint text and a link/button to the Dosing feature.
2. Click it → `dosingView.xhtml` opens **with "Nano-Reef" pre-selected** (query parameter `?aquariumId=1`; validated server-side against the user's own tanks).
3. Record a dosing entry there (e.g. Calcium, automatic) → appears in the Dosing list as before — no data-model change (US2-3).

## Scenario 3 — Activated carbon (US3, P3)

1. Enable **Activated carbon**, mode **Interval**, replacement every 28 days → save → displayed (enabled, INTERVAL, 28 d) (US3-1).
2. Interval mode without a replacement interval → inline validation error, nothing saved (US3-2).
3. Switch to **Continuous** → save → no replacement interval required or shown (US3-3).
4. Disable carbon → mode and interval cleared from the display; section shows "not used" (US3-4/5).

## Scenario 4 — AI-JSON export (US4, P4)

1. With the data from Scenarios 1+3 in place, open User Profile → download the AI export (`sabi-reef-data-YYYY-MM-DD.json`).
2. Verify per the contract in [contracts/ai-export-maintenance-block.md](contracts/ai-export-maintenance-block.md):
   - `_meta.sabiSchemaVersion` == `"1.1"`.
   - Every aquarium block contains `maintenance` with `regularInterval`, `events[]` (type/date/percentage/note, newest first; `percentage` is `null` for `CARBON_REPLACEMENT`) and `activatedCarbon` — values exactly as entered.
   - An aquarium **without** maintenance data still has the full block with `regularInterval: null`, `events: []`, carbon disabled (FR-016).
   - No `supplements` field; dosing data still in the top-level `dosings` array (Clarification #3).
   - All existing fields unchanged compared to a pre-feature export (additive only, FR-015).
3. Paste into an AI chatbot: "How often and to which percentage is water changed?" → correct answer from the block (SC-004).

## Scenario 5 — Security & deletion (FR-018, FR-019)

```bash
# Cross-user read attempt (user B's token against user A's tank 1) → expect 403
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer <userB-token>" \
  http://localhost:8081/api/tank/1/maintenance

# Same for PUT interval / carbon and POST/PUT/DELETE events → 403
```

- Delete the "Freshwater" tank in the UI → its maintenance profile + maintenance event rows are gone (verify via `GET /api/tank/2/maintenance` → 403, or DB: no rows in `aquarium_maintenance_profile` / `aquarium_maintenance` with `aquarium_id = 2`).

## Automated validation

```bash
# Backend unit + module tests (service tests, controller ITs, export tests)
cd sabi-server && mvn test -Dtest='MaintenanceServiceTest,MaintenanceControllerIT,UserProfileControllerTest,ReefDataExportServiceTest'

# Full backend suite (CI gate — constitution V)
cd sabi-server && mvn test

# i18n completeness (SC-005): every new maintenance.* key present in all 6 bundles
python3 /tmp/check_i18n_maintenance.py   # see tasks.md for the script; expect zero missing keys

# E2E (Playwright, real clicks only — AGENTS.md rules)
cd e2e && npx playwright test maintenanceFlow.spec.ts
```

**Expected**: all green; E2E additionally asserts no `???` / raw i18n keys on the page and CSS visibility of all interactive elements (computed style width/height > 0).
