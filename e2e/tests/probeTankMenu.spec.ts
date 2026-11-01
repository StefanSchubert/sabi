/**
 * DEBUG-PROBE (nicht Teil des Test-Suits): dumped das Tank-Menü-DOM und die
 * rohe AJAX-Response-Fragmente, um das Label-Verhalten des PF 15 SelectOneMenu
 * nach @form-Re-Render zu verstehen. NICHT kommittieren.
 */
import { test } from '@playwright/test';

async function dumpMenuRegion(page: import('@playwright/test').Page, tag: string) {
  const html = await page.evaluate(() => document.getElementById('maintenanceForm')!.innerHTML);
  const i = html.indexOf('aquarium');
  console.log(`\n===== ${tag} =====`);
  console.log(html.slice(Math.max(0, i - 100), i + 2500));
}

async function dumpFormData(page: import('@playwright/test').Page, tag: string) {
  const entries = await page.evaluate(() => {
    const fd = new FormData(document.getElementById('maintenanceForm')!);
    return Array.from(fd.entries()).filter(([k]) => k.includes('aquarium'));
  });
  console.log(`\n===== ${tag} (FormData aquarium-entries) =====`);
  console.log(JSON.stringify(entries, null, 2));
}

test('probe tank menu DOM + AJAX response', async ({ page }) => {
  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');

  await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });

  await dumpMenuRegion(page, 'MENU REGION BEFORE SELECTION');
  await dumpFormData(page, 'FORM DATA BEFORE SELECTION');

  // Nano-Reef auswählen und AJAX-Response-Mitte erfassen
  const trigger = page.locator('#maintenanceForm .ui-selectonemenu-trigger').first();
  await trigger.click();
  const panel = page.locator('[id$=":aquarium_panel"]');
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  const option = panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' });

  const respPromise = page.waitForResponse(
    (r) => r.request().method() === 'POST' && r.url().includes('maintenanceView.xhtml'),
    { timeout: 15_000 }
  );
  await option.click();
  const resp = await respPromise;
  const body = await resp.text();

  console.log('\n===== AJAX RESPONSE: menu region =====');
  const bi = body.indexOf('aquarium');
  console.log(body.slice(Math.max(0, bi - 100), bi + 2500));

  await page.waitForLoadState('networkidle');

  await dumpMenuRegion(page, 'MENU REGION AFTER RE-RENDER');
  await dumpFormData(page, 'FORM DATA AFTER RE-RENDER');

  // Reload → frischer GET
  await page.reload({ waitUntil: 'networkidle' });
  await dumpMenuRegion(page, 'MENU REGION AFTER RELOAD (fresh GET)');

  // ── Test-2-Sequenz reichern: select → öffnen → Escape ──
  const label = page.locator('#maintenanceForm .ui-selectonemenu').first().locator('.ui-selectonemenu-label');
  console.log('\n===== LABEL after reload:', await label.textContent());

  // Instrumentierung: logge jede selectedIndex-Änderung des Hidden-Selects,
  // robust gegen Node-Ersatz durch PF-Re-Render (frisches querySelector pro Tick + Node-ID).
  const installSelProbe = () => page.evaluate(() => {
    (window as any).__selLog = [] as Array<{ t: number; idx: number; val: string; node: number }>;
    (window as any).__lastKey = '';
    const tick = () => {
      const sel = document.querySelector('#maintenanceForm\\:aquarium_input') as HTMLSelectElement | null;
      if (!sel) return;
      const node = ((sel as any).__probeId ??= Math.floor(Math.random() * 1e9));
      const key = `${node}:${sel.selectedIndex}:${sel.value}`;
      if ((window as any).__lastKey !== key) {
        (window as any).__lastKey = key;
        (window as any).__selLog.push({ t: Date.now(), idx: sel.selectedIndex, val: sel.value, node });
      }
    };
    tick();
    if ((window as any).__selTimer) clearInterval((window as any).__selTimer);
    (window as any).__selTimer = setInterval(tick, 25);
  });
  await installSelProbe();

  const dumpSelLog = async (tag: string) => {
    const log = await page.evaluate(() => {
      const l = (window as any).__selLog.splice(0);
      return l;
    });
    console.log(`\n===== SELECT CHANGES: ${tag} =====`);
    for (const e of log) console.log(`  t=${e.t} idx=${e.idx} val=${JSON.stringify(e.val)} node=${e.node}`);
    if (log.length === 0) console.log('  (keine Änderungen)');
  };

  const trig = page.locator('#maintenanceForm .ui-selectonemenu-trigger').first();
  const panel2 = page.locator('[id$=":aquarium_panel"]');

  // Auswahl (wie in beforeEach)
  await trig.click();
  await panel2.waitFor({ state: 'visible', timeout: 8_000 });
  const respP = page.waitForResponse(
    (r) => r.request().method() === 'POST' && r.url().includes('maintenanceView.xhtml'),
    { timeout: 15_000 }
  );
  await panel2.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' }).click();
  await respP;
  await page.waitForLoadState('networkidle');
  await dumpSelLog('after selection (before reload)');
  console.log('\n===== LABEL after selection:', JSON.stringify(await label.textContent()));

  // Reload → frischer GET (neues Select-Element, window-Reset) → Probe neu installieren
  await page.reload({ waitUntil: 'networkidle' });
  await installSelProbe();

  // Auswahl nach Reload (wie in beforeEach des E2E-Tests)
  await trig.click();
  await panel2.waitFor({ state: 'visible', timeout: 8_000 });
  const respP2 = page.waitForResponse(
    (r) => r.request().method() === 'POST' && r.url().includes('maintenanceView.xhtml'),
    { timeout: 15_000 }
  );
  await panel2.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' }).click();
  await respP2;
  await page.waitForLoadState('networkidle');
  await dumpSelLog('after 2nd selection (post re-render)');
  console.log('\n===== LABEL after 2nd selection:', JSON.stringify(await label.textContent()));

  // Menü öffnen (wie im Test-Body)
  await trig.click();
  await panel2.waitFor({ state: 'visible', timeout: 8_000 });
  await page.waitForTimeout(500); // Transition (fade) abwarten → onEntered gelaufen
  await dumpSelLog('after open + 500ms');

  // Escape (wie im Test-Body)
  await page.keyboard.press('Escape');
  await page.waitForTimeout(300);
  await dumpSelLog('after Escape');
  console.log('\n===== LABEL after Escape:', JSON.stringify(await label.textContent()));
});
