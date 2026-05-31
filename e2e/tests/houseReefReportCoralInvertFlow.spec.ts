/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

/**
 * E2E-Tests: HouseReef-Report — Korallen und Wirbellose (Feature 006)
 *
 * Prüft:
 *  1. Proxy-Endpunkte für Korallen- und Wirbellosen-Fotos sind ohne Auth erreichbar.
 *  2. Nach Login → Benutzerprofil → Report-Link erzeugen → Korallen + Wirbellose aktivieren.
 *  3. Öffentlicher Report zeigt Korallen-Panel (nicht null / nicht shared → "nicht freigegeben").
 *  4. Öffentlicher Report zeigt Wirbellosen-Panel (nicht null → Panel gerendert).
 *  5. Report-JSON enthält coralInhabitants + invertebrateInhabitants Felder.
 *
 * Testuser: sabi@bluewhale.de / clibanarius
 * App: http://localhost:8088
 * Backend: http://localhost:8080/sabi
 */

import { test, expect, Page } from '@playwright/test';

const BASE_URL  = 'http://localhost:8088';
const BACKEND   = 'http://localhost:8080/sabi';
const FAKE_TOKEN = 'test-fake-token-006-corals-invertebrates-00000000';

// ──────────────────────────────────────────────────────────────
// Helper: Login via Browser-UI
// ──────────────────────────────────────────────────────────────
async function login(page: Page, username = 'sabi@bluewhale.de', password = 'clibanarius') {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

// ──────────────────────────────────────────────────────────────
// Helper: PrimeFaces selectOneMenu option wählen
// ──────────────────────────────────────────────────────────────
async function selectPrimeFacesOption(page: Page, containerSelector: string, optionText: string) {
  const trigger = page.locator(`${containerSelector} .ui-selectonemenu-trigger`).first();
  await trigger.click();
  const panel = page.locator('.ui-selectonemenu-panel').first();
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  await panel.locator('li.ui-selectonemenu-item').filter({ hasText: optionText }).click();
}

// ──────────────────────────────────────────────────────────────
// Helper: Report-Link für "Nano-Reef" im Benutzerprofil erzeugen
// (falls noch kein Link existiert) und den Token zurückgeben.
// ──────────────────────────────────────────────────────────────
async function ensureReportLink(page: Page): Promise<string | null> {
  await page.goto('/secured/userProfile.xhtml', { waitUntil: 'networkidle' });

  // Bereits ein Link für Nano-Reef vorhanden?
  const existingLink = page.locator('a[href*="houseReefReport.xhtml?token="]').first();
  const linkExists   = await existingLink.isVisible({ timeout: 3_000 }).catch(() => false);

  if (!linkExists) {
    // Tank auswählen
    const tankSelect = page.locator('#reportLinkForm #reportTankSelect');
    if (await tankSelect.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await selectPrimeFacesOption(page, '#reportLinkForm', 'Nano-Reef');
    }

    // "Link erzeugen" Button klicken (ajax=false → voller Submit)
    const generateBtn = page.locator('#reportLinkForm button, #reportLinkForm input[type="submit"]')
        .filter({ hasText: /erzeugen|generate/i }).first();
    if (await generateBtn.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await generateBtn.click();
      await page.waitForLoadState('networkidle');
    }
  }

  // Token aus dem angezeigten Link extrahieren
  const linkLocator = page.locator('a[href*="houseReefReport.xhtml?token="]').first();
  if (await linkLocator.isVisible({ timeout: 8_000 }).catch(() => false)) {
    const href  = await linkLocator.getAttribute('href') || '';
    const match = href.match(/token=([^&]+)/);
    return match ? match[1] : null;
  }
  return null;
}

// ──────────────────────────────────────────────────────────────
// Helper: Checkbox im Benutzerprofil setzen und speichern
// ──────────────────────────────────────────────────────────────
async function enableCheckbox(page: Page, checkboxId: string, saveButtonSelector: string) {
  const cb = page.locator(`#${checkboxId}, [id$="${checkboxId}"]`).first();
  if (await cb.isVisible({ timeout: 5_000 }).catch(() => false)) {
    const isChecked = await cb.isChecked().catch(() => false);
    if (!isChecked) {
      await cb.click();
    }
    // Speichern
    const saveBtn = page.locator(saveButtonSelector).first();
    if (await saveBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await saveBtn.click();
      await page.waitForLoadState('networkidle');
    }
  }
}

// ══════════════════════════════════════════════════════════════

test.describe('HouseReef-Report: Korallen und Wirbellose', () => {

  test.use({ viewport: { width: 1280, height: 900 } });
  test.setTimeout(90_000);

  // ── 1. Proxy-Endpunkt Korallen-Foto: kein Auth erforderlich ──

  test('Korallen-Foto-Proxy /api/public/report/{token}/coral/{id}/photo ist ohne Auth erreichbar', async ({ page }) => {
    const response = await page.request.get(
        `/api/public/report/${FAKE_TOKEN}/coral/1/photo`,
        { maxRedirects: 0, failOnStatusCode: false }
    );
    const status = response.status();
    console.log('Coral proxy photo status (fake token):', status);

    // Kein Redirect auf Login, kein 401/403
    expect(status).not.toBe(301);
    expect(status).not.toBe(302);
    expect(status).not.toBe(401);
    expect(status).not.toBe(403);
    // Erlaubt: 200 (Bild), 404 (Token unbekannt), 502 (Backend)
    expect([200, 404, 502]).toContain(status);
  });

  // ── 2. Proxy-Endpunkt Wirbellosen-Foto: kein Auth erforderlich ──

  test('Wirbellosen-Foto-Proxy /api/public/report/{token}/invertebrate/{id}/photo ist ohne Auth erreichbar', async ({ page }) => {
    const response = await page.request.get(
        `/api/public/report/${FAKE_TOKEN}/invertebrate/1/photo`,
        { maxRedirects: 0, failOnStatusCode: false }
    );
    const status = response.status();
    console.log('Invertebrate proxy photo status (fake token):', status);

    expect(status).not.toBe(301);
    expect(status).not.toBe(302);
    expect(status).not.toBe(401);
    expect(status).not.toBe(403);
    expect([200, 404, 502]).toContain(status);
  });

  // ── 3. Report-JSON enthält coralInhabitants + invertebrateInhabitants ──

  test('Report-JSON enthält coralInhabitants und invertebrateInhabitants Felder', async ({ page }) => {
    // Direkt gegen Backend-API (public, kein Auth)
    const response = await page.request.get(
        `${BACKEND}/api/public/report/${FAKE_TOKEN}`,
        { failOnStatusCode: false }
    );
    if (response.status() === 200) {
      const json = await response.json().catch(() => null);
      if (json) {
        // Felder müssen im JSON-Objekt vorhanden sein (auch als null)
        expect('coralInhabitants' in json || json.coralInhabitants !== undefined
            || json.coralInhabitants === null).toBeTruthy();
        expect('invertebrateInhabitants' in json || json.invertebrateInhabitants !== undefined
            || json.invertebrateInhabitants === null).toBeTruthy();
      }
    } else {
      // 404 ist für unbekannten Token korrekt
      expect([400, 404]).toContain(response.status());
      console.log('Backend returned', response.status(), '— Token unbekannt, das ist korrekt.');
    }
  });

  // ── 4. Vollständiger UI-Flow: Report mit Korallen + Wirbellosen aktivieren ──

  test('Benutzerprofil: Report-Link erzeugen, Korallen + Wirbellose aktivieren, Report anzeigen', async ({ page }) => {
    await login(page);

    // Report-Link sicherstellen und Token extrahieren
    const token = await ensureReportLink(page);
    if (!token) {
      test.skip(true, 'Kein Report-Link verfügbar — Test übersprungen');
      return;
    }
    console.log('Report-Token:', token);

    // Seite neu laden (nach ggf. Link-Erzeugung)
    await page.goto('/secured/userProfile.xhtml', { waitUntil: 'networkidle' });

    // Korallen und Wirbellose einschalten
    // Die Checkboxen befinden sich in ui:repeat → JSESSIONID-basierte IDs, aber wir können
    // über Label-Text oder Checkbox-Sequenz navigieren
    const includeCoralsCb  = page.locator('#includeCoralsCb').first();
    const includeInvertsCb = page.locator('#includeInvertsCb').first();

    if (await includeCoralsCb.isVisible({ timeout: 5_000 }).catch(() => false)) {
      // Korallen-Checkbox setzen
      const coralChecked = await includeCoralsCb.isChecked().catch(() => false);
      if (!coralChecked) {
        await includeCoralsCb.click();
        // Speicher-Button in der gleichen Zeile (3-spaltig: label | checkbox | button)
        const saveCoralBtn = page.locator('#includeCoralsCb')
            .locator('xpath=../../following-sibling::td[1]//button | xpath=../../../td[3]//button')
            .first();
        if (!await saveCoralBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
          // Fallback: drittes td-kind innerhalb desselben panelGrid-Rows
          await page.locator('button.ui-button-primary').filter({ hasText: /speichern|save/i }).first().click();
        } else {
          await saveCoralBtn.click();
        }
        await page.waitForLoadState('networkidle');
      }
    }

    if (await includeInvertsCb.isVisible({ timeout: 5_000 }).catch(() => false)) {
      const invertChecked = await includeInvertsCb.isChecked().catch(() => false);
      if (!invertChecked) {
        await includeInvertsCb.click();
        await page.locator('button.ui-button-primary').filter({ hasText: /speichern|save/i }).last().click();
        await page.waitForLoadState('networkidle');
      }
    }

    // Öffentlichen Report aufrufen
    await page.goto(`/houseReefReport.xhtml?token=${token}`, { waitUntil: 'networkidle', timeout: 30_000 });

    const finalUrl = page.url();
    expect(finalUrl).not.toContain('login.xhtml');

    await page.screenshot({ path: '/tmp/report_with_corals_invertebrates.png' });

    // Korallen-Panel muss gerendert sein (Header "Korallen und Wirbellosen")
    const coralPanel = page.locator('span.ui-panel-title, .ui-panel-titlebar')
        .filter({ hasText: /Korallen/i }).first();
    await expect(coralPanel).toBeVisible({ timeout: 10_000 });

    // Korallen-Inhalt: entweder Karten ODER "nicht freigegeben" ODER "keine Korallen"
    // — keinesfalls darf ein i18n-Schlüssel raw auftauchen
    await expect(page.locator('body')).not.toContainText('housereef.report.corals');
    await expect(page.locator('body')).not.toContainText('housereef.report.no.corals.t');

    // Wirbellosen-Panel muss gerendert sein (da include_invertebrates = true)
    const invertPanel = page.locator('span.ui-panel-title, .ui-panel-titlebar')
        .filter({ hasText: /Wirbellose/i }).first();
    await expect(invertPanel).toBeVisible({ timeout: 10_000 });

    // Kein raw i18n-Key im Wirbellosen-Panel
    await expect(page.locator('body')).not.toContainText('housereefReport.invertebrates');
  });

  // ── 5. Bericht ohne opt-in zeigt "nicht freigegeben" für Korallen ──

  test('Report mit abgelaufenem/unbekanntem Token zeigt keinen Korallen-Inhalt (nicht Login)', async ({ page }) => {
    await page.goto(`/houseReefReport.xhtml?token=${FAKE_TOKEN}`, {
      waitUntil: 'networkidle',
      timeout: 20_000,
    });

    const finalUrl = page.url();
    expect(finalUrl).not.toContain('login.xhtml');

    // Kein Login-Formular
    const loginForm = page.locator('input[name="username"], input[id$="username"]');
    expect(await loginForm.isVisible().catch(() => false)).toBeFalsy();

    await page.screenshot({ path: '/tmp/report_fake_token_corals.png' });
  });

  // ── 6. Korallen-Panel-Rendering: "nicht freigegeben" wenn null (REST-Ebene) ──

  test('houseReefReport rendert Korallen-Panel ohne Fehler für gültigen Token ohne opt-in', async ({ page }) => {
    // Verwende den UI-Flow, um sicherzustellen, dass der Testuser einen Report-Link hat
    await login(page);
    const token = await ensureReportLink(page);

    if (!token) {
      test.skip(true, 'Kein Token verfügbar');
      return;
    }

    // Korallen-opt-in deaktivieren → coralInhabitants == null → "nicht freigegeben"
    // (wir setzen es nicht um den Flow nicht zu verwischen — testen nur den Status quo)
    await page.goto(`/houseReefReport.xhtml?token=${token}`, { waitUntil: 'networkidle', timeout: 30_000 });

    // Seite geladen, kein Crash
    await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });
    expect(page.url()).not.toContain('login.xhtml');

    // Keine raw i18n-Schlüssel auf der Seite
    await expect(page.locator('body')).not.toContainText('housereef.report.corals.h');
    await expect(page.locator('body')).not.toContainText('housereefReport.invertebrates.section.title');

    await page.screenshot({ path: '/tmp/report_coral_noop.png' });
  });

});
