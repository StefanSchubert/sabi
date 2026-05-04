/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * E2E-Test: Fish Catalogue Proposal → Admin Approval → Fish Entry with approved catalogue entry
 *
 * Testuser: sabi@bluewhale.de / clibanarius  (Admin-User)
 * App läuft auf http://localhost:8088
 */

import { test, expect, Page } from '@playwright/test';

// Unique scientific name to avoid conflicts with existing entries
const SCIENTIFIC_NAME = `Testus playwrightus ${Date.now()}`;
const COMMON_NAME_DE = 'Playwright Testfisch';

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
async function selectPrimeFacesOption(page: Page, optionText: string) {
  const trigger = page.locator('.ui-selectonemenu .ui-selectonemenu-trigger').first();
  await trigger.click();
  const panel = page.locator('.ui-selectonemenu-panel').first();
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  await panel.locator('li.ui-selectonemenu-item').filter({ hasText: optionText }).click();
}

test.describe('Fish Catalogue: Vorschlag → Freigabe → Fisch anlegen', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });
  test.setTimeout(90_000);

  test('Katalogvorschlag erstellen, freigeben und im Fisch-Dialog nutzen', async ({ page }) => {

    // ─── 1. Login ───────────────────────────────────────────────
    await login(page);
    await expect(page).toHaveURL(/userportal|fishStock/);

    // ─── 2. Katalog-Vorschlagsseite öffnen ──────────────────────
    await page.goto('/secured/fishCatalogueProposalForm.xhtml');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('h2')).toBeVisible({ timeout: 8_000 });

    // ─── 3. Wissenschaftlichen Namen eingeben ────────────────────
    const scientificNameInput = page.locator('#proposalForm\\:scientificName, [id$="scientificName"]').first();
    await expect(scientificNameInput).toBeVisible({ timeout: 8_000 });
    await scientificNameInput.fill(SCIENTIFIC_NAME);
    // Blur auslösen (Duplicate-Check)
    await scientificNameInput.press('Tab');
    await page.waitForLoadState('networkidle');

    // ─── 4. Deutschen Allgemeinnamen im ersten Tab eintragen ─────
    // p:inputText ohne explizite id bekommt auto-generierte JSF-ID.
    // Wir nehmen das erste sichtbare text-Input im aktiven (DE) Tab-Panel.
    // Das DE-Tab ist beim Öffnen aktiv → sein Panel hat KEIN display:none.
    const firstCommonName = page.locator('.ui-tabs-panel').first().locator('input[type="text"]').first();
    await expect(firstCommonName).toBeVisible({ timeout: 8_000 });
    await firstCommonName.fill(COMMON_NAME_DE);

    // ─── 5. Formular absenden ────────────────────────────────────
    const saveBtn = page.locator('#proposalForm button').filter({ hasText: /speichern|save/i }).first();
    await expect(saveBtn).toBeVisible({ timeout: 5_000 });
    await saveBtn.click();
    await page.waitForLoadState('networkidle');

    // ─── 6. Katalogübersicht mit neuem Eintrag prüfen ────────────
    // Nach Submit soll die Katalogübersicht erscheinen (Panel mit Tabelle)
    const overviewPanel = page.locator('[id$="catalogueOverviewPanel"]').first();
    await expect(overviewPanel).toBeVisible({ timeout: 10_000 });

    // Der neue Eintrag soll den Status "In Prüfung" haben (vollständiger Name = eindeutig)
    const pendingRow = overviewPanel.locator('tr').filter({ hasText: SCIENTIFIC_NAME }).first();
    await expect(pendingRow).toBeVisible({ timeout: 8_000 });
    await expect(pendingRow).toContainText(/In Pr|review|Pending/i);

    // Screenshot zur Verifikation
    await page.screenshot({ path: '/tmp/catalogue_submitted.png' });

    // ─── 7. Admin-Seite öffnen ───────────────────────────────────
    await page.goto('/secured/admin/fishCatalogueAdminView.xhtml');
    await page.waitForLoadState('networkidle');

    // ─── 8. Vorschlag in der Admin-Tabelle prüfen ────────────────
    const adminTable = page.locator('#adminForm\\:pendingTable, [id$="pendingTable"]');
    await expect(adminTable).toBeVisible({ timeout: 10_000 });

    // Screenshot: Admin-Tabelle direkt nach Laden
    await page.screenshot({ path: '/tmp/catalogue_admin_pending.png' });

    const proposalRow = page.locator('tr').filter({ hasText: SCIENTIFIC_NAME }).first();
    await expect(proposalRow).toBeVisible({ timeout: 8_000 });

    // ─── 9. Detail-Dialog öffnen ─────────────────────────────────
    const openDetailBtn = proposalRow.locator('button.ui-button').first();
    await expect(openDetailBtn).toBeVisible({ timeout: 5_000 });
    await openDetailBtn.click();

    const detailDialog = page.locator('.ui-dialog').filter({ hasText: SCIENTIFIC_NAME.substring(0, 20) });
    await expect(detailDialog).toBeVisible({ timeout: 8_000 });

    await page.screenshot({ path: '/tmp/catalogue_admin_dialog.png' });

    // ─── 10. Freigeben ───────────────────────────────────────────
    const approveBtn = detailDialog.locator('button').filter({ hasText: /genehmigen|freigeben|approve/i }).first();
    await expect(approveBtn).toBeVisible({ timeout: 5_000 });
    await approveBtn.click();
    await page.waitForLoadState('networkidle');

    // Dialog sollte sich schließen
    await expect(detailDialog).not.toBeVisible({ timeout: 8_000 });

    // Eintrag sollte aus der PENDING-Liste verschwunden sein (vollständiger Name = eindeutig)
    await expect(page.locator('tr').filter({ hasText: SCIENTIFIC_NAME }).first())
        .not.toBeVisible({ timeout: 8_000 });

    await page.screenshot({ path: '/tmp/catalogue_admin_approved.png' });

    // ─── 11. Fisch-Stock-View → Fisch hinzufügen ─────────────────
    await page.goto('/secured/fishStockView.xhtml');
    await page.waitForLoadState('networkidle');

    // waitForResponse MUSS vor dem Click registriert sein!
    const ajaxDone = page.waitForResponse(
        resp => resp.url().includes('fishStockView') && resp.request().method() === 'POST',
        { timeout: 15_000 }
    );
    // Aquarium auswählen
    const trigger = page.locator('.ui-selectonemenu .ui-selectonemenu-trigger').first();
    await trigger.click();
    const selPanel = page.locator('.ui-selectonemenu-panel').first();
    await selPanel.waitFor({ state: 'visible', timeout: 8_000 });
    await selPanel.locator('li.ui-selectonemenu-item').filter({ hasText: /Nano-Reef|Freshwater/i }).first().click();
    await ajaxDone;
    await page.waitForLoadState('networkidle');

    // "Fisch hinzufügen"-Button (wird enabled nachdem Aquarium geladen)
    const addButton = page.locator('#fishStockForm button').filter({ hasText: /hinzuf|add/i }).first();
    await expect(addButton).toBeEnabled({ timeout: 20_000 });
    await addButton.click();

    const dialog = page.locator('.ui-dialog').filter({ hasText: /hinzuf|add fish/i }).first();
    await expect(dialog).toBeVisible({ timeout: 10_000 });

    // ─── 12. Katalog-AutoComplete: freigegebenen Eintrag suchen ──
    const catalogueSearch = dialog.locator('.ui-autocomplete input[type="text"]').first();
    await expect(catalogueSearch).toBeVisible({ timeout: 5_000 });

    // PrimeFaces AutoComplete benötigt echte Keyboard-Events (fill allein reicht nicht)
    await catalogueSearch.click();
    await catalogueSearch.clear();
    const searchTerm = SCIENTIFIC_NAME.substring(0, 10);
    await page.keyboard.type(searchTerm, { delay: 80 });

    // AutoComplete-Panel abwarten
    const acPanel = page.locator('#fishEntryForm\\:catalogueSearch_panel').first();
    await expect(acPanel).toBeVisible({ timeout: 10_000 });

    // Freigegebener Eintrag muss in den Vorschlägen enthalten sein
    const suggestion = acPanel.locator('li').filter({ hasText: SCIENTIFIC_NAME.substring(0, 20) }).first();
    await expect(suggestion).toBeVisible({ timeout: 8_000 });

    await page.screenshot({ path: '/tmp/catalogue_autocomplete.png' });

    // Eintrag auswählen
    await suggestion.click();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);

    // Wissenschaftlicher Name wurde übernommen (nach AJAX-Update)
    const scientificField = dialog.locator('[id$="scientificName"], input[id*="scientificName"]').first();
    await expect(scientificField).toHaveValue(new RegExp(SCIENTIFIC_NAME.substring(0, 15)), { timeout: 10_000 });

    await page.screenshot({ path: '/tmp/catalogue_fish_entry_filled.png' });
  });

});

