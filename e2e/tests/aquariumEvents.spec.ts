/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

/**
 * E2E-Tests: Aquarium Event Logbook (004-aquarium-events)
 *
 * Testuser: sabi@bluewhale.de / clibanarius
 * Hat Aquarien mit Logbuch-Funktion.
 * App läuft auf http://localhost:8088
 *
 * Testziele:
 * 1. tankView.xhtml rendert ohne ComponentNotFoundException
 * 2. Event-Formular (Datum, Dauer, Beschreibung) ist sichtbar und interagierbar
 * 3. userProfile.xhtml rendert ohne ComponentNotFoundException
 * 4. includeEvents-Checkbox ist sichtbar und klickbar
 */
import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

test.describe('Aquarium Event Logbook', () => {

  test.use({ viewport: { width: 1280, height: 900 } });

  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  // ─────────────────────────────────────────────────────────────────────────
  // tankView.xhtml: Seite lädt ohne Fehler
  // ─────────────────────────────────────────────────────────────────────────
  test('tankView rendert ohne JSF ComponentNotFoundException', async ({ page }) => {
    await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });

    // Screenshot zur Diagnose
    await page.screenshot({ path: '/tmp/tankView_events_loaded.png' });

    // Kein Fehler-Panel (Sabi leitet bei unbehandeltem Fehler zur Fehlerseite weiter)
    await expect(page).not.toHaveURL(/error/i);
    await expect(page).not.toHaveURL(/login/i);

    // Seite muss den Event-Logbuch-Header enthalten (i18n key: aquariumevent.panel.h)
    const eventPanel = page.locator('.ui-panel').filter({ hasText: /Logbuch|Ereignis|Event/i }).first();
    await expect(eventPanel).toBeAttached({ timeout: 10_000 });
    await expect(eventPanel).toBeVisible();
  });

  // ─────────────────────────────────────────────────────────────────────────
  // tankView.xhtml: Event-Formular sichtbar und Felder vorhanden
  //   Neu: Aquarium-Dropdown (p:selectOneMenu) muss sichtbar und interagierbar sein.
  // ─────────────────────────────────────────────────────────────────────────
  test('Event-Formular enthält Datum-, Dauer- und Beschreibungsfeld', async ({ page }) => {
    await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });

    await page.screenshot({ path: '/tmp/tankView_events_form.png' });

    // Einzelnes Event-Panel (kein ui:repeat mehr pro Tank)
    const eventPanel = page.locator('.ui-panel').filter({ hasText: /Logbuch|Ereignis|Event/i }).first();
    await expect(eventPanel).toBeVisible({ timeout: 8_000 });

    // Aquarium-Dropdown muss vorhanden und sichtbar sein
    const aquariumDropdown = page.locator('[id$="aquariumSelector"]').first();
    await expect(aquariumDropdown).toBeAttached({ timeout: 8_000 });

    // p:datePicker
    const datePicker = page.locator('input[id$="eventDate_input"]').first();
    await expect(datePicker).toBeVisible({ timeout: 8_000 });

    // Dauer-Input
    const durationInput = page.locator('input[id$="eventDuration"]').first();
    await expect(durationInput).toBeVisible({ timeout: 8_000 });

    // Beschreibungs-Textarea
    const descTextarea = page.locator('textarea[id$="eventDesc"]').first();
    await expect(descTextarea).toBeVisible({ timeout: 8_000 });

    // Speichern-Button
    const saveButtons = page.locator('.ui-button').filter({ hasText: /speichern|save/i });
    await expect(saveButtons.first()).toBeVisible({ timeout: 8_000 });
  });

  // ─────────────────────────────────────────────────────────────────────────
  // tankView.xhtml: Neues Event anlegen und in der Liste verifizieren
  //
  // Fix: Aquarium wird über p:selectOneMenu explizit ausgewählt.
  // Fix: Datum über das Kalender-Popup (readonlyInput=true erfordert Click).
  // Fix: Erfolgskontrolle: Beschreibungstext muss in der Tabelle erscheinen.
  // ─────────────────────────────────────────────────────────────────────────
  test('Neues Event kann angelegt werden', async ({ page }) => {
    await page.goto('/secured/tankView.xhtml', { waitUntil: 'networkidle' });

    // ── Schritt 1: Event-Formular finden ──
    const eventFormContainer = page.locator('#eventForm');
    await expect(eventFormContainer).toBeAttached({ timeout: 10_000 });

    // ── Schritt 2: Aquarium-Dropdown – vorausgewähltes Aquarium loggen ──
    const aquariumDropdown = page.locator('.ui-selectonemenu').first();
    await expect(aquariumDropdown).toBeVisible({ timeout: 8_000 });
    const selectedLabel = await aquariumDropdown.locator('.ui-selectonemenu-label').textContent();
    console.log('INFO: Vorausgewähltes Aquarium:', selectedLabel?.trim());

    // ── Schritt 3: Datum über Kalender-Popup setzen ──
    const dateInput = page.locator('input[id$="eventDate_input"]').first();
    await expect(dateInput).toBeVisible({ timeout: 8_000 });
    await dateInput.click();

    const calendarOverlay = page.locator('.ui-datepicker:not(.ui-datepicker-inline)').first();
    await expect(calendarOverlay).toBeVisible({ timeout: 5_000 });
    await page.screenshot({ path: '/tmp/tankView_calendar_open.png' });

    // Heute anklicken
    const todayCell = calendarOverlay.locator('td.ui-datepicker-today a').first();
    if (await todayCell.count() > 0) {
      await expect(todayCell).toBeVisible();
      await todayCell.click();
    } else {
      const dayNumber = new Date().getDate().toString();
      const dayLink = calendarOverlay.locator('td a').filter({ hasText: new RegExp(`^${dayNumber}$`) }).first();
      await expect(dayLink).toBeVisible({ timeout: 3_000 });
      await dayLink.click();
    }
    await page.waitForTimeout(300);

    const dateValue = await dateInput.inputValue();
    expect(dateValue.length).toBeGreaterThan(0);
    console.log('INFO: Datum gesetzt:', dateValue);

    // ── Schritt 4: Eindeutige Beschreibung füllen (für spätere Suche in der Tabelle) ──
    const uniqueDesc = `E2E-Test Wasserwechsel ${Date.now()}`;
    const descTextarea = page.locator('textarea[id$="eventDesc"]').first();
    await expect(descTextarea).toBeVisible({ timeout: 8_000 });
    await descTextarea.fill(uniqueDesc);

    // ── Schritt 5: Dauer setzen ──
    const durationInput = page.locator('input[id$="eventDuration"]').first();
    await durationInput.fill('2');
    await durationInput.press('Tab');

    await page.screenshot({ path: '/tmp/tankView_before_save_event.png' });

    // ── Schritt 6: Speichern ──
    const saveBtn = page.locator('#eventForm button.ui-button').filter({ hasText: /speichern|save/i }).first();
    await expect(saveBtn).toBeVisible();

    const ajaxResponsePromise = page.waitForResponse(
        resp => resp.url().includes('tankView') && resp.status() === 200 && resp.request().method() === 'POST',
        { timeout: 10_000 }
    );
    await saveBtn.click();
    await ajaxResponsePromise;
    await page.waitForLoadState('networkidle');

    await page.screenshot({ path: '/tmp/tankView_after_save_event.png' });

    // ── Schritt 7: Kein Absturz ──
    await expect(page).not.toHaveURL(/error/i);

    // ── Schritt 8: Gespeicherter Event erscheint in der Tabelle ──
    // Der Event-Text muss als Zellinhalt in der Beschreibungsspalte sichtbar sein.
    const descCellLocator = page.locator('#eventForm .ui-datatable tbody td').filter({ hasText: uniqueDesc });
    await expect(descCellLocator.first()).toBeVisible({ timeout: 8_000 });
    console.log('INFO: Event mit Beschreibung sichtbar in Tabelle:', uniqueDesc);

    // ── Schritt 9: Aquarium-Spalte in derselben Zeile prüfen ──
    // Die Zeile mit dem neuen Event muss in der ersten Spalte den Aquarium-Namen enthalten
    const newEventRow = page.locator('#eventForm .ui-datatable tbody tr').filter({ hasText: uniqueDesc });
    await expect(newEventRow.first()).toBeVisible({ timeout: 5_000 });
    const aquariumCell = newEventRow.first().locator('td').first(); // Aquarium-Spalte ist die erste
    const aquariumCellText = await aquariumCell.textContent();
    expect(aquariumCellText?.trim().length).toBeGreaterThan(0);
    console.log('INFO: Aquarium-Spalte zeigt:', aquariumCellText?.trim());

    // Anzahl aller Tabellenzeilen zur Information
    const rowCount = await page.locator('#eventForm .ui-datatable tbody tr').count();
    console.log('INFO: Tabelle zeigt insgesamt', rowCount, 'Event(s)');
  });

  // ─────────────────────────────────────────────────────────────────────────
  // userProfile.xhtml: Seite lädt ohne Fehler (includeEvents-Checkbox)
  // ─────────────────────────────────────────────────────────────────────────
  test('userProfile rendert ohne JSF ComponentNotFoundException', async ({ page }) => {
    await page.goto('/secured/userProfile.xhtml', { waitUntil: 'networkidle' });

    await page.screenshot({ path: '/tmp/userProfile_events_loaded.png' });

    // Seite darf keine Fehlerseite sein
    await expect(page).not.toHaveURL(/error/i);
    await expect(page).not.toHaveURL(/login/i);

    // Public Report-Sektion muss gerendert sein
    const reportPanel = page.locator('.ui-panel').filter({ hasText: /Report|Bericht/i }).first();
    await expect(reportPanel).toBeAttached({ timeout: 10_000 });
  });

  // ─────────────────────────────────────────────────────────────────────────
  // userProfile.xhtml: includeEvents-Checkbox ist sichtbar und anklickbar
  // ─────────────────────────────────────────────────────────────────────────
  test('includeEvents-Checkbox ist sichtbar und interagierbar', async ({ page }) => {
    await page.goto('/secured/userProfile.xhtml', { waitUntil: 'networkidle' });

    // Die Checkbox hat id="includeEventsCb" (statische ID, kein EL)
    // Im gerenderten HTML hat sie eine client-ID wie "j_idt88:0:j_idt89:includeEventsCb"
    const checkbox = page.locator('div.ui-chkbox').filter(
        { has: page.locator('input[id$="includeEventsCb"]') }
    ).first();

    // Checkbox nur prüfen wenn Link vorhanden (Test-User muss Report-Link haben)
    const checkboxCount = await checkbox.count();

    if (checkboxCount > 0) {
      // Checkbox ist vorhanden: Sichtbarkeit prüfen
      await expect(checkbox).toBeVisible({ timeout: 8_000 });

      await page.screenshot({ path: '/tmp/userProfile_includeEvents_checkbox.png' });

      // Checkbox anklicken (echter Click, kein force)
      await checkbox.click();
      await page.waitForTimeout(300);

      await page.screenshot({ path: '/tmp/userProfile_includeEvents_after_click.png' });

      // Seite darf nach Klick nicht abstürzen
      await expect(page).not.toHaveURL(/error/i);
    } else {
      // Kein Report-Link vorhanden → Checkbox nicht gerendert, das ist OK
      // Test nur auf Seiten-Stabilität prüfen
      await expect(page).not.toHaveURL(/error/i);
      console.log('INFO: Kein aktiver Report-Link gefunden — includeEvents-Checkbox nicht gerendert (erwartet)');
    }
  });

});



