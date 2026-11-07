/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Fish Stock View (fishStockView.xhtml) + Entry Page (fishStockEntryPage.xhtml)
 *
 * Testuser: sabi@bluewhale.de / clibanarius  (Passwort: 'clibanarius')
 * Hat 2 Aquarien: "Nano-Reef" (id=1) und "Freshwater" (id=2)
 * App läuft auf http://localhost:8088
 *
 * UI-Flow (Stand 2026): Fisch anlegen/bearbeiten/duplizieren ist KEIN Dialog mehr,
 * sondern eine Standalone-Seite (fishStockEntryPage.xhtml):
 *  - "Fisch hinzufügen" / Pencil / Copy-Button: ajax="false" → volle Navigation
 *  - Speichern: AJAX POST auf fishStockEntryPage.xhtml, danach Client-Redirect
 *    zurück zu fishStockView.xhtml (oncomplete="sabiUploadPhotoAndRedirect()")
 *  - Foto (falls gewählt) wird zusätzlich per fetch POST /secured/fishPhoto?fishId=X hochgeladen
 *  - Departure bleibt ein Dialog (#fishDepartureDialog) — siehe fishDepartureFlow.spec.ts
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
// Helper: Entry-Page (fishStockEntryPage.xhtml) — Page-Flow statt Dialog
// ──────────────────────────────────────────────────────────────

/** Heutiges Datum im dd.MM.yyyy-Format (Pattern des p:datePicker addedOn). */
function todayStr(): string {
  const t = new Date();
  return String(t.getDate()).padStart(2, '0') + '.'
       + String(t.getMonth() + 1).padStart(2, '0') + '.'
       + t.getFullYear();
}

/** Aquarium im Tank-Selector wählen (AJAX-Update des fishStockForm). */
async function selectTank(page: Page, tankName: string) {
  await Promise.all([
    waitForFishStockAjax(page),
    selectPrimeFacesOption(page, tankName),
  ]);
}

/**
 * "Fisch hinzufügen" klicken → volle Navigation zur Entry-Page (ajax="false").
 * Setzt voraus, dass vorher ein Tank gewählt wurde.
 */
async function openAddFishPage(page: Page) {
  await selectTank(page, 'Nano-Reef');
  const addButton = page.locator('#fishStockForm button').filter({ hasText: /hinzuf|add/i }).first();
  await expect(addButton).toBeEnabled({ timeout: 10_000 });
  await Promise.all([
    page.waitForURL(/fishStockEntryPage/),
    addButton.click(),
  ]);
  await page.waitForLoadState('networkidle');
  await expect(page.locator('#fishEntryForm')).toBeAttached({ timeout: 10_000 });
}

/** Pflichtfelder der Entry-Page ausfüllen (commonName + addedOn). */
async function fillRequiredEntryFields(page: Page, commonName: string, dateStr: string) {
  const commonNameInput = page.locator('#fishEntryForm [id$="commonName"]');
  await expect(commonNameInput).toBeVisible({ timeout: 5_000 });
  await commonNameInput.fill(commonName);

  const dateInput = page.locator('#fishEntryForm [id$="addedOn_input"]');
  await expect(dateInput).toBeVisible({ timeout: 5_000 });
  await dateInput.fill(dateStr);
  // Klick auf das Name-Feld schließt ein evtl. geöffnetes DatePicker-Overlay + blur
  await commonNameInput.click();
}

/**
 * Speichern auf der Entry-Page: AJAX POST auf fishStockEntryPage.xhtml,
 * danach Client-Redirect zurück zu fishStockView.xhtml.
 */
async function saveEntryPage(page: Page) {
  const saveButton = page.locator('#fishEntryForm button').filter({ hasText: /speicher|save/i }).first();
  await expect(saveButton).toBeVisible({ timeout: 5_000 });
  await saveButton.scrollIntoViewIfNeeded();
  await Promise.all([
    page.waitForResponse(resp =>
        resp.url().includes('fishStockEntryPage') && resp.status() === 200
        && resp.request().method() === 'POST',
      { timeout: 15_000 }),
    saveButton.click(),
  ]);
  // oncomplete → (ggf. Foto-Upload per fetch) → window.location.href = fishStockView.xhtml
  await page.waitForURL(/fishStockView\.xhtml/, { timeout: 30_000 });
  await page.waitForLoadState('networkidle');
}

/** Abbruch-Button (type="button", onclick-Redirect) → zurück zu fishStockView.xhtml. */
async function cancelEntryPage(page: Page) {
  const cancelButton = page.locator('#fishEntryForm button').filter({ hasText: /abbruch|cancel/i }).first();
  await expect(cancelButton).toBeVisible({ timeout: 5_000 });
  await Promise.all([
    page.waitForURL(/fishStockView\.xhtml/),
    cancelButton.click(),
  ]);
  await page.waitForLoadState('networkidle');
}

/** Edit-Button (Pencil) in der Fisch-Zeile → Entry-Page im Edit-Modus. */
async function openEditFishPage(page: Page, fishName: string) {
  const fishRow = page.locator('#fishStockForm tr').filter({ hasText: fishName }).first();
  await expect(fishRow).toBeVisible({ timeout: 8_000 });
  const editButton = fishRow.locator('button').filter({ has: page.locator('.pi-pencil') });
  await expect(editButton).toBeVisible();
  await Promise.all([
    page.waitForURL(/fishStockEntryPage/),
    editButton.click(),
  ]);
  await page.waitForLoadState('networkidle');
  await expect(page.locator('#fishEntryForm')).toBeAttached({ timeout: 10_000 });
}

/** Fisch über den Trash-Button löschen (AJAX-Update des fishStockForm). */
async function deleteFish(page: Page, fishName: string) {
  const fishRow = page.locator('#fishStockForm tr').filter({ hasText: fishName }).first();
  if (!(await fishRow.isVisible())) return;
  const trashButton = fishRow.locator('button').filter({ has: page.locator('.pi-trash') });
  await expect(trashButton).toBeVisible();
  await Promise.all([
    page.waitForResponse(resp =>
        resp.url().includes('fishStockView') && resp.status() === 200
        && resp.request().method() === 'POST',
      { timeout: 15_000 }),
    trashButton.click(),
  ]);
  await page.waitForLoadState('networkidle');
  await expect(page.locator('#fishStockForm')).not.toContainText(fishName, { timeout: 10_000 });
}

// ──────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────
test.describe('Fish Stock View', () => {

  // Entry-Page mit vielen Feldern + mehrere Navigationen pro Test → großzügiges Timeout.
  test.use({ viewport: { width: 1280, height: 1024 } });
  test.setTimeout(120_000);

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
    // Kein raw i18n-Key sichtbar (unabhängig von der Sprache des Testusers DE/EN)
    expect(labelText).not.toContain('common.');
    expect(labelText).not.toContain('???');
    // Entweder DE ("Auswählen...") oder EN ("Select one...") — beides ist korrekt übersetzt
    const isTranslated = labelText!.includes('Ausw') || labelText!.includes('Select');
    expect(isTranslated).toBe(true);
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
    // Menüpunkt für Fish Stock — DE: "Fischbestand", EN: "Fish Stock"
    const fishMenuItem = page.locator('.ui-menu-list .ui-menuitem-link')
        .filter({ hasText: /Fischbestand|Fish Stock/i });
    await expect(fishMenuItem).toBeVisible({ timeout: 5_000 });
  });

  // ── Fisch-Hinzufügen-Seite (früher Dialog, jetzt Standalone-Page) ──

  test('Fisch-hinzufügen-Seite öffnet sich mit allen Formularfeldern', async ({ page }) => {
    await openAddFishPage(page);

    // Seiten-Header: "Fisch hinzufügen" (de) / "Add Fish" (en) — nicht "Bearbeiten"
    const heading = page.locator('h2').first();
    await expect(heading).toContainText(/Fisch hinzufügen|Add Fish/i);

    // Pflichtfelder
    await expect(page.locator('#fishEntryForm [id$="commonName"]')).toBeVisible();
    await expect(page.locator('#fishEntryForm [id$="addedOn_input"]')).toBeVisible();

    // Optionale Felder
    await expect(page.locator('#fishEntryForm [id$="nickname"]')).toBeVisible();
    await expect(page.locator('#fishEntryForm [id$="scientificName"]')).toBeVisible();
    await expect(page.locator('#fishEntryForm [id$="externalRefUrl"]')).toBeVisible();
    await expect(page.locator('#fishEntryForm [id$="observedBehavior"]')).toBeVisible();

    // Foto-Upload-Feld (plain HTML input, kein PrimeFaces)
    const fileInput = page.locator('#photoFileInput');
    await expect(fileInput).toBeAttached();

    // Katalog-Suche (AutoComplete-Input)
    await expect(page.locator('#fishEntryForm [id$="catalogueSearch_input"]')).toBeVisible();

    // Katalog-Vorschlags-Link
    await expect(page.locator('#fishEntryForm a').filter({ hasText: /vorschlagen|propose/i })).toBeVisible();

    // Buttons: Speichern + Abbruch
    await expect(page.locator('#fishEntryForm button').filter({ hasText: /speicher|save/i })).toBeVisible();
    await expect(page.locator('#fishEntryForm button').filter({ hasText: /abbruch|cancel/i })).toBeAttached();

    // Zurück per Abbruch-Button (type="button" → Client-Redirect)
    await cancelEntryPage(page);
  });

  // ── Fisch anlegen und verifizieren ──────────────────────────

  test('Fisch anlegen — Pflichtfelder ausfüllen, speichern, in Tabelle sichtbar', async ({ page }) => {
    const fishName = `E2E-Testfisch-${Date.now()}`;

    // 1. Entry-Page öffnen (inkl. Aquarium-Auswahl)
    await openAddFishPage(page);

    // 2. Pflichtfelder ausfüllen + speichern (AJAX POST + Client-Redirect)
    await fillRequiredEntryFields(page, fishName, todayStr());
    await saveEntryPage(page);

    // 3. Fisch-Name in der "Aktuell im Becken"-Tabelle sichtbar
    //    (Tank-Auswahl überlebt den Redirect via userSession.getSelectedTank())
    await expect(page.locator('#fishStockForm')).toContainText(fishName, { timeout: 10_000 });

    // ── 4. Edit-Page öffnen und vorausgefüllte Felder prüfen ──
    await openEditFishPage(page, fishName);

    // Header wechselt zu "Bearbeiten" (de) / "Edit" (en)
    await expect(page.locator('h2').first()).toContainText(/Bearbeiten|Edit/i);

    // Artname (commonName) muss vorausgefüllt sein
    const editCommonName = page.locator('#fishEntryForm [id$="commonName"]');
    await expect(editCommonName).toHaveValue(fishName);

    // Einzugsdatum muss vorausgefüllt sein (dd.MM.yyyy Format)
    const editDateInput = page.locator('#fishEntryForm [id$="addedOn_input"]');
    await expect(editDateInput).toHaveValue(todayStr());

    // Zurück per Abbruch
    await cancelEntryPage(page);

    // ── Cleanup: Fisch löschen ──
    await deleteFish(page, fishName);
  });

  // ── Edit-Save: kein Duplikat (Bug 3 Regression-Test) ───────

  test('Fisch bearbeiten — Speichern aktualisiert den Eintrag, erstellt kein Duplikat', async ({ page }) => {
    const fishName = `E2E-EditTest-${Date.now()}`;

    // 1. Fisch anlegen
    await openAddFishPage(page);
    await fillRequiredEntryFields(page, fishName, todayStr());
    await saveEntryPage(page);
    await expect(page.locator('#fishStockForm')).toContainText(fishName, { timeout: 10_000 });

    // 2. Anzahl der Zeilen mit diesem Fischnamen zählen (sollte exakt 1 sein)
    const rowsBefore = await page.locator('#fishStockForm tr').filter({ hasText: fishName }).count();
    expect(rowsBefore).toBe(1);

    // 3. Edit-Page öffnen
    await openEditFishPage(page, fishName);

    // 4. Nickname hinzufügen und speichern
    const nicknameInput = page.locator('#fishEntryForm [id$="nickname"]');
    await expect(nicknameInput).toBeVisible({ timeout: 5_000 });
    await nicknameInput.fill('Nemo-Edit-Test');
    await saveEntryPage(page);

    // 5. KERN-ASSERTION: Es darf KEIN Duplikat entstanden sein — immer noch genau 1 Zeile
    const rowsAfter = await page.locator('#fishStockForm tr').filter({ hasText: fishName }).count();
    expect(rowsAfter).toBe(1);

    // 6. Die Editierung wirkt sich sichtbar aus: Nickname in der Zeile
    const fishRow = page.locator('#fishStockForm tr').filter({ hasText: fishName }).first();
    await expect(fishRow).toContainText('Nemo-Edit-Test');

    // 7. Cleanup: Fisch löschen
    await deleteFish(page, fishName);
  });

  // ── Foto-Upload ─────────────────────────────────────────────

  test('Foto-Upload: Datei kann ausgewählt werden und accept-Attribut stimmt', async ({ page }) => {
    await openAddFishPage(page);

    // File-Input finden (plain HTML input, kein PrimeFaces)
    const fileInput = page.locator('#photoFileInput');
    await expect(fileInput).toBeAttached();

    // accept-Attribut prüfen (nur Bildformate)
    const accept = await fileInput.getAttribute('accept');
    expect(accept).toContain('image/jpeg');
    expect(accept).toContain('image/png');

    // Testbild setzen → onchange-JS zeigt den Dateinamen an
    const testImagePath = path.resolve(__dirname, 'fixtures', 'test-fish.png');
    await fileInput.setInputFiles(testImagePath);
    const fileNameDisplay = page.locator('#photoFileNameDisplay');
    await expect(fileNameDisplay).toContainText('test-fish.png', { timeout: 5_000 });

    // Kein Fehler in der Message-Area
    const errorMessages = page.locator('#fishEntryMessages .ui-messages-error');
    await expect(errorMessages).not.toBeVisible({ timeout: 3_000 });

    // Zurück per Abbruch
    await cancelEntryPage(page);
  });

  test('Foto-Upload: Thumbnail in Tabelle sichtbar, Vorschau auf der Edit-Seite', async ({ page }) => {
    const fishName = `E2E-FotoTest-${Date.now()}`;
    const testImagePath = path.resolve(__dirname, 'fixtures', 'test-fish.png');

    // ── 1. Fisch anlegen + Foto hochladen ──
    await openAddFishPage(page);
    await fillRequiredEntryFields(page, fishName, todayStr());

    // Foto setzen (wird nach dem Speichern per fetch POST /secured/fishPhoto hochgeladen)
    const fileInput = page.locator('#photoFileInput');
    await fileInput.setInputFiles(testImagePath);

    await saveEntryPage(page);
    await expect(page.locator('#fishStockForm')).toContainText(fishName, { timeout: 10_000 });

    // ── 2. Thumbnail in "Currently in Tank"-Tabelle prüfen ──
    const fishRow = page.locator('#fishStockForm tr').filter({ hasText: fishName }).first();
    await expect(fishRow).toBeVisible({ timeout: 5_000 });

    // KERN-ASSERTION 1: <img> statt Placeholder — das Thumbnail muss sichtbar sein
    const thumbnail = fishRow.locator('img');
    await expect(thumbnail).toBeVisible({ timeout: 8_000 });

    // Thumbnail-Größe prüfen (nicht 0px — AGENTS.md CSS-Sichtbarkeit)
    const styles = await page.evaluate((fishNameStr) => {
      const rows = Array.from(document.querySelectorAll('tr'));
      const row = rows.find(r => r.textContent?.includes(fishNameStr));
      if (!row) return { error: 'row not found' };
      const img = row.querySelector('img');
      if (!img) return { error: 'img not found' };
      const cs = window.getComputedStyle(img);
      return { width: cs.width, height: cs.height, display: cs.display, src: img.getAttribute('src') };
    }, fishName);
    expect(styles).not.toHaveProperty('error');
    expect(styles.display).not.toBe('none');
    expect(styles.src).toContain('fishPhoto');

    // ── 3. Edit-Page öffnen — Foto-Vorschau muss sichtbar sein ──
    await openEditFishPage(page, fishName);

    // KERN-ASSERTION 2: Foto-Vorschau auf der Edit-Seite sichtbar (id="currentPhotoPreview")
    const photoPreview = page.locator('[id$="currentPhotoPreview"]');
    await expect(photoPreview).toBeVisible({ timeout: 8_000 });

    // Auch per computed styles validieren (AGENTS.md)
    const previewStyles = await page.evaluate(() => {
      const img = document.querySelector('[id$="currentPhotoPreview"]');
      if (!img) return { error: 'not found' };
      const cs = window.getComputedStyle(img);
      return { width: cs.width, height: cs.height, display: cs.display };
    });
    expect(previewStyles).not.toHaveProperty('error');
    expect(previewStyles.display).not.toBe('none');
    expect(previewStyles.width).not.toBe('0px');
    expect(previewStyles.height).not.toBe('0px');

    await cancelEntryPage(page);

    // ── Cleanup ──
    await deleteFish(page, fishName);
  });

  // ── Katalog-Vorschlag ──────────────────────────────────────

  test('Katalog-Vorschlag: Link navigiert zur Proposal-Seite mit i18n-Tabs', async ({ page }) => {
    await openAddFishPage(page);

    // "Neuen Katalogeintrag vorschlagen"-Link finden
    const proposalLink = page.locator('#fishEntryForm a').filter({ hasText: /vorschlagen|propose/i });
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

  test('Katalog-Vorschlag: Formular ausfüllen und speichern', async ({ page }) => {
    const uniqueName = `E2E-Testspecies-${Date.now()}`;

    // 1. Direkt zur Proposal-Seite navigieren
    await page.goto('/secured/fishCatalogueProposalForm.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('[id$="proposalForm"]')).toBeAttached({ timeout: 10_000 });

    // 2. Pflichtfeld: Wissenschaftlicher Name ausfüllen
    const scientificNameInput = page.locator('[id$="scientificName"]');
    await expect(scientificNameInput).toBeVisible();
    await scientificNameInput.fill(uniqueName);
    // Blur auslösen, um den onScientificNameBlur-AJAX zu triggern
    await page.locator('[id$="proposalForm"]').click();
    await page.waitForTimeout(500);

    // 3. Erster i18n-Tab (DE): Allgemeinname ausfüllen
    // Erster Tab sollte schon aktiv sein (DE)
    const firstTabCommonName = page.locator('.ui-tabs-panel').first().locator('input').first();
    await expect(firstTabCommonName).toBeVisible({ timeout: 5_000 });
    await firstTabCommonName.fill(`E2E-Testfisch-DE-${Date.now()}`);

    // 4. Kein ???-Platzhalter auf der Seite (i18n-Keys korrekt aufgelöst)
    await expect(page.locator('body')).not.toContainText('???common.chars.remaining.l???');

    // 5. Speichern klicken
    const saveButton = page.locator('[id$="proposalForm"] button').filter({ hasText: /speicher|save/i }).first();
    await expect(saveButton).toBeVisible();

    await Promise.all([
      page.waitForResponse(resp =>
          resp.url().includes('fishCatalogueProposalForm') && resp.status() === 200
          && resp.request().method() === 'POST',
        { timeout: 15_000 },
      ),
      saveButton.click(),
    ]);
    await page.waitForLoadState('networkidle');

    // 6. Erfolgsmeldung prüfen — MUSS eine Info-Nachricht sein, KEINE Fehlermeldung
    const messagesArea = page.locator('[id$="proposalMessages"]');
    await expect(messagesArea).toBeAttached({ timeout: 5_000 });

    // Prüfe, dass kein ???-Platzhalter in der Nachricht steht
    const messagesText = await messagesArea.textContent();
    expect(messagesText).not.toContain('???');

    // Es darf KEINE Fehlermeldung sein (weder i18n-Key noch resolved Error-Text)
    const errorMsg = page.locator('[id$="proposalMessages"] .ui-messages-error');
    await expect(errorMsg).not.toBeVisible({ timeout: 2_000 });

    // Es MUSS eine Info/Success-Nachricht sichtbar sein
    const infoMsg = page.locator('[id$="proposalMessages"] .ui-messages-info');
    await expect(infoMsg).toBeVisible({ timeout: 3_000 });
  });

  // ── Katalog-Suche (AutoComplete) ────────────────────────────

  test('Katalog-Suche: AutoComplete-Feld reagiert auf Eingabe ohne Fehler', async ({ page }) => {
    await openAddFishPage(page);

    // AutoComplete-Input finden
    const acInput = page.locator('#fishEntryForm [id$="catalogueSearch_input"]');
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

    // Zurück per Abbruch
    await cancelEntryPage(page);
  });

});
