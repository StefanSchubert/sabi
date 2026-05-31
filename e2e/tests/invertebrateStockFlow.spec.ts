/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Invertebrate Stock Flow
 *
 * Covers:
 *  1. InvertebrateStockView — basic rendering, no raw i18n keys
 *  2. Aquarium dropdown lists Nano-Reef, Add-button enables after selection
 *  3. Add invertebrate → fill required fields → save → visible in table
 *  4. Delete invertebrate → removed from table
 *  5. Edit invertebrate → change species name → save → updated in table
 *  6. Catalogue proposal link navigates to proposal form with 5 i18n-tabs
 *  7. Catalogue proposal → fill + save → redirect to invertebrateStockView
 *
 * Testuser: sabi@bluewhale.de / clibanarius
 * App: http://localhost:8088
 */
import { test, expect, Page } from '@playwright/test';
import path from 'path';

// ──────────────────────────────────────────────────────────────
// Helper: Login
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
async function selectPrimeFacesOption(page: Page, containerSelector: string, optionText: string) {
  const trigger = page.locator(`${containerSelector} .ui-selectonemenu-trigger`).first();
  await trigger.click();
  const panel = page.locator('.ui-selectonemenu-panel').first();
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  await panel.locator('li.ui-selectonemenu-item').filter({ hasText: optionText }).click();
}

// ──────────────────────────────────────────────────────────────
// Helper: Nano-Reef im InvertebrateStockView auswählen (AJAX)
// ──────────────────────────────────────────────────────────────
async function selectNanoReef(page: Page) {
  await selectPrimeFacesOption(page, '#tankSelectorForm', 'Nano-Reef');
  await page.waitForLoadState('networkidle');
  // Add-Button wird nach erfolgreichem AJAX aktiv
  await expect(
      page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first()
  ).toBeEnabled({ timeout: 10_000 });
}

// ──────────────────────────────────────────────────────────────
// Helper: Datum für p:datePicker befüllen
// ──────────────────────────────────────────────────────────────
function todayString(): string {
  const today = new Date();
  const dd = String(today.getDate()).padStart(2, '0');
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const yyyy = today.getFullYear();
  return `${dd}.${mm}.${yyyy}`;
}

// ──────────────────────────────────────────────────────────────
// Helper: Taxonomische Kategorie im Entry-Formular auswählen
// (taxonomicCategory hat @NotNull → muss bei jedem Save gesetzt sein)
//
// HINWEIS: PrimeFaces SelectOneMenu bindet nach dem Trigger-Click einen Document-Click-Listener
// mit ~250ms Verzögerung. Der Trigger-Click bubbled hoch und schließt das Panel nach exakt
// 251ms → Klick auf Panel-Item schlägt fehl. Workaround: nativen <select> direkt setzen
// (force:true wegen display:none durch PrimeFaces).
// ──────────────────────────────────────────────────────────────
async function selectTaxonomicCategory(page: Page, value: string = 'CRUSTACEAN') {
  // PrimeFaces rendert <select id="...taxonomicCategory_input"> versteckt (display:none).
  // force:true umgeht den Visibility-Check — das Element ist funktional vorhanden.
  await page.locator('select[id$="taxonomicCategory_input"]').selectOption({ value }, { force: true });
}

// ──────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────
test.describe('Invertebrate Stock Flow', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto('/secured/invertebrateStockView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#tankSelectorForm')).toBeAttached({ timeout: 8_000 });
  });

  // ── Basis-Rendering ─────────────────────────────────────────

  test('invertebrateStockView zeigt keine raw i18n-Keys', async ({ page }) => {
    await expect(page.locator('body')).not.toContainText('???');
    await expect(page.locator('body')).not.toContainText('common.please.select');
    await expect(page.locator('body')).not.toContainText('common.select.l');
    await expect(page.locator('body')).not.toContainText('invertebratestock.');
  });

  test('Aquarium-Dropdown listet Nano-Reef auf', async ({ page }) => {
    await page.locator('#tankSelectorForm .ui-selectonemenu-trigger').first().click();
    const panel = page.locator('.ui-selectonemenu-panel').first();
    await panel.waitFor({ state: 'visible', timeout: 8_000 });
    await expect(panel.locator('li').filter({ hasText: 'Nano-Reef' })).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('Wirbellose-Hinzufügen-Button wird nach Aquarium-Auswahl aktiv', async ({ page }) => {
    // Vor Auswahl: Button disabled
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeDisabled();

    await selectNanoReef(page);

    const addButtonAfter = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButtonAfter).toBeEnabled({ timeout: 10_000 });
  });

  // ── Wirbellose anlegen ──────────────────────────────────────

  test('Wirbellose anlegen — Pflichtfelder ausfüllen, speichern, in Tabelle sichtbar', async ({ page }) => {
    const invertName = `E2E-Invertebrat-${Date.now()}`;

    // 1. Nano-Reef wählen und Add-Button klicken
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');

    // Add-Button navigiert zur invertebrateStockEntryPage
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    // 2. Pflichtfeld: Art-/Handelsname
    const speciesNameInput = page.locator('[id$="speciesName"]');
    await expect(speciesNameInput).toBeVisible({ timeout: 8_000 });
    await speciesNameInput.fill(invertName);

    // 3. Pflichtfeld: Datum
    const dateInput = page.locator('[id$="addedOn_input"]');
    await dateInput.fill(todayString());
    await speciesNameInput.click(); // blur → DatePicker schließen
    await page.waitForTimeout(300);

    // 4. Pflichtfeld: Taxonomische Kategorie (@NotNull)
    await selectTaxonomicCategory(page);

    // 5. Speichern
    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await expect(saveButton).toBeVisible({ timeout: 5_000 });
    await saveButton.click({ force: true }); // force: true wegen potentiellem DatePicker-Overlay
    await page.waitForURL(/invertebrateStockView/, { timeout: 15_000 });

    // 5. Reload und Nano-Reef erneut wählen
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);

    // 6. Eintrag in Tabelle sichtbar
    await expect(page.locator('#invertebrateStockForm')).toContainText(invertName, { timeout: 10_000 });

    // ── Cleanup: Wirbellose löschen ──
    const invertRow = page.locator('tr').filter({ hasText: invertName }).first();
    if (await invertRow.isVisible()) {
      await invertRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
      await expect(page.locator('#invertebrateStockForm')).not.toContainText(invertName, { timeout: 10_000 });
    }
  });

  // ── Redirect nach Save/Cancel ────────────────────────────────

  test('Cancel auf Entry-Page → Redirect zu invertebrateStockView', async ({ page }) => {
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    // Cancel-Button klicken
    const cancelButton = page.locator('button').filter({ hasText: /abbruch|abbrech|cancel/i }).first();
    await expect(cancelButton).toBeVisible({ timeout: 5_000 });
    await cancelButton.click();
    await page.waitForURL(/invertebrateStockView/, { timeout: 10_000 });
    await expect(page).toHaveURL(/invertebrateStockView/);
  });

  test('Back-Link auf Entry-Page führt zu invertebrateStockView', async ({ page }) => {
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    // Back-Link prüfen und klicken
    const backLink = page.locator('a.sabi-back-link').first();
    await expect(backLink).toBeVisible({ timeout: 5_000 });
    const href = await backLink.getAttribute('href');
    expect(href).toContain('invertebrateStockView');
    await backLink.click();
    await page.waitForURL(/invertebrateStockView/, { timeout: 10_000 });
  });

  // ── Wirbellose bearbeiten ────────────────────────────────────

  test('Wirbellose bearbeiten — Art-Name ändern, speichern, in Tabelle aktualisiert', async ({ page }) => {
    const ts = Date.now();
    const originalName = `E2E-EditOrig-${ts}`;
    const updatedName  = `E2E-EditUpd-${ts}`; // no substring overlap with originalName

    // 1. Anlegen
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const speciesNameInput = page.locator('[id$="speciesName"]');
    await speciesNameInput.fill(originalName);
    const dateInput = page.locator('[id$="addedOn_input"]');
    await dateInput.fill(todayString());
    await speciesNameInput.click();
    await page.waitForTimeout(300);

    // Pflichtfeld: Taxonomische Kategorie (@NotNull)
    await selectTaxonomicCategory(page);

    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await saveButton.click({ force: true });
    await page.waitForURL(/invertebrateStockView/, { timeout: 15_000 });

    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#invertebrateStockForm')).toContainText(originalName, { timeout: 10_000 });

    // 2. Edit-Button klicken
    const invertRow = page.locator('tr').filter({ hasText: originalName }).first();
    await invertRow.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    // 3. Name ändern und speichern
    const speciesField = page.locator('[id$="speciesName"]');
    await expect(speciesField).toBeVisible({ timeout: 8_000 });
    await speciesField.fill('');
    await speciesField.fill(updatedName);
    const saveBtn = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveBtn.scrollIntoViewIfNeeded();
    await saveBtn.click({ force: true });
    await page.waitForURL(/invertebrateStockView/, { timeout: 15_000 });

    // 4. Aktualisierter Name in Tabelle
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#invertebrateStockForm')).toContainText(updatedName, { timeout: 10_000 });
    await expect(page.locator('#invertebrateStockForm')).not.toContainText(originalName, { timeout: 5_000 });

    // ── Cleanup ──
    const updatedRow = page.locator('tr').filter({ hasText: updatedName }).first();
    if (await updatedRow.isVisible()) {
      await updatedRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
    }
  });

  // ── Entry-Page: keine raw i18n-Keys ─────────────────────────

  test('invertebrateStockEntryPage zeigt keine raw i18n-Keys', async ({ page }) => {
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    await expect(page.locator('body')).not.toContainText('???');
    await expect(page.locator('body')).not.toContainText('invertebratestock.');
    await expect(page.locator('body')).not.toContainText('invertebratecatalogue.');
  });

  // ── Katalog-Vorschlag ────────────────────────────────────────

  test('Katalog-Vorschlag: Link navigiert zur Proposal-Seite mit i18n-Tabs', async ({ page }) => {
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    // "Neuen Eintrag vorschlagen"-Link finden
    const proposalLink = page.locator('a').filter({ hasText: /vorschlagen|propose/i });
    await expect(proposalLink).toBeVisible({ timeout: 8_000 });

    // href prüfen
    const href = await proposalLink.getAttribute('href');
    expect(href).toContain('invertebrateCatalogueProposalForm');

    // Link klicken → Proposal-Seite
    await proposalLink.click();
    await page.waitForLoadState('networkidle');

    // Proposal-Formular vorhanden
    await expect(page.locator('[id$="proposalForm"]')).toBeAttached({ timeout: 10_000 });

    // Wissenschaftlicher Name (Pflichtfeld)
    await expect(page.locator('[id$="scientificName"]')).toBeVisible({ timeout: 5_000 });

    // i18n-Tabs (DE/EN/ES/FR/IT) = 5 Tabs
    const tabs = page.locator('.ui-tabs-nav li');
    await expect(tabs).toHaveCount(5, { timeout: 5_000 });
  });

  test('Katalog-Vorschlag: Formular ausfüllen und speichern → Redirect zu invertebrateStockView', async ({ page }) => {
    await page.goto('/secured/invertebrateCatalogueProposalForm.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('[id$="proposalForm"]')).toBeAttached({ timeout: 10_000 });

    // Wissenschaftlichen Namen eintragen (Pflichtfeld)
    const scientificNameField = page.locator('[id$="scientificName"]');
    await expect(scientificNameField).toBeVisible({ timeout: 8_000 });
    await scientificNameField.fill(`E2E-InvertTestSpecies-${Date.now()}`);

    // Speichern
    const saveButton = page.locator('button[type="submit"]').filter({ hasText: /speicher|save/i }).first();
    await expect(saveButton).toBeVisible({ timeout: 5_000 });
    await saveButton.click();
    await page.waitForURL(/invertebrateStockView/, { timeout: 15_000 });
    await expect(page).toHaveURL(/invertebrateStockView/);
  });

  test('Katalog-Vorschlag: Back-Link und Cancel führen zu invertebrateStockView', async ({ page }) => {
    await page.goto('/secured/invertebrateCatalogueProposalForm.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('[id$="proposalForm"]')).toBeAttached({ timeout: 10_000 });

    // Back-Link prüfen
    const backLink = page.locator('a.sabi-back-link').first();
    await expect(backLink).toBeVisible({ timeout: 5_000 });
    const href = await backLink.getAttribute('href');
    expect(href).toContain('invertebrateStockView');

    // Cancel-Button prüfen (label = "Abbruch" via common.cancel.b)
    const cancelButton = page.locator('button[type="button"]').filter({ hasText: /abbruch|abbrech|cancel/i }).first();
    await expect(cancelButton).toBeVisible({ timeout: 5_000 });
    await cancelButton.click();
    await page.waitForURL(/invertebrateStockView/, { timeout: 10_000 });
    await expect(page).toHaveURL(/invertebrateStockView/);
  });

  // ── Foto-Upload ─────────────────────────────────────────────

  test('Foto-Upload: file-input vorhanden mit korrektem accept-Attribut', async ({ page }) => {
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const fileInput = page.locator('input[type="file"]#invertebratePhotoFileInput');
    await expect(fileInput).toBeAttached({ timeout: 8_000 });

    const accept = await fileInput.getAttribute('accept');
    expect(accept).toContain('image/jpeg');
    expect(accept).toContain('image/png');
  });

  test('Foto-Upload: Bild hochladen und Thumbnail in Tabelle sichtbar', async ({ page }) => {
    const invertName = `E2E-FotoInvert-${Date.now()}`;
    const testImagePath = path.resolve(__dirname, 'fixtures', 'test-fish.png');

    // 1. Zur Eingabeseite navigieren
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    // 2. Pflichtfelder ausfüllen
    const speciesNameInput = page.locator('[id$="speciesName"]');
    await expect(speciesNameInput).toBeVisible({ timeout: 8_000 });
    await speciesNameInput.fill(invertName);

    const dateInput = page.locator('[id$="addedOn_input"]');
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    await dateInput.fill(`${dd}.${mm}.${yyyy}`);
    await speciesNameInput.click();
    await page.waitForTimeout(300);

    // Pflichtfeld: Taxonomische Kategorie (@NotNull)
    await selectTaxonomicCategory(page);

    // 3. Foto setzen
    const fileInput = page.locator('input[type="file"]#invertebratePhotoFileInput');
    await expect(fileInput).toBeAttached({ timeout: 8_000 });
    await fileInput.setInputFiles(testImagePath);

    // 4. Speichern (oncomplete → sabiUploadInvertebratePhotoAndRedirect → fetch → redirect)
    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await expect(saveButton).toBeVisible({ timeout: 5_000 });
    await saveButton.click({ force: true });
    await page.waitForURL(/invertebrateStockView/, { timeout: 20_000 });

    // 5. Reload + Nano-Reef erneut wählen
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#invertebrateStockForm')).toContainText(invertName, { timeout: 10_000 });

    // 6. KERN-ASSERTION: Thumbnail in Tabelle vorhanden
    const invertRow = page.locator('tr').filter({ hasText: invertName }).first();
    await expect(invertRow).toBeVisible({ timeout: 5_000 });

    const thumbnail = invertRow.locator('img');
    await expect(thumbnail).toBeVisible({ timeout: 8_000 });

    const styles = await page.evaluate((nameStr) => {
      const rows = Array.from(document.querySelectorAll('tr'));
      const row = rows.find(r => r.textContent?.includes(nameStr));
      if (!row) return { error: 'row not found' };
      const img = row.querySelector('img');
      if (!img) return { error: 'img not found' };
      const cs = window.getComputedStyle(img);
      return {
        width: cs.width,
        height: cs.height,
        display: cs.display,
        src: img.getAttribute('src'),
      };
    }, invertName);

    expect(styles).not.toHaveProperty('error');
    expect(styles.display).not.toBe('none');
    expect(styles.src).toContain('invertebratePhoto');

    // 7. Edit-Seite öffnen — Foto-Vorschau muss sichtbar sein
    await invertRow.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const photoPreview = page.locator('img[src*="invertebratePhoto"]').first();
    await expect(photoPreview).toBeVisible({ timeout: 8_000 });

    // ── Cleanup ──
    await page.goto('/secured/invertebrateStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const invertRowCleanup = page.locator('tr').filter({ hasText: invertName }).first();
    if (await invertRowCleanup.isVisible()) {
      await invertRowCleanup.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
      await expect(page.locator('#invertebrateStockForm')).not.toContainText(invertName, { timeout: 10_000 });
    }
  });

  // ── Edit mit Taxonomie-Änderung (Regression: CNIDARIAN 400-Bug) ──────────────────────

  test('Zwei Einträge mit unterschiedlicher Taxonomie anlegen, bearbeiten und Änderung persistiert', async ({ page }) => {
    const ts = Date.now();
    const name1 = `E2E-TaxEdit1-${ts}`;
    const name2 = `E2E-TaxEdit2-${ts}`;

    // ── Hilfsfunktion: Eintrag anlegen ──
    async function createEntry(speciesName: string, taxonomy: string) {
      await page.goto('/secured/invertebrateStockView.xhtml', { waitUntil: 'networkidle' });
      await selectNanoReef(page);
      const addBtn = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
      await expect(addBtn).toBeEnabled({ timeout: 10_000 });
      await addBtn.click();
      await page.waitForLoadState('networkidle');
      await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

      await page.locator('[id$="speciesName"]').fill(speciesName);
      await page.locator('[id$="addedOn_input"]').fill(todayString());
      await page.locator('[id$="speciesName"]').click();
      await page.waitForTimeout(300);
      await selectTaxonomicCategory(page, taxonomy);

      const saveBtn = page.locator('button').filter({ hasText: /speicher|save/i }).first();
      await saveBtn.scrollIntoViewIfNeeded();
      await saveBtn.click({ force: true });
      await page.waitForURL(/invertebrateStockView/, { timeout: 15_000 });
    }

    // ── Hilfsfunktion: Taxonomy-Label im Edit-Formular lesen ──
    async function getSelectedTaxonomy(page: Page): Promise<string> {
      return page.evaluate(() => {
        const sel = document.querySelector<HTMLSelectElement>('select[id$="taxonomicCategory_input"]');
        return sel ? sel.value : '';
      });
    }

    // 1. Beide Einträge anlegen
    await createEntry(name1, 'CRUSTACEAN');
    await createEntry(name2, 'ECHINODERM');

    // 2. Eintrag 1: Taxonomie von CRUSTACEAN → CNIDARIAN ändern
    await page.goto('/secured/invertebrateStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#invertebrateStockForm')).toContainText(name1, { timeout: 10_000 });

    const row1 = page.locator('tr').filter({ hasText: name1 }).first();
    await row1.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    // Verify the loaded taxonomy
    const loadedTax1 = await getSelectedTaxonomy(page);
    expect(loadedTax1).toBe('CRUSTACEAN');

    // Taxonomie auf CNIDARIAN ändern und speichern
    await selectTaxonomicCategory(page, 'CNIDARIAN');
    const saveBtn1 = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveBtn1.scrollIntoViewIfNeeded();
    await saveBtn1.click({ force: true });
    await page.waitForURL(/invertebrateStockView/, { timeout: 15_000 });

    // 3. Eintrag 1 erneut öffnen → CNIDARIAN muss gespeichert sein
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const row1After = page.locator('tr').filter({ hasText: name1 }).first();
    await row1After.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const updatedTax1 = await getSelectedTaxonomy(page);
    expect(updatedTax1).toBe('CNIDARIAN');

    // 4. Eintrag 2: Taxonomie von ECHINODERM → MOLLUSC ändern
    await page.goto('/secured/invertebrateStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#invertebrateStockForm')).toContainText(name2, { timeout: 10_000 });

    const row2 = page.locator('tr').filter({ hasText: name2 }).first();
    await row2.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const loadedTax2 = await getSelectedTaxonomy(page);
    expect(loadedTax2).toBe('ECHINODERM');

    await selectTaxonomicCategory(page, 'MOLLUSC');
    const saveBtn2 = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveBtn2.scrollIntoViewIfNeeded();
    await saveBtn2.click({ force: true });
    await page.waitForURL(/invertebrateStockView/, { timeout: 15_000 });

    // 5. Eintrag 2 erneut öffnen → MOLLUSC muss gespeichert sein
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const row2After = page.locator('tr').filter({ hasText: name2 }).first();
    await row2After.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const updatedTax2 = await getSelectedTaxonomy(page);
    expect(updatedTax2).toBe('MOLLUSC');

    // ── Cleanup ──
    for (const name of [name1, name2]) {
      await page.goto('/secured/invertebrateStockView.xhtml', { waitUntil: 'networkidle' });
      await selectNanoReef(page);
      const row = page.locator('tr').filter({ hasText: name }).first();
      if (await row.isVisible()) {
        await row.locator('button').filter({ has: page.locator('.pi-trash') }).click();
        await page.waitForLoadState('networkidle');
      }
    }
  });

  test('Foto-Upload: zweiter Upload überschreibt erstes Foto (kein Duplicate-Key-Fehler)', async ({ page }) => {
    const invertName = `E2E-FotoReplace-${Date.now()}`;
    const testImagePath = path.resolve(__dirname, 'fixtures', 'test-fish.png');

    // Invertebrat anlegen und erstes Foto hochladen
    await selectNanoReef(page);
    const addButton = page.locator('#invertebrateStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const speciesNameInput = page.locator('[id$="speciesName"]');
    await speciesNameInput.fill(invertName);
    const dateInput = page.locator('[id$="addedOn_input"]');
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    await dateInput.fill(`${dd}.${mm}.${yyyy}`);
    await speciesNameInput.click();
    await page.waitForTimeout(300);

    // Pflichtfeld: Taxonomische Kategorie (@NotNull)
    await selectTaxonomicCategory(page);

    const fileInput = page.locator('input[type="file"]#invertebratePhotoFileInput');
    await fileInput.setInputFiles(testImagePath);

    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await saveButton.click({ force: true });
    await page.waitForURL(/invertebrateStockView/, { timeout: 20_000 });

    // Zweiten Upload: Edit-Seite öffnen und Foto erneut hochladen
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#invertebrateStockForm')).toContainText(invertName, { timeout: 10_000 });

    const invertRow = page.locator('tr').filter({ hasText: invertName }).first();
    await invertRow.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/invertebrateStockEntryPage/, { timeout: 10_000 });

    const fileInputEdit = page.locator('input[type="file"]#invertebratePhotoFileInput');
    await fileInputEdit.setInputFiles(testImagePath);

    const saveButtonEdit = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButtonEdit.scrollIntoViewIfNeeded();
    await saveButtonEdit.click({ force: true });
    // Kein Fehler → Redirect zu invertebrateStockView (kein Duplicate-Key mehr)
    await page.waitForURL(/invertebrateStockView/, { timeout: 20_000 });
    await expect(page).toHaveURL(/invertebrateStockView/);

    // ── Cleanup ──
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const invertRowCleanup = page.locator('tr').filter({ hasText: invertName }).first();
    if (await invertRowCleanup.isVisible()) {
      await invertRowCleanup.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
    }
  });

});
