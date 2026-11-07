/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Dosing View (007-water-maintenance-tracking, issues 4 & 5)
 *
 * Covers:
 *  1. Re-entry — tank selection is persisted in the session and restored on reload
 *     (issue 5: "re-entry shows empty lists" for dosings).
 *  2. Autocomplete — product names, categories and dosing methods offer previously
 *     entered values (from all of the user's tanks) as suggestions; free text is
 *     always allowed (issue 4).
 *
 * Rules (AGENTS.md): REAL clicks only (no force, no page.evaluate clicks),
 * explicit visibility assertions, cleanup of created records.
 *
 * Notes:
 *  - JSF component client-IDs are form-prefixed (dosingForm:xxx) → suffix matchers [id$=":xxx"].
 *  - The UI language is per-user (not the browser locale) → text assertions are bilingual
 *    (de/en regex), following the convention of aquariumEvents.spec.ts.
 *  - Tank and type selectors are PF p:selectOneMenu → panel dance (click the visible
 *    .ui-selectonemenu-label, pick li.ui-selectonemenu-item in the body-appended panel).
 *    Source of truth for the current value is the hidden <select> (see e2e/E2E_NOTES.md).
 *  - Autocomplete: use pressSequentially (NOT fill) so the keyup event fires the PF query.
 *    The suggestion panel is appended to <body> (PF 15) → generic locator
 *    .ui-autocomplete-panel .ui-autocomplete-item.
 *  - The save/new-record buttons have no explicit id → located by their icon
 *    (pi-check / pi-refresh) within #dosingForm.
 *
 * Testuser: sabi@bluewhale.de / clibanarius (Tanks: Nano-Reef id=1, Freshwater id=2)
 * App: http://localhost:8088
 */
import { test, expect, Page } from '@playwright/test';

// ──────────────────────────────────────────────────────────────
// Constants: bilingual labels (de/en)
// ──────────────────────────────────────────────────────────────
const RE = {
  selectPlaceholder: /Select one|Auswählen/,
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

/** Wartet auf die PF-AJAX-POST-Antwort der Dosing-View (muss VOR dem Klick
 *  registriert werden, damit das @form-Re-Render abgeschlossen ist). */
function waitForAjaxPost(page: Page) {
  return page.waitForResponse(
    (resp) => resp.request().method() === 'POST' && resp.url().includes('dosingView.xhtml'),
    { timeout: 15_000 }
  );
}

/**
 * Wählt eine Option im PF p:selectOneMenu (Panel-Tanz, siehe e2e/E2E_NOTES.md):
 * sichtbare Label anklicken → Panel (body-appended, id-Suffix :<field>_panel) öffnet sich →
 * Item per Label-Regex klicken. Der Option-Wert ist locale-unabhängig (Tank-ID / Enum-Name).
 *
 * Idempotent: Quelle der Wahrheit ist das versteckte <select> — bei bereits gewähltem
 * Wert wird kein Panel geöffnet und kein Request ausgelöst.
 */
async function selectPFOption(page: Page, fieldSuffix: string, optionValue: string, optionLabel: RegExp) {
  // PF 15: das versteckte <select> hat den Client-ID mit _input-Suffix (dosingForm:aquarium_input)
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
  await Promise.all([waitForAjaxPost(page), item.click()]);
  await page.waitForLoadState('networkidle');
  expect(await select.inputValue(), `ausgewählter Wert von ${fieldSuffix}`).toBe(optionValue);
}

/** Erfolgsmeldung (FacesMessage SEVERITY_INFO) im dosingForm. */
async function expectSuccessMessage(page: Page) {
  await expect(
    page.locator('[id$=":dosingMessages"] .ui-messages-info').first()
  ).toBeVisible({ timeout: 8_000 });
}

/** Speichern-Button (kein explizites id — per Icon pi-check im Form lokalisieren). */
function saveButton(page: Page) {
  return page.locator('#dosingForm button').filter({ has: page.locator('.pi-check') });
}

/** Neuer-Eintrag-Button (Icon pi-refresh). */
function newRecordButton(page: Page) {
  return page.locator('#dosingForm button').filter({ has: page.locator('.pi-refresh') });
}

// ──────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────

test.describe('Dosing View', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto('/secured/dosingView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#dosingForm')).toBeAttached({ timeout: 8_000 });
  });

  // ── 1. Re-Entry: Tank-Auswahl persistiert über Reload ─────────

  test('Re-Entry: Tank-Auswahl bleibt nach Reload erhalten und lädt die Liste', async ({ page }) => {
    const menu = page.locator('#dosingForm .ui-selectonemenu:has(select[id$=":aquarium_input"])').first();
    const label = menu.locator('.ui-selectonemenu-label');

    // Frische Session, 2 Tanks → kein Auto-Select, Placeholder sichtbar
    await expect(label).toBeVisible({ timeout: 8_000 });
    await expect(label).toHaveText(RE.selectPlaceholder);

    // Freshwater (id=2) auswählen
    await selectPFOption(page, 'aquarium', '2', /Freshwater/);
    await expect(label).toContainText('Freshwater');

    // Reload → Tank wird aus der Session wiederhergestellt (Issue 5)
    await page.reload({ waitUntil: 'networkidle' });
    const labelAfterReload = page.locator('#dosingForm .ui-selectonemenu:has(select[id$=":aquarium_input"])')
      .first().locator('.ui-selectonemenu-label');
    await expect(labelAfterReload).toContainText('Freshwater', { timeout: 8_000 });
    await expect(labelAfterReload).not.toHaveText(RE.selectPlaceholder);

    // Dosing-Tabelle rendert für den wiederhergestellten Tank
    await expect(page.locator('[id$=":dosingTable"]')).toBeVisible({ timeout: 8_000 });
  });

  // ── 2. Autocomplete + Freitext (Issue 4) ──────────────────────

  test('Autocomplete bietet gespeicherte Werte an; Freitext bleibt erlaubt', async ({ page }) => {
    test.setTimeout(60_000);

    const productName = page.locator('[id$=":productName_input"]');
    const amount = page.locator('[id$=":amount"]');
    const amountUnit = page.locator('[id$=":amountUnit"]');
    const table = page.locator('[id$=":dosingTable"]');

    // Tank auswählen (Freshwater, id=2)
    await selectPFOption(page, 'aquarium', '2', /Freshwater/);

    // 1. Neue Dosierung mit EINDEUTIGEM Produkt (Freitext, noch keine Vorschläge vorhanden)
    const uniqueProduct = `E2E-Produkt-${Date.now()}`;
    await productName.pressSequentially(uniqueProduct); // keyup feuert die PF-Query (fill würde sie nicht)
    await selectPFOption(page, 'dosingType', 'MANUAL_ADDITION', RE.manualAddition);
    await amount.fill('5');
    await amountUnit.fill('ml');

    await expect(saveButton(page)).toBeVisible();
    await Promise.all([waitForAjaxPost(page), saveButton(page).click()]);
    await page.waitForLoadState('networkidle');
    await expectSuccessMessage(page);

    const savedRow = table.locator('tbody tr').filter({ hasText: uniqueProduct });
    await expect(savedRow).toBeVisible({ timeout: 8_000 });

    // 2. Formular zurücksetzen
    await Promise.all([waitForAjaxPost(page), newRecordButton(page).click()]);
    await page.waitForLoadState('networkidle');

    // 3. Autocomplete: Präfix eingeben → das gerade gespeicherte Produkt wird vorgeschlagen
    //    (loadSuggestions() läuft nach dem Speichern erneut — Vorschläge sind aktuell)
    const prefix = uniqueProduct.slice(0, 12); // "E2E-Produkt-"
    await productName.pressSequentially(prefix);
    // PF rendert EINE Panel pro AutoComplete-Feld (alle im DOM, meist hidden) →
    // globaler .ui-autocomplete-Locator bricht in Strict Mode. Per Feld scopen:
    const panel = page.locator('[id$=":productName_panel"]');
    await expect(panel).toBeVisible({ timeout: 8_000 });
    const suggestion = panel.locator('.ui-autocomplete-item').filter({ hasText: uniqueProduct });
    await expect(suggestion).toBeVisible();

    // Vorschlag anklicken → Input wird mit dem Vollwert befüllt
    await suggestion.click();
    await expect(productName).toHaveValue(uniqueProduct);

    // 4. Freitext bleibt erlaubt: neues, noch nie eingetragenes Produkt speichern
    const freeTextProduct = `E2E-Freetext-${Date.now()}`;
    await productName.fill('');
    await productName.pressSequentially(freeTextProduct);
    await amount.fill('2');
    await amountUnit.fill('g');

    await Promise.all([waitForAjaxPost(page), saveButton(page).click()]);
    await page.waitForLoadState('networkidle');
    await expectSuccessMessage(page);

    const freeTextRow = table.locator('tbody tr').filter({ hasText: freeTextProduct });
    await expect(freeTextRow).toBeVisible({ timeout: 8_000 });

    // ── Cleanup: beide Einträge löschen ──
    for (const name of [freeTextProduct, uniqueProduct]) {
      const row = table.locator('tbody tr').filter({ hasText: name });
      await expect(row).toBeVisible({ timeout: 8_000 });
      const deleteButton = row.locator('button').filter({ has: page.locator('.pi-trash') });
      await expect(deleteButton).toBeVisible();
      await Promise.all([waitForAjaxPost(page), deleteButton.click()]);
      await page.waitForLoadState('networkidle');
      await expect(table.locator('tbody tr').filter({ hasText: name })).toHaveCount(0);
    }
  });

});
