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
- Nach jeder Änderung Byte-Check durchführen: `python3 -c "open(path,'rb').read()"` auf bytes > 127 prüfen
