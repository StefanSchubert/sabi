/*
 * TEMPORARY diagnostic spec — investigates the tank-selector E2E failure.
 * Focus: what does Escape do to PF15 selectOneMenu, and does re-clicking the
 * option fire an AJAX POST afterwards?
 */
import { test } from '@playwright/test';
import * as fs from 'fs';

test('diag: escape + reselect behaviour', async ({ page }) => {
  test.setTimeout(90_000);
  const log: string[] = [];

  const push = (line: string) => {
    log.push(line);
    fs.writeFileSync('/tmp/diag-log.txt', log.join('\n') + '\n');
  };

  let postCount = 0;
  page.on('request', (req) => {
    if (req.url().includes('maintenanceView.xhtml') && req.method() === 'POST') {
      const pd = req.postData() ?? '';
      const m = pd.match(/(aquarium|form:aquarium)[=:](\w*)/);
      postCount++;
      push(`POST#${postCount} aquarium-field=${m ? m[2] : '(none)'} [${pd.length}b]`);
    }
  });

  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');

  await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });
  const tankMenu = page.locator('#maintenanceForm .ui-selectonemenu').first();
  const labelEl = tankMenu.locator('.ui-selectonemenu-label');

  const dumpMenu = async (tag: string) => {
    const html = await tankMenu.evaluate((el) => el.outerHTML).catch(() => 'eval-failed');
    // strip long option lists, keep structure + value-carrying elements
    const compact = html.replace(/\s+/g, ' ').slice(0, 1200);
    push(`${tag} MENU-HTML: ${compact}`);
  };

  await dumpMenu('INIT');
  push(`INIT label="${(await labelEl.textContent())?.trim()}"`);

  // select Nano-Reef
  const trigger = tankMenu.locator('.ui-selectonemenu-trigger').first();
  await trigger.click();
  const panel = page.locator('[id$=":aquarium_panel"]');
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  await panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' }).click();
  await page.waitForTimeout(3_000);
  push(`AFTER SELECT label="${(await labelEl.textContent())?.trim()}" posts=${postCount}`);
  await dumpMenu('AFTER-SELECT');

  // open again + Escape
  await trigger.click();
  await panel.waitFor({ state: 'visible', timeout: 8_000 });
  const panelVisibleBeforeEscape = await panel.isVisible();
  push(`panel visible before Escape: ${panelVisibleBeforeEscape}`);
  await page.keyboard.press('Escape');
  await page.waitForTimeout(1_500);
  push(`AFTER ESCAPE label="${(await labelEl.textContent())?.trim()}" posts=${postCount}`);
  const panelVisibleAfterEscape = await panel.isVisible().catch(() => 'err');
  push(`panel visible after Escape: ${panelVisibleAfterEscape}`);
  await dumpMenu('AFTER-ESCAPE');

  // now re-click the option (like selectTank would)
  await trigger.click({ timeout: 10_000 }).catch((e) => push(`trigger click FAILED: ${String(e).slice(0, 200)}`));
  const panelVisibleAfterTrigger = await panel.isVisible().catch(() => 'err');
  push(`panel visible after trigger click: ${panelVisibleAfterTrigger}`);
  if (panelVisibleAfterTrigger === true) {
    await panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' }).click();
    await page.waitForTimeout(5_000);
    push(`AFTER RE-SELECT label="${(await labelEl.textContent())?.trim()}" posts=${postCount}`);
    await dumpMenu('AFTER-RESELECT');
  }

  push('DONE');
});
