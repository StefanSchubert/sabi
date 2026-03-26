<!--
SYNC IMPACT REPORT
==================
Version change:      (placeholder) → 1.0.0 (Erstratifizierung)
Modified principles: N/A — initiale Erstellung aus Template-Platzhaltern
Added sections:
  - Core Principles (8× ISO 25010 Qualitätsmerkmale)
  - ISO 25010 Standard Reference
  - Technology Stack & Development Constraints
  - Governance (inkl. Versioning Policy + Compliance Review)
Removed sections: N/A
Templates requiring updates:
  ✅ .specify/templates/spec-template.md    — Non-Functional Requirements (ISO 25010) ergänzt
  ✅ .specify/templates/tasks-template.md   — ISO 25010 Quality-Gates-Phase ergänzt
  ✅ .specify/templates/plan-template.md    — Constitution Check mit ISO 25010 Gates präzisiert
  ✅ .specify/templates/constitution-template.md — unverändert (memory/constitution.md ist die Instanz)
  ✅ .specify/templates/checklist-template.md   — keine Änderung erforderlich
  ✅ .specify/templates/agent-file-template.md  — keine Änderung erforderlich
Follow-up TODOs: keine offenen Platzhalter verblieben
-->

# SABI Project Constitution

## Core Principles

### I. Funktionale Eignung (Functional Suitability) — ISO 25010

Software im SABI-Projekt MUSS alle explizit definierten Nutzungsszenarien und
Benutzeranforderungen korrekt, vollständig und zweckmäßig erfüllen.

- Jede neue Funktion MUSS durch ein Issue/User-Story im Issue-Tracker beschrieben sein.
- Akzeptanzkriterien MÜSSEN vor der Implementierung definiert und nach der Implementierung
  überprüft werden.
- Unspezifiziertes Verhalten (Edge Cases) MUSS dokumentiert oder durch Tests abgedeckt sein.
- Pull Requests DÜRFEN NICHT gemergt werden, bevor alle offenen Akzeptanzkriterien erfüllt sind.

**Rationale:** Als Open-Science-Plattform trägt SABI Verantwortung gegenüber der Community.
Funktionen, die nicht den beschriebenen Zweck erfüllen, untergraben das Vertrauen der Nutzer
und verfälschen wissenschaftliche Datenauswertungen.

---

### II. Leistungseffizienz (Performance Efficiency) — ISO 25010

Software im SABI-Projekt MUSS ressourcenschonend und innerhalb akzeptabler Antwortzeiten
auf der Zielplattform (Raspberry Pi, ARM) betrieben werden können.

- REST-Endpunkte MÜSSEN unter normaler Last innerhalb von 2 Sekunden antworten.
- Datenbankabfragen MÜSSEN indiziert und auf N+1-Probleme geprüft sein.
- Neue Features MÜSSEN vor dem Go-live auf Ressourcenverbrauch (CPU, RAM, I/O) bewertet werden.
- Bulk-Operationen (z. B. Messdaten-Importe) MÜSSEN asynchron oder paginiert implementiert werden.

**Rationale:** Der Betrieb auf Raspberry Pis mit niedrigem Energieprofil ist ein erklärtes
Projektziel ("Climate-Friendly"). Ineffizienter Code gefährdet die Betriebsstabilität der
gesamten Plattform.

---

### III. Kompatibilität (Compatibility) — ISO 25010

Software im SABI-Projekt MUSS mit anderen Systemen koexistieren und interoperabel sein,
ohne deren Betrieb zu stören.

- Die öffentliche REST-API MUSS gemäß OpenAPI 3.x dokumentiert sein (via springdoc-v2).
- API-Änderungen MÜSSEN rückwärtskompatibel sein; Breaking Changes erfordern eine neue
  API-Versionsnummer.
- Externe Integrationen (z. B. IoT-Geräte, aquarium-IoT) MÜSSEN durch definierte
  Schnittstellen entkoppelt sein.
- Docker-Images MÜSSEN für AMD64 und ARM-Plattformen bereitgestellt werden.

**Rationale:** SABI ist eine offene Plattform. Drittanbieter und Community-Entwickler bauen
auf der API auf. Inkompatible Änderungen verursachen Mehraufwand und verringern die Akzeptanz.

---

### IV. Benutzbarkeit (Usability) — ISO 25010

Software im SABI-Projekt MUSS für die Zielgruppe (Aquaristik-Hobbyisten ohne technische
Vorkenntnisse) leicht erlernbar, effizient nutzbar und fehlertolerant sein.

- UI-Texte und Fehlermeldungen MÜSSEN internationalisiert (i18n) und in mindestens Deutsch
  und Englisch verfügbar sein.
- Formulare MÜSSEN client- und serverseitige Validierung mit verständlichen Fehlermeldungen
  enthalten.
- Neue UI-Komponenten MÜSSEN auf Barrierefreiheit (WCAG 2.1 Level AA) geprüft werden.
- Nutzer-Workflows SOLLTEN in maximal 3 Interaktionsschritten zum Ziel führen (wo möglich).

**Rationale:** Die Plattform richtet sich an Hobbyisten, nicht an IT-Fachleute. Komplexe
Bedienung verhindert die Datenerhebung und unterläuft das wissenschaftliche Ziel des Projekts.

---

### V. Zuverlässigkeit (Reliability) — ISO 25010

Software im SABI-Projekt MUSS unter den gegebenen Betriebsbedingungen stabil laufen und
im Fehlerfall vorhersehbar reagieren.

- Kritische Fehler MÜSSEN geloggt und monitoriert werden (Prometheus/Grafana-Integration).
- Datenverlust bei unerwarteten Fehlern MUSS durch Transaktionssicherheit (JPA/MariaDB)
  verhindert werden.
- Unit-Tests MÜSSEN vor jedem Merge in den Main-Branch grün sein (CI/CD via GitHub Actions).
- Geplante Ausfallzeiten (Deployments, Wartung) SOLLEN vorab kommuniziert werden.

**Rationale:** SABI akzeptiert bewusst keine 99%-SLA (klimafreundlicher Betrieb), aber
innerhalb der Betriebszeiten MUSS die Software zuverlässig funktionieren und Datenverlust
ausschließen.

---

### VI. Sicherheit (Security) — ISO 25010

Software im SABI-Projekt MUSS Nutzerdaten und Systemressourcen vor unbefugtem Zugriff,
Missbrauch und Datenverlust schützen.

- Abhängigkeiten MÜSSEN regelmäßig mit OWASP Dependency Check geprüft werden
  (NVD-API-Key erforderlich, s. DEVELOPERS_MANUAL.md).
- Sicherheitslücken hohen Risikos DÜRFEN NICHT öffentlich als Issue gemeldet werden;
  es gilt die Responsible-Disclosure-Policy (SECURITY.md).
- Authentifizierung und Autorisierung MÜSSEN für alle schreibenden API-Endpunkte
  erzwungen sein.
- Passwörter und Secrets DÜRFEN NIEMALS in den Source Code oder in die VCS-History gelangen.
- CodeQL MUSS in der CI/CD-Pipeline aktiv und fehlerfrei sein.

**Rationale:** SABI speichert Benutzerdaten (E-Mail, Messdaten, Tank-Informationen). Der
Schutz dieser Daten ist eine rechtliche und ethische Verpflichtung.

---

### VII. Wartbarkeit (Maintainability) — ISO 25010

Software im SABI-Projekt MUSS so strukturiert sein, dass Änderungen, Erweiterungen und
Fehleranalysen effizient durchgeführt werden können.

- Der Code MUSS der modularen Architektur folgen:
  `sabi-boundary` | `sabi-server` | `sabi-webclient` | `sabi-database`.
- Schema-Änderungen MÜSSEN über Flyway-Migrationsskripte verwaltet werden;
  bestehende Migrationsskripte sind unveränderlich.
- Neue Funktionalitäten MÜSSEN mit JUnit-Tests abgedeckt sein.
- Refactorings großen Umfangs MÜSSEN vorab kommuniziert und abgestimmt werden
  (CONTRIBUTING.md).
- Signifikante Designentscheidungen MÜSSEN als ADRs im Wiki (arc42) dokumentiert werden.

**Rationale:** Als langlebiges Open-Science-Projekt (Planung > 10 Jahre) ist Wartbarkeit
überlebenswichtig. Technische Schulden kumulieren schnell in Einzel-Maintainer-Projekten.

---

### VIII. Übertragbarkeit (Portability) — ISO 25010

Software im SABI-Projekt MUSS auf unterschiedlichen Zielplattformen (ARM/AMD64,
verschiedene Betriebssystemumgebungen) deploybar sein.

- Deployments MÜSSEN über Ansible-Playbooks reproduzierbar sein (`devops/ansible/`).
- Lokale Entwicklungsumgebungen MÜSSEN über Docker Compose ohne manuelle Schritte
  aufgesetzt werden können (`devops/sabi_docker_sdk/`).
- Plattformspezifische Abhängigkeiten MÜSSEN isoliert und konfigurierbar sein
  (AMD64: `docker-compose.yml`, ARM: `docker-compose-arm.yml`).
- Das Projekt MUSS mit dem jeweils definierten Java-LTS-Release kompatibel sein
  (derzeit Java 25).

**Rationale:** SABI läuft produktiv auf Raspberry Pis (ARM), wird lokal auf x86/ARM-Mac
entwickelt. Portabilität sichert die Entwickler-Produktivität und die Flexibilität beim
späteren Infrastructure-Wechsel.

---

## ISO 25010 Standard Reference

Dieser Abschnitt dokumentiert die verbindliche Verankerung von **ISO/IEC 25010**
(Systems and Software Quality Requirements and Evaluation – SQuaRE) als Entwicklungsstandard
für Qualitätsmerkmale im SABI-Projekt.

**Normative Grundlage**: ISO/IEC 25010:2011 und ISO/IEC 25010:2023

Die acht Qualitätsmerkmale des Standards bilden die acht Kernprinzipien dieser Konstitution
(Abschnitt „Core Principles"). Sie sind in ihrer Gesamtheit gleichrangig; keine
Qualitätsdimension darf dauerhaft zugunsten einer anderen vernachlässigt werden.

**Mapping Prinzipien → ISO 25010:**

| Prinzip | ISO 25010 Merkmal (DE)  | ISO 25010 Characteristic (EN) |
|---------|-------------------------|-------------------------------|
| I       | Funktionale Eignung     | Functional Suitability        |
| II      | Leistungseffizienz      | Performance Efficiency        |
| III     | Kompatibilität          | Compatibility                 |
| IV      | Benutzbarkeit           | Usability                     |
| V       | Zuverlässigkeit         | Reliability                   |
| VI      | Sicherheit              | Security                      |
| VII     | Wartbarkeit             | Maintainability               |
| VIII    | Übertragbarkeit         | Portability                   |

---

## Technology Stack & Development Constraints

Der folgende Stack ist verbindlich für das SABI-Projekt:

| Schicht           | Technologie                     | Constraint                              |
|-------------------|---------------------------------|-----------------------------------------|
| Backend           | Spring-Boot 4 + Java 25         | MUSS LTS-Java-Version verwenden         |
| Frontend          | JSF 2.3 + Primefaces 15.x       | MUSS i18n-fähig sein                    |
| Persistenz        | MariaDB 10.x + JPA (EclipseLink)| Schema-Evolution ausschließlich via Flyway |
| API-Dokumentation | OpenAPI 3.x (springdoc-v2)      | MUSS aktuell und vollständig sein       |
| CI/CD             | GitHub Actions (Maven + CodeQL) | MUSS bei jedem Push grün sein           |
| Monitoring        | Prometheus + Grafana            | MUSS in Produktion aktiv sein           |
| Deployment        | Ansible + Docker Compose        | MUSS reproduzierbar und skriptgesteuert sein |
| Security-Scan     | OWASP Dependency Check          | MUSS bei jedem Release ausgeführt werden |

**Zielplattformen**: Raspberry Pi (ARM, Produktion), x86/AMD64 (Entwicklung/Docker)

---

## Governance

Diese Konstitution ist das oberste Regelwerk des SABI-Projekts und hat Vorrang vor allen
anderen Praktiken und Konventionen.

**Änderungsverfahren:**

1. Änderungsvorschläge werden als GitHub Issue mit Label `constitution` eingereicht.
2. Der Maintainer (Stefan Schubert) hat 14 Tage Zeit zur Prüfung und Entscheidung.
3. Angenommene Änderungen werden in dieser Konstitution versioniert und per Commit
   mit Commit-Message `docs: amend constitution to vX.Y.Z (...)` dokumentiert.
4. Alle Pull Requests SOLLTEN auf Konstitutions-Konformität geprüft werden.
5. Verstöße gegen MUSS-Regeln DÜRFEN NICHT in den Main-Branch gemergt werden.

**Versioning Policy:**

| Änderungstyp                                      | Version Bump  | Beispiel                          |
|---------------------------------------------------|---------------|-----------------------------------|
| Prinzip entfernt oder grundlegend redefiniert     | MAJOR (X.0.0) | ISO 25010 Merkmal gestrichen      |
| Neues Prinzip oder wesentliche Erweiterung        | MINOR (x.Y.0) | Neues Qualitätsmerkmal ergänzt    |
| Formulierung, Typos, Klarstellungen               | PATCH (x.y.Z) | Regelformulierung präzisiert      |

**Compliance Review:**

- Die Konstitution MUSS bei jedem Major-Release des Projekts auf Aktualität geprüft werden.
- Bei Upgrades des Technology Stacks MUSS die Konstitution auf Konsistenz geprüft werden.
- Komplexität in Pull Requests MUSS durch Verweis auf ein Konstitutionsprinzip gerechtfertigt
  sein, wenn ein Reviewer Bedenken äußert.

**Version**: 1.0.0 | **Ratified**: 2026-03-26 | **Last Amended**: 2026-03-26
