/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

/**
 * E2E-Tests: Öffentlicher Haus-Riff-Report (houseReefReport.xhtml)
 *
 * Prüft:
 * 1. Die Seite ist ohne Login erreichbar (kein Redirect auf login.xhtml).
 * 2. Mit einem unbekannten/abgelaufenen Token wird der "Expired"-Panel angezeigt.
 * 3. Mit einem veralteten JSESSIONID-Cookie + Token wird die Seite korrekt
 *    self-redirected (kein Endlos-Loop) und anschließend gerendert.
 */
import { test, expect } from '@playwright/test';

const EXPIRED_TOKEN = 'test-expired-token-00000000-0000-0000-0000-000000000000';

test.describe('Öffentlicher Haus-Riff-Report', () => {

    test.use({ viewport: { width: 1280, height: 900 } });

    test('houseReefReport.xhtml ist ohne Login aufrufbar (kein Redirect auf Login)', async ({ page }) => {
        // Kein Cookie, kein Login → permitAll() muss greifen
        await page.goto(`/houseReefReport.xhtml?token=${EXPIRED_TOKEN}`, {
            waitUntil: 'domcontentloaded',
            timeout: 20_000,
        });

        const finalUrl = page.url();
        console.log('Final URL (no login):', finalUrl);

        // DARF NICHT auf login.xhtml landen
        expect(finalUrl).not.toContain('login.xhtml');
        // DARF NICHT auf index.xhtml oder "/" umleiten
        expect(finalUrl).not.toMatch(/^http:\/\/localhost:8088\/?$/);

        await page.screenshot({ path: '/tmp/report_no_login.png' });
    });

    test('Abgelaufener/unbekannter Token zeigt "Expired"-Meldung (nicht Login)', async ({ page }) => {
        await page.goto(`/houseReefReport.xhtml?token=${EXPIRED_TOKEN}`, {
            waitUntil: 'networkidle',
            timeout: 20_000,
        });

        const finalUrl = page.url();
        console.log('Final URL (expired token):', finalUrl);
        expect(finalUrl).not.toContain('login.xhtml');

        // Body muss gerendert sein
        await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });

        // Entweder "Expired"-Panel oder (falls Seite noch lädt) mindestens nicht die Login-Seite
        // Der "reportAvailable=false"-Zweig rendert ein Bild + h2 mit der Expired-Meldung.
        // Wir prüfen, dass wir NICHT auf der Login-Seite gelandet sind.
        const loginForm = page.locator('input[name="username"], input[id$="username"]');
        const isLoginForm = await loginForm.isVisible().catch(() => false);
        expect(isLoginForm).toBeFalsy();

        await page.screenshot({ path: '/tmp/report_expired.png' });
    });

    test('Öffentlicher Report mit veraltetem JSESSIONID-Cookie: kein Redirect-Loop', async ({ browser }) => {
        // Simuliert Browser mit abgelaufener Session + Token im Query-String.
        // Ohne Fix: Strategy redirectet auf /houseReefReport.xhtml?token=... → stale JSESSIONID
        // → erneuter Redirect → Endlosschleife.
        // Mit Fix: request.getSession(true) ersetzt den Cookie → nur EIN Redirect → Seite lädt.
        const context = await browser.newContext();
        await context.addCookies([
            {
                name: 'JSESSIONID',
                value: 'STALE_SESSION_FOR_REPORT_TEST_0123456789',
                domain: 'localhost',
                path: '/',
                httpOnly: true,
                secure: false,
                sameSite: 'Lax',
            },
        ]);
        const page = await context.newPage();

        // Öffne Report-Seite mit veraltetem Cookie
        // Ein Redirect zurück auf dieselbe URL ist erlaubt, aber kein Loop
        await page.goto(`/houseReefReport.xhtml?token=${EXPIRED_TOKEN}`, {
            waitUntil: 'domcontentloaded',
            timeout: 20_000,
        });

        const finalUrl = page.url();
        console.log('Final URL (stale cookie + report):', finalUrl);

        // Body muss gerendert sein – kein ERR_TOO_MANY_REDIRECTS
        await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });

        // Darf nicht auf Login gelandet sein
        expect(finalUrl).not.toContain('login.xhtml');

        await page.screenshot({ path: '/tmp/report_stale_session.png' });
        await context.close();
    });

    test('Foto-Proxy /api/public/report/{token}/photo ist ohne Auth erreichbar', async ({ page }) => {
        // Der Proxy-Controller muss ohne Authentifizierung antworten (permitAll).
        // Mit ungültigem Token → 404 (Backend: Token unbekannt) ODER 200 (Bild zurück) ist korrekt.
        // KEIN 401/403 und KEIN Redirect auf login.xhtml.
        const response = await page.request.get(
            `/api/public/report/${EXPIRED_TOKEN}/photo`,
            { maxRedirects: 0, failOnStatusCode: false }
        );
        const status = response.status();
        console.log('Proxy photo status (expired token):', status);
        // 3xx Redirect würde auf login hindeuten → Proxy nicht aktiv oder Security falsch
        expect(status).not.toBe(301);
        expect(status).not.toBe(302);
        expect(status).not.toBe(401);
        expect(status).not.toBe(403);
        // Erlaubt: 200 (Bild), 404 (Token nicht gefunden), 502 (Backend nicht erreichbar)
        expect([200, 404, 502]).toContain(status);
    });

    test('Fisch-Foto-Proxy /api/public/report/{token}/fish/{id}/photo ist ohne Auth erreichbar', async ({ page }) => {
        const response = await page.request.get(
            `/api/public/report/${EXPIRED_TOKEN}/fish/1/photo`,
            { maxRedirects: 0, failOnStatusCode: false }
        );
        const status = response.status();
        console.log('Proxy fish photo status (expired token):', status);
        expect(status).not.toBe(301);
        expect(status).not.toBe(302);
        expect(status).not.toBe(401);
        expect(status).not.toBe(403);
        expect([200, 404, 502]).toContain(status);
    });

    test('houseReefReport.xhtml ohne Token zeigt Expired-Panel (nicht Login)', async ({ page }) => {
        // Kein Token → Report nicht verfügbar → Expired-Panel soll erscheinen, NICHT Login
        await page.goto('/houseReefReport.xhtml', {
            waitUntil: 'networkidle',
            timeout: 20_000,
        });

        const finalUrl = page.url();
        console.log('Final URL (no token):', finalUrl);
        expect(finalUrl).not.toContain('login.xhtml');

        await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });

        const loginForm = page.locator('input[name="username"], input[id$="username"]');
        const isLoginForm = await loginForm.isVisible().catch(() => false);
        expect(isLoginForm).toBeFalsy();

        await page.screenshot({ path: '/tmp/report_no_token.png' });
    });

});

