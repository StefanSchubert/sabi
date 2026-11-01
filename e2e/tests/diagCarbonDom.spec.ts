/*
 * TEMPORARY diagnostic spec — instruments PrimeFaces.ab + ajax Queue + JS errors,
 * then replays the full test-5 sequence (tank → enable → save-error → INTERVAL).
 * Writes /tmp/diag-final.txt after EVERY step (crash-tolerant). DELETE AFTER USE.
 */
import { test } from '@playwright/test';
import * as fs from 'fs';

const OUT = '/tmp/diag-final.txt';

test('diag: ab/queue instrumentation across full sequence', async ({ page }) => {
  test.setTimeout(150_000);

  const results: Record<string, unknown> = {};
  const consoleErrors: string[] = [];
  let postCount = 0;

  page.on('response', (resp) => {
    if (resp.url().includes('maintenanceView.xhtml') && resp.request().method() === 'POST') postCount++;
  });
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text().slice(0, 300));
  });

  const flush = () => {
    try { fs.writeFileSync(OUT, JSON.stringify(results, null, 2)); } catch { /* ignore */ }
  };
  flush();

  // Capture page-level JS errors from the very first load
  await page.addInitScript(() => {
    (window as any).__diag = { abCalls: [] as unknown[], errors: [] as unknown[], queueLog: [] as unknown[] };
    window.addEventListener('error', (e) => {
      ((window as any).__diag.errors as unknown[]).push({
        t: Date.now(), msg: String(e.message).slice(0, 250),
        src: `${String(e.filename || '').slice(-60)}:${e.lineno}`,
      });
    });
  });

  // ── login ────────────────────────────────────────────────────
  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');

  await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });

  // ── instrument PrimeFaces (ab + Queue) ───────────────────────
  const instrumented = await page.evaluate(() => {
    const PFw = (window as any).PrimeFaces;
    if (!PFw) return { ok: false };
    const origAb = PFw.ab;
    PFw.ab = function (cfg: any, ext?: unknown) {
      ((window as any).__diag.abCalls as unknown[]).push({
        t: Date.now(), s: cfg?.s, e: cfg?.e,
        qLenBefore: PFw.ajax?.Queue?.requests?.length ?? -1,
      });
      return origAb(cfg, ext);
    };
    const Q = PFw.ajax?.Queue;
    if (Q) {
      const origOffer = Q.offer.bind(Q);
      Q.offer = function (a: any) {
        ((window as any).__diag.queueLog as unknown[]).push({ t: Date.now(), op: 'offer', qLenBefore: Q.requests.length });
        return origOffer(a);
      };
      const origPoll = Q.poll.bind(Q);
      Q.poll = function () {
        const r = origPoll();
        ((window as any).__diag.queueLog as unknown[]).push({ t: Date.now(), op: 'poll', qLenAfter: Q.requests.length });
        return r;
      };
    }
    return { ok: true, qLen: Q?.requests?.length ?? -1 };
  });
  results.instrumented = instrumented;
  flush();

  /** Snapshot diag counters + post count into results under label. */
  const snap = async (label: string, extra?: Record<string, unknown>) => {
    const d = await page.evaluate(() => ({
      abTail: ((window as any).__diag.abCalls as unknown[]).slice(-5),
      errTail: ((window as any).__diag.errors as unknown[]).slice(-4),
      qTail: ((window as any).__diag.queueLog as unknown[]).slice(-5),
    }));
    results[label] = { posts: postCount, ...d, ...extra };
    flush();
  };

  // ── step 1: tank selection ───────────────────────────────────
  const tankMenu = page.locator('#maintenanceForm .ui-selectonemenu').first();
  const labelEl = tankMenu.locator('.ui-selectonemenu-label');
  if (!(await labelEl.textContent())?.includes('Nano-Reef')) {
    await tankMenu.locator('.ui-selectonemenu-trigger').first().click();
    const panel = page.locator('[id$=":aquarium_panel"]');
    await panel.waitFor({ state: 'visible', timeout: 8_000 });
    await panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' }).first().click();
    await page.waitForTimeout(4_000);
  }
  await page.waitForLoadState('networkidle');
  await snap('afterTank');

  // ── step 2: ensure disabled, then enable carbon ──────────────
  const chkbox = page.locator('[id$=":carbonEnabled"] .ui-chkbox-box').first();
  const boxInput = page.locator('[id$=":carbonEnabled_input"]');
  if (await boxInput.isChecked()) {
    await chkbox.click();
    await page.waitForTimeout(4_000);
    await page.waitForLoadState('networkidle');
  }
  await chkbox.click();
  await page.waitForTimeout(4_000);
  await snap('afterEnable');
  results.enablePostFired = ((results.afterEnable as Record<string, unknown>)?.posts as number ?? 0) > ((results.afterTank as Record<string, unknown>)?.posts as number ?? 0);
  await page.waitForLoadState('networkidle');
  flush();

  // ── step 3: save WITHOUT mode → validation error ─────────────
  const saveCarbon = page.locator('[id$=":saveCarbonButton"]');
  await saveCarbon.click();
  await page.waitForTimeout(4_000);
  await snap('afterSave');
  results.savePostFired = ((results.afterSave as Record<string, unknown>)?.posts as number ?? 0) > ((results.afterEnable as Record<string, unknown>)?.posts as number ?? 0);
  await page.waitForLoadState('networkidle');
  flush();

  // ── step 4: mode menu — is it visible at all? ────────────────
  const modeMenu = page.locator('#maintenanceForm .ui-selectonemenu').nth(1);
  const modeMenuVisible = await modeMenu.isVisible().catch(() => false);
  results.modeMenuVisible = modeMenuVisible;
  flush();

  if (modeMenuVisible) {
    await modeMenu.locator('.ui-selectonemenu-trigger').first().click();
    const modePanel = page.locator('[id$=":carbonMode_panel"]');
    await modePanel.waitFor({ state: 'visible', timeout: 8_000 });
    await modePanel.locator('li.ui-selectonemenu-item').filter({ hasText: /Interval|Intervall/ }).first().click();
    await page.waitForTimeout(4_000);
    await snap('afterInterval');
    results.intervalPostFired = ((results.afterInterval as Record<string, unknown>)?.posts as number ?? 0) > ((results.afterSave as Record<string, unknown>)?.posts as number ?? 0);
    await page.waitForLoadState('networkidle');
    flush();
  }

  // ── final: full dumps ────────────────────────────────────────
  results.final = await page.evaluate(() => ({
    abAll: (window as any).__diag.abCalls,
    errsAll: (window as any).__diag.errors,
    qAll: (window as any).__diag.queueLog,
  }));
  results.consoleErrors = consoleErrors.slice(-10);
  flush();
});
