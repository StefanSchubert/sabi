/*
 * <!--
 *   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 *   - See project LICENSE file for the detailed terms and conditions.
 *   -->
 */

/**
 * E2E-Tests: DatePicker auf measureView (iPad Portrait vs. Landscape)
 *
 * Reproduziert: DatePicker mit touchUI="true" verschwindet sofort im Landscape-Modus.
 */
import { test, expect, Page, devices } from '@playwright/test';

// iPad User-Agent + Touch-Emulation
const iPadPortrait  = { ...devices['iPad (gen 7)'],  viewport: { width: 768,  height: 1024 } };
const iPadLandscape = { ...devices['iPad (gen 7) landscape'], viewport: { width: 1024, height: 768 } };
// iPhone Portrait
const iPhonePortrait = { ...devices['iPhone 14'], viewport: { width: 390, height: 844 } };

async function login(page: Page) {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');
}

async function openDatePicker(page: Page): Promise<boolean> {
  const datepickerInput = page.locator('[id$="measureDate_input"]');
  await expect(datepickerInput).toBeVisible({ timeout: 8_000 });

  await datepickerInput.tap().catch(() => datepickerInput.click());
  await page.waitForTimeout(800);

  // Nach Entfernung von touchUI="true" → nur noch normaler Overlay
  const touchOverlay = page.locator('.ui-datepicker-touch-ui');
  const normalOverlay = page.locator('.ui-datepicker:not(.ui-datepicker-inline)');

  const touchVisible = await touchOverlay.isVisible().catch(() => false);
  const normalVisible = await normalOverlay.first().isVisible().catch(() => false);

  console.log(`  touchUI overlay visible: ${touchVisible}, normal overlay visible: ${normalVisible}`);
  return normalVisible || touchVisible;
}

async function isOverlayWithinViewport(page: Page): Promise<{visible: boolean, withinViewport: boolean, details: object}> {
  return page.evaluate(() => {
    const el = document.querySelector('.ui-datepicker-touch-ui') as HTMLElement;
    if (!el) return { visible: false, withinViewport: false, details: { error: 'not found' } };
    const cs = window.getComputedStyle(el);
    if (cs.display === 'none' || cs.visibility === 'hidden') {
      return { visible: false, withinViewport: false, details: { display: cs.display } };
    }
    const rect = el.getBoundingClientRect();
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const withinViewport =
      rect.left < vw &&       // linke Kante sichtbar
      rect.right > 0 &&        // rechte Kante sichtbar
      rect.top < vh &&         // obere Kante sichtbar
      rect.bottom > 0;         // untere Kante sichtbar
    return {
      visible: true,
      withinViewport,
      details: {
        rect: { top: rect.top, left: rect.left, right: rect.right, bottom: rect.bottom,
                width: rect.width, height: rect.height },
        viewport: { width: vw, height: vh },
        overflowRight: Math.max(0, rect.right - vw),
        overflowBottom: Math.max(0, rect.bottom - vh),
      }
    };
  });
}

test.describe('DatePicker measureView – iPad Orientierungen', () => {

  test('DatePicker bleibt offen – iPad Portrait (768×1024)', async ({ browser }) => {
    const context = await browser.newContext(iPadPortrait);
    const page = await context.newPage();

    await login(page);
    await page.goto('/secured/measureView.xhtml', { waitUntil: 'networkidle' });
    await page.screenshot({ path: '/tmp/datepicker_portrait_before.png' });

    const isOpen = await openDatePicker(page);

    // readonlyInput="true" muss gesetzt sein → verhindert iOS-Keyboard → verhindert resize → Overlay bleibt offen
    const isReadonly = await page.locator('[id$="measureDate_input"]').getAttribute('readonly');
    expect(isReadonly, 'datePicker-Input muss readonly sein (verhindert iOS-Tastatur und damit Overlay-Close)').not.toBeNull();

    const result = await isOverlayWithinViewport(page);
    console.log('Portrait result:', JSON.stringify(result, null, 2));
    await page.screenshot({ path: '/tmp/datepicker_portrait_after.png' });

    expect(isOpen, 'DatePicker muss sichtbar sein').toBe(true);
    expect(result.withinViewport, 'DatePicker muss INNERHALB des Viewports liegen').toBe(true);
    await context.close();
  });

  test('DatePicker bleibt offen – iPad Landscape (1024×768)', async ({ browser }) => {
    const context = await browser.newContext(iPadLandscape);
    const page = await context.newPage();

    await login(page);
    await page.goto('/secured/measureView.xhtml', { waitUntil: 'networkidle' });
    await page.screenshot({ path: '/tmp/datepicker_landscape_before.png' });

    const isOpen = await openDatePicker(page);

    // readonlyInput="true" muss gesetzt sein → verhindert iOS-Keyboard → verhindert resize → Overlay bleibt offen
    const isReadonly = await page.locator('[id$="measureDate_input"]').getAttribute('readonly');
    expect(isReadonly, 'datePicker-Input muss readonly sein (verhindert iOS-Tastatur und damit Overlay-Close)').not.toBeNull();

    const result = await isOverlayWithinViewport(page);
    console.log('Landscape result:', JSON.stringify(result, null, 2));
    await page.screenshot({ path: '/tmp/datepicker_landscape_after.png' });

    expect(isOpen, 'DatePicker muss sichtbar sein').toBe(true);
    expect(result.withinViewport, 'DatePicker muss INNERHALB des Viewports liegen').toBe(true);
    await context.close();
  });

  test('DatePicker bleibt offen – iPhone Portrait (390×844)', async ({ browser }) => {
    const context = await browser.newContext(iPhonePortrait);
    const page = await context.newPage();

    await login(page);
    await page.goto('/secured/measureView.xhtml', { waitUntil: 'networkidle' });
    await page.screenshot({ path: '/tmp/datepicker_iphone_before.png' });

    const isOpen = await openDatePicker(page);

    const isReadonly = await page.locator('[id$="measureDate_input"]').getAttribute('readonly');
    expect(isReadonly, 'datePicker-Input muss readonly sein').not.toBeNull();

    const result = await isOverlayWithinViewport(page);
    console.log('iPhone Portrait result:', JSON.stringify(result, null, 2));
    await page.screenshot({ path: '/tmp/datepicker_iphone_after.png' });

    expect(isOpen, 'DatePicker muss auf iPhone sichtbar sein').toBe(true);
    expect(result.withinViewport, 'DatePicker muss INNERHALB des Viewports liegen').toBe(true);
    await context.close();
  });

});




