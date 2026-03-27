# Tasks: OpenID Connect Login via Google (sabi-150)

**Feature ID**: sabi-150 | **Branch**: `feature/sabi-150` | **GitHub Issue**: [#150](https://github.com/StefanSchubert/sabi/issues/150)  
**Generated**: 2026-03-26 | **Based on**: spec.md, plan.md, research.md, data-model.md, contracts/oidc-api.md, quickstart.md  
**Stack**: Spring Boot 4.0.3, Java 25, Spring Security, JSF/PrimeFaces, MariaDB, EclipseLink JPA, Flyway

---

## Implementation Strategy

**MVP = Phase 3 complete** (Backend-Endpunkt voll funktionsfähig + US-1 provisioniert neue Nutzer).
Danach Phase 4 (US-2 Email-Match), dann Phase 5–6 (Webclient-Frontend + US-4 Google-App-Registrierung).
Jede Phase ist unabhängig testbar.

**Dependency order** (darf NICHT übersprungen werden):
```
Phase 1 (Setup)
  └─► Phase 2 (Flyway + Boundary-DTOs)
        └─► Phase 3 (JPA Entities + OidcAuthController → US-1/US-3)
              ├─► Phase 4 (Email-Match-Linking → US-2)
              └─► Phase 6 (Webclient-Frontend → US-1/2/3)
Phase 5 (Google App Registration → US-4) — parallel möglich, aber required für E2E-Test
Phase 7 (Polish & Quality Gates) — erst wenn alle vorherigen Phasen grün sind
```

---

## Phase 1 — Setup

**Goal**: Projektvorbereitung; alle Blocking-Prerequisites die vor Code-Änderungen stehen.  
**Independent test**: `mvn package -pl sabi-webclient` baut ohne Fehler (neue Dependency vorhanden).

- [ ] T001 [P] Manueller Schritt: Google Cloud Console OAuth2-App-Registrierung anlegen (Consent Screen, Client-ID, Redirect-URIs für Dev + Prod) gemäß quickstart.md §1 — Ergebnis: Client-ID + Client-Secret für nachfolgende Konfigurationsschritte bereit; **muss vor E2E-Test abgeschlossen sein**
- [x] T002 [P] `spring-boot-starter-oauth2-client` Dependency zu `sabi-webclient/pom.xml` hinzufügen (analog der bereits vorhandenen Dependency in `sabi-server/pom.xml`); Scope: compile; Version: via Spring Boot BOM

---

## Phase 2 — Foundational

**Goal**: Datenbank-Schema und Boundary-DTOs, auf die alle nachfolgenden Phasen aufbauen.  
**Independent test**: Flyway-Migration läuft fehlerfrei durch (`mvn flyway:migrate` auf lokal laufendem Docker-MariaDB). DTOs kompilieren (`mvn package -pl sabi-boundary`).

- [x] T003 Flyway-Verzeichnis + Migration anlegen: `sabi-database/src/main/resources/db/migration/version1_4_0/V1_4_0_1__addOidcProviderLinkTable.sql` — DDL: neue Tabelle `oidc_provider_link` mit Feldern `id`, `user_id` (FK → `users.id` ON DELETE CASCADE), `provider` VARCHAR(20), `provider_subject` VARCHAR(255), `linked_at`, `created_on`, `lastmod_on`, `optlock`; Unique-Constraints auf `(provider, provider_subject)` und `(user_id, provider)` gemäß data-model.md §2
- [x] T004 Migration anlegen: `sabi-database/src/main/resources/db/migration/version1_4_0/V1_4_0_2__addOidcManagedFlagToUsers.sql` — DDL: `ALTER TABLE users ADD COLUMN oidc_managed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 if account was auto-provisioned via OIDC'` gemäß data-model.md §3; muss nach T003 ausgeführt werden (Versionsnummer sichert Reihenfolge)
- [x] T005 [P] Enum-Eintrag `OIDC_GOOGLE_AUTH("/api/auth/oidc/google")` zu `sabi-boundary/src/main/java/de/bluewhale/sabi/api/Endpoint.java` hinzufügen (analog bestehender Einträge wie `LOGIN`, `REGISTER`)
- [x] T006 [P] DTO `OidcLoginRequestTo.java` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/OidcLoginRequestTo.java` anlegen — Record oder Lombok-`@Data`-Klasse mit Feldern: `String idToken` (NotBlank), `String provider` (NotBlank, z. B. `"GOOGLE"`) gemäß contracts/oidc-api.md Request-Spec
- [x] T007 [P] DTO `OidcLoginResponseTo.java` in `sabi-boundary/src/main/java/de/bluewhale/sabi/model/OidcLoginResponseTo.java` anlegen — Felder: `String token`, `String email`, `String username`, `boolean provisioned` gemäß contracts/oidc-api.md Response-Spec (200 OK)

---

## Phase 3 — US-1 + US-3: Neuer Nutzer (Auto-Provisioning) & Rückkehrender OIDC-Nutzer [Backend]

**User Stories**: US-1 (Neuer Nutzer, erste OIDC-Anmeldung), US-3 (Rückkehrender OIDC-Nutzer via gespeichertem `sub`)  
**Goal**: `POST /api/auth/oidc/google` nimmt einen Google-ID-Token entgegen, validiert ihn, provisioniert neue Konten oder authentifiziert bekannte OIDC-Nutzer (via `sub`-Lookup) und gibt einen Sabi-JWT zurück.  
**Independent test**:
- `OidcProviderLinkRepositoryTest`: findByProviderAndProviderSubject findet angelegten Link
- `OidcUserServiceTest`: provisionOidcUser() erstellt UserEntity + OidcProviderLink; findBySub() gibt vorhandene Entity zurück
- `OidcAuthControllerTest`: `POST /api/auth/oidc/google` mit Mock-ID-Token → 200 mit Sabi-JWT (neue Provisioning-Pfad + Returning-User-Pfad)

- [x] T008 [P] [US1] JPA-Entity anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/OidcProviderLinkEntity.java` — `@Entity`, `@Table(name = "oidc_provider_link", schema = "sabi")`, Felder: `Long id` (PK, autoincrement), `UserEntity user` (`@ManyToOne`, FK `user_id`), `String provider`, `String providerSubject`, `LocalDateTime linkedAt`; Standard-Auditable-Felder (`createdOn`, `lastmodOn`, `optlock`) analog bestehender Entities (z. B. `AquariumEntity`)
- [x] T009 [P] [US1] Feld `oidcManaged` zu `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/UserEntity.java` hinzufügen — `@Basic @Column(name = "oidc_managed", nullable = false) private boolean oidcManaged = false;` (analog data-model.md §6)
- [x] T010 [US1] Spring-Data-Repository anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/OidcProviderLinkRepository.java` — `extends JpaRepository<OidcProviderLinkEntity, Long>`; Custom-Finder: `Optional<OidcProviderLinkEntity> findByProviderAndProviderSubject(String provider, String providerSubject)` und `Optional<OidcProviderLinkEntity> findByUserAndProvider(UserEntity user, String provider)` gemäß data-model.md §7; benötigt T008
- [x] T011 [P] [US1] Internes Value-Object anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/services/OidcClaims.java` — Java-Record mit Feldern: `String sub`, `String email`, `boolean emailVerified`, `String name`, `String locale`, `String provider`, `String nonce`; wird vom Controller nach ID-Token-Validierung befüllt und an den Service übergeben
- [x] T012 [US1] Methoden-Signaturen `provisionOidcUser(OidcClaims claims)` und `findUserBySub(String provider, String sub)` zu `sabi-server/src/main/java/de/bluewhale/sabi/services/UserService.java` (Interface) hinzufügen; Return-Typen: `UserEntity` bzw. `Optional<UserEntity>`; benötigt T011
- [x] T013 [US1] Implementierung in `sabi-server/src/main/java/de/bluewhale/sabi/services/UserServiceImpl.java`: `provisionOidcUser()` legt neuen `UserEntity` an (Status `ACTIVE`, `oidcManaged = true`, kein Passwort-Hash, `username` aus `claims.name()`, `language` aus `claims.locale()`), speichert `OidcProviderLinkEntity` via `OidcProviderLinkRepository`, alles in einer Transaktion (`@Transactional`); `findUserBySub()` delegiert an `OidcProviderLinkRepository.findByProviderAndProviderSubject()` → `Optional.map(link -> link.getUser())`; benötigt T010, T012
- [x] T014 [US1] Controller anlegen: `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/OidcAuthController.java` — `@RestController @RequestMapping`, `@Tag(name = "OIDC Authentication")`; Methode `processOidcLogin(@RequestBody @Valid OidcLoginRequestTo request)` → `ResponseEntity<OidcLoginResponseTo>`:
  1. Validiert Google-ID-Token via `NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")` — prüft Signatur, `iss == https://accounts.google.com`, `aud == clientId`, `exp`; bei Fehler HTTP 401 `{error: "INVALID_ID_TOKEN"}`
  2. Prüft `email_verified == true`; sonst HTTP 401 `{error: "EMAIL_NOT_VERIFIED"}`
  3. Mappt JWT-Claims auf `OidcClaims`-Record
  4. Ruft `userService.findUserBySub(provider, sub)` auf → **US-3-Pfad**: gefunden → direkt zu Schritt 6
  5. Wenn nicht gefunden → **US-1-Pfad**: `userService.provisionOidcUser(claims)` aufrufen
  6. Prüft Account-Status (gesperrt → HTTP 423 `{error: "ACCOUNT_LOCKED"}`)
  7. Gibt Sabi-JWT via `TokenAuthenticationService.createAuthorizationTokenFor(email)` zurück
  8. Audit-Log (5 Event-Typen aus contracts/oidc-api.md §Audit Log, kein PII im Klartext)
  - benötigt T007, T011, T013
- [x] T015 [US1] `sabi-server/src/main/java/de/bluewhale/sabi/configs/WebSecurityConfig.java` anpassen: `requestMatchers(Endpoint.OIDC_GOOGLE_AUTH.getPath()).permitAll()` ergänzen (analog bestehenden `LOGIN`, `REGISTER` Einträgen); benötigt T005, T014
- [x] T016 [P] [US1] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/persistence/OidcProviderLinkRepositoryTest.java` — `@Testcontainers` mit MariaDB (analog `UserRepositoryTest`); testet: (a) `findByProviderAndProviderSubject` findet angelegten Link, (b) Cascade-Delete löscht Link wenn User gelöscht wird, (c) Unique-Constraint `(provider, provider_subject)` wirft Exception bei Duplikat; benötigt T008, T010
- [x] T017 [P] [US1] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/services/OidcUserServiceTest.java` — JUnit5 + Mockito; mockt `OidcProviderLinkRepository` und `UserRepository`; testet: (a) `provisionOidcUser()` speichert User mit `oidcManaged=true` und erstellt Link, (b) `findUserBySub()` gibt `Optional.empty()` zurück wenn kein Link, (c) `findUserBySub()` gibt User zurück wenn Link existiert; benötigt T011, T013
- [x] T018 [US1] Test anlegen: `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/OidcAuthControllerTest.java` — `@WebMvcTest` + `MockBean` für UserService; testet: (a) gültiger Mock-ID-Token (Mockito stubbed Decoder) → 200 + JWT, (b) abgelaufener Token → 401 INVALID_ID_TOKEN, (c) `email_verified=false` → 401 EMAIL_NOT_VERIFIED, (d) gesperrter Account → 423 ACCOUNT_LOCKED, (e) Provisioning-Pfad → `provisioned=true`, (f) Returning-User-Pfad (US-3) → `provisioned=false`; analog `UserAuthControllerTest`; benötigt T014

---

## Phase 4 — US-2: Bestehender Nutzer (Email-Match-Linking) [Backend]

**User Story**: US-2 (Bestehender Sabi-Nutzer, dessen Google-Email mit vorhandener Sabi-Email übereinstimmt)  
**Goal**: Email-Match-Branch im Controller verknüpft das Google-Konto mit dem bestehenden Account (kein Duplikat, `oidcManaged` bleibt `false`, Passwort-Login erhalten).  
**Independent test**: `OidcAuthControllerTest` — POST mit Email eines bestehenden Nutzers (kein sub-Link vorhanden) → 200, `provisioned=false`, Account hat neuen OidcProviderLink; IDENTITY_CONFLICT-Test → 409.

- [x] T019 [US2] Methoden-Signatur `linkOidcIdentity(UserEntity existingUser, OidcClaims claims)` zu `sabi-server/src/main/java/de/bluewhale/sabi/services/UserService.java` Interface hinzufügen; Return-Typ: `OidcProviderLinkEntity`; benötigt T011, T012
- [x] T020 [US2] Implementierung in `sabi-server/src/main/java/de/bluewhale/sabi/services/UserServiceImpl.java`: `linkOidcIdentity()` erstellt `OidcProviderLinkEntity` für den bestehenden User (provider + sub aus claims), setzt `oidcManaged` **nicht** um (bleibt `false` per CL-2), persistiert Link via `OidcProviderLinkRepository.save()`; `@Transactional`; benötigt T013, T019
- [x] T021 [US2] Email-Match-Branch in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/OidcAuthController.java` ergänzen: zwischen Schritt 4 (sub-Lookup not found) und Schritt 5 (provision) aus T014: `userRepository.findByEmail(email)` → wenn gefunden: Prüfe ob bereits Link mit anderem `sub` existiert → wenn ja: HTTP 409 `{error: "IDENTITY_CONFLICT"}`; sonst: `userService.linkOidcIdentity(existingUser, claims)` + Audit-Log `OIDC_LINK_CREATED`; wenn nicht gefunden → weiter zu Provisioning (US-1-Pfad); benötigt T014, T020
- [x] T022 [P] [US2] Tests in `sabi-server/src/test/java/de/bluewhale/sabi/services/OidcUserServiceTest.java` erweitern: (a) `linkOidcIdentity()` erstellt Link für bestehenden User, (b) `oidcManaged` bleibt `false` nach Linking; benötigt T020
- [x] T023 [P] [US2] Tests in `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/OidcAuthControllerTest.java` erweitern: (a) Email-Match-Pfad → 200, `provisioned=false`, (b) IDENTITY_CONFLICT (selbe Email, anderer sub) → 409; benötigt T021

---

## Phase 5 — US-4: Google App Registration + Deployment-Konfiguration

**User Story**: US-4 (Application Owner — Google App Registration, Secrets sicher konfiguriert)  
**Goal**: Sabi ist als OAuth2-Client bei Google registriert; Secrets per Env-Var konfiguriert; Ansible-Deployment trägt Secrets sicher ins Produktionssystem.  
**Independent test**: `git grep -r GOOGLE_OIDC_CLIENT_SECRET .` gibt **0 Treffer** in getrakten Dateien; Ansible-Playbook `--syntax-check` läuft durch.

- [x] T024 [P] [US4] `sabi-webclient/src/main/resources/application.properties` ergänzen: OAuth2-Client-Konfiguration als Env-Var-Referenzen hinzufügen (kein Hardcoding): `spring.security.oauth2.client.registration.google.client-id=${GOOGLE_OIDC_CLIENT_ID}`, `spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_OIDC_CLIENT_SECRET}`, `spring.security.oauth2.client.registration.google.scope=openid,email,profile`, `spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google`, `spring.security.oauth2.client.provider.google.issuer-uri=https://accounts.google.com` gemäß research.md §6
- [ ] T025 [P] [US4] Ansible-Vault-Variablen ergänzen: `devops/ansible/group_vars/` geeignete Vault-Datei um `sabi_google_oidc_client_id` und `sabi_google_oidc_client_secret` erweitern (verschlüsselt per `ansible-vault`); **keine Klartextwerte committen** (C-2)
- [ ] T026 [US4] Ansible-Deployment-Template für `sabi-webclient` anpassen: `deploySabiWebclient.yml` bzw. zugehöriges `application.properties.j2`-Template um Einträge `spring.security.oauth2.client.registration.google.client-id={{ sabi_google_oidc_client_id }}` und `...client-secret={{ sabi_google_oidc_client_secret }}` erweitern gemäß quickstart.md §3; benötigt T024, T025
- [ ] T027 [US4] Verification: Google Cloud Console vollständig konfiguriert (Consent Screen Published/Testing, Redirect-URI Prod + Dev registriert, Client-ID + Secret notiert); Checkliste quickstart.md §1 vollständig abgehakt; benötigt T001

---

## Phase 6 — US-1/2/3: Webclient Frontend-Integration

**User Stories**: US-1 (Login-Button + OIDC-Flow im Browser), US-2/3 (selber Login-Pfad)  
**Goal**: „Login with Google"-Button auf `login.xhtml`; Spring Security OAuth2 Client im Webclient initiiert OIDC-Flow; `SabiOidcSuccessHandler` tauscht Google-ID-Token gegen Sabi-JWT und speichert ihn in `SabiDoorKeeper`; Fehlerseite für OIDC-Abbruch.  
**Independent test**: Lokaler Smoke-Test (quickstart.md §2): Klick auf „Login with Google" → Redirect zu Google → Rückleitung auf Dashboard; Sabi-JWT im Browser-Session gesetzt; Audit-Log zeigt `OIDC_LOGIN_SUCCESS`.

- [x] T028 [P] [US1] `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/config/WebSecurityConfig.java` erweitern: `.oauth2Login(oauth2 -> oauth2.successHandler(sabiOidcSuccessHandler))` konfigurieren; `requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()` ergänzen; `SabiOidcSuccessHandler` per `@Autowired` injizieren; benötigt T024, T029
- [x] T029 [US1] `SabiOidcSuccessHandler.java` anlegen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/security/SabiOidcSuccessHandler.java` — implementiert `AuthenticationSuccessHandler`; in `onAuthenticationSuccess()`: `OidcUser oidcUser = (OidcUser) authentication.getPrincipal()`; `String rawIdToken = oidcUser.getIdToken().getTokenValue()`; HTTP-POST an `sabi-server` `Endpoint.OIDC_GOOGLE_AUTH` mit `OidcLoginRequestTo`; bei HTTP 200: `SabiDoorKeeper.setAccessToken(response.token)`; Redirect auf `/secured/userportal.xhtml`; bei 401/423: Redirect auf `/login.xhtml?error=oidc`; bei 5xx: Redirect auf `/login.xhtml?error=server`; benötigt T006, T007, T028
- [x] T030 [P] [US1] i18n-Keys in alle Messages-Properties-Dateien ergänzen:
  - `sabi-webclient/src/main/resources/i18n/messages.properties` → `login.oidc.google.button=Login with Google`
  - `sabi-webclient/src/main/resources/i18n/messages_de.properties` → `login.oidc.google.button=Mit Google anmelden`
  - `sabi-webclient/src/main/resources/i18n/messages_en.properties` → `login.oidc.google.button=Login with Google`
  - Gleiche Keys auch in `messages_es.properties`, `messages_fr.properties`, `messages_it.properties` (Fallback auf EN-Wert akzeptabel)
- [x] T031 [US1] „Login with Google"-Button zu `sabi-webclient/src/main/resources/META-INF/resources/login.xhtml` hinzufügen — PrimeFaces `<p:commandButton>` oder `<h:outputLink>` mit `href="/oauth2/authorization/google"`, Label via `#{msg['login.oidc.google.button']}`, `aria-label="#{msg['login.oidc.google.button']}"` (WCAG 2.1 AA), visuell getrennt vom Username/Passwort-Formular; benötigt T030
- [x] T032 [US1] `OidcErrorView.java` anlegen in `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/OidcErrorView.java` — `@Named @RequestScoped`; liest Query-Parameter `?error=oidc|server|cancelled`; setzt entsprechende Faces-Message für Anzeige in `login.xhtml`; Scenario 4 + 6 aus spec.md; analog `MyErrorController`

---

## Phase 7 — Polish & Quality Gates (ISO 25010)

**Goal**: Alle 8 ISO-25010-Charakteristika verifiziert; keine offenen Security-Issues; Deployability bestätigt.

- [ ] T033 [P] ISO 25010-III (Kompatibilität): OpenAPI-Annotationen in `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/OidcAuthController.java` vervollständigen — `@Tag(name = "OIDC Authentication")`, `@Operation(summary = "Exchange Google ID token for Sabi JWT")`, `@ApiResponse` für 200/400/401/409/423/500; springdoc-v2 zeigt neuen Endpunkt unter `/v3/api-docs`; benötigt T014
- [ ] T034 [P] ISO 25010-VII (Wartbarkeit): `OidcAuthControllerTest` und `OidcUserServiceTest` in `sabi-server/src/test/java/de/bluewhale/sabi/DeveloperTestSuite.java` und `IntegrationTestSuite.java` registrieren (analog bestehender Controller-Tests); benötigt T018, T023
- [ ] T035 ISO 25010-VI (Sicherheit / C-2): Secret-Audit ausführen — `git grep -rn "GOOGLE_OIDC_CLIENT"` im Repo-Root darf **keinen Treffer in getrakten Dateien** liefern (nur Env-Var-Referenzen in application.properties sind ok); `git log --all --grep="GOOGLE_OIDC"` auf 0 Ergebnisse mit echten Werten prüfen; benötigt T024
- [ ] T036 ISO 25010-V (Zuverlässigkeit) + Flyway-Validierung: `docker-compose -f devops/sabi_docker_sdk/docker-compose.yml up -d` → `mvn flyway:migrate -pl sabi-database` → prüfen: beide Migrationen `V1_4_0_1` und `V1_4_0_2` in `flyway_schema_history` mit Status `SUCCESS`; `oidc_provider_link`-Tabelle und `oidc_managed`-Spalte in `users` vorhanden; benötigt T003, T004
- [ ] T037 ISO 25010-V (Zuverlässigkeit) — Regression: Standard-Passwort-Login nach allen OIDC-Änderungen verifizieren — `POST /api/auth/login` mit gültigen Credentials liefert weiterhin HTTP 200 + JWT; bestehende `UserAuthControllerTest` grün; benötigt T015 (WebSecurityConfig-Änderung)
- [ ] T038 ISO 25010-IV (Benutzbarkeit): WCAG-2.1-AA-Prüfung — „Login with Google"-Button in `login.xhtml` hat `aria-label`, ausreichenden Farbkontrast, ist per Tab erreichbar; Browser-DevTools Accessibility-Tree prüfen; benötigt T031
- [ ] T039 ISO 25010-VIII (Übertragbarkeit) — OQ-1 IPv6/DS-Lite E2E-Validierung nach Produktions-Deployment: Von einem externen IPv4-Netzwerk `https://<strato-dyndns-hostname>` aufrufen → „Login with Google" klicken → Google-Auth abschließen → Redirect zurück zu Sabi erfolgreich; bei Fehler: nginx `listen [::]:443 ssl` prüfen, Redirect-URI in Google Cloud Console auf exakten Match prüfen; benötigt T026, T027

---

## Dependencies Summary

| Task | Requires |
|------|----------|
| T003 | T001 (Konzept), keine Code-Deps |
| T004 | T003 (Verzeichnis) |
| T008 | T003, T004 (DB-Schema) |
| T009 | T003, T004 (oidc_managed-Spalte) |
| T010 | T008 |
| T011 | — (reines Value-Object) |
| T012 | T011 |
| T013 | T010, T012 |
| T014 | T006, T007, T011, T013 |
| T015 | T005, T014 |
| T016 | T008, T010 |
| T017 | T011, T013 |
| T018 | T014 |
| T019 | T011, T012 |
| T020 | T013, T019 |
| T021 | T014, T020 |
| T022 | T020 |
| T023 | T021 |
| T024 | T002 |
| T025 | T001 |
| T026 | T024, T025 |
| T028 | T024, T029 |
| T029 | T006, T007 |
| T030 | — |
| T031 | T030 |
| T032 | — |
| T033 | T014 |
| T034 | T018, T023 |
| T035 | T024 |
| T036 | T003, T004 |
| T037 | T015 |
| T038 | T031 |
| T039 | T026, T027 |

---

## Parallel Execution Opportunities

Die folgenden Gruppen können **gleichzeitig** von verschiedenen Entwicklern bearbeitet werden:

**Group A** (Phase 2, parallel ausführbar nach Phase 1):
- T003 + T004 (Flyway, muss sequentiell T003 → T004)
- T005, T006, T007 (Boundary, untereinander parallel)

**Group B** (Phase 3, parallel nach T010, T011, T013):
- T008 + T009 (JPA-Entities, komplett unabhängig)
- T016 + T017 (Tests, komplett unabhängig)

**Group C** (Phase 4 + Phase 5 parallel):
- T019–T023 (US-2 Backend) und T024–T027 (Google App Registration) können parallel laufen, da sie verschiedene Module betreffen

**Group D** (Phase 6, T030 und T032 parallel zu T028/T029/T031):
- T030 (i18n Keys) unabhängig von allem anderen in Phase 6
- T032 (OidcErrorView) parallel zu T028 + T029

**Group E** (Phase 7, alle parallel):
- T033–T039 haben keine gegenseitigen Abhängigkeiten

---

## Task Count Summary

| Phase | Name | Tasks | User Story |
|-------|------|-------|------------|
| 1 | Setup | 2 | — |
| 2 | Foundational | 5 | — |
| 3 | US-1 + US-3 Backend (Auto-Provisioning + Returning User) | 11 | US-1, US-3 |
| 4 | US-2 Backend (Email-Match-Linking) | 5 | US-2 |
| 5 | US-4 Google App Registration + Deployment | 4 | US-4 |
| 6 | Webclient Frontend-Integration | 5 | US-1, US-2, US-3 |
| 7 | Polish & Quality Gates (ISO 25010) | 7 | — |
| **Total** | | **39** | |

**Parallel opportunities identified**: 5 Gruppen (A–E)  
**Suggested MVP scope**: Phase 1 + Phase 2 + Phase 3 (Backend vollständig, testbar via `OidcAuthControllerTest` + REST-Client)

---

## File Change Reference

| File | Change | Phase | Task |
|------|--------|-------|------|
| `sabi-database/src/main/resources/db/migration/version1_4_0/V1_4_0_1__addOidcProviderLinkTable.sql` | NEW | 2 | T003 |
| `sabi-database/src/main/resources/db/migration/version1_4_0/V1_4_0_2__addOidcManagedFlagToUsers.sql` | NEW | 2 | T004 |
| `sabi-boundary/src/main/java/de/bluewhale/sabi/api/Endpoint.java` | MOD | 2 | T005 |
| `sabi-boundary/src/main/java/de/bluewhale/sabi/model/OidcLoginRequestTo.java` | NEW | 2 | T006 |
| `sabi-boundary/src/main/java/de/bluewhale/sabi/model/OidcLoginResponseTo.java` | NEW | 2 | T007 |
| `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/OidcProviderLinkEntity.java` | NEW | 3 | T008 |
| `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/UserEntity.java` | MOD | 3 | T009 |
| `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/OidcProviderLinkRepository.java` | NEW | 3 | T010 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/OidcClaims.java` | NEW | 3 | T011 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/UserService.java` | MOD | 3+4 | T012, T019 |
| `sabi-server/src/main/java/de/bluewhale/sabi/services/UserServiceImpl.java` | MOD | 3+4 | T013, T020 |
| `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/OidcAuthController.java` | NEW | 3+4 | T014, T021 |
| `sabi-server/src/main/java/de/bluewhale/sabi/configs/WebSecurityConfig.java` | MOD | 3 | T015 |
| `sabi-server/src/test/java/de/bluewhale/sabi/persistence/OidcProviderLinkRepositoryTest.java` | NEW | 3 | T016 |
| `sabi-server/src/test/java/de/bluewhale/sabi/services/OidcUserServiceTest.java` | NEW | 3+4 | T017, T022 |
| `sabi-server/src/test/java/de/bluewhale/sabi/rest/controller/OidcAuthControllerTest.java` | NEW | 3+4 | T018, T023 |
| `sabi-server/src/test/java/de/bluewhale/sabi/DeveloperTestSuite.java` | MOD | 7 | T034 |
| `sabi-server/src/test/java/de/bluewhale/sabi/IntegrationTestSuite.java` | MOD | 7 | T034 |
| `sabi-webclient/pom.xml` | MOD | 1 | T002 |
| `sabi-webclient/src/main/resources/application.properties` | MOD | 5 | T024 |
| `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/config/WebSecurityConfig.java` | MOD | 6 | T028 |
| `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/security/SabiOidcSuccessHandler.java` | NEW | 6 | T029 |
| `sabi-webclient/src/main/resources/i18n/messages*.properties` | MOD | 6 | T030 |
| `sabi-webclient/src/main/resources/META-INF/resources/login.xhtml` | MOD | 6 | T031 |
| `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/OidcErrorView.java` | NEW | 6 | T032 |
| `devops/ansible/group_vars/<vault-file>` | MOD | 5 | T025 |
| `devops/ansible/deploySabiWebclient.yml` (+ template) | MOD | 5 | T026 |

