# Sabi Development Glossary

Glossary of domain and engineering terms used in the Sabi codebase, specs, and documentation.
This file is maintained in English (the filename keeps the German spelling "Glossar").

Add new entries when a term proves ambiguous in review, specs, or chat. Keep entries short:
definition + where the term shows up in the codebase.

---

## Domain Terms

### Reefer
The aquarist — the person who runs a (reef) aquarium. In Sabi this is the registered user of
the application: the owner of tanks, fish, corals, invertebrates, measurements, and maintenance
data. Prefer "Reefer" over "user" or "aquarist" in product-facing text and specs when referring
to the human behind an account.

### Tank / Aquarium
The central aggregate; both terms refer to the same thing. The UI and most Java code say "tank"
(`AquariumTo`, `TankFishStockEntity`), while the database table is named `aquarium`. One reefer
can own multiple tanks; all per-tank data (fish stock, dosing, measurements, maintenance) hangs
off the tank via `aquarium_id`.

### Fish stock / Coral stock / Invertebrate stock
The living inventory currently in a tank, as opposed to the shared catalogues:

- **Fish stock** — individual fish in a tank (`TankFishStockEntity`), with size history, photos,
  and departure records.
- **Coral stock** — corals with growth tracking (growth records) and polyp tracking.
- **Invertebrate stock** — invertebrates other than corals (e.g. cleanup crew), with functional
  classification (mobility, ecological role, activity pattern) and water sensitivities.

### Catalogue / Catalogue link
A community-shared species reference (fish, coral, invertebrate). Stock entries can be linked to
a catalogue entry ("catalogue link"), which snapshots the species' names. Catalogue entries are
proposed by reefers and approved or rejected by admins; the proposing reefer is the **creator**
and may update their own entry.

### Dosing
Adding a product or substance to a tank. Two types (`DosingType`): `MANUAL_ADDITION` (one-time
manual addition) and `AUTOMATED_DOSING` (recurring schedule with interval and optional end
date). Recorded in the dedicated `aquarium_dosing` table — deliberately *not* in the generic
logbook.

### Measurement
A water-parameter measurement for a tank (e.g. nitrate, temperature), optionally with an email
reminder.

### Plague Center
The records for disease and parasite occurrences per tank (e.g. Aiptasia), including treatment
notes.

### Maintenance profile
Per-tank ongoing maintenance settings (spec `007`), stored in `aquarium_maintenance_profile`:
the regular water change interval (days + percentage) and the activated carbon setting.

### Water change
A recorded one-time water change event (date + percentage + optional note), stored in
`aquarium_water_change`. Back-dating is allowed; future dates are rejected.

### Activated carbon
The carbon filtering setting on the maintenance profile: `CONTINUOUS` (filtering runs
permanently) or `INTERVAL` (carbon replaced every N days).

### AI export / Reef data export
A JSON document (`ReefDataExportTo`) bundling a reefer's complete reef data — tanks, fish,
corals, invertebrates, dosing, measurements, maintenance — with a `sabiSchemaVersion` metadata
field. The intended consumer is an external AI chatbot that reasons about the reef state and
gives advice. Schema evolution is additive-only: bump `sabiSchemaVersion`, never break existing
fields.

### House reef report
The public, token-gated page (`houseReefReport.xhtml`) that shows a read-only snapshot of a
reefer's reef (photos, fish list) to visitors via a share link with an embedded token.

---

## Engineering Terms

### Boundary (sabi-boundary)
The shared Maven module containing all DTOs, enums, and API contracts between webclient and
backend. It is consumed as an external artifact from the local Maven repository — after any
change, bump the version in `sabi-boundary/pom.xml` **and** both consumer poms, then
`mvn install`. See the boundary section in `AGENTS.md`.

### Webclient
The JSF/PrimeFaces frontend (Spring Boot + JoinFaces). Runs locally on port 8088 during
development and talks to the backend through a REST API gateway. XHTML view changes require a
full restart — there is no Facelets hot reload.

### Backend / Server (sabi-server)
The Spring Boot REST API. Runs in Docker as the `sabi-as` container on port 8080 during
development.

### JWT Bearer token
Sabi's self-implemented authentication scheme: login returns `Authorization: Bearer <jwt>` in
the response header; every subsequent API call sends it back as the `Authorization` header.
Auth problems surface as **403** (not 401) when a request reaches the security config without
an authentication.

### Ownership check
The mandatory security pattern: every mutating operation resolves the JWT principal's email to
a `UserEntity` and loads the target record with a combined ID + user filter (never by ID
alone). See the ownership section in `AGENTS.md`.

### View bean
A `@RequestScope` JSF controller bean (e.g. `MaintenanceView`) that loads state from the
backend on every request. REST controllers must stay stateless — no `@SessionScope`.

### Flyway migration
Versioned SQL scripts (`Vx_y_z_n__description.sql`) in the `sabi-database` module, applied by
the Flyway container. Existing scripts are immutable — new changes go into a new script. No
hardcoded schema prefixes (the Flyway config supplies the `sabi` schema).

### SBOM
The CycloneDX Software Bill of Materials generated per Maven module at `target/bom.json` during
`mvn package`. Read it before suggesting dependency changes.

### Testcontainer
The disposable MariaDB used for JUnit tests. Test users must be created via `UserRepository` in
`@BeforeEach` — never reference hardcoded IDs.

### fakeSMTP
The dev Docker container that captures outgoing mail on port 2525 (password reset,
registration flows). Holding this port breaks `UserAuthControllerTest` locally.

### EclipseLink javaagent
The JVM agent required for IDE test runs and the Spring Boot run configuration. See
`DEVELOPERS_MANUAL.md`.

### Colima
The Docker runtime on the dev Mac. Port 8080 is an SSH forward from Colima into the Docker
network — trust backend logs over `curl` for error codes.

### Redeploy
`devops/sabi_docker_sdk/server_redeploy.sh` — rebuilds and restarts the backend Docker image.
Flags: `--boundary` (reinstall sabi-boundary first), `--flyway` (apply pending migrations).

### Spec (feature spec)
Each feature lives in `specs/<NNN>-<name>/` with `spec.md` (requirements), `plan.md`,
`data-model.md`, `contracts/`, `tasks.md` (T001… tasks), and `quickstart.md`.

### QGATE
Quality gates in `tasks.md` — checklist items (QGATE-I…VIII) that must be verified before a
feature counts as complete.
