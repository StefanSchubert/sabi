/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * Debug-Test: Netto-Volumen Speichern + Übersichtsseite Darstellung
 */
import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

test.describe('Tank Netto-Volumen Debug', () => {

  test.use({ viewport: { width: 1280, height: 900 } });

  test('Übersichtsseite - Screenshot und prüfe Layout', async ({ page }) => {
    await login(page);
    await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });
    await page.screenshot({ path: '/tmp/tankView_overview.png', fullPage: true });

    // Prüfe ob Brutto-Volumen sichtbar ist
    const grossLabels = page.locator('.ui-panelgrid').first();
    await expect(grossLabels).toBeVisible();

    // HTML der panelGrid-Zellen ausgeben
    const panelGridHtml = await page.locator('.ui-panelgrid').first().innerHTML();
    console.log('PanelGrid HTML:', panelGridHtml.substring(0, 3000));

    // Alle outputLabels in der Übersicht
    const allLabels = await page.locator('.ui-panelgrid .ui-outputlabel, .ui-panelgrid label').allTextContents();
    console.log('Alle Labels in PanelGrid:', allLabels);
  });

  test('Editor - Screenshot und prüfe sizeNet-Feld', async ({ page }) => {
    await login(page);
    await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });

    // Screenshot Übersicht vor Bearbeitung
    await page.screenshot({ path: '/tmp/tankView_before_edit.png', fullPage: true });

    // Ersten Tank bearbeiten
    const editButton = page.locator('[id$="editAction"]').first();
    await expect(editButton).toBeVisible({ timeout: 5_000 });
    await editButton.click();
    await page.waitForLoadState('networkidle');

    // Screenshot Editor
    await page.screenshot({ path: '/tmp/tankEditor_form.png', fullPage: true });

    // Prüfe ob sizeNet Feld vorhanden
    const sizeNetInput = page.locator('[id$="sizeNet"]');
    await expect(sizeNetInput).toBeVisible({ timeout: 5_000 });

    // Aktuellen Wert loggen
    const currentValue = await sizeNetInput.inputValue();
    console.log('Aktueller sizeNet-Wert:', currentValue);

    // Wert setzen
    await sizeNetInput.clear();
    await sizeNetInput.fill('350');

    // Screenshot nach Eingabe
    await page.screenshot({ path: '/tmp/tankEditor_filled.png' });

    // Speichern
    const saveButton = page.locator('[id$="tankform"] button').filter({ hasText: /speichern|save/i }).first();
    // Fallback: direkt über Stil
    const saveBtn = page.locator('button[style*="065f46"]').first();
    await expect(saveBtn).toBeVisible({ timeout: 5_000 });
    await saveBtn.click();
    await page.waitForLoadState('networkidle');

    // Screenshot nach Speichern
    await page.screenshot({ path: '/tmp/tankView_after_save.png', fullPage: true });

    // Gehe zurück zur Übersicht
    await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });
    await page.screenshot({ path: '/tmp/tankView_after_save_overview.png', fullPage: true });

    // Prüfe ob Netto-Volumen angezeigt wird
    const pageContent = await page.content();
    console.log('Enthält "350":', pageContent.includes('350'));
  });

  test('Direkte HTML-Analyse der Übersichtsseite', async ({ page }) => {
    await login(page);
    await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });
    await page.screenshot({ path: '/tmp/tankView_html_analysis.png', fullPage: true });

    // Alle grid-Zellen der ersten panelGrid auslesen
    const cells = await page.locator('.ui-panelgrid-content > *').all();
    console.log('Anzahl Zellen in erstem PanelGrid:', cells.length);

    for (let i = 0; i < Math.min(cells.length, 20); i++) {
      const text = await cells[i].textContent();
      const tag = await cells[i].evaluate(el => el.tagName);
      console.log(`  Zelle ${i + 1} [${tag}]: "${text?.trim().substring(0, 60)}"`);
    }
  });
});

