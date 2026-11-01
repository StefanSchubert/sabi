import { test, expect, Page } from '@playwright/test';

test('probe maintenance save flow', async ({ page }) => {
  const ajaxBodies: string[] = [];
  page.on('request', (req) => {
    if (req.method() === 'POST' && req.url().includes('maintenanceView')) {
      const body = req.postData() || '';
      ajaxBodies.push(body);
    }
  });

  await page.goto('/login.xhtml');
  await page.locator('#username').fill('sabi@bluewhale.de');
  await page.locator('#password').fill('clibanarius');
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');

  await page.goto('/secured/maintenanceView.xhtml', { waitUntil: 'networkidle' });
  const trigger = page.locator('#maintenanceForm .ui-selectonemenu-trigger').first();
  await trigger.click();
  const panel = page.locator('.ui-selectonemenu-panel').first();
  await panel.waitFor({ state: 'visible' });
  await panel.locator('li.ui-selectonemenu-item').filter({ hasText: 'Nano-Reef' }).click();
  await page.waitForLoadState('networkidle');

  // Dump interval input HTML (exact client ID)
  const inputHtml = await page.locator('[id$=":intervalDays"]').evaluate((el) => el.outerHTML);
  console.log('DAYS_INPUT_HTML:', inputHtml.slice(0, 400));

  // Dump carbon checkbox DOM chain (find correct click target)
  const chain = await page.locator('[id$=":carbonEnabled_input"]').evaluate((el) => {
    const parts: string[] = [];
    let cur: Element | null = el;
    for (let i = 0; i < 4 && cur; i++) {
      parts.push(cur.outerHTML.slice(0, 500));
      cur = cur.parentElement;
    }
    return parts.join('\n===\n');
  });
  console.log('CARBON_CHAIN:', chain);

  // Save interval 21/25
  ajaxBodies.length = 0;
  await page.locator('[id$=":intervalDays"]').fill('21');
  await page.locator('[id$=":intervalPercentage"]').fill('25');
  await page.locator('[id$=":saveIntervalButton"]').click();
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1_000);

  console.log('AJAX_BODY_COUNT:', ajaxBodies.length);
  ajaxBodies.forEach((b, i) => console.log(`AJAX_BODY_${i}:`, b.slice(0, 1500)));

  const daysVal = await page.locator('[id$=":intervalDays"]').inputValue();
  const pctVal = await page.locator('[id$=":intervalPercentage"]').inputValue();
  console.log('FIELD_VALUES days=%s pct=%s', daysVal, pctVal);

  const msgHtml = await page.locator('[id$=":maintenanceMessages"]').first().evaluate((el) => el.outerHTML);
  console.log('MSG_HTML:', msgHtml.slice(0, 1200));

  // Leave data in place; cleanup handled externally
});
