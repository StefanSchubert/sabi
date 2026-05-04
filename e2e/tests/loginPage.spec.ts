/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 *
 */

/**
 * E2E-Tests: Login-Seite
 *
 * Prüft u.a. das "Über Sabi"-Panel (toggleable p:panel).
 */
import { test, expect } from '@playwright/test';

test.describe('Login-Seite', () => {

  test.use({ viewport: { width: 1280, height: 900 } });

  test.beforeEach(async ({ page }) => {
    await page.goto('/login.xhtml', { waitUntil: 'networkidle' });
  });

  test('"Über Sabi"-Panel ist standardmäßig eingeklappt', async ({ page }) => {
    // PrimeFaces collapsed panel: content ist display:none
    const panelContent = page.locator('[id$="welcome_content"]');
    await expect(panelContent).not.toBeVisible();
  });

  test('"Über Sabi"-Panel lässt sich aufklappen', async ({ page }) => {
    // Toggle-Button im Panel-Header (via ID-Suffix, form-unabhängig)
    // PrimeFaces blendet den Toggle-Icon via CSS aus → Hover simulieren und force:true klicken
    const panelHeader = page.locator('[id$="welcome_header"]');
    await expect(panelHeader).toBeAttached({ timeout: 8_000 });

    // Screenshot vor dem Klick (Debugging)
    await page.screenshot({ path: '/tmp/before_toggle.png' });

    // Hover auf Header, damit PrimeFaces den Toggle-Button einblendet
    await panelHeader.hover();

    const toggleButton = page.locator('[id$="welcome_toggler"]');
    await expect(toggleButton).toBeAttached({ timeout: 5_000 });
    // Echter Click (Button hat jetzt 24x24px durch CSS-Fix)
    await toggleButton.click();

    // Warte auf Panel-Content sichtbar
    const panelContent = page.locator('[id$="welcome_content"]');
    await expect(panelContent).toBeVisible({ timeout: 10_000 });

    // Screenshot nach dem Klick
    await page.screenshot({ path: '/tmp/after_toggle.png' });

    // Inhalt muss GitHub-Link enthalten
    await expect(panelContent).toContainText('github', { ignoreCase: true });
  });

});





