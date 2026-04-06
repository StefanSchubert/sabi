/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Fish Stock View (fishStockView.xhtml)
 *
 * Testuser: sabi@bluewhale.de / clibanarius  (Passwort: 'clibanarius')
 * Hat 2 Aquarien: "Nano-Reef" (id=1) und "Freshwater" (id=2)
 * App läuft auf http://localhost:8088
 *
 * Hinweis: PrimeFaces selectOneMenu rendert IDs als JSF-Composite-IDs (z.B.
 * "tankSelectorForm:tankSelector") — die sich bei jeder Code-Änderung ändern können.
 * Daher verwenden wir klassen-basierte Selektoren statt fragiler ID-Selektoren.
 */
import { test, expect, Page } from '@playwright/test';
import path from 'path';

// ──────────────────────────────────────────────────────────────
// Helper: Login
// defaultSuccessUrl erzeugt HTTP 302 Redirect → Browser macht
// frischen GET auf /secured/userportal.xhtml.
// ──────────────────────────────────────────────────────────────
async function login(page: Page, username = 'sabi@bluewhale.de', password = 'clibanarius') {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

// ──────────────────────────────────────────────────────────────
// Helper: PrimeFaces selectOneMenu öffnen und Option wählen
// ──────────────────────────────────────────────────────────────
async function selectPrimeFacesOption(page: Page, optionText: string) {
  const trigger = page.locator('.ui-selectonemenu .ui-selectonemenu-trigger').first();
  await trigger.click();
  const panel = page.locator('.ui-selectonemenu-panel').first();
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  await panel.locator('li.ui-selectonemenu-item').filter({ hasText: optionText }).click();
}

// ──────────────────────────────────────────────────────────────
// Helper: Wait for AJAX response from fishStockView
// ──────────────────────────────────────────────────────────────
async function waitForFishStockAjax(page: Page) {
  await page.waitForResponse(resp =>
      resp.url().includes('fishStockView') && resp.status() === 200
      && resp.request().method() === 'POST',
  );
  await page.waitForLoadState('networkidle');
}

// ──────────────────────────────────────────────────────────────
// Helper: Aquarium auswählen und "Fisch hinzufügen" Dialog öffnen
// ──────────────────────────────────────────────────────────────
async function openAddFishDialog(page: Page) {
  await Promise.all([
    waitForFishStockAjax(page),
    selectPrimeFacesOption(page, 'Nano-Reef'),
  ]);
  const addButton = page.locator('#fishStockForm button').filter({ hasText: /hinzuf|add/i }).first();
  await expect(addButton).toBeEnabled({ timeout: 10_000 });
  await addButton.click();
  const dialog = page.locator('.ui-dialog').filter({ hasText: /hinzuf|add fish/i }).first();
  await expect(dialog).toBeVisible({ timeout: 10_000 });
  return dialog;
}

// ──────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────
test.describe('Fish Stock View', () => {

  // PrimeFaces-Dialoge mit vielen Feldern übersteigen das Standard-Viewport.
  // → Viewport auf 1280x1024 vergrößern.
  test.use({ viewport: { width: 1280, height: 1024 } });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto('/secured/fishStockView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#tankSelectorForm')).toBeAttached({ timeout: 8_000 });
  });

  // ── Basis-Tests ────────────────────────────────────────────

  test('fishStockView zeigt kein ungültiges i18n-Label', async ({ page }) => {
    await expect(page.locator('body')).not.toContainText('common.please.select');
    await expect(page.locator('body')).not.toContainText('common.select.l');
  });

  test('Aquarium-Dropdown zeigt übersetzten Placeholder', async ({ page }) => {
    const dropdownLabel = page.locator('.ui-selectonemenu-label').first();
    await expect(dropdownLabel).toBeVisible();
    const labelText = await dropdownLabel.textContent();
    expect(labelText).not.toContain('common.');
    expect(labelText).toContain('Ausw');
  });

  test('Dropdown listet nur eigene Aquarien auf', async ({ page }) => {
    await page.locator('.ui-selectonemenu .ui-selectonemenu-trigger').first().click();
    const panel = page.locator('.ui-selectonemenu-panel').first();
    await panel.waitFor({ state: 'visible', timeout: 8_000 });
    await expect(panel.locator('li').filter({ hasText: 'Nano-Reef' })).toBeVisible();
    await expect(panel.locator('li').filter({ hasText: 'Freshwater' })).toBeVisible();
    const items = await panel.locator('li.ui-selectonemenu-item').count();
    expect(items).toBe(3);
    await page.keyboard.press('Escape');
  });

  test('Fischbestand-Form wird nach Aquarium-Auswahl per AJAX aktualisiert', async ({ page }) => {
    await expect(page.locator('#fishStockForm')).toBeAttached();
    const addButton = page.locator('#fishStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeDisabled();

    await Promise.all([
      page.waitForResponse(resp =>
          resp.url().includes('fishStockView') && resp.status() === 200
          && resp.request().method() === 'POST'),
      selectPrimeFacesOption(page, 'Nano-Reef'),
    ]);
    await page.waitForLoadState('networkidle');

    const addButtonAfter = page.locator('#fishStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButtonAfter).toBeEnabled({ timeout: 10_000 });
  });

  test('Menüpunkt Fischbestand ist im Navigationsmenü', async ({ page }) => {
    await page.goto('/secured/userportal.xhtml', { waitUntil: 'networkidle' });
    await page.locator('.ui-menubutton button').first().click();
    const fishMenuItem = page.locator('.ui-menu-list .ui-menuitem-link')
        .filter({ hasText: 'Fischbestand' });
    await expect(fishMenuItem).toBeVisible({ timeout: 5_000 });
  });

  // ── Fisch-Hinzufügen-Dialog ────────────────────────────────

  test('Fisch-hinzufügen-Dialog öffnet sich mit allen Formularfeldern', async ({ page }) => {
    const dialog = await openAddFishDialog(page);

    // Pflichtfelder
    await expect(dialog.locator('[id$="commonName"]')).toBeVisible();
    await expect(dialog.locator('[id$="addedOn_input"]')).toBeVisible();

    // Optionale Felder
    await expect(dialog.locator('[id$="nickname"]')).toBeVisible();
    await expect(dialog.locator('[id$="scientificName"]')).toBeVisible();
    await expect(dialog.locator('[id$="externalRefUrl"]')).toBeVisible();
    await expect(dialog.locator('[id$="observedBehavior"]')).toBeVisible();

    // Foto-Upload-Feld (mode="simple" → rendert als input[type=file])
    await expect(dialog.locator('input[type="file"]')).toBeAttached();

    // Katalog-Suche (AutoComplete-Input)
    await expect(dialog.locator('[id$="catalogueSearch_input"]')).toBeVisible();

    // Katalog-Vorschlags-Link
    await expect(dialog.locator('a').filter({ hasText: /vorschlagen|propose/i })).toBeVisible();

    // Buttons: Speichern + Abbruch
    await expect(dialog.locator('button').filter({ hasText: /speicher|save/i })).toBeVisible();
    await expect(dialog.locator('button').filter({ hasText: /abbruch|cancel/i })).toBeAttached();

    // Dialog per Escape schließen (closeOnEscape="true")
    await page.keyboard.press('Escape');
    await expect(dialog).not.toBeVisible({ timeout: 5_000 });
  });

  // ── Fisch anlegen und verifizieren ──────────────────────────

  test('Fisch anlegen — Pflichtfelder ausfüllen, speichern, in Tabelle sichtbar', async ({ page }) => {
    const fishName = `E2E-Testfisch-${Date.now()}`;

    // 1. Dialog öffnen (inkl. Aquarium-Auswahl)
    const dialog = await openAddFishDialog(page);

    // 2. Pflichtfelder ausfüllen
    await dialog.locator('[id$="commonName"]').fill(fishName);

    const dateInput = dialog.locator('[id$="addedOn_input"]');
    await dateInput.click();
    await dateInput.clear();
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    await dateInput.fill(`${dd}.${mm}.${yyyy}`);
    // Fokus weg vom DatePicker → Overlay schließen
    await dialog.locator('[id$="commonName"]').click();
    await page.waitForTimeout(500);

    // 3. Speichern via PrimeFaces Widget-API (Button kann hinter DatePicker-Overlay liegen)
    await page.evaluate(() => {
      const btn = document.querySelector('[id$="fishEntryForm"] button[class*="ui-button"]:not([class*="secondary"])') as HTMLElement;
      if (btn) btn.click();
    });
    await page.waitForLoadState('networkidle');

    // 4. Dialog sollte sich geschlossen haben
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    // 5. Fisch-Name in der "Aktuell im Becken"-Tabelle sichtbar
    await expect(page.locator('#fishStockForm')).toContainText(fishName, { timeout: 10_000 });

    // ── Cleanup: Fisch löschen ──
    const fishRow = page.locator('tr').filter({ hasText: fishName }).first();
    if (await fishRow.isVisible()) {
      await fishRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
      await expect(page.locator('#fishStockForm')).not.toContainText(fishName, { timeout: 10_000 });
    }
  });

  // ── Foto-Upload ─────────────────────────────────────────────

  test('Foto-Upload: Datei kann ausgewählt werden und accept-Attribut stimmt', async ({ page }) => {
    const dialog = await openAddFishDialog(page);

    // File-Input finden
    const fileInput = dialog.locator('input[type="file"]');
    await expect(fileInput).toBeAttached();

    // accept-Attribut prüfen (nur Bildformate)
    const accept = await fileInput.getAttribute('accept');
    expect(accept).toContain('image/jpeg');
    expect(accept).toContain('image/png');

    // Testbild setzen
    const testImagePath = path.resolve(__dirname, 'fixtures', 'test-fish.png');
    await fileInput.setInputFiles(testImagePath);

    // Kein Fehler in der Message-Area
    const errorMessages = dialog.locator('.ui-messages-error');
    await expect(errorMessages).not.toBeVisible({ timeout: 3_000 });

    // Dialog per Escape schließen
    await page.keyboard.press('Escape');
  });

  // ── Katalog-Vorschlag ──────────────────────────────────────

  test('Katalog-Vorschlag: Link navigiert zur Proposal-Seite mit i18n-Tabs', async ({ page }) => {
    const dialog = await openAddFishDialog(page);

    // "Neuen Katalogeintrag vorschlagen"-Link finden
    const proposalLink = dialog.locator('a').filter({ hasText: /vorschlagen|propose/i });
    await expect(proposalLink).toBeVisible();

    // href prüfen
    const href = await proposalLink.getAttribute('href');
    expect(href).toContain('fishCatalogueProposalForm');

    // Link klicken → Proposal-Seite
    await proposalLink.click();
    await page.waitForLoadState('networkidle');

    // Proposal-Formular vorhanden (JSF-ID könnte prefixed sein)
    await expect(page.locator('[id$="proposalForm"]')).toBeAttached({ timeout: 10_000 });

    // Wissenschaftlicher Name (Pflichtfeld)
    await expect(page.locator('[id$="scientificName"]')).toBeVisible();

    // i18n-Tabs (DE/EN/ES/FR/IT) = 5 Tabs
    const tabs = page.locator('.ui-tabs-nav li');
    await expect(tabs).toHaveCount(5, { timeout: 5_000 });
  });

  // ── Katalog-Suche (AutoComplete) ────────────────────────────

  test('Katalog-Suche: AutoComplete-Feld reagiert auf Eingabe ohne Fehler', async ({ page }) => {
    const dialog = await openAddFishDialog(page);

    // AutoComplete-Input finden
    const acInput = dialog.locator('[id$="catalogueSearch_input"]');
    await expect(acInput).toBeVisible();

    // Mindestens 2 Zeichen tippen → AJAX-Suche
    await acInput.fill('Cl');
    await page.waitForTimeout(2_000); // debounce

    // AutoComplete-Panel — entweder Ergebnisse oder "keine Treffer"
    const acPanel = page.locator('.ui-autocomplete-panel').first();
    const panelVisible = await acPanel.isVisible();
    if (panelVisible) {
      const panelText = await acPanel.textContent();
      expect(panelText!.length).toBeGreaterThan(0);
    }

    // Kein Server-Fehler
    await expect(page.locator('body')).not.toContainText('500');

    // Dialog per Escape schließen
    await page.keyboard.press('Escape');
  });

});
