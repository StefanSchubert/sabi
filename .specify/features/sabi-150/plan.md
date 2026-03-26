# Implementation Plan: OpenID Connect Login via Google (sabi-150)

**Branch**: `feature/sabi-150` | **Date**: 2026-03-26 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `.specify/features/sabi-150/spec.md`

---

## Summary

„Login with Google" für neue und bestehende Nutzer. Spring Security OAuth2 Client im `sabi-webclient` übernimmt den vollständigen OIDC Authorization-Code-Flow (redirect → callback → token exchange). Nach erfolgreicher Google-Authentifizierung ruft der Webclient ein neues Backend-Endpoint im `sabi-server` auf, der den Google-ID-Token unabhängig validiert, das Konto provisioniert oder verknüpft und einen Sabi-JWT ausstellt — identisch zum bestehenden Passwort-Login.

---

## Technical Context

**Language/Version**: Java 25, Spring Boot 4.0.3  
**Primary Dependencies**:
- `spring-boot-starter-oauth2-client` (bereits in `sabi-server/pom.xml` → wird nach `sabi-webclient` verschoben/ergänzt)
- `spring-boot-starter-security` (vorhanden in beiden Modulen)
- `com.auth0:java-jwt:4.5.1` (vorhandene JWT-Bibliothek in sabi-server)
- `spring-boot-starter-oauth2-resource-server` — **nicht** benötigt; Google-ID-Token-Validierung erfolgt über Spring Security OAuth2 Client-interne JWKS-Prüfung

**Storage**: MariaDB 10.x + EclipseLink JPA; Schema-Evolution via Flyway (aktuell: V1_3_0_4)  
**New Flyway Migrations**: V1_4_0_1 (neue Tabelle `oidc_provider_link`) + V1_4_0_2 (Spalte `oidc_managed` in `users`)  
**Testing**: JUnit 5 + Testcontainers (MariaDB) + Mockito — bestehende Testinfrastruktur  
**Target Platform**: ARM (Raspberry Pi, Produktion) + AMD64 (Entwicklung/Docker)  
**Project Type**: Web Service (REST Backend) + JSF/PrimeFaces Webclient  
**Performance Goals**: OIDC-Callback ≤ 2 s (Konstitution § II); kein messbarer Mehraufwand gegenüber Passwort-Login  
**Constraints**: Secrets nicht im Repository (C-2); HTTPS Pflicht (C-1); state-Parameter CSRF-Schutz (C-3); nonce Token-Replay-Schutz (C-6)  
**Scale/Scope**: Einzelner Google-Provider (sabi-150); erweiterbar auf Apple/Microsoft ohne Modell-Änderungen (SC-8)

### OQ-2 Resolution — Callback im Webclient (sabi-webclient)

> **Entscheidung**: Der OIDC-Callback wird von `sabi-webclient` verarbeitet.
>
> **Begründung**:
> - Der Browser interagiert mit dem Webclient; Redirects landen natürlich dort.
> - Der Webclient verwaltet bereits die Session (SabiDoorKeeper-Pattern).
> - Spring Security OAuth2 Client im Webclient übernimmt `state`-/`nonce`-Management automatisch.
> - Der Webclient leitet den Google-ID-Token (raw JWT-String) nach erfolgreicher OIDC-Validierung an den neuen Backend-Endpoint `POST /api/auth/oidc/google` weiter.
> - Das Backend validiert den Google-ID-Token **unabhängig** über die Google-JWKS-Endpoint-Prüfung — kein blindes Vertrauen in Webclient-Claims.
> - `spring-boot-starter-oauth2-client` muss in `sabi-webclient/pom.xml` ergänzt werden (ist bereits in `sabi-server/pom.xml`; dort bleibt es als Transitivabhängigkeit erhalten).

### OQ-3 Resolution — Gesperrte Konten im OIDC-Flow

> **Entscheidung**: Der OIDC-Flow **respektiert** Account-Locks.
>
> **Begründung**: OIDC ist kein Bypass-Mechanismus für Account-Management. Ist ein Konto gesperrt, gibt der Backend-Endpoint HTTP 423 (Locked) zurück. Der Webclient zeigt eine entsprechende Fehlermeldung. Der Nutzer verbleibt auf der Login-Seite. Identisches Verhalten wie beim Passwort-Login mit gesperrtem Konto.

---

## Constitution Check

*GATE: Passed — Grundlage für Phase 0. Re-check nach Phase 1 Design abgeschlossen (siehe unten).*

| # | ISO 25010 Prinzip | Gate-Frage | Status |
|---|-------------------|------------|--------|
| I | Funktionale Eignung | GitHub Issue #150 + Akzeptanzkriterien (US-1–US-4) vorhanden? | ✅ |
| II | Leistungseffizienz | ARM-Impact bewertet: OIDC-Callback ist I/O-bound (Google-Netzwerk), keine CPU-Last auf Pi; JWT-Ausstellung minimal | ✅ |
| III | Kompatibilität | Bestehende REST-API unverändert; neues Endpoint `/api/auth/oidc/google` additiv; OpenAPI-Docs per springdoc-v2 ergänzen | ✅ |
| IV | Benutzbarkeit | „Login with Google"-Button i18n-fähig (DE/EN); WCAG 2.1 AA: Button mit aria-label; JSF PrimeFaces p:commandButton | ✅ |
| V | Zuverlässigkeit | Google-Ausfall: Passwort-Login bleibt funktionsfähig (Scenario 6 / graceful degradation); Transaktionssicherheit für Account-Provisioning | ✅ |
| VI | Sicherheit | ID-Token-Validierung (iss, aud, sig, nonce); state-CSRF-Schutz; Client-Secret nicht im Repo; Audit-Logging (FR-15) | ✅ |
| VII | Wartbarkeit | Neues Modul-Konzept: `OidcProviderLink` isoliert; Flyway für Schema; JUnit-Tests geplant; ADR für OQ-2-Entscheidung | ✅ |
| VIII | Übertragbarkeit | ARM + AMD64 via Docker/Ansible; keine plattformspezifischen OIDC-Abhängigkeiten | ✅ |

**Violations requiring justification**: keine

---

## Project Structure

### Documentation (this feature)

```text
.specify/features/sabi-150/
├── plan.md              ← dieses Dokument
├── research.md          ← Phase 0 Output
├── data-model.md        ← Phase 1 Output
├── quickstart.md        ← Phase 1 Output
├── contracts/
│   └── oidc-api.md      ← Phase 1 Output
└── tasks.md             ← Phase 2 Output (/speckit.tasks — noch nicht erstellt)
```

### Source Code (Repository Root)

```text
sabi-boundary/src/main/java/de/bluewhale/sabi/
└── api/
    └── Endpoint.java                          MOD  ← OIDC_GOOGLE_CALLBACK Eintrag ergänzen

sabi-database/src/main/resources/db/migration/
└── version1_4_0/
    ├── V1_4_0_1__addOidcProviderLinkTable.sql  NEW
    └── V1_4_0_2__addOidcManagedFlagToUsers.sql  NEW

sabi-server/src/main/java/de/bluewhale/sabi/
├── configs/
│   └── WebSecurityConfig.java                 MOD  ← OIDC-Endpoint permitAll
├── persistence/model/
│   ├── UserEntity.java                        MOD  ← oidcManaged-Flag ergänzen
│   └── OidcProviderLinkEntity.java            NEW
├── persistence/repositories/
│   └── OidcProviderLinkRepository.java        NEW
├── services/
│   ├── UserService.java (Interface)           MOD  ← provisionOidcUser() + linkOidcIdentity()
│   └── UserServiceImpl.java                  MOD  ← Implementierung
└── rest/controller/
    └── OidcAuthController.java               NEW  ← POST /api/auth/oidc/google

sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/
├── config/
│   └── WebSecurityConfig.java                MOD  ← OAuth2 Client + OIDC-Seite permitAll
├── security/
│   └── SabiOidcSuccessHandler.java           NEW  ← after OAuth2 success → call backend → store JWT
└── controller/
    └── (optional) OidcErrorView.java         NEW  ← Fehlerbehandlung bei OIDC-Abbruch

sabi-webclient/src/main/resources/
├── META-INF/resources/
│   └── login.xhtml                           MOD  ← "Login with Google"-Button
├── i18n/
│   ├── messages.properties                   MOD  ← login.oidc.google.button key
│   └── messages_de.properties               MOD  ← Übersetzung
└── application.properties (oder yaml)        MOD  ← spring.security.oauth2.client.*

sabi-server/src/main/resources/
└── application.properties                   MOD  ← google.oidc.clientId Referenz (Wert via Secret)
```

**Structure Decision**: Option 2 (Web Application). Backend = `sabi-server`, Frontend = `sabi-webclient`.
Der OIDC-Callback liegt im Webclient (OAuth2-Client). Das Backend hält die gesamte Provisioning-Logik.
Siehe OQ-2-Resolution oben.

---

## Complexity Tracking

> Keine Constitution-Verstöße. Keine ungewöhnlichen Komplexitätstreiber.
>
> Hinweis: Der neue `OidcAuthController` im Backend greift intern auf `UserService` zu — bestehender Muster aus `AuthenticationController`. Kein neues Architektur-Pattern nötig.

---

*Nächster Schritt: `/speckit.tasks` um sabi-150/tasks.md zu erzeugen.*

