/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * E2E-Test: Fish Departure Workflow
 *
 * Ablauf:
 *  1. Login + Aquarium wählen (Nano-Reef)
 *  2. Neuen Testfisch anlegen
 *  3. Departure-Button klicken → Dialog öffnet sich
 *  4. Datum und Grund (Gestorben) auswählen und speichern
 *  5. Fisch verschwindet aus "Currently in Tank"
 *  6. Fisch erscheint unter "Abgänge" / "Departed Fish"
 *
 * Testuser: sabi@bluewhale.de / clibanarius
 * App:      http://localhost:8088
 */

import { test, expect, Page } from '@playwright/test';

const FISH_NAME = `Departure-Test ${Date.now()}`;

// ── Helpers (gleiche Muster wie fishStockView.spec.ts) ────────────────────────

async function login(page: Page) {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

async function waitForFishStockAjax(page: Page) {
  await page.waitForResponse(
    resp => resp.url().includes('fishStockView') && resp.status() === 200
            && resp.request().method() === 'POST',
    { timeout: 15_000 }
  );
  await page.waitForLoadState('networkidle');
}

async function selectPrimeFacesOption(page: Page, optionText: string) {
  const trigger = page.locator('.ui-selectonemenu .ui-selectonemenu-trigger').first();
  await trigger.click();
  const panel = page.locator('.ui-selectonemenu-panel').first();
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  await panel.locator('li.ui-selectonemenu-item').filter({ hasText: optionText }).click();
}

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

// ─────────────────────────────────────────────────────────────────────────────

test.describe('Fish Departure Workflow', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });
  test.setTimeout(90_000);

  test('Fisch anlegen, Abgang erfassen, unter Abgänge prüfen', async ({ page }) => {

    // ─── 1. Login ────────────────────────────────────────────
    await login(page);
    await page.goto('/secured/fishStockView.xhtml', { waitUntil: 'networkidle' });

    // ─── 2. Fisch anlegen (Aquarium + Dialog öffnen) ─────────
    const entryDialog = await openAddFishDialog(page);

    const commonNameInput = entryDialog.locator('[id$="commonName"]');
    await commonNameInput.fill(FISH_NAME);

    const today = new Date();
    const dateStr = String(today.getDate()).padStart(2, '0') + '.'
                  + String(today.getMonth() + 1).padStart(2, '0') + '.'
                  + today.getFullYear();
    const dateInput = entryDialog.locator('[id$="addedOn_input"]');
    await dateInput.fill(dateStr);
    await commonNameInput.click(); // DatePicker schließen + blur
    await page.waitForTimeout(300);

    // Speichern (force:true wegen möglichem DatePicker-Overlay)
    const saveButton = entryDialog.locator('button').filter({ hasText: /speicher|save/i });
    await saveButton.scrollIntoViewIfNeeded();
    await Promise.all([
      page.waitForResponse(resp =>
        resp.url().includes('fishStockView') && resp.status() === 200
        && resp.request().method() === 'POST',
        { timeout: 15_000 }
      ),
      saveButton.click({ force: true }),
    ]);
    await page.waitForLoadState('networkidle');
    await expect(entryDialog).not.toBeVisible({ timeout: 10_000 });

    const fishRow = page.locator('#fishStockForm tr').filter({ hasText: FISH_NAME }).first();
    await expect(fishRow).toBeVisible({ timeout: 8_000 });
    await page.screenshot({ path: '/tmp/departure_01_fish_created.png' });

    // ─── 3. Departure-Button klicken (orange pi-arrow-right) ─
    const departureBtn = fishRow.locator('button.ui-button-warning').first();
    await expect(departureBtn).toBeVisible({ timeout: 5_000 });
    await Promise.all([
      page.waitForResponse(resp =>
        resp.url().includes('fishStockView') && resp.status() === 200
        && resp.request().method() === 'POST',
        { timeout: 10_000 }
      ),
      departureBtn.click(),
    ]);

    // ─── 4. Departure-Dialog befüllen ────────────────────────
    const depDialog = page.locator('#fishDepartureDialog');
    await expect(depDialog).toBeVisible({ timeout: 8_000 });
    await page.screenshot({ path: '/tmp/departure_02_dialog_open.png' });

    const depDateInput = depDialog.locator('[id$="departureDate_input"]').first();
    await expect(depDateInput).toBeVisible({ timeout: 5_000 });
    await depDateInput.fill(dateStr);
    // DatePicker durch Klick auf Dialog-Titel schließen (freie Fläche außerhalb des Pickers)
    await depDialog.locator('.ui-dialog-title').click();
    await page.waitForTimeout(300);

    // Abgangsgrund: "Gestorben" / DECEASED
    // SelectOneMenu direkt über Label-Element klicken (Trigger kann width:0 haben im Dialog)
    const reasonLabel = depDialog.locator('.ui-selectonemenu-label').first();
    await expect(reasonLabel).toBeVisible({ timeout: 5_000 });
    await reasonLabel.click();
    const reasonPanel = page.locator('.ui-selectonemenu-panel').last();
    await reasonPanel.waitFor({ state: 'visible', timeout: 5_000 });
    await reasonPanel.locator('li.ui-selectonemenu-item').filter({ hasText: /gestorben|deceased/i }).first().click();
    await page.screenshot({ path: '/tmp/departure_03_form_filled.png' });

    // Speichern
    const depSaveBtn = page.locator('#fishDepartureForm button').filter({ hasText: /speicher|save/i }).first();
    await expect(depSaveBtn).toBeVisible({ timeout: 5_000 });
    await Promise.all([
      page.waitForResponse(resp =>
        resp.url().includes('fishStockView') && resp.status() === 200
        && resp.request().method() === 'POST',
        { timeout: 10_000 }
      ),
      depSaveBtn.click(),
    ]);
    await page.waitForLoadState('networkidle');
    await expect(depDialog).not.toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: '/tmp/departure_04_saved.png' });

    // Seite neu laden: stellt sicher dass Server-seitige Aktualisierung reflektiert wird
    await page.reload({ waitUntil: 'networkidle' });
    // Aquarium wieder auswählen nach Reload
    await Promise.all([
      page.waitForResponse(resp =>
        resp.url().includes('fishStockView') && resp.status() === 200
        && resp.request().method() === 'POST',
        { timeout: 15_000 }
      ),
      selectPrimeFacesOption(page, 'Nano-Reef'),
    ]);
    await page.waitForLoadState('networkidle');

    // ─── 5. Fisch NICHT mehr in "Currently in Tank" ───────────
    await expect(
      page.locator('#fishStockForm tr').filter({ hasText: FISH_NAME }).first()
    ).not.toBeVisible({ timeout: 8_000 });

    // ─── 6. Departed-Fieldset aufklappen und Fisch prüfen ────
    const departedFieldset = page.locator('.ui-fieldset').filter({ hasText: /abg|departed/i }).first();
    await expect(departedFieldset).toBeVisible({ timeout: 8_000 });
    await departedFieldset.locator('.ui-fieldset-toggler').first().click();
    await page.waitForLoadState('networkidle');

    const departedTable = page.locator('[id$="departedFishTable"]').first();
    await expect(departedTable).toBeVisible({ timeout: 8_000 });
    const departedRow = departedTable.locator('tr').filter({ hasText: FISH_NAME }).first();
    await expect(departedRow).toBeVisible({ timeout: 8_000 });
    await expect(departedRow).toContainText(String(today.getFullYear()));
    await expect(departedRow).toContainText(/gestorben|deceased/i);

    await page.screenshot({ path: '/tmp/departure_05_departed_list.png' });
  });
});

