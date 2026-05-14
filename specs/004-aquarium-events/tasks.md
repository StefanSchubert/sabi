# Tasks: Aquarium Event Logbook

**Feature Branch**: `004-aquarium-events`  
**Input**: `specs/004-aquarium-events/spec.md` + `specs/004-aquarium-events/plan.md`  
**Status**: Ready for implementation

## Format: `- [ ] T-NNN [P?] [USn?] Title — path`

- **[P]**: Parallelizable (different files, no in-flight dependencies)
- **[USn]**: User Story label (US1 = Log event, US2 = Edit/Delete, US3 = Public report)
- Detailed task blocks follow each checklist line (what / files / depends on / AC)

---

## Phase 1: Setup — Database Migrations

**Purpose**: Create the two Flyway migrations that unblock all server-layer work.  
**⚠️ Both migrations are independent of each other and can be written in parallel.**

- [X] T-001 [P] Create `aquarium_event` table migration — `V1_6_0_1__addAquariumEventTable.sql`
  - **What**: Create the file `sabi-database/src/main/resources/db/migration/version1_6_0/V1_6_0_1__addAquariumEventTable.sql` with the exact SQL from plan.md §2 Migration 1. Columns: `id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT`, `aquarium_id BIGINT(20) UNSIGNED NOT NULL`, `event_date DATE NOT NULL`, `duration_hours DECIMAL(6,2) NULL`, `description TEXT NOT NULL`, `created_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`, `lastmod_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`, `optlock INT UNSIGNED NOT NULL DEFAULT 0`. Include composite index `idx_aquarium_event_aquarium_date (aquarium_id, event_date)` and FK `fk_aquarium_event_aquarium → aquarium(id) ON DELETE CASCADE`. Engine InnoDB, charset utf8. No schema prefix, backtick identifiers throughout.
  - **Files**: `sabi-database/src/main/resources/db/migration/version1_6_0/V1_6_0_1__addAquariumEventTable.sql` *(create)*
  - **Depends on**: —
  - **AC**:
    - [X] File path matches Flyway's `version1_6_0` location convention
    - [X] All 8 columns present with correct MariaDB types and nullability
    - [X] `lastmod_on` and `optlock` used (not `updated_on`/`version`)
    - [X] FK constraint cascades deletes from `aquarium`
    - [X] Migration applies cleanly on a fresh MariaDB schema (`mvn flyway:migrate` green)

- [X] T-002 [P] Add `include_events` column migration — `V1_6_0_2__addIncludeEventsToPublicReportLink.sql`
  - **What**: Create `sabi-database/src/main/resources/db/migration/version1_6_0/V1_6_0_2__addIncludeEventsToPublicReportLink.sql`. SQL: `ALTER TABLE \`public_report_link\` ADD COLUMN \`include_events\` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'When 1, events (past 365 days) are included in the public report';`
  - **Files**: `sabi-database/src/main/resources/db/migration/version1_6_0/V1_6_0_2__addIncludeEventsToPublicReportLink.sql` *(create)*
  - **Depends on**: —
  - **AC**:
    - [X] `TINYINT(1) NOT NULL DEFAULT 0` (MariaDB boolean convention)
    - [X] No `NOT NULL` constraint without default (safe for existing rows)
    - [X] Migration applies cleanly on top of V1_6_0_1 and on a v1.5.x baseline

**Checkpoint**: Migrations written → sabi-server entity/repository work can begin.

---

## Phase 2: Foundational — Boundary Layer

**Purpose**: Define the shared DTOs and endpoint enum entry that sabi-server and sabi-webclient both depend on. All four tasks touch separate files and can run in parallel after Phase 1.

- [X] T-003 [P] Create `AquariumEventTo` DTO — `AquariumEventTo.java`
  - **What**: Create new class in `sabi-boundary`. Fields: `Long id`, `@NotNull Long aquariumId`, `@NotNull LocalDate eventDate`, `@Positive BigDecimal durationHours` (nullable), `@NotNull String description`, `LocalDateTime createdOn`, `LocalDateTime updatedOn` (maps to `lastmod_on`), `long optlock`. Annotations: `@Data`, `implements Serializable`. Import `io.swagger.v3.oas.annotations.media.Schema` for Swagger docs. Full class as specified in plan.md §3.1.
  - **Files**: `sabi-boundary/src/main/java/de/bluewhale/sabi/model/AquariumEventTo.java` *(create)*
  - **Depends on**: —
  - **AC**:
    - [X] All 8 fields present with correct types (especially `BigDecimal` for durationHours, `LocalDate` for eventDate)
    - [X] `@NotNull` on `aquariumId`, `eventDate`, `description`; `@Positive` on `durationHours`
    - [X] `updatedOn` field name (not `lastmodOn`) in the DTO
    - [X] `implements Serializable`; compiles with `mvn compile -pl sabi-boundary`

- [X] T-004 [P] Extend `PublicReportLinkTo` with `includeEvents` field
  - **What**: Add `private boolean includeEvents = false;` with `@Schema` annotation to the existing `PublicReportLinkTo` class. Add after the `createdOn` field as shown in plan.md §3.2.
  - **Files**: `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReportLinkTo.java` *(modify)*
  - **Depends on**: —
  - **AC**:
    - [X] Field defaults to `false`
    - [X] No existing field renamed or removed
    - [X] `sabi-boundary` compiles cleanly

- [X] T-005 Extend `PublicReefReportTo` with `recentEvents` field
  - **What**: Add `private List<AquariumEventTo> recentEvents;` to `PublicReefReportTo`. Semantics: `null` = opted-out, empty `list` = opted-in but no events. Add after `measurementsByUnit`. Import `AquariumEventTo` from same package. See plan.md §3.3.
  - **Files**: `sabi-boundary/src/main/java/de/bluewhale/sabi/model/PublicReefReportTo.java` *(modify)*
  - **Depends on**: T-003
  - **AC**:
    - [X] Field type `List<AquariumEventTo>` (not array)
    - [X] No `@NotNull`; null is intentional when `includeEvents = false`
    - [X] `sabi-boundary` compiles cleanly

- [X] T-006 [P] Add `TANK_EVENTS` entry to `Endpoint.java`
  - **What**: Add the enum constant `TANK_EVENTS("/api/tank")` to `Endpoint.java` in sabi-boundary. This shares the base path with `TankController`; call-site code appends `"/" + tankId + "/events"`. See plan.md §3.4.
  - **Files**: `sabi-boundary/src/main/java/de/bluewhale/sabi/api/Endpoint.java` *(modify)*
  - **Depends on**: —
  - **AC**:
    - [X] Constant added without modifying or removing existing entries
    - [X] Path value is `"/api/tank"`
    - [X] `sabi-boundary` compiles cleanly

**Checkpoint**: Boundary layer complete → sabi-server and sabi-webclient can proceed in parallel.

---

## Phase 3: User Story 1 — Log a Notable Reef Event (Priority: P1) 🎯 MVP

**Goal**: A logged-in user can create a new event (date, optional duration, description) on the aquarium detail page and immediately see it at the top of the event list.

**Independent Test**: Log in as a user with ≥1 aquarium → open `tankView.xhtml` → create an event with date + description → verify it appears first in the list. No other user story required.

### Implementation

- [X] T-007 [P] [US1] Create `AquariumEventEntity` extending `Auditable`
  - **What**: Create JPA entity class. Annotations: `@Table(name = "aquarium_event")`, `@Entity`, `@Data`, `@EqualsAndHashCode(callSuper = false)`. Extend `Auditable` (inherits `createdOn`, `lastmodOn`, `optlock`). Own fields: `@Id @GeneratedValue Long id`, `@Column("aquarium_id") Long aquariumId`, `@Column("event_date") LocalDate eventDate`, `@Column("duration_hours", precision=6, scale=2) BigDecimal durationHours` (nullable), `@Column(columnDefinition="TEXT") String description`. Use `jakarta.persistence.*`. Full class as in plan.md §4.1.
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/AquariumEventEntity.java` *(create)*
  - **Depends on**: T-001
  - **AC**:
    - [X] Extends `Auditable` (no separate `createdOn`/`lastmodOn`/`optlock` fields — inherited)
    - [X] `@Table(name = "aquarium_event")` — no explicit schema attribute needed (existing entities use spring datasource schema)
    - [X] `durationHours` is nullable; `description` is `columnDefinition="TEXT"`
    - [X] Jakarta EE 9+ namespace (`jakarta.persistence.*`, not `javax`)
    - [X] Compiles with `mvn compile -pl sabi-server`

- [X] T-008 [P] [US1] Create `AquariumEventRepository`
  - **What**: Create Spring Data JPA repository interface extending `JpaRepository<AquariumEventEntity, Long>`. Define three query methods exactly as in plan.md §4.3: (1) `findByAquariumIdOrderByEventDateDesc(Long aquariumId)` — all events newest-first; (2) `findByAquariumIdAndEventDateGreaterThanEqualOrderByEventDateDesc(Long aquariumId, LocalDate cutoff)` — rolling 365-day window for public report; (3) `findByIdAndAquariumId(Long id, Long aquariumId)` — ownership-safe single-event lookup returning `Optional<AquariumEventEntity>`.
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/AquariumEventRepository.java` *(create)*
  - **Depends on**: T-007
  - **AC**:
    - [X] All three method signatures match plan.md §4.3 exactly
    - [X] `@NotNull` on parameters (Jakarta validation)
    - [X] Return type of (3) is `Optional<AquariumEventEntity>` (not `List`)
    - [X] `@Repository` not required (Spring Data auto-detects); but interface is in the correct package
    - [X] Compiles with `mvn compile -pl sabi-server`

- [X] T-009 [US1] Create `AquariumEventMapper` (MapStruct)
  - **What**: Create MapStruct mapper interface annotated `@Mapper(componentModel = "spring", uses = {MappingUtils.class})`. Four mapping methods as specified in plan.md §4.4: (1) `mapEntityToTo(AquariumEventEntity) → AquariumEventTo` (maps `lastmodOn` → `updatedOn`); (2) `mapEntitiesToTos(List<AquariumEventEntity>) → List<AquariumEventTo>`; (3) `mapToToEntity(AquariumEventTo) → AquariumEventEntity`; (4) `mergeToIntoEntity(AquariumEventTo, @MappingTarget AquariumEventEntity)` (ignores `id` and `aquariumId`).
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/mapper/AquariumEventMapper.java` *(create)*
  - **Depends on**: T-003, T-007
  - **AC**:
    - [X] `@Mapping(target = "updatedOn", source = "lastmodOn")` in `mapEntityToTo`
    - [X] `mergeToIntoEntity` ignores `id` and `aquariumId` (prevents overwrite on update)
    - [X] MapStruct generates implementation at compile time (`mvn generate-sources -pl sabi-server` produces `AquariumEventMapperImpl`)

- [X] T-010 [US1] Create `AquariumEventService` interface and `AquariumEventServiceImpl` (list + create)
  - **What**: (A) Create service interface `AquariumEventService` with two methods for US1: `listEventsForTank(Long aquariumId, String userEmail)` and `createEvent(Long aquariumId, AquariumEventTo eventTo, String userEmail)`. See full signatures in plan.md §4.5. (B) Create `AquariumEventServiceImpl` annotated `@Service @Slf4j @Transactional(readOnly=true)`. Inject `AquariumEventRepository`, `AquariumRepository`, `UserRepository`, `AquariumEventMapper`. Implement `resolveOwnedAquarium(Long, String)` private helper (plan.md §4.6). Implement `listEventsForTank` and `createEvent` per plan.md §4.6 logic. Leave `updateEvent` and `deleteEvent` as `TODO` stubs for T-018.
  - **Files**:
    - `sabi-server/src/main/java/de/bluewhale/sabi/services/AquariumEventService.java` *(create)*
    - `sabi-server/src/main/java/de/bluewhale/sabi/services/AquariumEventServiceImpl.java` *(create)*
  - **Depends on**: T-003, T-008, T-009
  - **AC**:
    - [X] `createEvent` returns `ResultTo<AquariumEventTo>` with type `INFO` on success; `ERROR` when ownership check fails
    - [X] `createEvent` calls `entity.setId(null)` before save to prevent client-supplied ID from being used
    - [X] `listEventsForTank` returns empty list (not null) when ownership check fails
    - [X] `@Transactional` on write methods overrides class-level `readOnly=true`
    - [X] Compiles with `mvn compile -pl sabi-server`

- [X] T-011 [US1] Create `AquariumEventController` (GET + POST endpoints)
  - **What**: Create `@RestController @RequestMapping("api/tank") @Slf4j` class. Inject `AquariumEventService`. Implement two endpoints for US1: (1) `GET /{tankId}/events` → `ResponseEntity<List<AquariumEventTo>>` HTTP 200; (2) `POST /{tankId}/events` → `ResponseEntity<AquariumEventTo>` HTTP 201 on success, HTTP 403 on ownership error. Both require `@RequestHeader AUTH_TOKEN`. Full implementation per plan.md §4.9. Leave PUT and DELETE as `TODO` stubs for T-019.
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/AquariumEventController.java` *(create)*
  - **Depends on**: T-010
  - **AC**:
    - [X] `@RequestMapping("api/tank")` — no leading slash (consistent with other controllers)
    - [X] POST returns `ResponseEntity.status(HttpStatus.CREATED).body(result.getValue())`
    - [X] Ownership error → HTTP 403 (not 404)
    - [X] `@Valid` annotation on `@RequestBody` parameter for bean validation
    - [X] Spring Security picks up the controller as a secured endpoint (no `/api/public/**` bypass)

- [X] T-012 [P] [US1] Create webclient `AquariumEventService` interface and `AquariumEventServiceImpl` (list + create)
  - **What**: (A) Create interface `AquariumEventService extends Serializable` with two methods for US1: `listEventsForTank(Long aquariumId, String token)` and `createEvent(Long aquariumId, AquariumEventTo event, String token)`, both throwing `BusinessException`. See plan.md §5.2. (B) Create `@Named @RequestScope @Slf4j AquariumEventServiceImpl extends APIServiceImpl`. Implement the two US1 methods using `RestTemplate` + `objectMapper` + `RestHelper.prepareAuthedHttpHeader(token)`, calling `sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events"`. Handle `JacksonException` and `RestClientException`. Call `renewBackendToken(response)` after each exchange. See plan.md §5.3.
  - **Files**:
    - `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/AquariumEventService.java` *(create)*
    - `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/AquariumEventServiceImpl.java` *(create)*
  - **Depends on**: T-003, T-006
  - **AC**:
    - [X] Uses `Endpoint.TANKS.getPath()` (existing constant) to build URI — not hardcoded string
    - [X] `listEventsForTank` deserializes `AquariumEventTo[]` and returns `Arrays.asList(items)`
    - [X] `createEvent` deserializes response body into `AquariumEventTo` and returns it
    - [X] Both methods call `renewBackendToken(response)` to propagate refreshed JWT
    - [X] Compiles with `mvn compile -pl sabi-webclient`

- [X] T-013 [US1] Create `AquariumEventView` CDI bean (init + list + create)
  - **What**: Create `@Named @RequestScope @Slf4j @Getter @Setter` CDI bean implementing `Serializable`. Inject `UserSession`, `TankListView`, `AquariumEventService`. Fields: `Map<Long, List<AquariumEventTo>> eventsByTank`, `Map<Long, AquariumEventTo> editFormByTank`. Implement `@PostConstruct init()` that iterates `tankListView.getTanks()` and pre-loads events. Implement `getEventsForTank(Long)`, `getEditFormForTank(Long)`, `saveEvent(Long)` (create path only — branch on `form.getId() == null`), `resetForm(Long)`. Full implementation per plan.md §5.4. Leave `startEditEvent` and `deleteEvent` as `TODO` stubs for T-021.
  - **Files**: `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/AquariumEventView.java` *(create)*
  - **Depends on**: T-012
  - **AC**:
    - [X] `@PostConstruct init()` handles `BusinessException` per tank (logs error, puts empty list)
    - [X] `saveEvent` prepends new event to `eventsByTank.get(aquariumId)` at index 0
    - [X] `saveEvent` resets `editFormByTank` after successful save
    - [X] `saveEvent` calls `MessageUtil.info(...)` on success and `MessageUtil.error(...)` on `BusinessException`
    - [X] `getEditFormForTank` uses `computeIfAbsent` to initialize blank form
    - [X] `@RequestScope` — new instance per HTTP request

- [X] T-014 [US1] Add Events logbook panel (form + datatable) to `tankView.xhtml`
  - **What**: Inside the `<ui:repeat>` over tanks in `tankView.xhtml`, append the Events logbook panel below the existing action buttons for each tank. Full XHTML structure from plan.md §5.6: (1) outer `<p:panel header="#{msg['aquariumevent.panel.h']}">`, (2) scoped `<p:messages id="aquariumEventMessages_#{tank.id}">`, (3) `<h:form id="eventForm_#{tank.id}">` containing a `<p:panelGrid columns="2">` with `p:datePicker` (date, required), `p:inputText` with `f:convertNumber` (duration, optional), `p:inputTextarea` (description, required), plus Save and New buttons and two `p:staticMessage` indicators for edit/create mode, (4) separate `<h:form id="eventList_#{tank.id}">` containing `<p:dataTable id="eventDT_#{tank.id}">` with date/duration/description columns. At this stage, Add edit and delete columns as empty placeholders (the actionable buttons are added in T-022).
  - **Files**: `sabi-webclient/src/main/resources/META-INF/resources/secured/tankView.xhtml` *(modify)*
  - **Depends on**: T-013
  - **AC**:
    - [X] `p:datePicker` has `showTime="false"` and `required="true"` with `requiredMessage` i18n key
    - [X] `p:inputTextarea` has `required="true"` with `requiredMessage` i18n key
    - [X] Duration field uses `<f:convertNumber>` converter
    - [X] Save button calls `aquariumEventView.saveEvent(tank.id)` with `ajax="true"` and `update="eventForm_#{tank.id} eventList_#{tank.id}"`
    - [X] datatable `emptyMessage` references i18n key
    - [X] Description column uses `style="white-space:pre-wrap; word-break:break-word;"` for line-break preservation
    - [X] Each form has a unique scoped `id` using `tank.id`

**Checkpoint ✅ US1 complete**: Login → open any tank → fill date + description → Save → event appears at top of list.

---

## Phase 4: User Story 2 — Edit or Delete an Existing Event (Priority: P2)

**Goal**: A logged-in user can edit any field of an existing event and can permanently delete events with a confirmation dialog.

**Independent Test**: Using an event created in US1 — click edit → modify description → save → verify change; then click delete → confirm → verify event is gone.

### Implementation

- [X] T-015 [US2] Add `updateEvent()` and `deleteEvent()` to `AquariumEventServiceImpl`
  - **What**: Fill in the `TODO` stubs in `AquariumEventServiceImpl`. `updateEvent`: (1) `resolveOwnedAquarium`, (2) `findByIdAndAquariumId(eventId, aquariumId)` — empty → `ResultTo.ERROR`, (3) `mapper.mergeToIntoEntity(eventTo, entity)`, (4) `save(entity)` — let `ObjectOptimisticLockingFailureException` propagate, (5) return `ResultTo.INFO`. `deleteEvent`: (1) `resolveOwnedAquarium`, (2) `findByIdAndAquariumId` — empty → `ResultTo.ERROR`, (3) `delete(entity)`, (4) return `ResultTo.INFO`. See plan.md §4.6.
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/services/AquariumEventServiceImpl.java` *(modify)*
  - **Depends on**: T-010
  - **AC**:
    - [X] Both methods annotated `@Transactional` (write, overrides `readOnly=true`)
    - [X] `mergeToIntoEntity` is used for update (not `mapToToEntity` which would lose `id`/`aquariumId`)
    - [X] `ObjectOptimisticLockingFailureException` is NOT caught here — propagates to controller
    - [X] Delete uses `repository.delete(entity)`, not `deleteById` (entity already fetched for ownership check)
    - [X] Service interface `AquariumEventService.java` updated with the two new method signatures

- [X] T-016 [US2] Add PUT and DELETE endpoints to `AquariumEventController`
  - **What**: Fill in the `TODO` stubs in `AquariumEventController`. `PUT /{tankId}/events/{eventId}` → HTTP 200 on success, 403 on ownership error, 409 on `ObjectOptimisticLockingFailureException`. `DELETE /{tankId}/events/{eventId}` → HTTP 200 on success, 403 on ownership error. Full method signatures per plan.md §4.9.
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/AquariumEventController.java` *(modify)*
  - **Depends on**: T-011, T-015
  - **AC**:
    - [X] PUT catches `ObjectOptimisticLockingFailureException` and returns `ResponseEntity.status(HttpStatus.CONFLICT).build()`
    - [X] DELETE returns `ResponseEntity.ok().build()` (HTTP 200, not 204)
    - [X] Both endpoints require `@RequestHeader AUTH_TOKEN`
    - [X] `@Valid` on PUT `@RequestBody`

- [X] T-017 [P] [US2] Add `updateEvent()` and `deleteEvent()` to webclient `AquariumEventServiceImpl`
  - **What**: Fill in the `TODO` stubs in the webclient `AquariumEventServiceImpl`. `updateEvent(Long aquariumId, Long eventId, AquariumEventTo event, String token)`: PUT to `sabiBackendUrl + .../aquariumId/events/eventId`, serialize body, return deserialized `AquariumEventTo`. `deleteEvent(Long aquariumId, Long eventId, String token)`: DELETE to same URI, no response body. Both call `renewBackendToken`. Throw `BusinessException(NETWORK_ERROR)` on `RestClientException`. Update the webclient `AquariumEventService` interface with the two new method signatures. Full code in plan.md §5.3.
  - **Files**:
    - `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/AquariumEventService.java` *(modify — add 2 method signatures)*
    - `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/AquariumEventServiceImpl.java` *(modify — implement 2 methods)*
  - **Depends on**: T-012
  - **AC**:
    - [X] `updateEvent` throws `BusinessException` (not raw exception) on network error
    - [X] `deleteEvent` void return; throws `BusinessException` on network error
    - [X] URI construction uses `Endpoint.TANKS.getPath()` + path segments (no hardcoded strings)

- [X] T-018 [US2] Add `startEditEvent()` and `deleteEvent()` to `AquariumEventView`
  - **What**: Fill in the `TODO` stubs in `AquariumEventView`. `startEditEvent(Long aquariumId, AquariumEventTo event)`: deep-copy all fields (id, aquariumId, eventDate, durationHours, description, optlock) into a new `AquariumEventTo`, put into `editFormByTank`. `deleteEvent(Long aquariumId, Long eventId)`: call `aquariumEventService.deleteEvent`, remove from `eventsByTank` list, show success/error message via `MessageUtil`. Extend `saveEvent` to handle the update branch (`form.getId() != null`): call `updateEvent`, replace in list. Full code in plan.md §5.4.
  - **Files**: `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/AquariumEventView.java` *(modify)*
  - **Depends on**: T-013, T-017
  - **AC**:
    - [X] `startEditEvent` copies `optlock` field (required for optimistic lock on PUT)
    - [X] `saveEvent` update branch replaces list entry using `list.replaceAll(e -> e.getId().equals(updated.getId()) ? updated : e)`
    - [X] `deleteEvent` uses `list.removeIf(e -> e.getId().equals(eventId))`
    - [X] After edit, form is reset via `editFormByTank.put(aquariumId, new AquariumEventTo())`

- [X] T-019 [US2] Add edit button, delete button, and per-row `p:confirmDialog` to `tankView.xhtml`
  - **What**: In the `eventDT_#{tank.id}` datatable, add two action columns (replacing the placeholder columns from T-014): (A) Edit column with `<p:commandButton icon="pi pi-pencil" action="#{aquariumEventView.startEditEvent(tank.id, event)}" update="eventForm_#{tank.id}" ajax="true"/>`. (B) Delete column with a `<p:commandButton icon="pi pi-trash" styleClass="ui-button-danger" ajax="true" oncomplete="PF('confirmDeleteEvent_#{event.id}').show()"/>` and a `<p:confirmDialog widgetVar="confirmDeleteEvent_#{event.id}" header="#{msg['aquariumevent.delete.confirm.header']}" message="#{msg['aquariumevent.delete.confirm.message']}" modal="true">` containing a Confirm button calling `aquariumEventView.deleteEvent(tank.id, event.id)` and a Cancel button. Full XHTML per plan.md §5.6.
  - **Files**: `sabi-webclient/src/main/resources/META-INF/resources/secured/tankView.xhtml` *(modify)*
  - **Depends on**: T-014, T-018
  - **AC**:
    - [X] `widgetVar` uses `event.id` to make each dialog unique per row: `confirmDeleteEvent_#{event.id}`
    - [X] Delete button triggers dialog via `oncomplete` (not `onclick`) to execute after ajax response
    - [X] Confirm button inside dialog: `update="eventList_#{tank.id}"` and `oncomplete="PF(...).hide()"`
    - [X] Cancel button is `type="button"` (no form submit) with `onclick="PF(...).hide()"`
    - [X] Edit button: `update="eventForm_#{tank.id}"` so form switches to edit mode inline

**Checkpoint ✅ US2 complete**: Create → edit description → save → change visible; delete → confirm → entry gone.

---

## Phase 5: User Story 3 — Include Events in Public HouseReef Report (Priority: P3)

**Goal**: An authenticated user can opt-in to showing their recent events (past 12 months) in the public `houseReefReport.xhtml`. An unauthenticated visitor sees the events section when the flag is active.

**Independent Test**: Enable `includeEvents` for a tank with one event –6 months ago and one event –13 months ago → access public report URL → only the recent event appears; older event is absent.

### Implementation

- [X] T-020 [P] [US3] Extend `PublicReportLinkEntity` with `includeEvents` column mapping
  - **What**: Add `@Column(name = "include_events", nullable = false) @Basic private boolean includeEvents = false;` to `PublicReportLinkEntity`. Add getter/setter (or Lombok handles it). See plan.md §4.2.
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/PublicReportLinkEntity.java` *(modify)*
  - **Depends on**: T-002
  - **AC**:
    - [X] Column name `include_events` (matches migration V1_6_0_2)
    - [X] Default `false` prevents non-null constraint violation on existing rows
    - [X] `sabi-server` compiles cleanly

- [X] T-021 [US3] Extend `PublicReportService` + `PublicReportServiceImpl` (server) for `includeEvents` and `recentEvents`
  - **What**: (A) Add `boolean updateIncludeEvents(Long aquariumId, boolean includeEvents, String userEmail)` to `PublicReportService` interface (plan.md §4.7). (B) In `PublicReportServiceImpl`: (1) inject `AquariumEventRepository` and `AquariumEventMapper`; (2) implement `updateIncludeEvents` — resolve user, resolve aquarium, load link, set flag, save (plan.md §4.8); (3) in existing `getReport()` method, after inhabitants/measurement assembly add the `if (link.isIncludeEvents())` block that sets `report.setRecentEvents(...)` using `cutoff = LocalDate.now().minusDays(365)`; (4) in the link-to-DTO mapping helper, add `to.setIncludeEvents(entity.isIncludeEvents())` so the flag propagates to the gateway.
  - **Files**:
    - `sabi-server/src/main/java/de/bluewhale/sabi/services/PublicReportService.java` *(modify)*
    - `sabi-server/src/main/java/de/bluewhale/sabi/services/PublicReportServiceImpl.java` *(modify)*
  - **Depends on**: T-003, T-005, T-008, T-009, T-020
  - **AC**:
    - [X] `updateIncludeEvents` returns `false` when no report link exists for the aquarium
    - [X] 365-day filter uses `LocalDate.now().minusDays(365)` — not `minusMonths(12)` (per spec rolling window rule)
    - [X] When `includeEvents == false`, `report.getRecentEvents()` remains `null` (not empty list)
    - [X] When `includeEvents == true` but no events exist, `report.getRecentEvents()` is an empty list (not null)
    - [X] `updateIncludeEvents` is `@Transactional`

- [X] T-022 [US3] Add `PUT /api/report/link/{aquariumId}/include-events` to `PublicReportController`
  - **What**: Add new endpoint inside `PublicReportController.java`. `@PutMapping(value = "/api/report/link/{aquariumId}/include-events")`. Parameters: `@PathVariable Long aquariumId`, `@RequestParam boolean includeEvents`, `@RequestHeader AUTH_TOKEN String token`, `Principal principal`. Calls `publicReportService.updateIncludeEvents(...)`. Returns HTTP 200 on success, HTTP 403 on false return. Add OpenAPI `@Operation` and `@ApiResponses` annotations. Full code per plan.md §4.10.
  - **Files**: `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/PublicReportController.java` *(modify)*
  - **Depends on**: T-021
  - **AC**:
    - [X] Endpoint path is `/api/report/link/{aquariumId}/include-events` (consistent with existing `/api/report/link/...` endpoints)
    - [X] Returns `ResponseEntity.ok().build()` on success
    - [X] Returns `ResponseEntity.status(HttpStatus.FORBIDDEN).build()` when `updateIncludeEvents` returns false
    - [X] Requires valid JWT (not under `/api/public/**` bypass)

- [X] T-023 [P] [US3] Extend webclient `PublicReportService` + `PublicReportServiceImpl` with `updateIncludeEventsFlag`
  - **What**: (A) Add method signature `void updateIncludeEventsFlag(Long aquariumId, boolean includeEvents, String token) throws BusinessException` to webclient `PublicReportService` interface. (B) Implement in `PublicReportServiceImpl`: PUT to `sabiBackendUrl + Endpoint.REPORT_LINK.getPath() + "/" + aquariumId + "/include-events?includeEvents=" + includeEvents`. Use `RestHelper.prepareAuthedHttpHeader(token)`. Catch `HttpClientErrorException.Forbidden` → throw `BusinessException(AUTHORIZATION_EXCEPTION)`; catch `RestClientException` → throw `BusinessException(NETWORK_ERROR)`. Call `renewBackendToken`. Full code per plan.md §5.5.
  - **Files**:
    - `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/PublicReportService.java` *(modify)*
    - `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/PublicReportServiceImpl.java` *(modify)*
  - **Depends on**: T-004
  - **AC**:
    - [X] Uses `Endpoint.REPORT_LINK.getPath()` (existing constant, check current name in Endpoint.java)
    - [X] `HttpClientErrorException.Forbidden` is caught specifically (before generic `RestClientException`)
    - [X] No response body parsing needed (server returns `Void`)

- [X] T-024 [US3] Extend `UserProfileView` with `includeEventsMap` and `saveIncludeEvents(Long)`
  - **What**: Add to `UserProfileView.java`: (1) field `private Map<Long, Boolean> includeEventsMap = new LinkedHashMap<>()`, (2) in `init()` after `reportLinks.put(tank.getId(), link)` add `includeEventsMap.put(tank.getId(), link != null && link.isIncludeEvents())`, (3) new action method `public String saveIncludeEvents(Long tankId)` that reads `includeEventsMap.getOrDefault(tankId, false)`, calls `publicReportService.updateIncludeEventsFlag(tankId, value, ...)`, updates `reportLinks.get(tankId).setIncludeEvents(value)`, shows success/error via `MessageUtil`, returns `USER_PROFILE_VIEW_PAGE.getNavigationableAddress()`. Full code per plan.md §5.5.
  - **Files**: `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/UserProfileView.java` *(modify)*
  - **Depends on**: T-023
  - **AC**:
    - [X] `includeEventsMap` populated in `init()` — reflects current DB state on page load
    - [X] `saveIncludeEvents` uses `ajax="false"` (full page reload to confirm save — per spec FR-021)
    - [X] Null-safe: handles `link == null` in `init()` (tank with no report link)
    - [X] After save, `link.setIncludeEvents(value)` keeps in-memory state consistent

- [X] T-025 [US3] Add `includeEvents` checkbox + Save button to `userProfile.xhtml`
  - **What**: Inside `<h:form id="reportLinkRow_#{tank.id}">`, after the existing `validUntil` display row and before the "Link widerrufen" button, add a `<p:panelGrid columns="3" styleClass="ui-noborder">` containing: (1) `<p:outputLabel for="includeEventsCb_#{tank.id}" value="#{msg['aquariumevent.includeevents.label.l']}"/>`, (2) `<p:selectBooleanCheckbox id="includeEventsCb_#{tank.id}" value="#{userProfileView.includeEventsMap[tank.id]}"/>`, (3) `<p:commandButton value="#{msg['common.save.b']}" action="#{userProfileView.saveIncludeEvents(tank.id)}" ajax="false" styleClass="ui-button-primary"/>`. Rendered only when the report link row is rendered (no additional `rendered` condition needed as it is already inside the conditional block). Full XHTML per plan.md §5.7.
  - **Files**: `sabi-webclient/src/main/resources/META-INF/resources/secured/userProfile.xhtml` *(modify)*
  - **Depends on**: T-024
  - **AC**:
    - [X] Checkbox is inside the existing `reportLinkRow_#{tank.id}` form (no new surrounding form)
    - [X] Save button uses `ajax="false"` (full page reload per FR-021 and the spec clarification)
    - [X] Checkbox binds to `userProfileView.includeEventsMap[tank.id]` (map lookup syntax)
    - [X] No additional form wrapping (the existing form already wraps the row)

- [X] T-026 [US3] Add Events section to `houseReefReport.xhtml`
  - **What**: After the vitaldata charts section and before the closing `</div>` of `report-body`, add a `<ui:fragment rendered="#{houseReefReportView.report.recentEvents != null}">` block containing: section `<h2>` header, an empty-state paragraph `rendered="#{empty ...recentEvents}"`, and a `<ui:repeat>` over events each rendered as a card `<div>` showing date (`f:convertDateTime type="localDate"`), optional duration `(rendered="#{ev.durationHours != null}")`, and description with `white-space:pre-wrap`. The section is strictly read-only (no forms, no buttons). Colors per existing palette: `#075985` header, `#f0f9ff` card background. Full XHTML per plan.md §5.8.
  - **Files**: `sabi-webclient/src/main/resources/META-INF/resources/houseReefReport.xhtml` *(modify)*
  - **Depends on**: T-005, T-021
  - **AC**:
    - [X] Section is **not rendered** (`rendered="false"`) when `recentEvents == null` (opted-out)
    - [X] Section **is rendered** with empty-state message when `recentEvents` is an empty list
    - [X] No form, no commandButton, no input anywhere in the events section (read-only, FR-016)
    - [X] `f:convertDateTime type="localDate"` (not `date`) for `LocalDate` values
    - [X] Duration suffix uses i18n key `aquariumevent.report.hours.suffix.t`

**Checkpoint ✅ US3 complete**: Enable checkbox on profile → public report shows recent events only; disable → no events section.

---

## Phase 6: Polish — i18n Bundles

**Purpose**: Add all 25 new i18n keys to all 6 message bundle files. All 6 tasks are parallel (different files). These keys are referenced by the XHTML pages completed in Phases 3–5.

> **Key table** (from plan.md §5.9): `aquariumevent.panel.h`, `aquariumevent.field.date.l`, `aquariumevent.field.date.placeholder.t`, `aquariumevent.field.duration.l`, `aquariumevent.field.duration.placeholder.t`, `aquariumevent.field.description.l`, `aquariumevent.field.description.placeholder.t`, `aquariumevent.validation.date.required.t`, `aquariumevent.validation.description.required.t`, `aquariumevent.validation.duration.positive.t`, `aquariumevent.mode.newrecord.l`, `aquariumevent.mode.editrecord.l`, `aquariumevent.list.empty.t`, `aquariumevent.column.date.h`, `aquariumevent.column.duration.h`, `aquariumevent.column.description.h`, `aquariumevent.delete.confirm.header`, `aquariumevent.delete.confirm.message`, `aquariumevent.save.success.t`, `aquariumevent.delete.success.t`, `aquariumevent.includeevents.label.l`, `aquariumevent.includeevents.saved.t`, `aquariumevent.report.section.h`, `aquariumevent.report.no.events.t`, `aquariumevent.report.hours.suffix.t`. Plus `common.confirm.b` and `common.cancel.b` if not already present.

- [X] T-027 [P] Add all 25 i18n keys to `messages.properties` (fallback / EN values)
  - **What**: Append all keys from plan.md §5.9 key table using English values. `messages.properties` serves as fallback for all locales.
  - **Files**: `sabi-webclient/src/main/resources/i18n/messages.properties` *(modify)*
  - **Depends on**: T-014 (all XHTML pages that reference keys should be ready)
  - **AC**: All 25 keys (plus `common.confirm.b` / `common.cancel.b` if missing) present and non-empty

- [X] T-028 [P] Add all 25 i18n keys to `messages_de.properties` (German)
  - **What**: Append keys with German values from plan.md §5.9 DE column.
  - **Files**: `sabi-webclient/src/main/resources/i18n/messages_de.properties` *(modify)*
  - **Depends on**: T-027
  - **AC**: All 25 keys present; German text uses correct umlauts (ä, ö, ü, ß)

- [X] T-029 [P] Add all 25 i18n keys to `messages_en.properties` (English)
  - **What**: Append keys with English values (same as `messages.properties` fallback).
  - **Files**: `sabi-webclient/src/main/resources/i18n/messages_en.properties` *(modify)*
  - **Depends on**: T-027
  - **AC**: All 25 keys present; values match `messages.properties` exactly

- [X] T-030 [P] Add all 25 i18n keys to `messages_es.properties` (Spanish)
  - **What**: Append keys with Spanish translations (derive from EN/DE reference in plan.md §5.9; use consistent terminology with existing ES bundle entries).
  - **Files**: `sabi-webclient/src/main/resources/i18n/messages_es.properties` *(modify)*
  - **Depends on**: T-027
  - **AC**: All 25 keys present and non-empty; no untranslated EN placeholders left

- [X] T-031 [P] Add all 25 i18n keys to `messages_fr.properties` (French)
  - **What**: Append keys with French translations consistent with existing FR bundle style.
  - **Files**: `sabi-webclient/src/main/resources/i18n/messages_fr.properties` *(modify)*
  - **Depends on**: T-027
  - **AC**: All 25 keys present and non-empty

- [X] T-032 [P] Add all 25 i18n keys to `messages_it.properties` (Italian)
  - **What**: Append keys with Italian translations consistent with existing IT bundle style.
  - **Files**: `sabi-webclient/src/main/resources/i18n/messages_it.properties` *(modify)*
  - **Depends on**: T-027
  - **AC**: All 25 keys present and non-empty

---

## Phase 7: Tests

**Purpose**: Automated verification of the two security-critical behaviours (ownership enforcement and 12-month filter) and two service edge-cases.

- [X] T-033 [P] Integration test: `AquariumEventController` CRUD ownership (HTTP 201 / 403)
  - **What**: Create `AquariumEventControllerIT.java` using the existing Testcontainer + Spring Boot test setup (pattern from `TestContainerSampleTest`). Register two users (user A owns tank X). Test: (1) POST `/api/tank/{tankX.id}/events` as user A → assert HTTP 201 and body contains generated `id`. (2) POST same endpoint as user B → assert HTTP 403 and no event created. (3) PUT with stale `optlock` value → assert HTTP 409.
  - **Files**: `sabi-server/src/test/java/de/bluewhale/sabi/rest/AquariumEventControllerIT.java` *(create)*
  - **Depends on**: T-011, T-016
  - **AC**:
    - [X] Test uses `TestDataFactory` or equivalent helpers for user/tank setup
    - [X] All three assertions pass
    - [X] Test is annotated to be part of `IntegrationTestSuite`

- [X] T-034 [P] Integration test: 12-month event filter in `PublicReportServiceImpl`
  - **What**: Create or extend a service-level test. Insert two events for tank X: one with `eventDate = LocalDate.now().minusMonths(6)` (in window) and one with `eventDate = LocalDate.now().minusMonths(13)` (outside window). Set `includeEvents = true` on the report link. Call `publicReportService.getReport(linkToken)`. Assert `report.getRecentEvents()` contains exactly 1 event (the –6 month one). Assert the –13 month event is absent.
  - **Files**: `sabi-server/src/test/java/de/bluewhale/sabi/services/PublicReportServiceTest.java` *(create or modify — add test method)*
  - **Depends on**: T-021
  - **AC**:
    - [X] Exactly 1 event in `recentEvents`
    - [X] The present event's `eventDate` is `>=` cutoff (–365 days, not –12 months calendar)
    - [X] Test passes on both AMD64 and ARM64 (no platform-specific date handling)

- [X] T-035 [P] Unit test: `AquariumEventServiceImpl.createEvent()` with ownership failure
  - **What**: Mock `userRepository.findByEmail(...)` to return a valid user; mock `aquariumRepository.getAquariumEntityByIdAndUser_IdIs(...)` to return `null` (ownership mismatch). Call `createEvent(99L, eventTo, "user@test.com")`. Assert returned `ResultTo.getMessage().getType() == Message.CATEGORY.ERROR`. Assert `aquariumEventRepository.save(...)` was never called.
  - **Files**: `sabi-server/src/test/java/de/bluewhale/sabi/services/AquariumEventServiceTest.java` *(create)*
  - **Depends on**: T-010
  - **AC**:
    - [X] Uses Mockito (`@ExtendWith(MockitoExtension.class)`)
    - [X] `verify(aquariumEventRepository, never()).save(any())` assertion present
    - [X] Test is part of `ServiceTestSuite`

- [X] T-036 [P] Unit test: `PublicReportServiceImpl.updateIncludeEvents()` when no link exists
  - **What**: Mock `publicReportLinkRepository.findByAquariumId(...)` to return `Optional.empty()`. Call `updateIncludeEvents(1L, true, "user@test.com")`. Assert return value is `false`. Assert `publicReportLinkRepository.save(...)` was never called.
  - **Files**: `sabi-server/src/test/java/de/bluewhale/sabi/services/PublicReportServiceTest.java` *(create or modify — add test method)*
  - **Depends on**: T-021
  - **AC**:
    - [X] Method returns `false` (not throws exception)
    - [X] `verify(publicReportLinkRepository, never()).save(any())`
    - [X] Test is unit-isolated (no Spring context, no Testcontainer)

---

## Phase 8: ISO 25010 Quality Gates *(SABI-Konstitution v1.0.0)*

**Purpose**: Final checklist before merge to confirm all eight quality characteristics are satisfied.

- [ ] QGATE-I   **Funktionale Eignung**: All 21 FRs (FR-001 – FR-021) checked off against running application; US1/US2/US3 acceptance scenarios pass
- [ ] QGATE-II  **Leistungseffizienz**: Event list for a tank with 200 entries loads in < 1 s on ARM64 (local or CI environment); report assembly delta < 200 ms with `includeEvents=true`
- [ ] QGATE-III **Kompatibilität**: OpenAPI spec updated to reflect `AquariumEventController` endpoints and the new `PublicReportController` endpoint; no breaking changes to existing endpoints; both Flyway migrations apply on v1.5.x baseline
- [ ] QGATE-IV  **Benutzbarkeit**: All 25 new i18n keys non-empty in all 6 bundle files (T-027–T-032 complete); `p:datePicker` visually consistent with `measureView.xhtml`; WCAG 2.1 AA contrast validated for new UI elements (Sabi palette: `#075985`, `#0369a1`, `#1e293b`, `#64748b`)
- [ ] QGATE-V   **Zuverlässigkeit**: CI pipeline green (Maven build + CodeQL); optimistic locking test (T-033, HTTP 409 scenario) passes; `@Transactional` on all write methods confirmed
- [ ] QGATE-VI  **Sicherheit**: OWASP Dependency Check green (no new dependency added); T-033 HTTP 403 cross-user test passes; no event data reachable without valid JWT or valid share token; no secrets in code
- [ ] QGATE-VII **Wartbarkeit**: T-033 – T-036 all passing; Flyway migration files are immutable (not edited after commit); `AquariumEventEntity` extends `Auditable` confirmed; MapStruct generates cleanly
- [ ] QGATE-VIII **Übertragbarkeit**: Docker build succeeds on AMD64 and ARM64; no Ansible or Docker-Compose changes required (application-layer change only)

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Migrations)       — No dependencies; start immediately
Phase 2 (Boundary)         — No dependencies; can run in parallel with Phase 1
Phase 3 (US1)              — Requires Phase 1 (T-001) + Phase 2 (T-003, T-006) complete
Phase 4 (US2)              — Requires Phase 3 complete
Phase 5 (US3)              — Requires Phase 2 (T-004, T-005) + Phase 3 (T-008, T-009) complete;
                             can start after Phase 3 checkpoint, in parallel with Phase 4
Phase 6 (i18n)             — Requires Phases 3–5 XHTML tasks complete (keys must be known)
Phase 7 (Tests)            — Requires Phase 3 (T-033 unit), Phase 5 (T-034/T-036) complete
Phase 8 (QGates)           — Requires ALL phases complete
```

### User Story Task Dependencies

| Task | Depends on |
|---|---|
| T-003 | — |
| T-004 | — |
| T-005 | T-003 |
| T-006 | — |
| T-007 | T-001 |
| T-008 | T-007 |
| T-009 | T-003, T-007 |
| T-010 | T-003, T-008, T-009 |
| T-011 | T-010 |
| T-012 | T-003, T-006 |
| T-013 | T-012 |
| T-014 | T-013 |
| T-015 | T-010 |
| T-016 | T-011, T-015 |
| T-017 | T-012 |
| T-018 | T-013, T-017 |
| T-019 | T-014, T-018 |
| T-020 | T-002 |
| T-021 | T-003, T-005, T-008, T-009, T-020 |
| T-022 | T-021 |
| T-023 | T-004 |
| T-024 | T-023 |
| T-025 | T-024 |
| T-026 | T-005, T-021 |
| T-027 | T-014, T-019, T-025, T-026 (all XHTML) |
| T-028 to T-032 | T-027 |
| T-033 | T-011, T-016 |
| T-034 | T-021 |
| T-035 | T-010 |
| T-036 | T-021 |

---

## Parallel Execution Examples

### After Phase 1 + Phase 2 complete — start US1 back-end in parallel with US3 prep

```
Stream A (US1 server):      T-007 → T-008 → T-009 → T-010 → T-011
Stream B (US1 webclient):   T-012 → T-013 → T-014
Stream C (US3 server prep): T-020 (can start as soon as T-002 done)
```

### Within Phase 7 (Tests) — all four test tasks are independent

```
T-033 [P]  AquariumEventControllerIT  (integration, needs DB)
T-034 [P]  PublicReportServiceTest 12-month filter  (integration, needs DB)
T-035 [P]  AquariumEventServiceTest ownership unit  (Mockito, no DB)
T-036 [P]  PublicReportServiceTest no-link unit  (Mockito, no DB)
```

### Within Phase 6 (i18n) — all six bundle files are independent

```
T-027 [P]  messages.properties        T-030 [P]  messages_es.properties
T-028 [P]  messages_de.properties     T-031 [P]  messages_fr.properties
T-029 [P]  messages_en.properties     T-032 [P]  messages_it.properties
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. **Phase 1**: Write T-001 (aquarium_event migration)
2. **Phase 2**: Write T-003 (DTO), T-006 (Endpoint enum)
3. **Phase 3**: T-007 → T-011 (server stack), T-012 → T-014 (webclient stack)
4. **i18n subset**: Add US1 keys to `messages.properties` + `messages_de.properties` (enough for dev testing)
5. **🛑 Validate**: Log in → create event → event appears in list → US1 done
6. Proceed to US2 (Phase 4), then US3 (Phase 5), then full i18n (Phase 6)

### Incremental Delivery

| Milestone | Tasks completed | Demonstrable capability |
|---|---|---|
| MVP | Phases 1–3 | Create + view events per tank |
| US2 | + Phase 4 | Full CRUD (edit + delete with confirmation) |
| US3 | + Phase 5 | Opt-in public report events section |
| Full i18n | + Phase 6 | All 6 locales complete |
| Release-ready | + Phases 7–8 | Tests green, QGates passed |

---

## Notes

- **No schema prefix** in Flyway migrations (MariaDB connects to `sabi` DB directly; schema attribute omitted)
- **Jakarta EE 9+** namespace throughout (`jakarta.*` not `javax.*`)
- **Ownership check chain**: JWT → `userRepository.findByEmail(principal.getName())` → `aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId())` → `AquariumEventEntity.aquariumId` match
- **`common.confirm.b` / `common.cancel.b`**: Check existing bundles before adding; skip if already present (plan.md §5.9 note)
- **Optimistic locking**: `optlock` field in `AquariumEventTo` must be sent back on PUT; server returns HTTP 409 on conflict
- **Public report events**: Never via a dedicated public endpoint — always assembled server-side inside `PublicReportServiceImpl.getReport()` and returned as part of `PublicReefReportTo.recentEvents`
- **Commit cadence**: Commit after each completed task; squash-merge per story branch before PR

