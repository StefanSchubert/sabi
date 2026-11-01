# E2E Knowledge Base (Playwright)

Persistent knowledge for future sessions: environment, PrimeFaces 15 quirks, and the
established test patterns. **Read this before writing or debugging E2E tests** — it
captures lessons that previously cost hours of re-discovery across context resets.

## Environment

| Component | Where | Notes |
|---|---|---|
| Backend (REST) | Docker container `sabi-as`, `http://localhost:8080/sabi` | see `devops/sabi_docker_sdk/docker-compose.yml`; DB client inside the container is `mariadb` (no `mysql` binary) |
| Webclient (JSF) | **runs LOCALLY on `http://localhost:8088`** (NOT in Docker) | this is what E2E targets (`e2e/playwright.config.ts` baseURL); started with the spring-instrument javaagent, see `DEVELOPERS_MANUAL.md` |
| Test user | `sabi@bluewhale.de` / `clibanarius` | tanks: "Nano-Reef" (id 1), "Freshwater" (id 2) |
| UI language | per-user, NOT browser locale | this user = German → all text assertions must be bilingual regexes (`de\|en`) |

**XHTML/CSS changes require a full webclient restart** (no Facelets hot-reload, see AGENTS.md).
Build: `cd sabi-webclient && mvn package -DskipTests` (~1 min). Run:
`cd e2e && npx playwright test <spec>` (30 s timeout per assertion, chromium).

## PrimeFaces 15 `selectOneMenu` — known quirks (why E2E-critical pages avoid it)

1. **Re-selecting an already-selected option fires NO change event.** PF's `selectItem()`
   compares the clicked option value with the current selection; if equal → no behavior
   event → no AJAX POST. A test that clicks the option again hangs on `waitForResponse`
   until timeout (observed: 15 s, then the whole test fails).
2. **Label and hidden select can disagree.** After Escape (or an interrupted interaction),
   PF 15 resets the visible label to the placeholder while the hidden `<select>` keeps its
   value (client-side only, no POST). Any "is it selected?" check must read the hidden
   select's value — never the label text.
3. **Panels are global in the DOM.** Every `selectOneMenu` renders a panel
   (`[id$=":xxx_panel"]`) into the page. When multiple menus exist, a global `.first()`
   locator is ambiguous — scope panel locators per client ID.
4. **`networkidle` races with XHR dispatch.** The AJAX POST for a change event can be
   dispatched after `networkidle` is reached → always register
   `page.waitForResponse(...)` BEFORE the triggering click (`Promise.all` pattern).

**Decision (007-water-maintenance-tracking, 2026-11):** for pages under E2E test, use plain
`<h:selectOneMenu>` (native `<select>`) instead of `p:selectOneMenu` where deterministic
automation matters. Native select + Playwright `selectOption()` is fully deterministic: no
trigger, no panel, no JS quirk. `p:ajax` still works inside `h:selectOneMenu` (JSF 2.3+
behavior events on standard components; the default event for select components is
`change`). Precedent in the codebase: `tankEditor.xhtml` already uses `h:selectOneMenu`.

Note: a native `<select>` also fires no change event when re-selecting the same value →
keep "skip if already selected" guards in helpers (see `selectTank` in
`maintenanceFlow.spec.ts`).

## Established patterns (`maintenanceFlow.spec.ts` is the reference implementation)

- **`login(page)`**: `/login.xhtml` → fill `#username`/`#password` → click
  `button[type="submit"]` → `networkidle`.
- **`waitForAjaxPost(page)`**: `page.waitForResponse(resp => POST && url contains '<view>.xhtml', 15s)` —
  must be registered BEFORE the triggering click:
  `await Promise.all([waitForAjaxPost(page), button.click()])`, then `networkidle`.
- **JSF client IDs**: form-prefixed (`maintenanceForm:xxx`) → use suffix matchers
  `[id$=":xxx"]`. Native selects are located as `#<formId> select[id$=":componentId"]`.
- **CSS visibility** (AGENTS.md): `expectCssVisible(page, selector)` — bounding-box
  width/height > 0, `display != none`, `visibility != hidden`. Use
  `getBoundingClientRect` (not computed width — inline elements return "auto"/NaN).
- **Messages**: success = `[id$=":maintenanceMessages"] .ui-messages-info`;
  failure = `.ui-messages-warn` / `.ui-messages-error` (PF renders one container per severity).
- **PF checkbox**: click the visible `.ui-chkbox-box` wrapper — the raw input lives in
  `.ui-helper-hidden-accessible` and catches no pointer events.
- **Cleanup**: every test that creates records must delete them again (notes carry a
  `Date.now()` suffix so rows are uniquely findable).
- **No `force: true`, no `page.evaluate(() => el.click())`** (AGENTS.md): real clicks only;
  if an element can't be clicked, that is a bug to fix, not a workaround.

## Diagnostic specs (do NOT commit)

The `e2e/tests/` folder may contain temporary diagnostic specs (`diag*.spec.ts`,
`probe*.spec.ts`) — debug artifacts from debugging sessions, excluded from commits.
