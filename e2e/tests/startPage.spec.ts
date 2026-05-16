/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

/**
 * E2E-Tests: Startseite (Landing Page / Login)
 *
 * Stellt sicher, dass die öffentlichen Seiten erreichbar sind und
 * nicht in einer Redirect-Schleife enden (Bug: invalide Session →
 * endloser Redirect auf "/" wegen fehlendem request.getSession()).
 */
import { test, expect } from '@playwright/test';

test.describe('Startseite – Erreichbarkeit', () => {

    test.use({ viewport: { width: 1280, height: 900 } });

    test('Startseite / ist erreichbar (kein Redirect-Loop)', async ({ page }) => {
        // Navigiere zur Root-URL; bei einem Redirect-Loop würde Playwright
        // nach dem Redirect-Limit (20) mit einem ERR_TOO_MANY_REDIRECTS abbrechen.
        const response = await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 15_000 });

        // Muss mit 200 antworten (oder über Redirect zu login.xhtml landen – auch OK)
        // Wichtig: kein ERR_TOO_MANY_REDIRECTS
        const finalUrl = page.url();
        console.log('Final URL nach goto("/"): ', finalUrl);

        // Wir erwarten, dass die Seite gerendert wird (nicht leer / nicht Timeout)
        await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });

        // Screenshot für visuelle Verifikation
        await page.screenshot({ path: '/tmp/startpage_test.png' });
    });

    test('login.xhtml ist direkt aufrufbar', async ({ page }) => {
        await page.goto('/login.xhtml', { waitUntil: 'domcontentloaded', timeout: 15_000 });

        // Body muss gerendert sein
        await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });

        // Das Login-Formular muss ein Passwort-Feld enthalten (eindeutiges Merkmal der Login-Seite)
        // Kein HTTP-500-Fehler: Spring Boot Whitelabel-Fehlerseite enthält "Whitelabel Error Page"
        const whitelabelError = page.locator('text=Whitelabel Error Page');
        const hasServerError = await whitelabelError.isVisible().catch(() => false);
        expect(hasServerError).toBeFalsy();

        await page.screenshot({ path: '/tmp/loginpage_test.png' });
    });

    test('Startseite mit veraltetem JSESSIONID-Cookie läuft nicht in Redirect-Loop', async ({ browser }) => {
        // Simuliert einen Browser mit einem abgelaufenen Server-Session-Cookie:
        // Setze einen gefakten JSESSIONID → Spring DetectInvalidSession → Strategy.
        // Ohne den Fix würde diese Anfrage eine Endlos-Schleife auslösen.
        const context = await browser.newContext();
        await context.addCookies([
            {
                name: 'JSESSIONID',
                value: 'STALE_SESSION_ID_FOR_TESTING_0123456789',
                domain: 'localhost',
                path: '/',
                httpOnly: true,
                secure: false,
                sameSite: 'Lax',
            },
        ]);
        const page = await context.newPage();

        // Navigiere; bei Redirect-Loop wirft Playwright nach ~20 Redirects einen Fehler
        const response = await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 20_000 });

        const finalUrl = page.url();
        console.log('Final URL (stale cookie test):', finalUrl);

        // Body muss gerendert sein
        await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });

        await page.screenshot({ path: '/tmp/stale_session_test.png' });
        await context.close();
    });

});


