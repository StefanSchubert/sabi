/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Measurement View Re-Entry (007-water-maintenance-tracking, issue 5)
 *
 * Covers:
 *  - Re-entry — the tank selected in the measurement form is persisted in the session
 *    and restored when the page is re-entered (previously: empty tank selection after
 *    leaving and coming back).
 *
 * Rules (AGENTS.md): REAL clicks only (no force, no page.evaluate clicks),
 * explicit visibility assertions.
 *
 * Notes:
 *  - The tank selector is a PF p:selectOneMenu (form id "measureform" — lowercase!) →
 *    panel dance (click the visible .ui-selectonemenu-label, pick li.ui-selectonemenu-item
 *    in the body-appended panel). Source of truth for the current value is the hidden
 *    <select> (see e2e/E2E_NOTES.md).
 *  - The UI language is per-user (not the browser locale) → text assertions are bilingual
 *    (de/en regex).
 *
 * Testuser: sabi@bluewhale.de / clibanarius (Tanks: Nano-Reef id=1, Freshwater id=2)
 * App: http://localhost:8088
 */
import { test, expect, Page } from '@playwright/test';

const RE = {
  selectPlaceholder: /Select one|Auswählen/,
};

async function login(page: Page, username = 'sabi@bluewhale.de', password = 'clibanarius') {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

/** Wartet auf die PF-AJAX-POST-Antwort der Measurement-View. */
function waitForAjaxPost(page: Page) {
  return page.waitForResponse(
    (resp) => resp.request().method() === 'POST' && resp.url().includes('measureView.xhtml'),
    { timeout: 15_000 }
  );
}

test.describe('Measurement View Re-Entry', () => {

  test.use({ viewport: { width: 1280, height: 1024 } });

  test('Re-Entry: Tank-Auswahl bleibt nach Reload erhalten', async ({ page }) => {
    await login(page);
    await page.goto('/secured/measureView.xhtml', { waitUntil: 'networkidle' });
    await expect(page.locator('#measureform')).toBeAttached({ timeout: 8_000 });

    // PF 15: das versteckte <select> hat den Client-ID mit _input-Suffix (measureform:tankselection_input)
    const menu = page.locator('#measureform .ui-selectonemenu:has(select[id$=":tankselection_input"])').first();
    const label = menu.locator('.ui-selectonemenu-label');

    // Frische Session, 2 Tanks → kein Auto-Select, Placeholder sichtbar
    await expect(label).toBeVisible({ timeout: 8_000 });
    await expect(label).toHaveText(RE.selectPlaceholder);

    // Freshwater (id=2) im PF-Panel auswählen (itemSelect persistiert den Tank in der Session)
    await label.click(); // öffnet das Panel (reines Client-Side-Rendering, kein POST)
    const panel = page.locator('[id$=":tankselection_panel"]');
    await expect(panel).toBeVisible({ timeout: 8_000 });
    const item = panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Freshwater' });
    await expect(item).toBeVisible();
    await Promise.all([waitForAjaxPost(page), item.click()]);
    await page.waitForLoadState('networkidle');

    // Verstecktes <select> ist die Quelle der Wahrheit (Option-Wert = Tank-ID)
    const select = menu.locator('select');
    expect(await select.inputValue()).toBe('2');

    // Reload → Tank wird aus der Session wiederhergestellt (Issue 5)
    await page.reload({ waitUntil: 'networkidle' });
    const labelAfterReload = page.locator('#measureform .ui-selectonemenu:has(select[id$=":tankselection_input"])')
      .first().locator('.ui-selectonemenu-label');
    await expect(labelAfterReload).toContainText('Freshwater', { timeout: 8_000 });
    await expect(labelAfterReload).not.toHaveText(RE.selectPlaceholder);
  });

});
