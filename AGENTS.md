# Agentic Guidelines for Sabi

Maintain this file in english. If you find sections in 
german from previous contributions, please translate them to english.

## Communication Preferences
**Language & Tone**: German (Deutsch), informal "Du" form
- User preference: Direct, peer-to-peer communication
- Use "Du" (not "Sie") in all responses
- Keep responses concise and action-oriented
- Technical discussions on equal footing

## Context Management (MANDATORY)
**Token Status Reporting**: ALWAYS end your response with token count in format:
```
[XXXk tokens used]
```

**Context Refresh Protocol**:
- Every 50k tokens: Re-read AGENTS.md explicitly
- At 80-100k tokens: Recommend session restart to user
- NEVER exceed 120k tokens without explicit user approval

**How to refresh context at 50k intervals**:
1. Pause current work
2. Say: "🔄 Context Refresh (50k tokens reached) - Re-reading AGENTS.md"
3. Use read_file tool to read `AGENTS.md` (root of repository)
4. Summarize: "✅ Key rules refreshed: [list top 3 critical rules]"
5. Continue work

## Technical Context: SBOM (Software Bill of Materials)

**MANDATORY: Before starting work on any module, read the SBOM to understand the current dependency landscape.**

Each Maven module generates a CycloneDX SBOM during `mvn package`. The SBOM files are located at:

| Module           | SBOM Location                          |
|------------------|----------------------------------------|
| `sabi-server`    | `sabi-server/target/bom.json`          |
| `sabi-webclient` | `sabi-webclient/target/bom.json`       |
| `captcha`        | `captcha/target/bom.json`              |
| `sabi-boundary`  | `sabi-boundary/target/bom.json`        |

**Why read the SBOM:**
- Understand which libraries and versions are actually in use (including transitive dependencies)
- Identify relevant frameworks before suggesting dependency changes
- Avoid suggesting libraries that are already present under a different artifact ID
- Check the exact versions of key components (Spring Boot, PrimeFaces, EclipseLink, etc.)
- Determine correct API namespaces (e.g. `jakarta.*` vs `javax.*` — Jakarta EE 9+ uses `jakarta.*`)

**When the SBOM is stale (regenerate before use):**
- After any `pom.xml` dependency change
- After a fresh `git clone` / branch switch
- When `target/` does not exist or is older than `pom.xml`

**How to generate (if not yet present):**
```bash
cd <module-dir> && mvn package -DskipTests | tee /tmp/mvn-build.out
# SBOM written to target/bom.xml and target/bom.json
# Then read: read_file target/bom.json (grep for "name" + "version" fields)
```

**How to read efficiently (key components to extract):**
```bash
# Extract name+version pairs from bom.json
python3 -c "
import json, sys
bom = json.load(open('target/bom.json'))
for c in bom.get('components', []):
    print(c.get('name',''), c.get('version',''))
" | grep -E "spring-boot|primefaces|eclipselink|joinfaces|jakarta" | tee /tmp/sbom-key-deps.out
```

---

## Terminal Output (MANDATORY)

**Problem**: The IntelliJ Copilot Plugin often does NOT display terminal output directly — the
`run_in_terminal` output appears empty or truncated even when the command succeeded.
Additionally, shell escaping of special characters can break the terminal state (zsh
event-not-found, dquote loop, etc.).

**Core rule: EVERY terminal command whose output needs to be evaluated must write via `| tee`
to a file. Output is read exclusively via `read_file`.**

```zsh
# Pattern for all terminal commands:
some_command | tee /tmp/output.out
# → then: read_file /tmp/output.out
```

**Rule: NEVER run scripts as inline `-c "..."` — always via file:**
1. Write the script using the `create_file` tool to `/tmp/scriptname.py`
2. Run in terminal: `python3 /tmp/scriptname.py | tee /tmp/scriptname.out`
3. Read output: use `read_file` tool on `/tmp/scriptname.out`

**Applies to shell one-liners with special characters** (parentheses, exclamation marks, backticks):
- Instead of `command | grep "foo(bar)"` → write a script to `/tmp/`
- For complex grep/sed/awk: always prefer a `/tmp/` script

**Applies to simple diagnostic commands** (env checks, grep, find, mvn, etc.):
- `echo "VAR=$VAR" | tee /tmp/check.out` instead of reading `echo` output directly
- `find ... | tee /tmp/find.out` instead of evaluating output in the terminal

---

## Tool Reliability: replace_string_in_file (MANDATORY)

**`replace_string_in_file` reports "success" but sometimes writes nothing — for ALL file types.**

Observed cases so far:
- `.properties` files: when `oldString` contains raw UTF-8 bytes but the file uses `\uXXXX`
  escapes (or vice versa) — tool reports success, file unchanged.
- `.xhtml` files: for larger `replace_string_in_file` calls with multi-line `oldString` on XHTML
  files, the tool regularly reports "success" but does NOT apply the change
  (observed May 2026: first two calls on `fishStockTab.xhtml` ignored, third call worked).

**Mandatory rules:**
1. **After EVERY `replace_string_in_file` call**: run `read_file` on the modified file and
   visually verify the change — do NOT trust the tool's success message.
2. **For multi-line replacements in XHTML/XML**: always read the file first with `read_file`,
   copy `oldString` exactly from the current file content (respect whitespace).
3. **Repeated failure despite correct `oldString`**: use `insert_edit_into_file` as a fallback —
   this tool works contextually and is more robust for large XHTML blocks.
4. **After failure**: re-read the file (`read_file`), then retry with a corrected `oldString`.

**Key principle: Tool output ≠ disk content. Only `read_file` shows what is actually there.**

---

## Development Environment Setup

> **Before starting any backend work, read [`DEVELOPERS_MANUAL.md`](./DEVELOPERS_MANUAL.md) in the
> repository root.** It is the authoritative reference for:
> - EclipseLink javaagent setup (required for IDE test runs and the Spring Boot run-config)
> - JUnit test rules: always create test users via `UserRepository` in `@BeforeEach` — never use
>   hardcoded IDs that reference non-existent rows in the Testcontainer DB
> - EclipseLink L1 cache pitfalls in repository tests (`em.clear()` after `saveAndFlush()`)
> - Flyway migration rules (no hardcoded schema prefix, immutable existing scripts)
> - Docker redeploy checklist

- MacBook CLI with **BSD Unix tools** (not GNU) → different syntax!
  - `sed -i ''` instead of `sed -i` (BSD sed requires an empty extension argument)
  - `grep` without `--line-buffered` etc. (BSD grep, not GNU grep)
  - `find` without `-printf` (BSD find, not GNU find)
  - When in doubt: use `gsed`, `ggrep`, `gfind` (Homebrew GNU tools)
- IntelliJ Copilot Plugin for code generation
- Docker for local testing
- Deployment to production via Ansible Playbook

### JSF/Facelets Hot-Reload (IMPORTANT)

**XHTML changes ALWAYS require a server restart.** There is no hot-reload for Facelets:

- JoinFaces/Spring Boot does **not** detect classpath resource timestamps at runtime
- Even with `joinfaces.jsf.project-stage: development`, compiled Facelets views are **cached**
- Copying modified XHTML files to `target/classes/` has **NO effect** — the server must be
  restarted completely
- **Symptom of a stale view**: Auto-generated JSF IDs (e.g. `j_idt30`) instead of explicitly
  assigned IDs (`tankSelector`), or AJAX updates targeting non-existent components

**Rule: After every XHTML change → restart the server → only then test.**

### Spring Security + JSF: Forward vs. Redirect

**NEVER use `successForwardUrl()` in combination with JSF.**

A Servlet forward after successful authentication carries the `jakarta.faces.ViewState` POST
parameter from the login page into the target page. JSF then tries to apply the login page's
ViewState to the target page's component tree → `ArrayIndexOutOfBoundsException` in
`UIComponentBase.restoreState`.

**Always use `defaultSuccessUrl(url, true)`** — this produces an HTTP 302 redirect, the browser
sends a fresh GET request, and JSF creates a new view without state conflicts.

---

## i18n Message Bundle Handling (MANDATORY)

### Supported Languages (MANDATORY)

**Sabi supports exactly these 5 languages:**

| Code | Language   | Bundle File                |
|------|------------|----------------------------|
| `de` | Deutsch    | `messages_de.properties`   |
| `en` | English    | `messages_en.properties`   |
| `es` | Español    | `messages_es.properties`   |
| `fr` | Français   | `messages_fr.properties`   |
| `it` | Italiano   | `messages_it.properties`   |

Additionally, `messages.properties` exists as the **fallback bundle** (English).

**New features MUST populate all 6 files (5 languages + fallback) with new i18n keys.**
Missing translations are a release blocker. After every feature implementation, verify that no
key is missing from any of the language files.

**Encoding rule for `.properties` files:**
- Spring Boot is configured with `spring.messages.encoding: UTF-8` → raw UTF-8 bytes are valid
- `\uXXXX` escapes are still allowed and preferred for consistency with existing content
- **NEVER** write raw Latin-1 bytes (`0x80`–`0xFF`) into properties files
- **NEVER** insert U+FFFD replacement characters (`\xef\xbf\xbd`)

**Python helper scripts for file operations:**
- Always write as proper UTF-8: use the `create_file` tool (not heredoc in terminal)
- Explicitly specify encoding when writing files: `open(path, 'w', encoding='utf-8')`
- For binary byte operations on properties files: only use `\uXXXX` escape sequences as target
- Write helper scripts to `/tmp/` and call them with `python3 /tmp/scriptname.py`
- **No heredoc** for Python code in the terminal — shell escaping corrupts special characters

**When editing message bundles via tools:**
- `replace_string_in_file` writes in the tool's file encoding → always specify special chars as `\uXXXX`
- **WARNING: `replace_string_in_file` fails silently** when `oldString` contains raw UTF-8 chars
  (ö, ü, è …) but the file uses `\uXXXX` escape sequences (or vice versa). The tool reports
  "success" but writes nothing.
- **WARNING: `insert_edit_into_file` also fails silently** for `.properties` files — the tool
  reports "success" and even displays the file content including the new keys, but the keys are
  often NOT actually written to disk (observed in 5 of 6 files).
- **Most reliable method: Python helper script.** For new i18n keys, write a script to
  `/tmp/append_keys.py` (via `create_file`) that appends keys using
  `open(path, 'r+', encoding='utf-8')`, then run with
  `python3 /tmp/append_keys.py | tee /tmp/append_keys.out`.
- **ALWAYS perform a byte-check after every change**: write a Python script to `/tmp/bytecheck.py`,
  run with `python3 /tmp/bytecheck.py | tee /tmp/bytecheck.out`, then use `read_file` on
  `/tmp/bytecheck.out` (NO inline `python3 -c "..."` — shell escaping corrupts special characters!)
- **Trust the byte-check MORE than the tool output** — only the byte-check shows what is actually
  on disk.

---

## Sitemap Maintenance (MANDATORY)

The sitemap is located at:
`sabi-webclient/src/main/resources/META-INF/resources/sitemap.xml`

It is served publicly via Spring Security `permitAll()` for `/sitemap.xml`.
The `robots.txt` references it at `https://sabi-project.net/sitemap.xml`.

**Rules:**
- **When a new publicly accessible page is added** (i.e. added to `WebSecurityConfig` with
  `permitAll()` AND intended for search engine indexing), add a corresponding `<url>` entry to
  `sitemap.xml`.
- **Update `<lastmod>`** of affected entries to the current date whenever the page content
  changes significantly.
- **Do NOT add** to the sitemap: token-gated pages (`houseReefReport.xhtml`), technical
  endpoints (`/actuator/**`, `/.well-known/**`, `/api/**`), auth-flow pages
  (`login.xhtml`, `logout.xhtml`, `pwreset.xhtml`, `preregistration.xhtml`).

**Currently indexed public pages (as of 2026-05-16):**

| URL | Priority | Note |
|-----|----------|------|
| `https://sabi-project.net/` | 1.0 | Landing page |
| `https://sabi-project.net/register.xhtml` | 0.6 | Registration |
| `https://sabi-project.net/gdpr.xhtml` | 0.4 | Privacy Policy |
| `https://sabi-project.net/terms_of_usage.xhtml` | 0.4 | Terms of Usage |
| `https://sabi-project.net/impressum.xhtml` | 0.3 | Legal Notice |
| `https://sabi-project.net/credits.xhtml` | 0.3 | Credits |

---

## UI Style Guide (MANDATORY)

> **Before implementing any frontend feature, read [`UI_StyleGuide.md`](./UI_StyleGuide.md) in the
> repository root.**

The style guide is the single source of truth for:
- Color palette (Marine Theme, light + dark mode)
- Accessibility rules (WCAG AA contrast requirements, forbidden color combos)
- Navigation patterns (**no dialogs for input forms** — use full pages)
- Back navigation: breadcrumb link at top + Cancel button at bottom
- Form structure, button conventions, PrimeFaces component rules
- Responsive breakpoints

**Key rules at a glance (details in UI_StyleGuide.md):**
- Input forms → standalone pages, never `p:dialog`
- Every input page → `sabi-back-link` breadcrumb + Cancel button with `type="button" onclick="window.location.href=..."`
- Save button → `background:#065f46` (dark green, 8:1 contrast)
- Links on light bg → `#0369a1` or `#075985` (≥ 5.7:1)
- `lightblue` / `yellow` as text color on light bg → **FORBIDDEN**

---

## Playwright E2E Tests: Quality Rules (MANDATORY)

### No `force: true` and no `page.evaluate(() => el.click())` as standard click

**Problem**: `page.evaluate(() => el.click())` and `.click({ force: true })` completely bypass CSS
visibility and layout checks. A test can be **green** even though the element is **invisible or
unclickable** for real users (e.g. `width: 0px`, `visibility: hidden`, `overflow: hidden`,
covered by another element).

**Lesson learned (April 2026)**: A PrimeFaces toggle button had `width: 0px; height: 0px` due to
missing CSS rules. The test was written green using `page.evaluate(() => el.click())` — but real
users could never see or click the element.

**Rules:**
- **Default**: Always use a real Playwright click without `force`: `await locator.click()`
- **Before clicking**: Explicitly check visibility: `await expect(locator).toBeVisible()`
- **`force: true` / `page.evaluate click`**: ONLY as a last resort for genuine technical obstacles
  (e.g. a transient overlay), and then with an explanatory comment **why**
- **After a workaround**: Fix the actual CSS/layout bug instead of keeping the workaround
  permanently in the test

### Validate CSS visibility of elements

When an element is found but does not seem clickable, always check the **computed styles**:

```javascript
// Diagnostic script: check computed styles of an element
const styles = await page.evaluate((sel) => {
  const el = document.querySelector(sel);
  if (!el) return { error: 'not found' };
  const cs = window.getComputedStyle(el);
  return { width: cs.width, height: cs.height, display: cs.display,
           visibility: cs.visibility, overflow: cs.overflow };
}, '.my-selector');
```

Elements with `width: 0px` or `height: 0px` are effectively invisible — that is a **bug**,
not a test problem.

### Screenshot-based verification for UI bugs

For visual problems (icon invisible, text invisible) always take screenshots and
**visually confirm** before declaring the test green:

```typescript
await page.screenshot({ path: '/tmp/debug_before.png' });
// ... action ...
await page.screenshot({ path: '/tmp/debug_after.png' });
// Open screenshots in IDE and verify visually!
```

A green test alone is not proof of correct rendering.

---

## Frontend-REST Design Preferred

Favor `@RequestScope` beans for REST controllers to ensure statelessness and thread safety.
Avoid using `@SessionScope` or `@Component` for REST controllers, as they can lead to shared
state issues in a multi-threaded environment. Always inject dependencies via constructor injection
and keep REST controllers focused on handling HTTP requests and responses without maintaining any
internal state.

## Backend REST API: Auth Token Pattern (MANDATORY)

### How the JWT token flows through the stack

**Sabi uses a custom HTTP header `Authorization` with `Bearer` prefix** — not a standard OAuth2
flow, but a self-implemented JWT scheme:

1. **Login** → Backend returns `Authorization: Bearer <jwt>` in the response header
2. **Webclient** reads this header and stores it in `UserSession.sabiBackendToken`
   - Token already includes the `Bearer` prefix
3. **All backend calls** set this token via `RestHelper.prepareAuthedHttpHeader(token)`
   as the `Authorization` header
4. **`JWTAuthorizationFilter`** checks: `token.startsWith("Bearer ")` → sets Authentication in
   SecurityContext
   - If token is missing or has no Bearer prefix → filter lets the request through
     **without Authentication**
   - Spring Security's `anyRequest().authenticated()` then returns **403**

**Important: Auth errors manifest as 403, not 401!**
The filter returns 401 for a missing/invalid token, but if the request reaches the security
config without a SecurityContext → 403 Forbidden.

### Multipart endpoints: `MultipartFile` instead of `byte[]`

**NEVER use `@RequestParam("file") byte[] fileBytes` for multipart uploads.**

Spring cannot automatically convert `MultipartFile` to `byte[]` — this leads to a
`MethodArgumentTypeMismatchException` (400), which sometimes manifests as a 403 on the client.

**Correct:**
```java
// Backend Controller
@PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Void> upload(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,  // ← MultipartFile, NOT byte[]
        @RequestHeader(name = AUTH_TOKEN) String token,
        Principal principal) {
    byte[] bytes = file.getBytes();
    String ct = file.getContentType() != null ? file.getContentType() : "image/jpeg";
    // ...
}
```

**Webclient-side** (RestTemplate with ByteArrayResource):
```java
ByteArrayResource resource = new ByteArrayResource(bytes) {
    @Override public String getFilename() { return "photo.jpg"; }
};
MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
parts.add("file", resource);
HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token, MediaType.MULTIPART_FORM_DATA);
restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(parts, headers), String.class);
```

### Redeploy Docker backend (checklist)

**Preferred method: use the redeploy script.**

```bash
# Standard redeploy (no boundary changes, no DB migrations):
cd devops/sabi_docker_sdk && bash server_redeploy.sh

# After sabi-boundary changes (pom.xml or source):
bash server_redeploy.sh --boundary

# After new Flyway migration scripts:
bash server_redeploy.sh --flyway

# Both at once:
bash server_redeploy.sh --boundary --flyway
```

**Script location:** `devops/sabi_docker_sdk/server_redeploy.sh`

**What the script does:**
1. Stops + removes the `sabi-as` container
2. `--boundary`: `mvn install -DskipTests` in `sabi-boundary/`
3. `mvn package -DskipTests` in `sabi-server/`
4. Runs `copyjars.sh` to copy the JAR into the Docker context
5. `docker compose -f docker-compose-arm.yml up --build -d sabi-backend`
6. `--flyway`: runs the Flyway container (`docker compose run --rm flyway`) — it exits after applying pending migrations
7. Tails the container log for 25 s so startup errors are visible immediately

**For AMD64 servers** use `docker-compose.yml`; for ARM development (MacBook M1/M2/M3/M4)
always use `docker-compose-arm.yml` with `Dockerfile_ARM`.

**Manual steps (fallback, if script is not usable):**
```bash
cd sabi-server && mvn package -DskipTests
cd devops/sabi_docker_sdk && bash copyjars.sh
docker compose -f docker-compose-arm.yml stop sabi-backend
docker compose -f docker-compose-arm.yml up --build -d sabi-backend
# With migrations:
docker compose -f docker-compose-arm.yml run --rm flyway
```

---

## Backend Logging: PII Rules (MANDATORY)

**Email addresses and user names are personal data (GDPR/DSGVO) and MUST NOT appear in log files.**

### Allowed vs. Forbidden

| ✅ Allowed in logs         | ❌ Forbidden in logs          |
|---------------------------|-------------------------------|
| `user_id=42`              | `stefan@example.com`          |
| `aquarium_id=104`         | `principal.getName()` at INFO+ |
| Action/resource references | Usernames or display names   |

### Rules by Log Level

- **INFO / WARN / ERROR**: NEVER log email addresses or `principal.getName()`. Use `user_id=` (from the already-resolved `UserEntity`) instead.
- **DEBUG**: Strongly prefer `user_id=` too. Since DEBUG is disabled in production, the risk is lower — but consistency is better.
- **TRACE**: No additional restrictions.

### Standard Patterns

```java
// ✅ CORRECT — use resolved user entity ID
UserEntity user = userRepository.getByEmail(userEmail);
log.info("Created link for aquarium {} by user_id={}", aquariumId, user.getId());

// ✅ CORRECT — omit user identity if not meaningful
log.error("Rejected profile update: incomplete data submitted ({})", profileTo);

// ❌ WRONG — leaks PII into log file
log.info("Created link for aquarium {} by {}", aquariumId, userEmail);
log.debug("GET /api/tank for {}", principal.getName());
```

### Where to find the user_id

After ownership check, `user.getId()` is always in scope. If the user entity is not yet resolved (e.g. validation before DB lookup), omit the user reference from the log message.

---

## Backend Security: Ownership Checks (MANDATORY)

### Principle: Every mutating operation MUST verify that the caller owns the affected record.

**Sabi uses the JWT principal email as the single source of truth for identity.**
`principal.getName()` in every controller returns the authenticated user's email address.
This email is passed down to the service layer in every call.

### The Standard Ownership Pattern (ALL services must follow this)

```java
// 1. Resolve caller to UserEntity (never trust IDs from the request body)
UserEntity user = userRepository.getByEmail(userEmail);
if (user == null) {
    return new ResultTo<>(dto, Message.error(TankMessageCodes.UNKNOWN_USER, userEmail));
}

// 2. Load the record WITH an ownership filter (never load by ID alone)
SomeEntity entity = someRepository.findByIdAndUserId(recordId, user.getId());
// or: aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
// or: measurementRepository.getByIdAndUser(measurementId, user);

// 3. Check and reject if not found (= not owned)
if (entity == null) {
    return new ResultTo<>(dto, Message.error(SomeMessageCodes.NOT_YOUR_RECORD, recordId));
}

// 4. Only now perform the operation
```

### Verified Ownership Coverage (audited 2026-05-03)

| Controller | Operation | Ownership Method | Status |
|---|---|---|---|
| TankController | DELETE `/{id}` (removeTank) | `getAquariumEntityByIdAndUser_IdIs` | ✅ |
| TankController | PUT `` (updateTank) | `getAquariumEntityByIdAndUser_IdIs` | ✅ |
| TankController | POST/GET/DELETE `/{id}/photo` | `getAquariumEntityByIdAndUser_IdIs` | ✅ |
| FishStockController | PUT `/{fishId}` (updateFish) | `findByIdAndUserId` | ✅ |
| FishStockController | DELETE `/{fishId}` (deleteFish) | `findByIdAndUserId` | ✅ |
| FishStockController | PUT `/{fishId}/departure` | `findByIdAndUserId` | ✅ |
| FishStockController | DELETE `/{fishId}/catalogue-link` | `findByIdAndUserId` | ✅ |
| FishStockController | POST/DELETE `/{fishId}/photo` | `findByIdAndUserId` | ✅ |
| FishStockController | POST `/{fishId}/size` | `findByIdAndUserId` | ✅ |
| MeasurementController | POST `` (addMeasurement) | tank: `getAquariumEntityByIdAndUser_IdIs` | ✅ |
| MeasurementController | PUT `` (updateMeasurement) | `getByIdAndUser` | ✅ |
| MeasurementController | DELETE `/{id}` (removeMeasurement) | `getByIdAndUser` | ✅ |
| MeasurementController | PUT/DELETE `/reminder` | `pReminderTo.getUserId() != user.getId()` | ✅ |
| PlagueCenterController | POST `/record` (addPlagueRecord) | tank: `getAquariumEntityByIdAndUser_IdIs` | ✅ |
| FishCatalogueController | POST `/` (proposeEntry) | creator set from JWT | ✅ |
| FishCatalogueController | PUT `/{id}` (updateEntry) | `isCreator` OR `isAdmin` check | ✅ |
| FishCatalogueAdminController | PUT `/{id}/approve`, `/{id}/reject` | `isAdmin()` check in service | ✅ |
| UserProfileController | PUT `` (updateProfile) | `getByEmail(principalName)` | ✅ |

### Rules for New Endpoints

**NEVER** do this in a service method:
```java
// ❌ INSECURE: loads by ID without ownership check
Optional<FishEntity> fish = fishRepository.findById(fishId);
```

**ALWAYS** do this:
```java
// ✅ SECURE: combined ID + user constraint
Optional<TankFishStockEntity> fish = tankFishStockRepository.findByIdAndUserId(fishId, user.getId());
```

**Controller response codes for failed ownership**:
- Return `403 FORBIDDEN` (not `404`) when a record exists but doesn't belong to the caller —
  to avoid leaking existence information, either `403` or `404` is acceptable, but be consistent.
- Log as `WARN` or `ERROR` with message: "Attempted access on record {id} by non-owner {email}"

### Long ID Comparison Warning

When comparing `long` (primitive) with `Long` (object) using `!=`:
```java
// ✅ Safe: long primitive != Long object → Java auto-unboxes Long to long
if (reminderTo.getUserId() != userEntity.getId()) { ... }
```
But **avoid** `Long != Long` (object reference comparison) — always use `.equals()` for `Long` objects:
```java
// ❌ Broken for IDs > 127 (outside JVM Long cache)
if (!entity.getUserId().equals(user.getId())) { ... }  // ← always use .equals()
```

