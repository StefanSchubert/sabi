/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Maintenance Flow (007-water-maintenance-tracking)
 *
 * Covers:
 *  1. Maintenance View — basic rendering, all three sections, CSS visibility of interactive elements
 *  2. Tank selection via PrimeFaces selectOneMenu
 *  3. Regular interval — set, persist across reload, clear
 *  4. Water change records — create (today + back-dated), newest-first order, edit, delete
 *  5. Activated carbon — enable, mode required, INTERVAL range validation, CONTINUOUS, disable (FR-011)
 *  6. Supplements cross-link — opens dosing view with tank pre-selected
 *
 * Rules (AGENTS.md): REAL clicks only (no force, no page.evaluate clicks),
 * explicit visibility assertions, computed width/height > 0 checks, cleanup of created records.
 *
 * Notes:
 *  - JSF component client-IDs are form-prefixed (maintenanceForm:xxx) → suffix matchers [id$=":xxx"].
 *  - The UI language is per-user (not the browser locale) → text assertions are bilingual
 *    (de/en regex), following the convention of aquariumEvents.spec.ts.
 *
 * Testuser: sabi@bluewhale.de / clibanarius (Tank: Nano-Reef)
 * App: http://localhost:8088
 */
import { test, expect, Page } from '@playwright/test';
import * as fs from 'fs';

// ──────────────────────────────────────────────────────────────
// TEMPORARY instrumentation (diagReal.spec.ts — DELETE AFTER USE)
// ──────────────────────────────────────────────────────────────

/** Injects a JS-error + PrimeFaces.ab/Queue logger into the page. */
async function instrumentPage(page: Page) {
  await page.addInitScript(() => {
    (window as any).__diag = { abCalls: [] as unknown[], errors: [] as unknown[], queueLog: [] as unknown[] };
    window.addEventListener('error', (e) => {
      ((window as any).__diag.errors as unknown[]).push({
        t: Date.now(), msg: String(e.message).slice(0, 250),
        src: `${String(e.filename || '').slice(-60)}:${e.lineno}`,
      });
    });
  });
}

/** Wraps PrimeFaces.ab + ajax Queue (call after the maintenance view has loaded). */
async function instrumentPrimeFaces(page: Page) {
  await page.evaluate(() => {
    const PFw = (window as any).PrimeFaces;
    if (!PFw || (window as any).__diagInstrumented) return;
    (window as any).__diagInstrumented = true;
    const origAb = PFw.ab;
    PFw.ab = function (cfg: any, ext?: unknown) {
      ((window as any).__diag.abCalls as unknown[]).push({
        t: Date.now(), s: cfg?.s, e: cfg?.e,
        qLenBefore: PFw.ajax?.Queue?.requests?.length ?? -1,
      });
      return origAb(cfg, ext);
    };
    const Q = PFw.ajax?.Queue;
    if (Q) {
      const origOffer = Q.offer.bind(Q);
      Q.offer = function (a: any) {
        ((window as any).__diag.queueLog as unknown[]).push({ t: Date.now(), op: 'offer', qLenBefore: Q.requests.length });
        return origOffer(a);
      };
      const origPoll = Q.poll.bind(Q);
      Q.poll = function () {
        const r = origPoll();
        ((window as any).__diag.queueLog as unknown[]).push({ t: Date.now(), op: 'poll', qLenAfter: Q.requests.length });
        return r;
      };
    }
  });
}

test.afterEach(async ({ page }, testInfo) => {
  try {
    const d = await page.evaluate(() => ({
      abAll: (window as any).__diag?.abCalls ?? [],
      errsAll: (window as any).__diag?.errors ?? [],
      qAll: (window as any).__diag?.queueLog ?? [],
    }));
    const safe = testInfo.title.replace(/[^a-z0-9]+/gi, '_').slice(0, 60);
    fs.writeFileSync(`/tmp/diag-real-${safe}.txt`, JSON.stringify({ status: testInfo.status, ...d }, null, 2));
  } catch { /* page may be closed */ }
});

// ──────────────────────────────────────────────────────────────
// Constants: bilingual labels (de/en)
// ──────────────────────────────────────────────────────────────
const RE = {
  pageTitle: /Maintenance|Wartung/,
  waterChangesSection: /Water Changes|Wasserwechsel/,
  supplementsSection: /Supplements|Zusatzmittel/,
  carbonSection: /Activated Carbon|Aktivkohle/,
  crossLink: /Open Dosing view|Dosierungsfunktion öffnen/,
  carbonNotUsed: /Not used|Nicht in Gebrauch/,
  modeInterval: /Interval \(replace regularly\)|Intervall \(regelmäßig wechseln\)/,
  modeContinuous: /Continuous \(always in the filter\)|Dauerhaft \(immer im Filter\)/,
  selectPlaceholder: /Select one|Auswählen/,
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

/** Wartet auf die PF-AJAX-POST-Antwort der Maintenance-View (muss VOR dem Klick
 *  registriert werden — networkidle allein raced mit der XHR-Dispatch und das
 *  @form-Re-Render würde eingegebene Werte löschen). */
function waitForAjaxPost(page: Page) {
  return page.waitForResponse(
    (resp) => resp.request().method() === 'POST' && resp.url().includes('maintenanceView.xhtml'),
    { timeout: 15_000 }
  );
}

/**
 * Wählt Nano-Reef im Tank-Selektor aus (PrimeFaces selectOneMenu, erstes Menü im
 * maintenanceForm — das carbonMode-Menü folgt in der DOM-Reihenfolge).
 * Wartet auf die AJAX-Antwort, damit das @form-Re-Render vor weiterer Interaktion abgeschlossen ist.
 *
 * Idempotent: PF 15 feuert bei Wiederwahl des bereits gewählten Eintrags KEIN change-Event
 * (selectItem() vergleicht den Option-Wert mit dem aktuell selektierten) → kein AJAX-POST.
 * Quelle der Wahrheit ist daher der Wert des versteckten <select>, NICHT das Label-Text:
 * PF 15 setzt das Label nach Escape auf den Platzhalter zurück, behält aber den Select-Wert
 * (client-seitig, ohne POST) — Label-Check würde hier zu einem unnötigen Re-Klick führen,
 * der keinen POST auslöst (gleicher Wert) und damit in einem Timeout endet.
 * Nach einem Reload ist die Auswahl NICHT persistiert (viewMap lebt nur im ViewState) →
 * Select-Wert leer → vollständige Auswahl mit POST.
 */
async function selectTank(page: Page, tankName = 'Nano-Reef') {
  const tankMenu = page.locator('#maintenanceForm .ui-selectonemenu').first();
  const select = tankMenu.locator('select');
  await expect(select).toBeAttached({ timeout: 8_000 });
  const desiredValue = await select.locator('option').filter({ hasText: tankName }).getAttribute('value');
  if ((await select.inputValue()) === desiredValue) {
    return; // Tank bereits ausgewählt — Wiederwahl würde keinen POST auslösen (PF 15)
  }
  const trigger = tankMenu.locator('.ui-selectonemenu-trigger').first();
  await trigger.click();
  // Panel-Locator auf das Tank-Menü begrenzen — globale .first() wäre mehrdeutig,
  // wenn zusätzlich das carbonMode-Panel im DOM existiert.
  const panel = page.locator('[id$=":aquarium_panel"]');
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  const option = panel.locator('li.ui-selectonemenu-item').filter({ hasText: tankName });
  await expect(option).toBeVisible();
  await Promise.all([waitForAjaxPost(page), option.click()]);
  await page.waitForLoadState('networkidle');
}

/** dd.MM.yyyy-Formatierung (explizites Picker-Pattern der Seite). */
function formatDate(d: Date): string {
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}.${mm}.${d.getFullYear()}`;
}

/** CSS-Sichtbarkeit prüfen (AGENTS.md): bounding box width/height > 0, display != none, visibility != hidden.
 *  getBoundingClientRect statt computed width/height — Inline-Elemente (<a>) liefern "auto"/NaN. */
async function expectCssVisible(page: Page, selector: string) {
  const styles = await page.locator(selector).first().evaluate((el) => {
    const cs = window.getComputedStyle(el);
    const rect = el.getBoundingClientRect();
    return { width: rect.width, height: rect.height, display: cs.display, visibility: cs.visibility };
  });
  expect(styles.display, `${selector} must not be display:none`).not.toBe('none');
  expect(styles.visibility, `${selector} must not be visibility:hidden`).not.toBe('hidden');
  expect(styles.width, `${selector} width must be > 0`).toBeGreaterThan(0);
  expect(styles.height, `${selector} height must be > 0`).toBeGreaterThan(0);
}

/** Erfolgsmeldung (FacesMessage SEVERITY_INFO) im maintenanceForm.
 *  PrimeFaces rendert je Severity einen Container: .ui-messages-info / -warn / -error (Plural). */
async function expectSuccessMessage(page: Page) {
  await expect(
    page.locator('[id$=":maintenanceMessages"] .ui-messages-info').first()
  ).toBeVisible({ timeout: 8_000 });
}

/** Validierungs-/Fehlermeldung (SEVERITY_WARN aus der View oder SEVERITY_ERROR vom JSF-Validator). */
async function expectFailureMessage(page: Page) {
  await expect(
    page.locator('[id$=":maintenanceMessages"] .ui-messages-warn, [id$=":maintenanceMessages"] .ui-messages-error').first()
  ).toBeVisible({ timeout: 8_000 });
}

/** Klick auf das sichtbare Checkbox-Box-Wrapper (der rohe _input liegt in
 *  .ui-helper-hidden-accessible und fängt keine Pointer-Events ab). */
async function clickCarbonCheckbox(page: Page) {
  const box = page.locator('[id$=":carbonEnabled"] .ui-chkbox-box').first();
  await expect(box).toBeVisible({ timeout: 8_000 });
  await box.click();
}

// ──────────────────────────────────────────────────────────────
// Tests
// ──────────────────────────────────────────────────────────────

test.describe('Maintenance Flow', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });

  test.beforeEach(async ({ page }) => {
    await instrumentPage(page);
    await login(page);
    await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#maintenanceForm')).toBeAttached({ timeout: 8_000 });
    await instrumentPrimeFaces(page);
    await selectTank(page);
  });

  // ── 1. Basis-Rendering ───────────────────────────────────────

  test('maintenanceView rendert alle drei Sektionen ohne rohe i18n-Keys', async ({ page }) => {
    // Keine rohen Keys / Fehlplatzhalter
    await expect(page.locator('body')).not.toContainText('???');
    await expect(page.locator('body')).not.toContainText('maintenance.page.title');
    await expect(page.locator('body')).not.toContainText('common.select.l');
    await expect(page.locator('body')).not.toContainText('maintenance.carbon');

    // Alle drei Sektions-Überschriften sichtbar (bilingual, locale-unabhängig)
    await expect(page.locator('h3').filter({ hasText: RE.waterChangesSection }).first()).toBeVisible();
    await expect(page.locator('h3').filter({ hasText: RE.supplementsSection }).first()).toBeVisible();
    await expect(page.locator('h3').filter({ hasText: RE.carbonSection }).first()).toBeVisible();

    // Back-Link vorhanden (UI_StyleGuide)
    await expect(page.locator('.sabi-back-link').first()).toBeVisible();

    // Aktivkohle-Sektion: Checkbox sichtbar; je nach Zustand "Nicht in Gebrauch" oder Modus-Menü
    const checkbox = page.locator('[id$=":carbonEnabled_input"]');
    await expect(checkbox).toBeVisible();
    if (await checkbox.isChecked()) {
      await expect(page.locator('#maintenanceForm .ui-selectonemenu').nth(1)).toBeVisible();
    } else {
      await expect(page.locator('#maintenanceForm').getByText(RE.carbonNotUsed).first()).toBeVisible();
    }

    // CSS-Sichtbarkeit aller interaktiven Elemente (computed width/height > 0)
    await expectCssVisible(page, '[id$=":saveIntervalButton"]');
    await expectCssVisible(page, '[id$=":clearIntervalButton"]');
    await expectCssVisible(page, '[id$=":saveWaterChangeButton"]');
    await expectCssVisible(page, '[id$=":newRecordButton"]');
    await expectCssVisible(page, '[id$=":saveCarbonButton"]');
    await expectCssVisible(page, '[id$=":cancelButton"]');
    const crossLink = page.locator('a').filter({ hasText: RE.crossLink });
    await expect(crossLink.first()).toBeVisible();
    await expectCssVisible(page, `a:has-text("Open Dosing view"), a:has-text("Dosierungsfunktion öffnen")`);
  });

  test('Tank-Selektor listet Nano-Reef auf und wählt ihn aus', async ({ page }) => {
    // Dropdown öffnen, Option vorhanden (Panel-Locator auf das Tank-Menü begrenzt)
    const trigger = page.locator('#maintenanceForm .ui-selectonemenu-trigger').first();
    await expect(trigger).toBeVisible();
    await trigger.click();
    const panel = page.locator('[id$=":aquarium_panel"]');
    await panel.waitFor({ state: 'visible', timeout: 8_000 });
    await expect(panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' })).toBeVisible();
    await page.keyboard.press('Escape');

    // Auswählen → Wasserwechsel-Tabelle rendert (leer oder mit Einträgen)
    await selectTank(page);
    await expect(page.locator('[id$=":waterChangeTable"]')).toBeVisible({ timeout: 8_000 });
  });

  // ── 3. Regelmäßiges Intervall ────────────────────────────────

  test('Regelmäßiges Intervall setzen, persistiert nach Reload, wieder löschen', async ({ page }) => {
    // 1. Intervall 21 Tage / 25 % setzen
    const daysInput = page.locator('[id$=":intervalDays"]');
    const pctInput = page.locator('[id$=":intervalPercentage"]');
    await expect(daysInput).toBeVisible();
    await expect(pctInput).toBeVisible();
    await daysInput.fill('21');
    await pctInput.fill('25');

    const saveInterval = page.locator('[id$=":saveIntervalButton"]');
    await expect(saveInterval).toBeVisible();
    await Promise.all([waitForAjaxPost(page), saveInterval.click()]);
    await page.waitForLoadState('networkidle');
    await expectSuccessMessage(page);

    // 2. Persistenz: Reload + Tank neu wählen
    await page.reload({ waitUntil: 'networkidle' });
    await selectTank(page);
    await expect(page.locator('[id$=":intervalDays"]')).toHaveValue('21', { timeout: 8_000 });
    await expect(page.locator('[id$=":intervalPercentage"]')).toHaveValue('25');

    // 3. Intervall löschen
    const clearButton = page.locator('[id$=":clearIntervalButton"]');
    await expect(clearButton).toBeVisible();
    await Promise.all([waitForAjaxPost(page), clearButton.click()]);
    await page.waitForLoadState('networkidle');
    await expectSuccessMessage(page);

    // 4. Nach Reload leer
    await page.reload({ waitUntil: 'networkidle' });
    await selectTank(page);
    await expect(page.locator('[id$=":intervalDays"]')).toHaveValue('', { timeout: 8_000 });
    await expect(page.locator('[id$=":intervalPercentage"]')).toHaveValue('');
  });

  // ── 4. Wasserwechsel-Erfassung ───────────────────────────────

  test('Wasserwechsel erfassen (heute + rückdatiert), Reihenfolge, bearbeiten, löschen', async ({ page }) => {
    const todayNote = `E2E-Heute-${Date.now()}`;
    const pastNote = `E2E-Rueckstand-${Date.now()}`;

    const dateInput = page.locator('[id$=":changedOn_input"]');
    const pctInput = page.locator('[id$=":percentage"]');
    const noteInput = page.locator('[id$=":note"]');
    const saveRecord = page.locator('[id$=":saveWaterChangeButton"]');
    const table = page.locator('[id$=":waterChangeTable"]');

    // 1. Heutiger Wasserwechsel: Datum ist heute vorbefüllt
    await expect(dateInput).toBeVisible();
    await expect(dateInput).toHaveValue(formatDate(new Date()));

    await pctInput.fill('30');
    await noteInput.fill(todayNote);
    await expect(saveRecord).toBeVisible();
    await Promise.all([waitForAjaxPost(page), saveRecord.click()]);
    await page.waitForLoadState('networkidle');

    // Eintrag in der Tabelle sichtbar
    const todayRow = table.locator('tbody tr').filter({ hasText: todayNote });
    await expect(todayRow).toBeVisible({ timeout: 8_000 });

    // 2. Rückdatierter Wasserwechsel (14 Tage zurück, 50 %)
    const resetButton = page.locator('[id$=":newRecordButton"]');
    await expect(resetButton).toBeVisible();
    await Promise.all([waitForAjaxPost(page), resetButton.click()]);
    await page.waitForLoadState('networkidle');

    const past = new Date();
    past.setDate(past.getDate() - 14);
    await dateInput.fill(formatDate(past));
    await pctInput.fill('50');
    await noteInput.fill(pastNote);

    await Promise.all([waitForAjaxPost(page), saveRecord.click()]);
    await page.waitForLoadState('networkidle');

    const pastRow = table.locator('tbody tr').filter({ hasText: pastNote });
    await expect(pastRow).toBeVisible({ timeout: 8_000 });

    // 3. Reihenfolge: neueste zuerst (heute vor rückdatiert)
    const rows = table.locator('tbody tr');
    const todayIndex = await rows.filter({ hasText: todayNote }).first().evaluate((el) =>
      Array.from(el.parentElement.children).indexOf(el));
    const pastIndex = await rows.filter({ hasText: pastNote }).first().evaluate((el) =>
      Array.from(el.parentElement.children).indexOf(el));
    expect(todayIndex, 'heutiger Eintrag muss vor rückdatiertem erscheinen').toBeLessThan(pastIndex);

    // 4. Bearbeiten: Prozentsatz des rückdatierten Eintrags auf 40 ändern
    const editButton = pastRow.locator('button').filter({ has: page.locator('.pi-pencil') });
    await expect(editButton).toBeVisible();
    await Promise.all([waitForAjaxPost(page), editButton.click()]);
    await page.waitForLoadState('networkidle');

    await expect(pctInput).toHaveValue('50');
    await pctInput.fill('40');
    await Promise.all([waitForAjaxPost(page), saveRecord.click()]);
    await page.waitForLoadState('networkidle');

    // Persistenz: Reload → 40 % (Prozent-Zelle, Spalte 1)
    await page.reload({ waitUntil: 'networkidle' });
    await selectTank(page);
    const editedRow = table.locator('tbody tr').filter({ hasText: pastNote });
    await expect(editedRow).toBeVisible({ timeout: 8_000 });
    await expect(editedRow.locator('td').nth(1)).toHaveText('40');

    // 5. Löschen: rückdatierten Eintrag entfernen
    const deleteButton = editedRow.locator('button').filter({ has: page.locator('.pi-trash') });
    await expect(deleteButton).toBeVisible();
    await Promise.all([waitForAjaxPost(page), deleteButton.click()]);
    await page.waitForLoadState('networkidle');
    await expect(table.locator('tbody tr').filter({ hasText: pastNote })).toHaveCount(0);

    // ── Cleanup: heutigen Eintrag löschen ──
    const cleanupRow = table.locator('tbody tr').filter({ hasText: todayNote });
    await expect(cleanupRow).toBeVisible({ timeout: 8_000 });
    const cleanupDelete = cleanupRow.locator('button').filter({ has: page.locator('.pi-trash') });
    await expect(cleanupDelete).toBeVisible();
    await Promise.all([waitForAjaxPost(page), cleanupDelete.click()]);
    await page.waitForLoadState('networkidle');
    await expect(table.locator('tbody tr').filter({ hasText: todayNote })).toHaveCount(0);
  });

  // ── 5. Aktivkohle ────────────────────────────────────────────

  test('Aktivkohle: aktivieren, Modus-Pflicht, INTERVAL-Validierung, CONTINUOUS, deaktivieren', async ({ page }) => {
    const checkbox = page.locator('[id$=":carbonEnabled_input"]');
    await expect(checkbox).toBeVisible();
    const saveCarbon = page.locator('[id$=":saveCarbonButton"]');
    await expect(saveCarbon).toBeVisible();

    // Ausgangszustand sicherstellen: deaktiviert
    if (await checkbox.isChecked()) {
      await Promise.all([waitForAjaxPost(page), clickCarbonCheckbox(page)]);
      await page.waitForLoadState('networkidle');
      await Promise.all([waitForAjaxPost(page), saveCarbon.click()]);
      await page.waitForLoadState('networkidle');
      await expectSuccessMessage(page);
    }

    // "Nicht in Gebrauch" sichtbar
    await expect(page.locator('#maintenanceForm').getByText(RE.carbonNotUsed).first()).toBeVisible();

    // 1. Aktivieren → Modus-Auswahl erscheint (AJAX-Update)
    await Promise.all([waitForAjaxPost(page), clickCarbonCheckbox(page)]);
    await page.waitForLoadState('networkidle');
    const modeMenu = page.locator('#maintenanceForm .ui-selectonemenu').nth(1);
    await expect(modeMenu).toBeVisible({ timeout: 8_000 });

    // 2. Speichern ohne Modus → Warnung aus dem Bean (Modus fehlt, incompleted_formdata)
    await Promise.all([waitForAjaxPost(page), saveCarbon.click()]);
    await page.waitForLoadState('networkidle');
    await expectFailureMessage(page);

    // 3. INTERVAL wählen → Wechselintervall erscheint (AJAX-Update)
    await modeMenu.locator('.ui-selectonemenu-trigger').first().click();
    // Panel-Locator per Client-ID — globale .first() würde das (versteckte) Tank-Panel treffen
    const modePanel = page.locator('[id$=":carbonMode_panel"]');
    await modePanel.waitFor({ state: 'visible', timeout: 8_000 });
    await Promise.all([waitForAjaxPost(page),
      modePanel.locator('li.ui-selectonemenu-item').filter({ hasText: RE.modeInterval }).click()]);
    await page.waitForLoadState('networkidle');

    const replacementInput = page.locator('[id$=":carbonReplacementIntervalDays"]');
    await expect(replacementInput).toBeVisible({ timeout: 8_000 });

    // 4. Out-of-range (999 > 365) → Validierungsfehler
    await replacementInput.fill('999');
    await Promise.all([waitForAjaxPost(page), saveCarbon.click()]);
    await page.waitForLoadState('networkidle');
    await expectFailureMessage(page);

    // 5. Gültiges Intervall (28) → gespeichert
    await replacementInput.fill('28');
    await Promise.all([waitForAjaxPost(page), saveCarbon.click()]);
    await page.waitForLoadState('networkidle');
    await expectSuccessMessage(page);

    // Persistenz: Reload → enabled + INTERVAL + 28
    await page.reload({ waitUntil: 'networkidle' });
    await selectTank(page);
    await expect(page.locator('[id$=":carbonEnabled_input"]')).toBeChecked({ timeout: 8_000 });
    await expect(page.locator('#maintenanceForm .ui-selectonemenu').nth(1).locator('.ui-selectonemenu-label'))
      .toContainText(/Interval|Intervall/);
    await expect(page.locator('[id$=":carbonReplacementIntervalDays"]')).toHaveValue('28');

    // 6. CONTINUOUS wählen → Wechselintervall verschwindet (AJAX-Update)
    const modeMenu2 = page.locator('#maintenanceForm .ui-selectonemenu').nth(1);
    await expect(modeMenu2).toBeVisible();
    await modeMenu2.locator('.ui-selectonemenu-trigger').first().click();
    const modePanel2 = page.locator('[id$=":carbonMode_panel"]');
    await modePanel2.waitFor({ state: 'visible', timeout: 8_000 });
    await Promise.all([waitForAjaxPost(page),
      modePanel2.locator('li.ui-selectonemenu-item').filter({ hasText: RE.modeContinuous }).click()]);
    await page.waitForLoadState('networkidle');
    // Intervall-Zeile ist immer im DOM (rendered=-Gating würde den JSF-Komponentenbaum
    // über Partial-Requests brechen) — bei CONTINUOUS per CSS :has() unsichtbar.
    await expect(page.locator('[id$=":carbonReplacementIntervalDays"]')).not.toBeVisible();

    await Promise.all([waitForAjaxPost(page), saveCarbon.click()]);
    await page.waitForLoadState('networkidle');
    await expectSuccessMessage(page);

    // 7. Deaktivieren → "Nicht in Gebrauch" (FR-011: Modus wird deterministisch geleert)
    const checkbox2 = page.locator('[id$=":carbonEnabled_input"]');
    await expect(checkbox2).toBeChecked();
    await Promise.all([waitForAjaxPost(page), clickCarbonCheckbox(page)]);
    await page.waitForLoadState('networkidle');
    await Promise.all([waitForAjaxPost(page), saveCarbon.click()]);
    await page.waitForLoadState('networkidle');

    // Persistenz: Reload → deaktiviert, "Nicht in Gebrauch"
    await page.reload({ waitUntil: 'networkidle' });
    await selectTank(page);
    await expect(page.locator('[id$=":carbonEnabled_input"]')).not.toBeChecked({ timeout: 8_000 });
    await expect(page.locator('#maintenanceForm').getByText(RE.carbonNotUsed).first()).toBeVisible();
  });

  // ── 6. Cross-Link zur Dosierungsfunktion ─────────────────────

  test('Cross-Link öffnet Dosing-View mit vorselektiertem Tank', async ({ page }) => {
    const link = page.locator('a').filter({ hasText: RE.crossLink });
    await expect(link).toBeVisible({ timeout: 8_000 });

    // CSS-Sichtbarkeit des Links (computed width/height > 0)
    await expectCssVisible(page, 'a:has-text("Open Dosing view"), a:has-text("Dosierungsfunktion öffnen")');

    await link.first().click();
    await page.waitForLoadState('networkidle');

    // Dosing-View geöffnet, Tank vorselektiert (Nano-Reef statt "Select one...")
    await expect(page).toHaveURL(/dosingView/, { timeout: 10_000 });
    const dosingTankMenu = page.locator('#dosingForm .ui-selectonemenu').first();
    await expect(dosingTankMenu.locator('.ui-selectonemenu-label')).toContainText('Nano-Reef', { timeout: 8_000 });
    await expect(dosingTankMenu.locator('.ui-selectonemenu-label')).not.toHaveText(RE.selectPlaceholder);
  });

});
