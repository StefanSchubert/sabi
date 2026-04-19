# Agentic Guidelines for Sabi

## Communication Preferences
**Language & Tone**: German (Deutsch), informal "Du" form
- User preference: Direct, peer-to-peer communication
- Use "Du" (not "Sie") in all responses
- Keep responses concise and action-oriented
- Technical discussions on equal footing

## Session Management (MANDATORY)
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

---

## Terminal-Ausgaben (MANDATORY)

**Problem**: Das IntelliJ Copilot Plugin zeigt Terminal-Ausgaben oft NICHT direkt an – die
`run_in_terminal`-Ausgabe erscheint leer oder abgeschnitten, auch wenn der Befehl erfolgreich war.
Zusätzlich kann Shell-Escaping bei Sonderzeichen den Terminal-State brechen (zsh event-not-found,
dquote-Loop etc.).

**Grundregel: JEDER Terminal-Befehl dessen Ausgabe ausgewertet wird, muss via `| tee` in eine
Datei schreiben. Die Ausgabe wird ausschließlich über `read_file` eingelesen.**

```zsh
# Muster für alle Terminal-Befehle:
some_command | tee /tmp/ausgabe.out
# → danach: read_file /tmp/ausgabe.out
```

**Regel: Skripte NIEMALS als inline `-c "..."` ausführen – immer via Datei:**
1. Skript mit `create_file`-Tool nach `/tmp/skriptname.py` schreiben
2. Im Terminal ausführen: `python3 /tmp/skriptname.py | tee /tmp/skriptname.out`
3. Ausgabe einlesen: `read_file`-Tool auf `/tmp/skriptname.out`

**Gilt auch für Shell-Einzeiler mit Sonderzeichen** (Klammern, Ausrufezeichen, Backticks):
- Statt `command | grep "foo(bar)"` → Skript nach `/tmp/` schreiben
- Bei komplexen grep/sed/awk: immer `/tmp/`-Skript bevorzugen

**Gilt auch für einfache Diagnose-Befehle** (env-Checks, grep, find, mvn, etc.):
- `echo "VAR=$VAR" | tee /tmp/check.out` statt `echo "VAR=$VAR"` direkt lesen
- `find ... | tee /tmp/find.out` statt Ausgabe im Terminal auswerten

---

## Development Environment Setup

- MacBook CLI mit **BSD Unix Tools** (kein GNU) → abweichende Syntax!
  - `sed -i ''` statt `sed -i` (BSD sed braucht leeres Extension-Argument)
  - `grep` ohne `--line-buffered` etc. (BSD grep, nicht GNU grep)
  - `find` ohne `-printf` (BSD find, nicht GNU find)
  - Bei Zweifel: `gsed`, `ggrep`, `gfind` (Homebrew GNU-Tools) verwenden
- IntelliJ Copilot Plugin für Code-Generierung
- Docker für lokales Testen
- Deployment nach Prod via Ansible Playbook

### JSF/Facelets Hot-Reload (WICHTIG)

**XHTML-Änderungen erfordern IMMER einen Server-Neustart.** Es gibt keinen Hot-Reload für Facelets:

- JoinFaces/Spring Boot erkennt **keine** Classpath-Ressourcen-Timestamps zur Laufzeit
- Selbst mit `joinfaces.jsf.project-stage: development` werden kompilierte Facelets-Views **gecacht**
- Das Kopieren geänderter XHTML-Dateien nach `target/classes/` hat **KEINEN Effekt** — der Server
  muss vollständig durchgestartet werden
- **Symptom bei veralteter View**: Auto-generierte JSF-IDs (z.B. `j_idt30`) statt explizit vergebener
  IDs (`tankSelector`), oder AJAX-Updates die ins Leere gehen

**Regel: Nach jeder XHTML-Änderung → Server neu starten → erst dann testen.**

### Spring Security + JSF: Forward vs. Redirect

**NIEMALS `successForwardUrl()` in Kombination mit JSF verwenden.**

Ein Servlet-Forward nach erfolgreicher Authentifizierung trägt den `jakarta.faces.ViewState`
POST-Parameter der Login-Seite in die Zielseite. JSF versucht dann, den ViewState der Login-Seite
auf den Komponentenbaum der Zielseite anzuwenden → `ArrayIndexOutOfBoundsException` in
`UIComponentBase.restoreState`.

**Immer `defaultSuccessUrl(url, true)` verwenden** — das erzeugt einen HTTP 302 Redirect,
der Browser macht einen frischen GET-Request, und JSF erstellt eine neue View ohne State-Konflikt.

---

## i18n Message Bundle Handling (MANDATORY)

### Unterstützte Sprachen (MANDATORY)

**Sabi unterstützt exakt diese 5 Sprachen:**

| Code | Sprache    | Bundle-Datei               |
|------|------------|----------------------------|
| `de` | Deutsch    | `messages_de.properties`   |
| `en` | English    | `messages_en.properties`   |
| `es` | Español    | `messages_es.properties`   |
| `fr` | Français   | `messages_fr.properties`   |
| `it` | Italiano   | `messages_it.properties`   |

Zusätzlich existiert `messages.properties` als **Fallback-Bundle** (Englisch).

**Neue Features MÜSSEN alle 6 Dateien (5 Sprachen + Fallback) mit neuen i18n-Keys befüllen.**
Fehlende Übersetzungen sind ein Release-Blocker. Nach jeder Feature-Implementierung ist zu prüfen,
dass kein Key in einer der Sprachdateien fehlt.

**Encoding-Regel für `.properties`-Dateien:**
- Spring Boot ist mit `spring.messages.encoding: UTF-8` konfiguriert → rohe UTF-8-Bytes sind gültig
- `\uXXXX`-Escapes sind weiterhin erlaubt und bevorzugt für Konsistenz mit dem Bestand
- **NIEMALS** rohe Latin-1-Bytes (`0x80`–`0xFF`) in properties-Dateien schreiben
- **NIEMALS** U+FFFD Replacement Characters (`\xef\xbf\xbd`) einfügen

**Python-Hilfsskripte für File-Operationen:**
- Immer als echtes UTF-8 schreiben: `create_file`-Tool verwenden (nicht heredoc im Terminal)
- Bei File-Writes explizit `encoding='utf-8'` angeben: `open(path, 'w', encoding='utf-8')`
- Bei binären Byte-Operationen auf properties-Dateien: nur `\uXXXX`-Escape-Sequenzen als Ziel verwenden
- Hilfsskripte nach `/tmp/` schreiben und mit `python3 /tmp/skriptname.py` aufrufen
- **Kein heredoc** für Python-Code im Terminal – Shell-Escaping korrumpiert Sonderzeichen

**Beim Bearbeiten von Message Bundles via Tools:**
- `replace_string_in_file` schreibt in der Datei-Encoding des Tools → Sonderzeichen immer als `\uXXXX` angeben
- **ACHTUNG: `replace_string_in_file` versagt lautlos** wenn `oldString` rohe UTF-8-Zeichen (ö, ü, è …)
  enthält, die Datei aber `\uXXXX`-Escape-Sequenzen verwendet (oder umgekehrt). Das Tool meldet
  "Erfolg", schreibt aber nichts.
- **ACHTUNG: `insert_edit_into_file` versagt ebenfalls lautlos** bei `.properties`-Dateien — das Tool
  meldet "Erfolg" und zeigt sogar den Datei-Inhalt inklusive der neuen Keys an, aber die Keys werden
  oft NICHT tatsächlich auf die Disk geschrieben (beobachtet in 5 von 6 Dateien).
- **Zuverlässigste Methode: Python-Hilfsskript.** Für neue i18n-Keys ein Skript nach `/tmp/append_keys.py`
  schreiben (via `create_file`), das die Keys per `open(path, 'r+', encoding='utf-8')` anfügt, dann
  mit `python3 /tmp/append_keys.py | tee /tmp/append_keys.out` ausführen.
- **IMMER nach jeder Änderung Byte-Check durchführen**: Python-Skript nach `/tmp/bytecheck.py` schreiben,
  mit `python3 /tmp/bytecheck.py | tee /tmp/bytecheck.out` ausführen, dann `read_file` auf `/tmp/bytecheck.out`
  (KEIN `python3 -c "..."` inline – Shell-Escaping korrumpiert Sonderzeichen!)
- Dem Byte-Check **MEHR vertrauen als der Tool-Ausgabe** — nur der Byte-Check zeigt, was wirklich
  auf der Disk steht.

---

## Barrierefreiheit / Accessibility (MANDATORY)

**WCAG AA Kontrast-Mindestanforderungen:**
- Normaler Text (< 18px / < 14px bold): **mindestens 4.5:1** Kontrast gegen Hintergrund
- Großer Text (≥ 18px normal / ≥ 14px bold): **mindestens 3:1**
- UI-Komponenten (Buttons, Inputs, Icons): **mindestens 3:1**

**Verbotene Farb-Kombinationen:**
- `color: lightblue` auf weißem/hellem Hintergrund → Kontrast ~1.4:1 → **VERBOTEN**
- `color: yellow` auf weißem Hintergrund → **VERBOTEN**
- Generell: Keine Pastellfarben als Textfarbe auf hellem Hintergrund

**Empfohlene Farben (SABI Marine Theme):**
- Links auf weißem/hellem Hintergrund: `#0369a1` (--sabi-primary, ~5.7:1) oder `#075985` (~7:1)
- Links auf dunklem Hintergrund (Header/Footer): `#bae6fd` (--sabi-primary-light, ~5.2:1)
- Fehlertexte (rot): `#b91c1c` (~5.5:1 auf weiß) statt `red` (4.5:1)

**HTML in i18n-Properties:**
- Inline-`style="color:..."` in Message-Bundle-HTML: **immer WCAG-konforme Farbe** verwenden
- `<a>`-Tags in Properties **immer korrekt schließen**: `</a>` nicht `</>`
- Links erkennbar machen: `text-decoration: underline` oder andere visuelle Unterscheidung

---

## Playwright E2E-Tests: Qualitätsregeln (MANDATORY)

### Kein `force: true` und kein `page.evaluate(() => el.click())` als Standard-Click

**Problem**: `page.evaluate(() => el.click())` und `.click({ force: true })` umgehen CSS-Sichtbarkeit
und Layout-Prüfungen vollständig. Ein Test kann damit **grün** sein, obwohl das Element für echte
Nutzer **unsichtbar oder unklickbar** ist (z.B. `width: 0px`, `visibility: hidden`,
`overflow: hidden`, überdeckt durch ein anderes Element).

**Gelernte Lektion (April 2026)**: Ein PrimeFaces Toggle-Button hatte `width: 0px; height: 0px`
durch fehlende CSS-Regeln. Der Test wurde mit `page.evaluate(() => el.click())` grün geschrieben —
der echte Nutzer konnte das Element aber nie sehen oder klicken.

**Regeln:**
- **Standard**: Immer echten Playwright-Click ohne `force` verwenden: `await locator.click()`
- **Vor dem Click**: Sichtbarkeit explizit prüfen: `await expect(locator).toBeVisible()`
- **`force: true` / `page.evaluate click`**: NUR als letztes Mittel bei echten technischen
  Hindernissen (z.B. transienter Overlay), und dann mit erklärendem Kommentar **warum**
- **Nach einem Workaround**: Den eigentlichen CSS/Layout-Bug fixen statt den Workaround
  dauerhaft im Test zu belassen

### CSS-Sichtbarkeit von Elementen validieren

Wenn ein Element gefunden wird aber nicht klickbar scheint, immer die **computed styles** prüfen:

```javascript
// Diagnoseskript: computed styles eines Elements prüfen
const styles = await page.evaluate((sel) => {
  const el = document.querySelector(sel);
  if (!el) return { error: 'not found' };
  const cs = window.getComputedStyle(el);
  return { width: cs.width, height: cs.height, display: cs.display,
           visibility: cs.visibility, overflow: cs.overflow };
}, '.mein-selektor');
```

Elemente mit `width: 0px` oder `height: 0px` sind effektiv unsichtbar — das ist ein **Bug**,
kein Testproblem.

### Screenshot-basierte Verifikation bei UI-Bugs

Bei visuellen Problemen (Icon unsichtbar, Text unsichtbar) immer Screenshots machen und
**visuell bestätigen** bevor der Test als grün gilt:

```typescript
await page.screenshot({ path: '/tmp/debug_before.png' });
// ... Aktion ...
await page.screenshot({ path: '/tmp/debug_after.png' });
// Screenshots in IDE öffnen und visuell prüfen!
```

Ein grüner Test allein ist kein Beweis für korrekte Darstellung.

---

## Backend REST-API: Auth-Token-Pattern (MANDATORY)

### Wie das JWT-Token durch den Stack fließt

**Sabi verwendet einen eigenen HTTP-Header `Authorization` mit `Bearer`-Präfix** — kein Standard-OAuth2-Flow,
sondern ein selbst implementiertes JWT-Schema:

1. **Login** → Backend gibt `Authorization: Bearer <jwt>` im Response-Header zurück
2. **Webclient** liest diesen Header und speichert ihn in `UserSession.sabiBackendToken`
   - Token enthält bereits das `Bearer`-Präfix
3. **Alle Backend-Calls** setzen diesen Token über `RestHelper.prepareAuthedHttpHeader(token)`
   als `Authorization`-Header
4. **`JWTAuthorizationFilter`** prüft: `token.startsWith("Bearer ")` → setzt Authentication im SecurityContext
   - Wenn Token fehlt oder kein Bearer-Präfix → Filter lässt Request **ohne Authentication** durch
   - Spring Security's `anyRequest().authenticated()` liefert dann **403**

**Wichtig: Auth-Fehler manifestieren sich als 403, nicht als 401!**
Der Filter gibt bei fehlendem/ungültigem Token 401 zurück, aber wenn der Request die Security-Config
erreicht ohne gesetzten SecurityContext → 403 Forbidden.

### Multipart-Endpunkte: `MultipartFile` statt `byte[]`

**NIEMALS `@RequestParam("file") byte[] fileBytes` für Multipart-Uploads verwenden.**

Spring kann `MultipartFile` nicht automatisch in `byte[]` konvertieren — das führt zu
`MethodArgumentTypeMismatchException` (400), die sich aber manchmal als 403 am Client manifestiert.

**Richtig:**
```java
// Backend Controller
@PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Void> upload(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,  // ← MultipartFile, NICHT byte[]
        @RequestHeader(name = AUTH_TOKEN) String token,
        Principal principal) {
    byte[] bytes = file.getBytes();
    String ct = file.getContentType() != null ? file.getContentType() : "image/jpeg";
    // ...
}
```

**Webclient-seitig** (RestTemplate mit ByteArrayResource):
```java
ByteArrayResource resource = new ByteArrayResource(bytes) {
    @Override public String getFilename() { return "photo.jpg"; }
};
MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
parts.add("file", resource);
HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token, MediaType.MULTIPART_FORM_DATA);
restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(parts, headers), String.class);
```

### Docker-Backend neu deployen (Checkliste)

Nach Änderungen am Backend (`sabi-server`):
```bash
# 1. JAR bauen
cd sabi-server && mvn package -DskipTests

# 2. JAR in Docker-Assets kopieren
cd devops/sabi_docker_sdk && bash copyjars.sh

# 3. Container stoppen, Image neu bauen, starten (ARM Mac!)
docker compose -f docker-compose-arm.yml stop sabi-backend
docker compose -f docker-compose-arm.yml up --build -d sabi-backend

# 4. Auf Start warten (~20-30 Sekunden), dann testen
```

**Für AMD64-Server** `docker-compose.yml` verwenden; für ARM-Entwicklung (MacBook M1/M2/M3/M4)
immer `docker-compose-arm.yml` mit `Dockerfile_ARM`.
