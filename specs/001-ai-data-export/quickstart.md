# Quickstart: AI Chatbot Data Export (001-ai-data-export)

**Phase**: 1 — Design  
**Date**: 2026-04-04

---

## What is being built?

A one-click "Download reef data" button in the Sabi user profile that generates a structured JSON file of all the user's reef data (aquariums, measurements, plague records, fish, corals, treatments) for use with AI chatbots like ChatGPT.

---

## Prerequisites

- Sabi development environment running (see `devops/sabi_docker_sdk/`)
- Java 25, Maven 3.x
- Feature branch: `001-ai-data-export`

---

## Modules Touched

| Module | Changes |
|--------|---------|
| `sabi-boundary` | New TOs (`ReefDataExportTo`, `AquariumExportTo`, etc.) + `Endpoint.USER_PROFILE_EXPORT` |
| `sabi-server` | New `ReefDataExportService` + `ReefDataExportServiceImpl` + endpoint in `UserProfileController` |
| `sabi-webclient` | Updated `UserService` + `UserServiceImpl` + `UserProfileView` + `userProfile.xhtml` + i18n keys |

**No `sabi-database` changes. No Flyway migrations required.**

---

## Implementation Order

1. **sabi-boundary**: Add new TOs + Endpoint enum entry (no dependencies on other modules)
2. **sabi-server**: Add `ReefDataExportService` and endpoint (depends on boundary TOs)
3. **sabi-webclient**: Add download method + UI panel (depends on boundary Endpoint enum)

---

## Manual Test (Smoke Test)

1. Start local Docker stack: `cd devops/sabi_docker_sdk && docker-compose up`
2. Log in to webclient with a test user that has at least one tank and measurements
3. Navigate to **User Profile**
4. Scroll to the new "AI Chatbot Data Export" panel
5. Click **"Download reef data"** — browser should download `sabi-reef-data-YYYY-MM-DD.json`
6. Open the JSON file and verify:
   - `_meta.sabiSchemaVersion` is `"1.0"`
   - `aquariums` array is non-empty and contains your test tank
   - At least one measurement has `unitNameResolved: true`
   - No email, username, or password fields present
7. Log in as a **new user with no tanks** to verify the button is disabled and the hint text is shown

---

## Running the Tests

```bash
# sabi-server module tests (includes new export endpoint integration test)
cd sabi-server && mvn test -Dtest="UserProfileControllerTest,ReefDataExportServiceTest"

# Full build
mvn clean verify
```

---

## Key Files

| File | Purpose |
|------|---------|
| `sabi-boundary/.../model/ReefDataExportTo.java` | Top-level export TO |
| `sabi-boundary/.../api/Endpoint.java` | Add `USER_PROFILE_EXPORT` |
| `sabi-server/.../services/ReefDataExportService.java` | Service interface |
| `sabi-server/.../services/ReefDataExportServiceImpl.java` | Service implementation |
| `sabi-server/.../rest/controller/UserProfileController.java` | Add `/export` endpoint |
| `sabi-webclient/.../apigateway/UserService.java` | Add `downloadReefDataExport` |
| `sabi-webclient/.../apigateway/UserServiceImpl.java` | Implement REST call |
| `sabi-webclient/.../controller/UserProfileView.java` | Add `downloadReefData()` action |
| `sabi-webclient/.../resources/secured/userProfile.xhtml` | Add AI export panel |
| `sabi-webclient/.../resources/i18n/messages_de.properties` | German i18n keys |
| `sabi-webclient/.../resources/i18n/messages_en.properties` | English i18n keys |

