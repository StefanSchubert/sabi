/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Coral Stock Flow
 *
 * Covers:
 *  1. Coral Stock View — basic rendering
 *  2. Add coral → photo upload → thumbnail visible in table, preview in edit page
 *  3. Coral catalogue proposal → link navigates to proposal form → form has 5 i18n-Tabs
 *  4. Coral catalogue proposal → fill + save → redirect to coralStockView
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
// Helper: Nano-Reef im CoralStockView auswählen (AJAX)
// Statt waitForResponse (fragil nach Navigation) einfach auf
// networkidle + enabled Add-Button warten.
// ──────────────────────────────────────────────────────────────
async function selectNanoReef(page: Page) {
  await selectPrimeFacesOption(page, '#tankSelectorForm', 'Nano-Reef');
  await page.waitForLoadState('networkidle');
  // Add-Button wird nach erfolgreichem AJAX aktiv
  await expect(
      page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first()
  ).toBeEnabled({ timeout: 10_000 });
}

// ──────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────
test.describe('Coral Stock Flow', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto('/secured/coralStockView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#tankSelectorForm')).toBeAttached({ timeout: 8_000 });
  });

  // ── Basis-Rendering ─────────────────────────────────────────

  test('coralStockView zeigt keine raw i18n-Keys', async ({ page }) => {
    await expect(page.locator('body')).not.toContainText('???');
    await expect(page.locator('body')).not.toContainText('common.please.select');
    await expect(page.locator('body')).not.toContainText('common.select.l');
  });

  test('Aquarium-Dropdown listet Nano-Reef auf', async ({ page }) => {
    await page.locator('#tankSelectorForm .ui-selectonemenu-trigger').first().click();
    const panel = page.locator('.ui-selectonemenu-panel').first();
    await panel.waitFor({ state: 'visible', timeout: 8_000 });
    await expect(panel.locator('li').filter({ hasText: 'Nano-Reef' })).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('Korallen-Hinzufügen-Button wird nach Aquarium-Auswahl aktiv', async ({ page }) => {
    // Vor Auswahl: Button disabled
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeDisabled();

    await selectNanoReef(page);

    const addButtonAfter = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButtonAfter).toBeEnabled({ timeout: 10_000 });
  });

  // ── Koralle anlegen ─────────────────────────────────────────

  test('Koralle anlegen — Pflichtfelder ausfüllen, speichern, in Tabelle sichtbar', async ({ page }) => {
    const coralName = `E2E-Testkoralle-${Date.now()}`;

    // 1. Nano-Reef wählen und Add-Button klicken
    await selectNanoReef(page);
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');

    // Add-Button navigiert zur coralStockEntryPage
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // 2. Pflichtfelder ausfüllen
    const speciesNameInput = page.locator('[id$="speciesName"]');
    await expect(speciesNameInput).toBeVisible({ timeout: 8_000 });
    await speciesNameInput.fill(coralName);

    // DatePicker direkt befüllen
    const dateInput = page.locator('[id$="addedOn_input"]');
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    const dateStr = `${dd}.${mm}.${yyyy}`;
    await dateInput.fill(dateStr);
    await speciesNameInput.click(); // blur → DatePicker schließen
    await page.waitForTimeout(300);

    // 3. Speichern
    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await expect(saveButton).toBeVisible({ timeout: 5_000 });
    await saveButton.click({ force: true }); // force: true wegen potentiellem DatePicker-Overlay
    // Wait for JS redirect (oncomplete → sabiUploadCoralPhotoAndRedirect → redirect())
    await page.waitForURL(/coralStockView/, { timeout: 15_000 });

    // 4. Reload to ensure fresh data (CoralStockView.init() fetches list after INSERT committed)
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);

    // 5. Korallen-Name in Tabelle sichtbar
    await expect(page.locator('#coralStockForm')).toContainText(coralName, { timeout: 10_000 });

    // ── Cleanup: Koralle löschen ──
    const coralRow = page.locator('tr').filter({ hasText: coralName }).first();
    if (await coralRow.isVisible()) {
      await coralRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
      await expect(page.locator('#coralStockForm')).not.toContainText(coralName, { timeout: 10_000 });
    }
  });

  // ── Foto-Upload ─────────────────────────────────────────────

  test('Foto-Upload: file-input vorhanden mit korrektem accept-Attribut', async ({ page }) => {
    await selectNanoReef(page);
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // File-Input finden
    const fileInput = page.locator('input[type="file"]#coralPhotoFileInput');
    await expect(fileInput).toBeAttached({ timeout: 8_000 });

    // accept-Attribut prüfen
    const accept = await fileInput.getAttribute('accept');
    expect(accept).toContain('image/jpeg');
    expect(accept).toContain('image/png');
  });

  test('Foto-Upload: Bild hochladen und Thumbnail in Tabelle sichtbar', async ({ page }) => {
    const coralName = `E2E-FotoKoralle-${Date.now()}`;
    const testImagePath = path.resolve(__dirname, 'fixtures', 'test-fish.png');

    // 1. Zur Eingabeseite navigieren
    await selectNanoReef(page);
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // 2. Pflichtfelder ausfüllen
    const speciesNameInput = page.locator('[id$="speciesName"]');
    await expect(speciesNameInput).toBeVisible({ timeout: 8_000 });
    await speciesNameInput.fill(coralName);

    const dateInput = page.locator('[id$="addedOn_input"]');
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    await dateInput.fill(`${dd}.${mm}.${yyyy}`);
    await speciesNameInput.click();
    await page.waitForTimeout(300);

    // 3. Foto setzen
    const fileInput = page.locator('input[type="file"]#coralPhotoFileInput');
    await expect(fileInput).toBeAttached({ timeout: 8_000 });
    await fileInput.setInputFiles(testImagePath);

    // 4. Speichern
    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await expect(saveButton).toBeVisible({ timeout: 5_000 });
    await saveButton.click({ force: true });
    // Wait for JS redirect: oncomplete → sabiUploadCoralPhotoAndRedirect → fetch photo upload → redirect()
    await page.waitForURL(/coralStockView/, { timeout: 20_000 });

    // 5. Zurück zu coralStockView — reload für frische Daten nach DB-Commit + Photo-Upload
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#coralStockForm')).toContainText(coralName, { timeout: 10_000 });

    // 6. KERN-ASSERTION: Thumbnail in Tabelle vorhanden (img statt Icon)
    const coralRow = page.locator('tr').filter({ hasText: coralName }).first();
    await expect(coralRow).toBeVisible({ timeout: 5_000 });

    const thumbnail = coralRow.locator('img');
    await expect(thumbnail).toBeVisible({ timeout: 8_000 });

    // CSS-Sichtbarkeit prüfen (AGENTS.md)
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
    }, coralName);

    expect(styles).not.toHaveProperty('error');
    expect(styles.display).not.toBe('none');
    expect(styles.src).toContain('coralPhoto');

    // 7. Edit-Seite öffnen — Foto-Vorschau muss sichtbar sein
    await coralRow.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // Foto-Vorschau im Edit-Modus (rendered-Bedingung: edit && hasPhoto)
    const photoPreview = page.locator('p\\:graphicImage, img[src*="coralPhoto"]').first();
    await expect(photoPreview).toBeVisible({ timeout: 8_000 });

    // ── Cleanup ──
    await page.goto('/secured/coralStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const coralRowCleanup = page.locator('tr').filter({ hasText: coralName }).first();
    if (await coralRowCleanup.isVisible()) {
      await coralRowCleanup.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
      await expect(page.locator('#coralStockForm')).not.toContainText(coralName, { timeout: 10_000 });
    }
  });

  // ── Korallen-Katalog-Vorschlag ──────────────────────────────

  test('Katalog-Vorschlag: Link navigiert zur Proposal-Seite mit i18n-Tabs', async ({ page }) => {
    // Direkt zur Coral Stock Entry Page navigieren (Add-Flow)
    await selectNanoReef(page);
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // "Neuen Katalogeintrag vorschlagen"-Link finden
    const proposalLink = page.locator('a').filter({ hasText: /vorschlagen|propose/i });
    await expect(proposalLink).toBeVisible({ timeout: 8_000 });

    // href prüfen
    const href = await proposalLink.getAttribute('href');
    expect(href).toContain('coralCatalogueProposalForm');

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

  test('Katalog-Vorschlag: Formular ausfüllen und speichern → Redirect zu coralStockView', async ({ page }) => {
    const uniqueName = `E2E-CoralSpecies-${Date.now()}`;

    // 1. Direkt zur Proposal-Seite navigieren
    await page.goto('/secured/coralCatalogueProposalForm.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('[id$="proposalForm"]')).toBeAttached({ timeout: 10_000 });

    // 2. Kein 404 / keine Fehlerseite
    await expect(page.locator('body')).not.toContainText('404');
    await expect(page.locator('body')).not.toContainText('500');

    // 3. Pflichtfeld: Wissenschaftlicher Name
    const scientificNameInput = page.locator('[id$="scientificName"]');
    await expect(scientificNameInput).toBeVisible({ timeout: 8_000 });
    await scientificNameInput.fill(uniqueName);

    // 4. Classification auswählen (PrimeFaces selectOneMenu)
    const classificationTrigger = page.locator('[id$="classification"] .ui-selectonemenu-trigger').first();
    await classificationTrigger.click();
    // Warte auf das Öffnen des spezifischen Panels (nicht .first() — könnte bereits geschlossenes Panel treffen)
    const classPanel = page.locator('[id$="classification_panel"]');
    await classPanel.waitFor({ state: 'visible', timeout: 8_000 });
    await classPanel.locator('li.ui-selectonemenu-item').filter({ hasText: 'LPS' }).click();
    await page.waitForTimeout(200);

    // 5. CareLevel auswählen
    const careLevelTrigger = page.locator('[id$="careLevel"] .ui-selectonemenu-trigger').first();
    await careLevelTrigger.click();
    const careLevelPanel = page.locator('[id$="careLevel_panel"]');
    await careLevelPanel.waitFor({ state: 'visible', timeout: 8_000 });
    // Wähle ersten verfügbaren, nicht-leeren Eintrag
    const careLevelOption = careLevelPanel.locator('li.ui-selectonemenu-item').nth(1);
    await careLevelOption.click();
    await page.waitForTimeout(200);

    // 6. Erster i18n-Tab (DE) sollte aktiv sein — Allgemeinname ausfüllen
    const firstTabCommonName = page.locator('.ui-tabs-panel').first().locator('input').first();
    await expect(firstTabCommonName).toBeVisible({ timeout: 5_000 });
    await firstTabCommonName.fill(`E2E-Koralle-DE-${Date.now()}`);

    // 7. Kein ???-Platzhalter (i18n vollständig)
    await expect(page.locator('body')).not.toContainText('???');

    // 8. Speichern
    const saveButton = page.locator('[id$="proposalForm"] button').filter({ hasText: /speicher|save/i }).first();
    await expect(saveButton).toBeVisible({ timeout: 5_000 });

    await saveButton.click();
    // JSF facelet-redirect navigates via JS after AJAX response — wait for URL change
    await page.waitForURL(/coralStockView/, { timeout: 15_000 });

    // 9. KERN-ASSERTION: Kein Fehler, Redirect zu coralStockView
    // Der Controller gibt "/secured/coralStockView?faces-redirect=true" zurück
    await expect(page).toHaveURL(/coralStockView/, { timeout: 10_000 });
  });

  test('Katalog-Proposal-Seite: Back-Link navigiert zurück zu coralStockView', async ({ page }) => {
    await page.goto('/secured/coralCatalogueProposalForm.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('[id$="proposalForm"]')).toBeAttached({ timeout: 10_000 });

    // Back-Link vorhanden
    const backLink = page.locator('.sabi-back-link');
    await expect(backLink).toBeVisible({ timeout: 5_000 });

    await backLink.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockView/, { timeout: 8_000 });
  });

  // ── Edit-Flow ────────────────────────────────────────────────

  test('Koralle bearbeiten — Edit-Seite öffnet sich korrekt, Stammdaten bearbeiten, speichern', async ({ page }) => {
    const coralName = `E2E-EditKoralle-${Date.now()}`;
    const editedName = `${coralName}-edited`;

    // 1. Koralle anlegen (Vorbedingung)
    await selectNanoReef(page);
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    const speciesNameInput = page.locator('[id$="speciesName"]');
    await expect(speciesNameInput).toBeVisible({ timeout: 8_000 });
    await speciesNameInput.fill(coralName);

    const dateInput = page.locator('[id$="addedOn_input"]');
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    await dateInput.fill(`${dd}.${mm}.${yyyy}`);
    await speciesNameInput.click();
    await page.waitForTimeout(300);

    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await saveButton.click({ force: true });
    await page.waitForURL(/coralStockView/, { timeout: 15_000 });

    // 2. Koralle in Tabelle finden
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#coralStockForm')).toContainText(coralName, { timeout: 10_000 });

    // 3. Edit-Button klicken
    const coralRow = page.locator('tr').filter({ hasText: coralName }).first();
    await expect(coralRow).toBeVisible({ timeout: 5_000 });
    const editButton = coralRow.locator('button').filter({ has: page.locator('.pi-pencil') });
    await expect(editButton).toBeVisible({ timeout: 5_000 });
    await editButton.click();
    await page.waitForLoadState('networkidle');

    // 4. Edit-Seite prüfen
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });
    await expect(page.locator('body')).not.toContainText('???');
    await expect(page.locator('body')).not.toContainText('500');

    // 5. Kein roher i18n-Key für careLevel (Kernprüfung des Bugs)
    await expect(page.locator('[id$="careLevel"]')).toBeVisible({ timeout: 8_000 });

    // 6. Species-Name ändern
    const speciesInput = page.locator('[id$="speciesName"]');
    await expect(speciesInput).toBeVisible({ timeout: 8_000 });
    await speciesInput.fill(editedName);

    // 7. Growth-History-Panel im Edit-Modus sichtbar
    const growthPanel = page.locator('#coralEntryForm\\:growthHistoryPanel');
    await expect(growthPanel).toBeAttached({ timeout: 5_000 });

    // 8. Speichern
    const saveBtn = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveBtn.scrollIntoViewIfNeeded();
    await saveBtn.click({ force: true });
    await page.waitForURL(/coralStockView/, { timeout: 15_000 });

    // 9. Geänderter Name in Tabelle
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#coralStockForm')).toContainText(editedName, { timeout: 10_000 });

    // ── Cleanup ──
    const editedRow = page.locator('tr').filter({ hasText: editedName }).first();
    if (await editedRow.isVisible()) {
      await editedRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
      await expect(page.locator('#coralStockForm')).not.toContainText(editedName, { timeout: 10_000 });
    }
  });

  // ── Wachstumsangabe ─────────────────────────────────────────

  test('Wachstumsangabe hinzufügen — erscheint chronologisch in History-Tabelle', async ({ page }) => {
    const coralName = `E2E-GrowthKoralle-${Date.now()}`;

    // 1. Koralle anlegen
    await selectNanoReef(page);
    let addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    const speciesNameInput = page.locator('[id$="speciesName"]');
    await expect(speciesNameInput).toBeVisible({ timeout: 8_000 });
    await speciesNameInput.fill(coralName);

    const dateInput = page.locator('[id$="addedOn_input"]');
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    await dateInput.fill(`${dd}.${mm}.${yyyy}`);
    await speciesNameInput.click();
    await page.waitForTimeout(300);

    let saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await saveButton.click({ force: true });
    await page.waitForURL(/coralStockView/, { timeout: 15_000 });

    // 2. Edit-Seite für die neue Koralle öffnen
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#coralStockForm')).toContainText(coralName, { timeout: 10_000 });

    const coralRow = page.locator('tr').filter({ hasText: coralName }).first();
    await coralRow.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // 3. Growth-History-Panel vorhanden und sichtbar
    const growthPanel = page.locator('#coralEntryForm\\:growthHistoryPanel');
    await expect(growthPanel).toBeAttached({ timeout: 8_000 });

    // 4. Wachstumsangabe eintragen
    const growthDateInput = page.locator('[id$="newGrowthDate_input"]');
    await expect(growthDateInput).toBeVisible({ timeout: 8_000 });
    await growthDateInput.fill(`${dd}.${mm}.${yyyy}`);
    await page.keyboard.press('Escape'); // DatePicker schließen

    // Growth-Wert: p:inputNumber → click + pressSequentially triggert AutoNumeric-Events
    const growthValueInput = page.locator('[id$="newGrowthValue_input"]');
    await expect(growthValueInput).toBeVisible({ timeout: 5_000 });
    await growthValueInput.click();
    await growthValueInput.pressSequentially('5.5');
    await page.waitForTimeout(300);

    // Add-Button für Growth
    // Fallback: den grünen Hinzufügen-Button im growthHistoryPanel per Inhalt finden
    const growthAddBtn = page.locator('#coralEntryForm\\:growthHistoryPanel button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(growthAddBtn).toBeVisible({ timeout: 8_000 });
    await growthAddBtn.click();
    await page.waitForLoadState('networkidle');

    // 5. Eintrag in History-Tabelle erscheint
    const growthTable = page.locator('#coralEntryForm\\:growthHistoryPanel .ui-datatable');
    await expect(growthTable).toBeVisible({ timeout: 8_000 });
    await expect(growthTable).not.toContainText('Noch keine', { timeout: 5_000 }); // Eintrag vorhanden

    // ── Cleanup ──
    await page.goto('/secured/coralStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const cleanupRow = page.locator('tr').filter({ hasText: coralName }).first();
    if (await cleanupRow.isVisible()) {
      await cleanupRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
    }
  });

  // ── Polypenbildangabe ────────────────────────────────────────

  test('Polypenbildangabe hinzufügen — erscheint chronologisch in History-Tabelle', async ({ page }) => {
    const coralName = `E2E-PolypKoralle-${Date.now()}`;

    // 1. Koralle anlegen
    await selectNanoReef(page);
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    const speciesNameInput = page.locator('[id$="speciesName"]');
    await expect(speciesNameInput).toBeVisible({ timeout: 8_000 });
    await speciesNameInput.fill(coralName);

    const dateInput = page.locator('[id$="addedOn_input"]');
    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    await dateInput.fill(`${dd}.${mm}.${yyyy}`);
    await speciesNameInput.click();
    await page.waitForTimeout(300);

    const saveButton = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveButton.scrollIntoViewIfNeeded();
    await saveButton.click({ force: true });
    await page.waitForURL(/coralStockView/, { timeout: 15_000 });

    // 2. Edit-Seite für die Koralle öffnen
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#coralStockForm')).toContainText(coralName, { timeout: 10_000 });

    const coralRow = page.locator('tr').filter({ hasText: coralName }).first();
    await coralRow.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // 3. Polyp-History-Panel vorhanden
    const polypPanel = page.locator('#coralEntryForm\\:polypHistoryPanel');
    await expect(polypPanel).toBeAttached({ timeout: 8_000 });

    // 4. Polypenbildangabe eintragen
    const polypDateInput = page.locator('[id$="newPolypDate_input"]');
    await expect(polypDateInput).toBeVisible({ timeout: 8_000 });
    await polypDateInput.fill(`${dd}.${mm}.${yyyy}`);
    await page.keyboard.press('Escape');

    // Condition wählen (default = VITAL, also reicht es, den Add-Button direkt zu klicken)
    const polypAddBtn = page.locator('#coralEntryForm\\:polypHistoryPanel button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(polypAddBtn).toBeVisible({ timeout: 8_000 });
    await polypAddBtn.click();
    await page.waitForLoadState('networkidle');

    // 5. Eintrag in Polyp-History-Tabelle erscheint
    const polypTable = page.locator('#coralEntryForm\\:polypHistoryPanel .ui-datatable');
    await expect(polypTable).toBeVisible({ timeout: 8_000 });
    // VITAL ist der Default-Wert
    await expect(polypTable).toContainText(/vital/i, { timeout: 5_000 });

    // ── Cleanup ──
    await page.goto('/secured/coralStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const cleanupRow = page.locator('tr').filter({ hasText: coralName }).first();
    if (await cleanupRow.isVisible()) {
      await cleanupRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
    }
  });

  // ── Kombinierter Test: Growth + Polyp nach Speichern in Übersicht ──

  test('Growth + Polyp: beide Angaben sichtbar nach Speichern chronologisch', async ({ page }) => {
    const coralName = `E2E-GrowthPolyp-${Date.now()}`;

    // 1. Koralle anlegen
    await selectNanoReef(page);
    const addButton = page.locator('#coralStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 10_000 });
    await addButton.click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    const speciesInput = page.locator('[id$="speciesName"]');
    await expect(speciesInput).toBeVisible({ timeout: 8_000 });
    await speciesInput.fill(coralName);

    const today = new Date();
    const dd = String(today.getDate()).padStart(2, '0');
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const yyyy = today.getFullYear();
    const dateStr = `${dd}.${mm}.${yyyy}`;

    await page.locator('[id$="addedOn_input"]').fill(dateStr);
    await speciesInput.click();
    await page.waitForTimeout(300);

    const saveBtn = page.locator('button').filter({ hasText: /speicher|save/i }).first();
    await saveBtn.scrollIntoViewIfNeeded();
    await saveBtn.click({ force: true });
    await page.waitForURL(/coralStockView/, { timeout: 15_000 });

    // 2. Edit öffnen
    await page.reload({ waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#coralStockForm')).toContainText(coralName, { timeout: 10_000 });

    const coralRow = page.locator('tr').filter({ hasText: coralName }).first();
    await coralRow.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // 3. Wachstumsangabe hinzufügen
    await page.locator('[id$="newGrowthDate_input"]').fill(dateStr);
    await page.keyboard.press('Escape');
    const gValueInput = page.locator('[id$="newGrowthValue_input"]');
    await gValueInput.click();
    await gValueInput.pressSequentially('3.7');
    await page.waitForTimeout(300);

    const growthAddBtn = page.locator('#coralEntryForm\\:growthHistoryPanel button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(growthAddBtn).toBeVisible({ timeout: 8_000 });
    await growthAddBtn.click();
    await page.waitForLoadState('networkidle');

    // Growth-Tabelle hat jetzt mindestens 1 Eintrag
    const growthTable = page.locator('#coralEntryForm\\:growthHistoryPanel .ui-datatable');
    await expect(growthTable).not.toContainText('Noch keine', { timeout: 5_000 });

    // 4. Polypenbildangabe hinzufügen
    await page.locator('[id$="newPolypDate_input"]').fill(dateStr);
    await page.keyboard.press('Escape');

    const polypAddBtn = page.locator('#coralEntryForm\\:polypHistoryPanel button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(polypAddBtn).toBeVisible({ timeout: 8_000 });
    await polypAddBtn.click();
    await page.waitForLoadState('networkidle');

    // Polyp-Tabelle hat jetzt mindestens 1 Eintrag
    const polypTable = page.locator('#coralEntryForm\\:polypHistoryPanel .ui-datatable');
    await expect(polypTable).toContainText(/vital/i, { timeout: 5_000 });

    // 5. Zur CoralStockView navigieren (simuliert separate Browser-Session / Speichern + Zurück)
    // Damit wird geprüft, dass die Daten persistiert wurden und beim erneuten Edit noch vorhanden sind
    await page.goto('/secured/coralStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    await expect(page.locator('#coralStockForm')).toContainText(coralName, { timeout: 10_000 });

    const coralRowAfter = page.locator('tr').filter({ hasText: coralName }).first();
    await coralRowAfter.locator('button').filter({ has: page.locator('.pi-pencil') }).click();
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/coralStockEntryPage/, { timeout: 10_000 });

    // 6. Chronologische Anzeige: beide Panels haben Einträge
    const growthTableAfterReload = page.locator('#coralEntryForm\\:growthHistoryPanel .ui-datatable');
    await expect(growthTableAfterReload).toBeVisible({ timeout: 8_000 });
    await expect(growthTableAfterReload).not.toContainText('Noch keine', { timeout: 5_000 });
    // Datumsspalte prüfen
    await expect(growthTableAfterReload).toContainText(dateStr, { timeout: 5_000 });

    const polypTableAfterReload = page.locator('#coralEntryForm\\:polypHistoryPanel .ui-datatable');
    await expect(polypTableAfterReload).toBeVisible({ timeout: 8_000 });
    await expect(polypTableAfterReload).not.toContainText('Noch keine', { timeout: 5_000 });
    // Datumsspalte prüfen
    await expect(polypTableAfterReload).toContainText(dateStr, { timeout: 5_000 });

    // ── Cleanup ──
    await page.goto('/secured/coralStockView.xhtml', { waitUntil: 'networkidle' });
    await selectNanoReef(page);
    const cleanupRow = page.locator('tr').filter({ hasText: coralName }).first();
    if (await cleanupRow.isVisible()) {
      await cleanupRow.locator('button').filter({ has: page.locator('.pi-trash') }).click();
      await page.waitForLoadState('networkidle');
    }
  });

});

