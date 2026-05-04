/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/*
 * Debug-Test: Netzwerk-Request beim Speichern analysieren
 */
import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

test('Network-Request beim Tank-Speichern analysieren', async ({ page }) => {
  // Network-Requests mitloggen
  const requests: { url: string, method: string, body: string }[] = [];
  const responses: { url: string, status: number, body: string }[] = [];

  page.on('request', req => {
    if (req.url().includes('8080') || req.url().includes('tank')) {
      const body = req.postData() || '';
      requests.push({ url: req.url(), method: req.method(), body: body.substring(0, 500) });
    }
  });

  page.on('response', async resp => {
    if (resp.url().includes('8080') || (resp.url().includes('tank') && !resp.url().includes('.xhtml'))) {
      try {
        const body = await resp.text();
        responses.push({ url: resp.url(), status: resp.status(), body: body.substring(0, 500) });
      } catch (e) {
        responses.push({ url: resp.url(), status: resp.status(), body: 'error reading body' });
      }
    }
  });

  await login(page);
  await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });

  // Ersten Tank bearbeiten
  const editButton = page.locator('[id$="editAction"]').first();
  await expect(editButton).toBeVisible({ timeout: 5_000 });
  await editButton.click();
  await page.waitForLoadState('networkidle');

  // Aktuellen Wert des sizeNet-Feldes prüfen
  const sizeNetInput = page.locator('[id$="sizeNet"]');
  await expect(sizeNetInput).toBeVisible({ timeout: 5_000 });
  const currentVal = await sizeNetInput.inputValue();
  console.log('sizeNet vor Eingabe:', currentVal);

  // View-State und alle Formfeld-IDs ausgeben
  const formInputs = await page.locator('[id*="tankform"] input, [id*="tankform"] select').all();
  console.log('Anzahl Formfelder:', formInputs.length);
  for (const input of formInputs) {
    const id = await input.getAttribute('id');
    const val = await input.inputValue().catch(() => 'N/A');
    console.log(`  Feld [${id}] = "${val}"`);
  }

  // Wert setzen
  await sizeNetInput.clear();
  await sizeNetInput.fill('350');

  // Sicherstellen dass der Wert gesetzt ist
  await expect(sizeNetInput).toHaveValue('350');
  console.log('sizeNet nach Eingabe: 350');

  // Screenshot
  await page.screenshot({ path: '/tmp/tankEditor_before_save.png' });

  // Alle AJAX-Requests vor dem Klick aufzeichnen
  const networkLog: string[] = [];
  page.on('request', req => {
    networkLog.push(`REQUEST: ${req.method()} ${req.url()} body=${(req.postData() || '').substring(0, 300)}`);
  });
  page.on('response', async resp => {
    const body = await resp.text().catch(() => 'N/A');
    networkLog.push(`RESPONSE: ${resp.status()} ${resp.url()} body=${body.substring(0, 300)}`);
  });

  // Save-Button klicken
  const saveBtn = page.locator('button[style*="065f46"]').first();
  await expect(saveBtn).toBeVisible();
  await saveBtn.click();

  // Warten bis AJAX-Request durch ist
  await page.waitForTimeout(3000);
  await page.screenshot({ path: '/tmp/tankEditor_after_save_click.png' });

  // Alle Request/Response-Logs ausgeben
  console.log('\n=== Network Log (bei Save) ===');
  for (const entry of networkLog) {
    console.log(entry);
  }

  // Manuell zu Übersicht navigieren
  await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });
  await page.screenshot({ path: '/tmp/tankView_after_save2.png', fullPage: true });

  const content = await page.content();
  console.log('\nEnthält "350":', content.includes('350'));
  console.log('Enthält "Netto":', content.includes('Netto'));
});

