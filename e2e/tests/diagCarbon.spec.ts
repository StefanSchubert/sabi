/*
 * TEMPORARY diagnostic spec — reproduces test-5 cleanup block (uncheck + save)
 * with full request/response logging of the carbon section.
 */
import { test } from '@playwright/test';
import * as fs from 'fs';

test('diag: carbon uncheck + save trace', async ({ page }) => {
  test.setTimeout(90_000);
  const log: string[] = [];

  const push = (line: string) => {
    log.push(line);
    fs.writeFileSync('/tmp/diag-log.txt', log.join('\n') + '\n');
  };

  let postCount = 0;
  page.on('request', (req) => {
    if (req.url().includes('maintenanceView.xhtml') && req.method() === 'POST') {
      postCount++;
      const pd = req.postData() ?? '';
      push(`POST#${postCount} [${pd.length}b] BODY: ${decodeURIComponent(pd)}`);
    }
  });

  page.on('response', async (resp) => {
    if (resp.url().includes('maintenanceView.xhtml') && resp.request().method() === 'POST') {
      try {
        const body = await resp.text();
        // extract messages list items and the carbon section state from the fragment
        const msgItems = [...body.matchAll(/ui-messages-(?:info|warn|error)[^>]*>[\s\S]*?<\/div>/g)].map((m) => m[0].replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 200));
        const menuMatch = body.match(/carbonMode_label[^>]*>([^<]*)</);
        const menuSelect = body.match(/carbonMode_input[\s\S]*?<\/select>/);
        const selectedOpt = menuSelect ? (menuSelect[0].match(/selected="selected"[^>]*>|<option selected[\s\S]*?value="(\w+)"/) || menuSelect[0].match(/<option value="(\w+)" selected/)) : null;
        const notUsed = body.includes('carbon.not_used') || /Not used|Nicht in Gebrauch/.test(body);
        const replInFragment = body.includes('carbonReplacementIntervalDays');
        push(`RESP#${postCount} status=${resp.status()} msgs=[${msgItems.join(' | ')}] menuLabel="${menuMatch ? menuMatch[1].trim() : '(absent)'}" notUsedInFragment=${notUsed} replInputInFragment=${replInFragment}`);
      } catch {
        push(`RESP#${postCount} status=${resp.status()} no-body`);
      }
    }
  });

  const carbonState = async (tag: string) => {
    try {
      const checked = await page.locator('[id$=":carbonEnabled_input"]').isChecked();
      const menuVisible = await page.locator('#maintenanceForm .ui-selectonemenu').nth(1).isVisible();
      const menuLabel = menuVisible ? (await page.locator('#maintenanceForm .ui-selectonemenu').nth(1).locator('.ui-selectonemenu-label').textContent())?.trim() : '(hidden)';
      const menuSelectValue = menuVisible ? await page.locator('#maintenanceForm .ui-selectonemenu').nth(1).locator('select').inputValue().catch(() => 'n/a') : '(hidden)';
      const replVisible = await page.locator('[id$=":carbonReplacementIntervalDays"]').isVisible().catch(() => false);
      const replValue = replVisible ? await page.locator('[id$=":carbonReplacementIntervalDays"]').inputValue() : '(hidden)';
      const notUsed = await page.locator('#maintenanceForm').getByText(/Not used|Nicht in Gebrauch/).first().isVisible().catch(() => false);
      const msgInfo = await page.locator('[id$=":maintenanceMessages"] .ui-messages-info').first().isVisible().catch(() => false);
      const msgErr = await page.locator('[id$=":maintenanceMessages"] .ui-messages-error').first().isVisible().catch(() => false);
      const msgWarn = await page.locator('[id$=":maintenanceMessages"] .ui-messages-warn').first().isVisible().catch(() => false);
      push(`${tag}: checked=${checked} menuLabel="${menuLabel}" selectValue="${menuSelectValue}" replacement=${replVisible ? replValue : '(hidden)'} notUsed=${notUsed} msg info=${msgInfo} warn=${msgWarn} error=${msgErr}`);
    } catch (e) {
      push(`${tag}: ERROR ${String(e).slice(0, 150)}`);
    }
  };

  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');

  await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });

  // selectTank (like beforeEach)
  const tankMenu = page.locator('#maintenanceForm .ui-selectonemenu').first();
  const labelEl = tankMenu.locator('.ui-selectonemenu-label');
  if (!(await labelEl.textContent())?.includes('Nano-Reef')) {
    await tankMenu.locator('.ui-selectonemenu-trigger').first().click();
    const panel = page.locator('[id$=":aquarium_panel"]');
    await panel.waitFor({ state: 'visible', timeout: 8_000 });
    await Promise.all([
      page.waitForResponse((r) => r.request().method() === 'POST' && r.url().includes('maintenanceView.xhtml'), { timeout: 15_000 }),
      panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' }).click(),
    ]);
    await page.waitForLoadState('networkidle');
  }
  await carbonState('INIT');

  // cleanup block: uncheck
  const box = page.locator('[id$=":carbonEnabled"] .ui-chkbox-box').first();
  await box.click();
  await page.waitForResponse((r) => r.request().method() === 'POST' && r.url().includes('maintenanceView.xhtml'), { timeout: 15_000 });
  await page.waitForLoadState('networkidle');
  await carbonState('AFTER UNCHECK');

  // save
  const saveCarbon = page.locator('[id$=":saveCarbonButton"]');
  await Promise.all([
    page.waitForResponse((r) => r.request().method() === 'POST' && r.url().includes('maintenanceView.xhtml'), { timeout: 15_000 }),
    saveCarbon.click(),
  ]);
  await page.waitForLoadState('networkidle');
  await carbonState('AFTER SAVE');

  // wait a bit — check for late re-renders (autoUpdate etc.)
  await page.waitForTimeout(4_000);
  await carbonState('AFTER +4s');

  push(`total posts: ${postCount}`);
  push('DONE');
});
