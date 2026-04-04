# CI/CD Pipeline – Dokumentation

## Überblick

Die Pipeline besteht aus zwei Jobs:

| Job | Zweck |
|-----|-------|
| `build` | Kompiliert alle Module + OWASP Dependency Check für alle Module |
| `test` | Führt Unit-Tests für CAPTCHA, sabi-boundary und sabi-server aus |

---

## Benötigte GitHub Secrets

Folgende Secrets müssen im Repository unter **Settings → Secrets and variables → Actions** hinterlegt sein:

| Secret | Beschreibung | Registrierung |
|--------|-------------|---------------|
| `NVD_API_KEY` | API-Key für die NVD (National Vulnerability Database) | https://nvd.nist.gov/developers/request-an-api-key |

> **OSS Index deaktiviert (Stand: April 2026)**
> Sonatype OSS Index migriert am **28.04.2026** zu Sonatype Guide. Das OWASP
> `dependency-check-maven` Plugin unterstützt den neuen Endpunkt noch nicht. Der
> `ossIndexAnalyzerEnabled`-Parameter ist daher in allen `pom.xml` auf `false` gesetzt.
> Die NVD (National Vulnerability Database) ist die primäre CVE-Datenquelle und ausreichend.
> Nach der vollständigen Migration kann OSS Index / Sonatype Guide wieder aktiviert werden –
> dann wird ein weiterer Secret (`SONATYPE_GUIDE_TOKEN`) benötigt.

---

## Lokale Entwicklungsumgebung einrichten

Damit lokal und in der Pipeline **identisch** gearbeitet wird, werden dieselben Credentials
als Shell-Umgebungsvariablen gesetzt.

### 1. Umgebungsvariablen in `~/.zshrc` eintragen

```zsh
# ── OWASP Dependency Check ──────────────────────────────────────────
# NVD API Key – pom.xml liest via ${env.NVD_API_KEY}, kein -D nötig
export NVD_API_KEY="<dein-nvd-api-key>"

# Variablen auch für GUI-Apps (IntelliJ via Dock/Launchpad) bereitstellen.
# launchctl setenv wird bei JEDER neuen Terminal-Session ausgeführt.
launchctl setenv NVD_API_KEY "$NVD_API_KEY"
# ────────────────────────────────────────────────────────────────────
```

Danach Terminal neu starten oder `source ~/.zshrc` ausführen und prüfen:
```zsh
launchctl getenv NVD_API_KEY         # muss API-Key ausgeben
```

### 2. IntelliJ (neu) starten

Die `.zshrc` setzt die Variablen über `launchctl setenv`, damit auch GUI-Apps (Dock/Launchpad)
sie sehen. Da die `pom.xml` den NVD-Key direkt via `${env.NVD_API_KEY}` aus der
Prozess-Umgebung liest, wird kein `MAVEN_OPTS` benötigt.

> ⚠️ **Wichtig:** Ein bereits laufender IntelliJ-Prozess bekommt **keine** neuen `launchctl`-Vars
> injiziert. IntelliJ muss **nach** dem ersten Ausführen von `~/.zshrc` (oder nach einer
> Systemanmeldung) einmal **komplett neu gestartet** werden.

Prüfen, ob `launchctl` die Var korrekt hält:
```zsh
launchctl getenv NVD_API_KEY         # muss API-Key ausgeben
```

Ist der Wert leer → erst `source ~/.zshrc` ausführen, dann IntelliJ starten.

---

## OWASP Dependency Check lokal ausführen

Der Check ist hinter dem Maven-Profil `owasp-check` geschützt und läuft **nicht** bei normalen
Build-/Test-Aufrufen. Da die `pom.xml` den NVD-Key direkt über `${env.NVD_API_KEY}` aus der
Umgebung liest, reicht im Terminal:

```zsh
# Beispiel sabi-boundary
cd sabi-boundary
mvn verify -P owasp-check

# Alle Module nacheinander (aus dem Repo-Root)
for module in captcha sabi-boundary sabi-server sabi-webclient sabi-database; do
  echo "=== OWASP Check: $module ==="
  (cd $module && mvn verify -P owasp-check)
done
```

> **Voraussetzung:** Die Umgebungsvariable `NVD_API_KEY` muss im aktuellen Terminal gesetzt
> sein (wird via `~/.zshrc` exportiert).
> Prüfen: `launchctl getenv NVD_API_KEY` – muss API-Key ausgeben.

---

## Funktionsweise der Credential-Auflösung

```
┌─────────────────────────────────────────────────────────────┐
│                    NVD_API_KEY                              │
├──────────────────────┬──────────────────────────────────────┤
│ Lokal                │ GitHub Actions                       │
│ ~/.zshrc             │ secrets.NVD_API_KEY                  │
│ export NVD_API_KEY=… │ → env: NVD_API_KEY                   │
│ launchctl setenv …   │                                      │
└──────────┬───────────┴──────────────┬───────────────────────┘
           │                          │
           ▼                          ▼
   Prozess-Umgebung (env)
           │
           ▼
   pom.xml <properties>:  <nvd.api.key>${env.NVD_API_KEY}</nvd.api.key>
           │
           ▼
   Plugin-Config:  <nvdApiKey>${nvd.api.key}</nvdApiKey>
           │
           ▼
   dependency-check-maven → NVD API ✅
```

---

## Warum der OWASP Check ein eigenes Profil ist

Der OWASP Dependency Check lädt beim ersten Aufruf die komplette NVD-Datenbank herunter
(mehrere Minuten). Damit er nicht bei jedem normalen `mvn compile` / `mvn test` läuft, ist
er in allen Modulen hinter dem Profil `owasp-check` geschützt:

```
Normaler Build:   mvn compile              → kein OWASP Check
OWASP explizit:   mvn verify -P owasp-check → OWASP Check läuft
```

In der Pipeline wird der Check einmalig pro Modul im `build`-Job ausgeführt.

---

## OSS Index / Sonatype Guide – Roadmap

| Datum | Status |
|-------|--------|
| bis 28.04.2026 | OSS Index unter `ossindex.sonatype.org` (alt) |
| ab 28.04.2026 | Migration zu **Sonatype Guide** abgeschlossen |
| offen | OWASP Plugin mit Sonatype Guide Unterstützung |

Sobald das `dependency-check-maven` Plugin Sonatype Guide unterstützt:
1. `<ossIndexAnalyzerEnabled>false</ossIndexAnalyzerEnabled>` aus allen `pom.xml` entfernen
2. Sonatype Guide Token als `SONATYPE_GUIDE_TOKEN` in GitHub Secrets hinterlegen
3. Workflow und `pom.xml` entsprechend aktualisieren
