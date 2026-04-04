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
- Every 50k tokens: Re-read copilot-instructions.md explicitly
- At 80-100k tokens: Recommend session restart to user
- NEVER exceed 120k tokens without explicit user approval

**How to refresh context at 50k intervals**:
1. Pause current work
2. Say: "🔄 Context Refresh (50k tokens reached) - Re-reading copilot-instructions.md"
3. Use read_file tool to read .github/copilot-instructions.md (line 0-500)
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

---

## i18n Message Bundle Handling (MANDATORY)

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
- Nach jeder Änderung Byte-Check durchführen: Python-Skript nach `/tmp/bytecheck.py` schreiben,
  mit `python3 /tmp/bytecheck.py | tee /tmp/bytecheck.out` ausführen, dann `read_file` auf `/tmp/bytecheck.out`
  (KEIN `python3 -c "..."` inline – Shell-Escaping korrumpiert Sonderzeichen!)

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

