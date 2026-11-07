/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Relogin Re-Entry (007-water-maintenance-tracking, Defekt 2026-11-07)
 *
 * Gemeldeter Defekt: Nach Logout + Login werden bereits erfasste Daten nicht in der
 * UI für Dosing noch für die Wartung gezeigt.
 *
 * Root Cause: Die Tank-Auswahl lebt in der session-scoped Bean
 * UserSession.lastSelectedTankId. Logout invalidiert die Session (logout.xhtml →
 * endSession()), und bei Nutzern mit mehreren Tanks existiert kein Fallback-Select
 * (nur Single-Tank-Auto-Select) → Placeholder + leere Listen, obwohl die Daten
 * in der DB vorhanden sind.
 *
 * Dieser Test assertet das BEAUGTETE Verhalten (FAIL vor dem Fix, PASS danach):
 *  1. Session 1: Dosing-Eintrag + Wasserwechsel-Event auf dem ersten Tank
 *     (Nano-Reef, id=1 — niedrigste ID) anlegen und Sichtbarkeit verifizieren.
 *  2. Logout über das Header-Menü (Session wird auf /logout.xhtml invalidiert).
 *  3. Session 2 (frischer Login): dosingView + maintenanceView öffnen OHNE
 *     manuelle Tank-Auswahl → die in Session 1 angelegten Einträge müssen sichtbar sein.
 *
 * Rules (AGENTS.md): REAL clicks only (no force, no page.evaluate clicks),
 * explicit visibility assertions, cleanup of created records.
 *
 * Notes:
 *  - JSF component client-IDs are form-prefixed (dosingForm:xxx) → suffix matchers [id$=":xxx"].
 *  - The UI language is per-user (not the browser locale) → text assertions are bilingual
 *    (de/en regex).
 *  - Dosing tank selector: PF p:selectOneMenu → panel dance (see e2e/E2E_NOTES.md).
 *    Autocomplete needs pressSequentially (NOT fill) so the keyup fires the PF query.
 *  - Maintenance tank selector: NATIVE <select> (h:selectOneMenu) → selectOption().
 *  - Logout: p:menuButton (Hamburger) im Header → Menüpunkt "Logout"
 *    (hartkodiert englisch in header.xhtml).
 *  - recordedOn (Dosing) und changedOn (Wasserwechsel) sind vom Bean vorbefüllt
 *    (jetzt bzw. heute) und readonly — keine Interaktion nötig.
 *
 * Testuser: sabi@bluewhale.de / clibanarius (Tanks: Nano-Reef id=1, Freshwater id=2)
 * App: http://localhost:8088
 */
import { test, expect, Page } from '@playwright/test';

// ──────────────────────────────────────────────────────────────
// Constants: bilingual labels (de/en)
// ──────────────────────────────────────────────────────────────
const RE = {
  manualAddition: /Manual addition|Manuelle Zugabe/,
};

// ──────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────

async function login(page: Page, username = 'sabi@bluewhale.de', password = 'clibanarius') {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

/** Wartet auf die PF-AJAX-POST-Antwort der Dosing-View. */
function waitForDosingAjax(page: Page) {
  return page.waitForResponse(
    (resp) => resp.request().method() === 'POST' && resp.url().includes('dosingView.xhtml'),
    { timeout: 15_000 }
  );
}

/** Wartet auf die PF-AJAX-POST-Antwort der Maintenance-View. */
function waitForMaintenanceAjax(page: Page) {
  return page.waitForResponse(
    (resp) => resp.request().method() === 'POST' && resp.url().includes('maintenanceView.xhtml'),
    { timeout: 15_000 }
  );
}

/**
 * Wählt eine Option im PF p:selectOneMenu der Dosing-View (Panel-Tanz, siehe
 * e2e/E2E_NOTES.md). Idempotent: Quelle der Wahrheit ist das versteckte <select>
 * (PF 15: Client-ID mit _input-Suffix) — bei bereits gewähltem Wert kein POST.
 */
async function selectPFOption(page: Page, fieldSuffix: string, optionValue: string, optionLabel: RegExp) {
  const menu = page.locator(`#dosingForm .ui-selectonemenu:has(select[id$=":${fieldSuffix}_input"])`).first();
  await expect(menu).toBeVisible({ timeout: 8_000 });
  const select = menu.locator('select');
  if ((await select.inputValue()) === optionValue) {
    return; // bereits ausgewählt — Wiederwahl würde keinen Request auslösen
  }
  const label = menu.locator('.ui-selectonemenu-label');
  await expect(label).toBeVisible();
  await label.click(); // öffnet das Panel (reines Client-Side-Rendering, kein POST)
  const panel = page.locator(`[id$=":${fieldSuffix}_panel"]`);
  await expect(panel).toBeVisible({ timeout: 8_000 });
  const item = panel.locator('li.ui-selectonemenu-item').filter({ hasText: optionLabel });
  await expect(item).toBeVisible();
  await Promise.all([waitForDosingAjax(page), item.click()]);
  await page.waitForLoadState('networkidle');
  expect(await select.inputValue(), `ausgewählter Wert von ${fieldSuffix}`).toBe(optionValue);
}

/**
 * Wählt einen Tank im nativen <select> der Maintenance-View (h:selectOneMenu).
 * Idempotent: Wiederwahl des bereits gewählten Werts feuert kein change-Event.
 */
async function selectTank(page: Page, tankName = 'Nano-Reef') {
  const select = page.locator('#maintenanceForm select[id$=":aquarium"]');
  await expect(select).toBeAttached({ timeout: 8_000 });
  const desiredValue = await select.locator('option').filter({ hasText: tankName }).getAttribute('value');
  if ((await select.inputValue()) === desiredValue) {
    return; // Tank bereits ausgewählt — kein POST nötig
  }
  await Promise.all([waitForMaintenanceAjax(page), select.selectOption({ value: desiredValue })]);
  await page.waitForLoadState('networkidle');
}

/** Logout über das Header-Menü (p:menuButton → Menüpunkt "Logout").
 *  Die Session wird beim Rendern von /logout.xhtml invalidiert (f:viewAction → endSession). */
async function logout(page: Page) {
  const menuButton = page.locator('.ui-menubutton button').first();
  await expect(menuButton).toBeVisible({ timeout: 8_000 });
  await menuButton.click();
  const logoutItem = page.locator('.ui-menuitem a').filter({ hasText: 'Logout' });
  await expect(logoutItem).toBeVisible({ timeout: 8_000 });
  await logoutItem.click();
  await page.waitForURL(/logout\.xhtml/, { timeout: 15_000 });
}

// ──────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────

test.describe('Relogin Re-Entry', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });

  test('Nach Logout/Login werden Dosing- und Maintenance-Daten wieder gezeigt', async ({ page }) => {
    test.setTimeout(120_000);

    const dosingName = `E2E-Relogin-Dosing-${Date.now()}`;
    const wcNote = `E2E-Relogin-WC-${Date.now()}`;

    // ── Session 1: Datenerfassung auf dem ersten Tank (Nano-Reef, id=1) ──
    await login(page);

    // 1. Dosing-Eintrag
    await page.goto('/secured/dosingView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#dosingForm')).toBeAttached({ timeout: 8_000 });
    await selectPFOption(page, 'aquarium', '1', /Nano-Reef/);

    const productName = page.locator('[id$=":productName_input"]');
    const amount = page.locator('[id$=":amount"]');
    const amountUnit = page.locator('[id$=":amountUnit"]');

    await productName.pressSequentially(dosingName); // keyup feuert die PF-Query
    await selectPFOption(page, 'dosingType', 'MANUAL_ADDITION', RE.manualAddition);
    await amount.fill('5');
    await amountUnit.fill('ml');

    const saveButton = page.locator('#dosingForm button').filter({ has: page.locator('.pi-check') });
    await expect(saveButton).toBeVisible();
    await Promise.all([waitForDosingAjax(page), saveButton.click()]);
    await page.waitForLoadState('networkidle');

    const dosingTable = page.locator('[id$=":dosingTable"]');
    await expect(dosingTable.locator('tbody tr').filter({ hasText: dosingName }))
      .toBeVisible({ timeout: 8_000 });

    // 2. Wasserwechsel-Event (Maintenance)
    await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#maintenanceForm')).toBeAttached({ timeout: 8_000 });
    await selectTank(page, 'Nano-Reef');

    const pctInput = page.locator('[id$=":percentage"]');
    const noteInput = page.locator('[id$=":note"]');
    await pctInput.fill('20');
    await noteInput.fill(wcNote);

    const saveRecord = page.locator('[id$=":saveWaterChangeButton"]');
    await expect(saveRecord).toBeVisible();
    await Promise.all([waitForMaintenanceAjax(page), saveRecord.click()]);
    await page.waitForLoadState('networkidle');

    const wcTable = page.locator('[id$=":waterChangeTable"]');
    await expect(wcTable.locator('tbody tr').filter({ hasText: wcNote }))
      .toBeVisible({ timeout: 8_000 });

    // ── Logout (Session-Invalidierung) ──
    await logout(page);

    // ── Session 2: frischer Login → Daten müssen OHNE manuelle Tank-Auswahl
    //              wieder sichtbar sein (Defekt: Placeholder + leere Listen) ──
    await login(page);

    // Dosing: Eintrag aus Session 1 sichtbar
    await page.goto('/secured/dosingView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#dosingForm')).toBeAttached({ timeout: 8_000 });
    await expect(page.locator('[id$=":dosingTable"] tbody tr').filter({ hasText: dosingName }))
      .toBeVisible({ timeout: 8_000 });

    // Maintenance: Eintrag aus Session 1 sichtbar
    await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#maintenanceForm')).toBeAttached({ timeout: 8_000 });
    await expect(page.locator('[id$=":waterChangeTable"] tbody tr').filter({ hasText: wcNote }))
      .toBeVisible({ timeout: 8_000 });

    // ── Cleanup: beide Einträge per UI löschen (Session 2) ──

    // Wasserwechsel-Event löschen (Seite ist bereits geöffnet)
    const wcRow = page.locator('[id$=":waterChangeTable"] tbody tr').filter({ hasText: wcNote });
    await expect(wcRow).toBeVisible({ timeout: 8_000 });
    const wcDelete = wcRow.locator('button').filter({ has: page.locator('.pi-trash') });
    await expect(wcDelete).toBeVisible();
    await Promise.all([waitForMaintenanceAjax(page), wcDelete.click()]);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('[id$=":waterChangeTable"] tbody tr').filter({ hasText: wcNote }))
      .toHaveCount(0);

    // Dosing-Eintrag löschen
    await page.goto('/secured/dosingView.xhtml', { waitUntil: 'networkidle' });
    const dosingRow = page.locator('[id$=":dosingTable"] tbody tr').filter({ hasText: dosingName });
    await expect(dosingRow).toBeVisible({ timeout: 8_000 });
    const dosingDelete = dosingRow.locator('button').filter({ has: page.locator('.pi-trash') });
    await expect(dosingDelete).toBeVisible();
    await Promise.all([waitForDosingAjax(page), dosingDelete.click()]);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('[id$=":dosingTable"] tbody tr').filter({ hasText: dosingName }))
      .toHaveCount(0);
  });

});
