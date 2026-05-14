# Implementation Plan: Aquarium Event Logbook

**Feature Branch**: `004-aquarium-events`  
**Spec**: `specs/004-aquarium-events/spec.md`  
**Plan Created**: 2026-06-05  
**Status**: Ready for Implementation

---

## 1. Architecture Overview

### Modules Touched

| Module | Role |
|---|---|
| `sabi-database` | Two new Flyway migrations (version1_6_0) |
| `sabi-boundary` | New `AquariumEventTo` DTO; extend `PublicReportLinkTo`, `PublicReefReportTo`; new `TANK_EVENTS` Endpoint entry |
| `sabi-server` | New entity, repository, mapper, service, controller; extend `PublicReportLinkEntity`, `PublicReportService/Impl`, `PublicReportController` |
| `sabi-webclient` | New `AquariumEventView` CDI bean; new `AquariumEventService` API gateway interface+impl; extend `UserProfileView`; modify `tankView.xhtml`, `userProfile.xhtml`, `houseReefReport.xhtml`; add i18n keys to all 6 bundles |

### New Files to Create

```
sabi-database/src/main/resources/db/migration/version1_6_0/
  V1_6_0_1__addAquariumEventTable.sql
  V1_6_0_2__addIncludeEventsToPublicReportLink.sql

sabi-boundary/src/main/java/de/bluewhale/sabi/model/
  AquariumEventTo.java

sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/
  AquariumEventEntity.java

sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/
  AquariumEventRepository.java

sabi-server/src/main/java/de/bluewhale/sabi/mapper/
  AquariumEventMapper.java

sabi-server/src/main/java/de/bluewhale/sabi/services/
  AquariumEventService.java
  AquariumEventServiceImpl.java

sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/
  AquariumEventController.java

sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/
  AquariumEventService.java
  AquariumEventServiceImpl.java

sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/
  AquariumEventView.java
```

### Files to Modify

| File | Change |
|---|---|
| `sabi-boundary/.../model/PublicReportLinkTo.java` | Add `includeEvents` boolean field (default `false`) |
| `sabi-boundary/.../model/PublicReefReportTo.java` | Add `recentEvents` field: `List<AquariumEventTo>` (null when not included) |
| `sabi-boundary/.../api/Endpoint.java` | Add `TANK_EVENTS("/api/tank")` entry |
| `sabi-server/.../model/PublicReportLinkEntity.java` | Add `includeEvents` boolean column mapping |
| `sabi-server/.../services/PublicReportService.java` | Add `updateIncludeEvents()` method signature |
| `sabi-server/.../services/PublicReportServiceImpl.java` | Implement `updateIncludeEvents()`; inject `AquariumEventRepository`; populate `recentEvents` in `getReport()` |
| `sabi-server/.../rest/controller/PublicReportController.java` | Add `PUT /api/report/link/{aquariumId}/include-events` endpoint |
| `sabi-webclient/.../apigateway/PublicReportService.java` | Add `updateIncludeEvents()` method signature |
| `sabi-webclient/.../apigateway/PublicReportServiceImpl.java` | Implement `updateIncludeEvents()` REST call |
| `sabi-webclient/.../controller/UserProfileView.java` | Add `saveIncludeEvents(Long tankId)` action and `includeEventsMap` |
| `sabi-webclient/.../resources/secured/tankView.xhtml` | Add Events panel (form + list) per tank |
| `sabi-webclient/.../resources/secured/userProfile.xhtml` | Add `includeEvents` checkbox + Save button to `reportLinkRow_#{tank.id}` form |
| `sabi-webclient/.../resources/houseReefReport.xhtml` | Add Events section (rendered conditionally on `report.recentEvents != null`) |
| `sabi-webclient/.../resources/i18n/messages.properties` | Add all new i18n keys (fallback) |
| `sabi-webclient/.../resources/i18n/messages_de.properties` | Add all new i18n keys (German) |
| `sabi-webclient/.../resources/i18n/messages_en.properties` | Add all new i18n keys (English) |
| `sabi-webclient/.../resources/i18n/messages_es.properties` | Add all new i18n keys (Spanish) |
| `sabi-webclient/.../resources/i18n/messages_fr.properties` | Add all new i18n keys (French) |
| `sabi-webclient/.../resources/i18n/messages_it.properties` | Add all new i18n keys (Italian) |

---

## 2. Database Layer

### Migration 1 — New `aquarium_event` table

**File**: `sabi-database/src/main/resources/db/migration/version1_6_0/V1_6_0_1__addAquariumEventTable.sql`

```sql
-- Aquarium Event Logbook: free-form events per aquarium (feature 004-aquarium-events).
-- Each event captures a mandatory date, optional duration in hours, and mandatory description.
-- Sorted newest-first for authenticated logbook display (eventDate DESC).

CREATE TABLE `aquarium_event`
(
    `id`             BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `aquarium_id`    BIGINT(20) UNSIGNED NOT NULL,
    `event_date`     DATE                NOT NULL                    COMMENT 'Calendar date the event occurred (no time component)',
    `duration_hours` DECIMAL(6,2)        NULL                        COMMENT 'Optional duration in hours; must be > 0 when present',
    `description`    TEXT                NOT NULL                    COMMENT 'Free-form multi-line description; line breaks preserved',
    `created_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `lastmod_on`     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `optlock`        INT UNSIGNED        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_aquarium_event_aquarium_date` (`aquarium_id`, `event_date`),
    CONSTRAINT `fk_aquarium_event_aquarium`
        FOREIGN KEY (`aquarium_id`) REFERENCES `aquarium` (`id`)
        ON DELETE CASCADE
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8
    COMMENT = 'Free-form logbook events per aquarium; feature 004-aquarium-events';
```

### Migration 2 — Add `include_events` column to `public_report_link`

**File**: `sabi-database/src/main/resources/db/migration/version1_6_0/V1_6_0_2__addIncludeEventsToPublicReportLink.sql`

```sql
-- Adds opt-in flag for including aquarium events in the public HouseReef Report.
-- Defaults to FALSE (0) for all existing and new report links, so no existing report is affected.

ALTER TABLE `public_report_link`
    ADD COLUMN `include_events` TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'When 1, events (past 365 days) are included in the public report';
```

---

## 3. Boundary Layer

### 3.1 New DTO: `AquariumEventTo`

**File**: `sabi-boundary/src/main/java/de/bluewhale/sabi/model/AquariumEventTo.java`

```java
package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transfer object for a single aquarium logbook event.
 * Used by the authenticated CRUD API and by public report assembly.
 * Feature: 004-aquarium-events.
 */
@Data
public class AquariumEventTo implements Serializable {

    @Schema(description = "Internal PK, null when creating a new event.")
    private Long id;

    @NotNull
    @Schema(description = "ID of the aquarium this event belongs to.", required = true)
    private Long aquariumId;

    @NotNull
    @Schema(description = "Calendar date the event occurred (no time-of-day).", required = true)
    private LocalDate eventDate;

    @Positive
    @Schema(description = "Optional duration in hours; must be > 0 when present.", required = false)
    private BigDecimal durationHours;

    @NotNull
    @Schema(description = "Free-form multi-line description. Line breaks are preserved.", required = true)
    private String description;

    @Schema(description = "Timestamp when this record was created (server-set; ignored on POST/PUT).")
    private LocalDateTime createdOn;

    @Schema(description = "Timestamp of last modification (server-set; ignored on POST/PUT).")
    private LocalDateTime updatedOn;   // maps to DB column lastmod_on via entity

    @Schema(description = "Optimistic lock version (server-set; must be sent back on PUT to detect concurrent edits).")
    private long optlock;
}
```

### 3.2 Extended DTO: `PublicReportLinkTo`

Add one field to the existing class:

```java
// In PublicReportLinkTo.java — add after the createdOn field:
@Schema(description = "When true, events from the past 365 days are included in the public report.", required = false)
private boolean includeEvents = false;
```

### 3.3 Extended DTO: `PublicReefReportTo`

Add one field to the existing class:

```java
// In PublicReefReportTo.java — add after measurementsByUnit:
@Schema(description = "Events from the past 365 days; null when includeEvents = false for this report link.")
private List<AquariumEventTo> recentEvents;   // null = not opted-in; empty list = opted-in but no events
```

### 3.4 New Endpoint entry

Add to `sabi-boundary/src/main/java/de/bluewhale/sabi/api/Endpoint.java`:

```java
// 004-aquarium-events (shares base path with TankController)
TANK_EVENTS("/api/tank"),
```

---

## 4. Backend Layer (sabi-server)

### 4.1 Entity: `AquariumEventEntity`

**File**: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/model/AquariumEventEntity.java`  
**Package**: `de.bluewhale.sabi.persistence.model`

```java
@Table(name = "aquarium_event", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class AquariumEventEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    /** FK to aquarium.id — stored as plain Long (no JPA association object needed). */
    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "event_date", nullable = false)
    @Basic
    private LocalDate eventDate;

    /** Optional; positive decimal. Validated at controller level before persist. */
    @Column(name = "duration_hours", nullable = true, precision = 6, scale = 2)
    @Basic
    private BigDecimal durationHours;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @Basic
    private String description;
}
```

**Imports needed**: `jakarta.persistence.*`, `lombok.Data`, `lombok.EqualsAndHashCode`, `java.math.BigDecimal`, `java.time.LocalDate`

### 4.2 Extension: `PublicReportLinkEntity`

Add one field mapping:

```java
// In PublicReportLinkEntity.java — add after validUntil:
@Column(name = "include_events", nullable = false)
@Basic
private boolean includeEvents = false;
```

### 4.3 Repository: `AquariumEventRepository`

**File**: `sabi-server/src/main/java/de/bluewhale/sabi/persistence/repositories/AquariumEventRepository.java`  
**Package**: `de.bluewhale.sabi.persistence.repositories`

```java
public interface AquariumEventRepository extends JpaRepository<AquariumEventEntity, Long> {

    /**
     * Returns all events for a tank, newest first.
     * Used by the authenticated logbook view (no date filter).
     */
    @NotNull
    List<AquariumEventEntity> findByAquariumIdOrderByEventDateDesc(@NotNull Long aquariumId);

    /**
     * Returns events for a tank on or after the given cutoff date, newest first.
     * Used by the public report assembly (rolling 365-day window, FR-014).
     */
    @NotNull
    List<AquariumEventEntity> findByAquariumIdAndEventDateGreaterThanEqualOrderByEventDateDesc(
            @NotNull Long aquariumId, @NotNull LocalDate cutoff);

    /**
     * Ownership-safe single-event lookup.
     * Returns empty if the event does not exist or belongs to a different aquarium.
     * Used before update and delete to enforce FR-009.
     */
    Optional<AquariumEventEntity> findByIdAndAquariumId(@NotNull Long id, @NotNull Long aquariumId);
}
```

**Imports**: `org.springframework.data.jpa.repository.JpaRepository`, `jakarta.validation.constraints.NotNull`, `java.time.LocalDate`, `java.util.List`, `java.util.Optional`

### 4.4 Mapper: `AquariumEventMapper`

**File**: `sabi-server/src/main/java/de/bluewhale/sabi/mapper/AquariumEventMapper.java`  
**Package**: `de.bluewhale.sabi.mapper`

```java
@Mapper(componentModel = "spring", uses = {MappingUtils.class})
public interface AquariumEventMapper {

    @Mappings({
        @Mapping(target = "id",           source = "id"),
        @Mapping(target = "aquariumId",   source = "aquariumId"),
        @Mapping(target = "eventDate",    source = "eventDate"),
        @Mapping(target = "durationHours",source = "durationHours"),
        @Mapping(target = "description",  source = "description"),
        @Mapping(target = "createdOn",    source = "createdOn"),
        @Mapping(target = "updatedOn",    source = "lastmodOn"),   // lastmodOn → updatedOn
        @Mapping(target = "optlock",      source = "optlock"),
    })
    AquariumEventTo mapEntityToTo(@NotNull AquariumEventEntity entity);

    List<AquariumEventTo> mapEntitiesToTos(@NotNull List<AquariumEventEntity> entities);

    @Mappings({
        @Mapping(target = "id",           source = "id"),
        @Mapping(target = "aquariumId",   source = "aquariumId"),
        @Mapping(target = "eventDate",    source = "eventDate"),
        @Mapping(target = "durationHours",source = "durationHours"),
        @Mapping(target = "description",  source = "description"),
    })
    AquariumEventEntity mapToToEntity(@NotNull AquariumEventTo to);

    @Mappings({
        @Mapping(target = "eventDate",    source = "eventDate"),
        @Mapping(target = "durationHours",source = "durationHours"),
        @Mapping(target = "description",  source = "description"),
        @Mapping(target = "id",           ignore = true),
        @Mapping(target = "aquariumId",   ignore = true),
    })
    void mergeToIntoEntity(@NotNull AquariumEventTo to,
                           @NotNull @MappingTarget AquariumEventEntity entity);
}
```

### 4.5 Service Interface: `AquariumEventService`

**File**: `sabi-server/src/main/java/de/bluewhale/sabi/services/AquariumEventService.java`  
**Package**: `de.bluewhale.sabi.services`

```java
public interface AquariumEventService {

    /**
     * Returns all events for the given tank (newest first).
     * Verifies that aquariumId belongs to userEmail before returning data; returns empty list on mismatch.
     *
     * @param aquariumId  tank PK
     * @param userEmail   authenticated user (ownership check)
     * @return list of events, never null
     */
    @NotNull
    List<AquariumEventTo> listEventsForTank(@NotNull Long aquariumId, @NotNull String userEmail);

    /**
     * Creates a new event for the given tank.
     * Returns HTTP-relevant result; the aquarium ownership is verified before insert.
     *
     * @param aquariumId  tank PK
     * @param eventTo     event data (id should be null)
     * @param userEmail   authenticated user
     * @return ResultTo wrapping the persisted DTO with its generated id on INFO, ERROR on ownership fail
     */
    @Transactional
    @NotNull
    ResultTo<AquariumEventTo> createEvent(@NotNull Long aquariumId,
                                          @NotNull AquariumEventTo eventTo,
                                          @NotNull String userEmail);

    /**
     * Updates an existing event.
     * Verifies: (a) event.aquariumId == aquariumId, (b) aquarium belongs to userEmail.
     *
     * @param aquariumId  tank PK
     * @param eventId     event PK
     * @param eventTo     updated data (must include optlock for optimistic locking)
     * @param userEmail   authenticated user
     * @return ResultTo wrapping the updated DTO on INFO, ERROR on ownership/not-found
     */
    @Transactional
    @NotNull
    ResultTo<AquariumEventTo> updateEvent(@NotNull Long aquariumId,
                                          @NotNull Long eventId,
                                          @NotNull AquariumEventTo eventTo,
                                          @NotNull String userEmail);

    /**
     * Permanently deletes an event.
     * Verifies: (a) event.aquariumId == aquariumId, (b) aquarium belongs to userEmail.
     *
     * @param aquariumId  tank PK
     * @param eventId     event PK
     * @param userEmail   authenticated user
     * @return INFO on success, ERROR on ownership/not-found
     */
    @Transactional
    @NotNull
    ResultTo<AquariumEventTo> deleteEvent(@NotNull Long aquariumId,
                                          @NotNull Long eventId,
                                          @NotNull String userEmail);
}
```

### 4.6 Service Implementation: `AquariumEventServiceImpl`

**File**: `sabi-server/src/main/java/de/bluewhale/sabi/services/AquariumEventServiceImpl.java`  
**Package**: `de.bluewhale.sabi.services`  
**Annotation**: `@Service @Slf4j @Transactional(readOnly = true)`

**Key fields**:
```java
@Autowired AquariumEventRepository aquariumEventRepository;
@Autowired AquariumRepository aquariumRepository;
@Autowired UserRepository userRepository;
@Autowired AquariumEventMapper aquariumEventMapper;
```

**Ownership check helper** (reuse the pattern from `PublicReportServiceImpl`):
```java
/**
 * Returns the resolved AquariumEntity if it belongs to the user, null otherwise.
 * Resolves user by email first, then checks aquarium owner FK.
 */
private AquariumEntity resolveOwnedAquarium(Long aquariumId, String userEmail) {
    UserEntity user = userRepository.findByEmail(userEmail);
    if (user == null) return null;
    return aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
}
```

**`listEventsForTank`**:
```java
// 1. resolveOwnedAquarium; return emptyList on null
// 2. aquariumEventRepository.findByAquariumIdOrderByEventDateDesc(aquariumId)
// 3. map to TOs and return
```

**`createEvent`**:
```java
// @Transactional (override readOnly)
// 1. resolveOwnedAquarium; return ERROR ResultTo on null
// 2. map eventTo → entity, set aquariumId, clear id
// 3. save entity
// 4. map saved entity → TO, wrap in ResultTo.INFO
```

**`updateEvent`**:
```java
// @Transactional
// 1. resolveOwnedAquarium; return ERROR on null
// 2. aquariumEventRepository.findByIdAndAquariumId(eventId, aquariumId) → empty? ERROR
// 3. mergeToIntoEntity(eventTo, existingEntity)
// 4. save (JPA optimistic lock will throw ObjectOptimisticLockingFailureException on conflict)
// 5. wrap updated entity in ResultTo.INFO
```

**`deleteEvent`**:
```java
// @Transactional
// 1. resolveOwnedAquarium; return ERROR on null
// 2. aquariumEventRepository.findByIdAndAquariumId(eventId, aquariumId) → empty? ERROR
// 3. aquariumEventRepository.delete(entity)
// 4. return ResultTo.INFO
```

**Note on optimistic locking**: If concurrent edit is detected in `updateEvent`, let `ObjectOptimisticLockingFailureException` propagate to the controller, which maps it to HTTP 409.

### 4.7 Extension to `PublicReportService` interface

Add to `sabi-server/src/main/java/de/bluewhale/sabi/services/PublicReportService.java`:

```java
/**
 * Persists the includeEvents flag for an existing report link.
 * Verifies that the aquarium belongs to the user before updating.
 *
 * @param aquariumId    tank PK
 * @param includeEvents new flag value
 * @param userEmail     authenticated user email
 * @return true on success, false if aquarium/link does not exist or belongs to another user
 */
@Transactional
boolean updateIncludeEvents(@NotNull Long aquariumId, boolean includeEvents, @NotNull String userEmail);
```

### 4.8 Extension to `PublicReportServiceImpl`

Add these changes to `sabi-server/src/main/java/de/bluewhale/sabi/services/PublicReportServiceImpl.java`:

```java
// NEW field injection:
@Autowired
private AquariumEventRepository aquariumEventRepository;

@Autowired
private AquariumEventMapper aquariumEventMapper;

// NEW method:
@Override
@Transactional
public boolean updateIncludeEvents(Long aquariumId, boolean includeEvents, String userEmail) {
    UserEntity user = userRepository.findByEmail(userEmail);
    if (user == null) return false;
    AquariumEntity aquarium = aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId());
    if (aquarium == null) return false;
    Optional<PublicReportLinkEntity> linkOpt = publicReportLinkRepository.findByAquariumId(aquariumId);
    if (linkOpt.isEmpty()) return false;
    PublicReportLinkEntity link = linkOpt.get();
    link.setIncludeEvents(includeEvents);
    publicReportLinkRepository.save(link);
    return true;
}

// ADD inside getReport(), after existing inhabitants/measurement assembly:
if (link.isIncludeEvents()) {
    LocalDate cutoff = LocalDate.now().minusDays(365);
    List<AquariumEventEntity> eventEntities =
        aquariumEventRepository.findByAquariumIdAndEventDateGreaterThanEqualOrderByEventDateDesc(
            link.getAquariumId(), cutoff);
    report.setRecentEvents(aquariumEventMapper.mapEntitiesToTos(eventEntities));
}
// else: recentEvents stays null (not opted-in)
```

**Conversion of link entity to DTO** — also add `includeEvents` in `mapLinkEntityToTo` helper (or add it manually in `getLinkForTank`):
```java
PublicReportLinkTo to = new PublicReportLinkTo();
to.setId(entity.getId());
to.setAquariumId(entity.getAquariumId());
to.setLinkToken(entity.getLinkToken());
to.setValidUntil(entity.getValidUntil());
to.setCreatedOn(entity.getCreatedOn());
to.setIncludeEvents(entity.isIncludeEvents());   // NEW: propagate flag to gateway
```

### 4.9 New REST Controller: `AquariumEventController`

**File**: `sabi-server/src/main/java/de/bluewhale/sabi/rest/controller/AquariumEventController.java`  
**Package**: `de.bluewhale.sabi.rest.controller`

```java
@RestController
@RequestMapping("api/tank")
@Slf4j
public class AquariumEventController {

    @Autowired AquariumEventService aquariumEventService;
    @Autowired TankService tankService;   // for ownership pre-check consistent with MeasurementController pattern

    // GET /api/tank/{tankId}/events
    @GetMapping(value = "/{tankId}/events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<AquariumEventTo>> listEvents(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId,
            Principal principal) {
        // Ownership verified inside service
        List<AquariumEventTo> events = aquariumEventService.listEventsForTank(tankId, principal.getName());
        return ResponseEntity.ok(events);
    }

    // POST /api/tank/{tankId}/events  → HTTP 201
    @PostMapping(value = "/{tankId}/events",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AquariumEventTo> createEvent(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId,
            @RequestBody @Valid AquariumEventTo eventTo,
            Principal principal) {
        ResultTo<AquariumEventTo> result = aquariumEventService.createEvent(tankId, eventTo, principal.getName());
        if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
            log.warn("createEvent rejected for tank {} / user {}: {}", tankId, principal.getName(), result.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.getValue());
    }

    // PUT /api/tank/{tankId}/events/{eventId}  → HTTP 200
    @PutMapping(value = "/{tankId}/events/{eventId}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AquariumEventTo> updateEvent(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId,
            @PathVariable Long eventId,
            @RequestBody @Valid AquariumEventTo eventTo,
            Principal principal) {
        try {
            ResultTo<AquariumEventTo> result = aquariumEventService.updateEvent(tankId, eventId, eventTo, principal.getName());
            if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(result.getValue());
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict updating event {} in tank {}", eventId, tankId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // DELETE /api/tank/{tankId}/events/{eventId}  → HTTP 200
    @DeleteMapping(value = "/{tankId}/events/{eventId}",
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> deleteEvent(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId,
            @PathVariable Long eventId,
            Principal principal) {
        ResultTo<AquariumEventTo> result = aquariumEventService.deleteEvent(tankId, eventId, principal.getName());
        if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().build();
    }
}
```

**Additional imports**: `jakarta.validation.Valid`, `org.springframework.orm.ObjectOptimisticLockingFailureException`

### 4.10 Extension to `PublicReportController`

Add new endpoint inside existing `PublicReportController.java`:

```java
// PUT /api/report/link/{aquariumId}/include-events
@Operation(summary = "Set or clear the includeEvents flag for the active report link of the given aquarium.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Flag updated."),
    @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or no link exists."),
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
})
@PutMapping(value = "/api/report/link/{aquariumId}/include-events")
public ResponseEntity<Void> updateIncludeEvents(
        @PathVariable Long aquariumId,
        @RequestParam(value = "includeEvents") boolean includeEvents,
        @RequestHeader(name = AUTH_TOKEN, required = true) String token,
        Principal principal) {
    log.debug("PUT /api/report/link/{}/include-events?includeEvents={} for {}", aquariumId, includeEvents, principal.getName());
    boolean success = publicReportService.updateIncludeEvents(aquariumId, includeEvents, principal.getName());
    return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
}
```

---

## 5. Frontend Layer (sabi-webclient)

### 5.1 New Endpoint entry (`Endpoint.java`)

```java
// 004-aquarium-events
TANK_EVENTS("/api/tank"),                               // base path — add /{tankId}/events at call site
REPORT_LINK_INCLUDE_EVENTS("/api/report/link"),         // add /{aquariumId}/include-events at call site
```

Alternatively, `TANK_EVENTS` can reuse the existing `TANKS` entry; call-site code simply appends `"/" + tankId + "/events"`.

### 5.2 New API Gateway Interface: `AquariumEventService`

**File**: `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/AquariumEventService.java`

```java
public interface AquariumEventService extends Serializable {

    List<AquariumEventTo> listEventsForTank(Long aquariumId, String token) throws BusinessException;

    AquariumEventTo createEvent(Long aquariumId, AquariumEventTo event, String token) throws BusinessException;

    AquariumEventTo updateEvent(Long aquariumId, Long eventId, AquariumEventTo event, String token)
            throws BusinessException;

    void deleteEvent(Long aquariumId, Long eventId, String token) throws BusinessException;
}
```

### 5.3 API Gateway Implementation: `AquariumEventServiceImpl`

**File**: `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/apigateway/AquariumEventServiceImpl.java`  
**Extends**: `APIServiceImpl`  
**Annotations**: `@Named @RequestScope @Slf4j`

```java
@Override
public List<AquariumEventTo> listEventsForTank(Long aquariumId, String token) throws BusinessException {
    String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events";
    ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
    try {
        AquariumEventTo[] items = objectMapper.readValue(response.getBody(), AquariumEventTo[].class);
        return Arrays.asList(items);
    } catch (JacksonException e) {
        log.error("Failed to parse event list from {}", uri, e);
        throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
    }
}

@Override
public AquariumEventTo createEvent(Long aquariumId, AquariumEventTo event, String token) throws BusinessException {
    String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events";
    RestTemplate restTemplate = new RestTemplate();
    try {
        String body = objectMapper.writeValueAsString(event);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
        renewBackendToken(response);
        return objectMapper.readValue(response.getBody(), AquariumEventTo.class);
    } catch (RestClientException | JacksonException e) {
        log.error("Failed to create event for aquarium {}", aquariumId, e);
        throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
    }
}

@Override
public AquariumEventTo updateEvent(Long aquariumId, Long eventId, AquariumEventTo event, String token) throws BusinessException {
    String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events/" + eventId;
    RestTemplate restTemplate = new RestTemplate();
    try {
        String body = objectMapper.writeValueAsString(event);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, request, String.class);
        renewBackendToken(response);
        return objectMapper.readValue(response.getBody(), AquariumEventTo.class);
    } catch (RestClientException | JacksonException e) {
        log.error("Failed to update event {} for aquarium {}", eventId, aquariumId, e);
        throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
    }
}

@Override
public void deleteEvent(Long aquariumId, Long eventId, String token) throws BusinessException {
    String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events/" + eventId;
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
    HttpEntity<String> request = new HttpEntity<>(headers);
    try {
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, request, String.class);
        renewBackendToken(response);
    } catch (RestClientException e) {
        log.error("Failed to delete event {} for aquarium {}", eventId, aquariumId, e);
        throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
    }
}
```

### 5.4 New CDI Bean: `AquariumEventView`

**File**: `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/AquariumEventView.java`  
**Annotations**: `@Named @RequestScope @Slf4j @Getter @Setter`  
**Implements**: `Serializable`

```java
@Named
@RequestScope
@Slf4j
@Getter
@Setter
public class AquariumEventView implements Serializable {

    @Inject UserSession userSession;
    @Inject TankListView tankListView;           // to get the currently iterated tank
    @Autowired AquariumEventService aquariumEventService;

    /** Map: aquariumId → list of events (loaded lazily per tank panel). */
    private Map<Long, List<AquariumEventTo>> eventsByTank = new LinkedHashMap<>();

    /** The event currently being edited in the inline form. One shared instance per view (per-tank forms use hidden id). */
    private Map<Long, AquariumEventTo> editFormByTank = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // Pre-load events for all active tanks so the view renders in one pass.
        List<AquariumTo> activeTanks = tankListView.getTanks();
        if (activeTanks == null) return;
        for (AquariumTo tank : activeTanks) {
            try {
                List<AquariumEventTo> events =
                    aquariumEventService.listEventsForTank(tank.getId(), userSession.getSabiBackendToken());
                eventsByTank.put(tank.getId(), events);
                editFormByTank.put(tank.getId(), new AquariumEventTo());   // blank "new" form
            } catch (BusinessException e) {
                log.error("Could not load events for tank {}: {}", tank.getId(), e.getMessage());
                eventsByTank.put(tank.getId(), new ArrayList<>());
                editFormByTank.put(tank.getId(), new AquariumEventTo());
            }
        }
    }

    /** Returns events for the given tank (newest first). */
    public List<AquariumEventTo> getEventsForTank(Long aquariumId) {
        return eventsByTank.getOrDefault(aquariumId, Collections.emptyList());
    }

    /** Returns the active edit form for the given tank. */
    public AquariumEventTo getEditFormForTank(Long aquariumId) {
        return editFormByTank.computeIfAbsent(aquariumId, id -> new AquariumEventTo());
    }

    /**
     * Saves the event form for the given tank (create or update based on id == null).
     * Called by the Save button in the tank's event form.
     */
    public void saveEvent(Long aquariumId) {
        AquariumEventTo form = getEditFormForTank(aquariumId);
        form.setAquariumId(aquariumId);
        try {
            if (form.getId() == null) {
                AquariumEventTo created = aquariumEventService.createEvent(
                    aquariumId, form, userSession.getSabiBackendToken());
                // prepend to top of list
                eventsByTank.getOrDefault(aquariumId, new ArrayList<>()).add(0, created);
            } else {
                AquariumEventTo updated = aquariumEventService.updateEvent(
                    aquariumId, form.getId(), form, userSession.getSabiBackendToken());
                // replace in list
                List<AquariumEventTo> list = eventsByTank.get(aquariumId);
                if (list != null) {
                    list.replaceAll(e -> e.getId().equals(updated.getId()) ? updated : e);
                }
            }
            editFormByTank.put(aquariumId, new AquariumEventTo());   // reset form
            MessageUtil.info("aquariumEventMessages_" + aquariumId,
                             "aquariumevent.save.success.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not save event for tank {}: {}", aquariumId, e.getMessage());
            MessageUtil.error("aquariumEventMessages_" + aquariumId,
                              "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    /**
     * Populates the edit form for the given tank/event (called from edit icon).
     */
    public void startEditEvent(Long aquariumId, AquariumEventTo event) {
        AquariumEventTo copy = new AquariumEventTo();
        // copy all fields so the form binds to a separate instance
        copy.setId(event.getId());
        copy.setAquariumId(event.getAquariumId());
        copy.setEventDate(event.getEventDate());
        copy.setDurationHours(event.getDurationHours());
        copy.setDescription(event.getDescription());
        copy.setOptlock(event.getOptlock());
        editFormByTank.put(aquariumId, copy);
    }

    /**
     * Permanently deletes the given event (called from confirm button in p:confirmDialog).
     */
    public void deleteEvent(Long aquariumId, Long eventId) {
        try {
            aquariumEventService.deleteEvent(aquariumId, eventId, userSession.getSabiBackendToken());
            List<AquariumEventTo> list = eventsByTank.get(aquariumId);
            if (list != null) list.removeIf(e -> e.getId().equals(eventId));
            MessageUtil.info("aquariumEventMessages_" + aquariumId,
                             "aquariumevent.delete.success.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not delete event {} for tank {}: {}", eventId, aquariumId, e.getMessage());
            MessageUtil.error("aquariumEventMessages_" + aquariumId,
                              "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    /** Resets the edit form for the given tank (New Entry button). */
    public void resetForm(Long aquariumId) {
        editFormByTank.put(aquariumId, new AquariumEventTo());
    }
}
```

### 5.5 Extension to `UserProfileView`

Add to `sabi-webclient/src/main/java/de/bluewhale/sabi/webclient/controller/UserProfileView.java`:

**New field**:
```java
/**
 * Map: aquariumId → current includeEvents flag value (bound to checkbox in UI).
 * Populated in init() from the fetched PublicReportLinkTo.includeEvents.
 */
private Map<Long, Boolean> includeEventsMap = new LinkedHashMap<>();
```

**In `init()`**, after `reportLinks.put(tank.getId(), link)`:
```java
includeEventsMap.put(tank.getId(),
    link != null && link.isIncludeEvents());
```

**New action method**:
```java
/**
 * FR-021: Persists the includeEvents flag for the report link of the given tank.
 * Called by the explicit Save button in reportLinkRow_#{tank.id}.
 */
public String saveIncludeEvents(Long tankId) {
    Boolean value = includeEventsMap.getOrDefault(tankId, false);
    try {
        publicReportService.updateIncludeEventsFlag(tankId, value, userSession.getSabiBackendToken());
        // keep reportLinks in sync
        PublicReportLinkTo link = reportLinks.get(tankId);
        if (link != null) link.setIncludeEvents(value);
        MessageUtil.info("messages", "aquariumevent.includeevents.saved.t", userSession.getLocale());
    } catch (BusinessException e) {
        log.error("Could not save includeEvents for tank {}: {}", tankId, e.getMessage());
        MessageUtil.error("messages", "common.error.internal_server_problem.t", userSession.getLocale());
    }
    return USER_PROFILE_VIEW_PAGE.getNavigationableAddress();
}
```

**Also add `updateIncludeEventsFlag` to the webclient `PublicReportService` interface and `PublicReportServiceImpl`**:
```java
// Interface addition:
void updateIncludeEventsFlag(@NotNull Long aquariumId, boolean includeEvents, @NotNull String token)
        throws BusinessException;

// Impl (in PublicReportServiceImpl):
@Override
public void updateIncludeEventsFlag(Long aquariumId, boolean includeEvents, String token) throws BusinessException {
    String uri = sabiBackendUrl + Endpoint.REPORT_LINK.getPath() + "/" + aquariumId
                 + "/include-events?includeEvents=" + includeEvents;
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
    HttpEntity<String> request = new HttpEntity<>(headers);
    try {
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, request, String.class);
        renewBackendToken(response);
    } catch (HttpClientErrorException.Forbidden e) {
        throw new BusinessException(CommonExceptionCodes.AUTHORIZATION_EXCEPTION);
    } catch (RestClientException e) {
        log.error("Failed to update includeEvents for aquarium {}", aquariumId, e);
        throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
    }
}
```

### 5.6 XHTML: `tankView.xhtml` — Events Panel

Add an **Events panel** below the existing `<ui:repeat>` tank loop (i.e., inside each iterated `<p:panel>` for the tank, appended after the `<!-- Action buttons -->` div). Key structure:

```xml
<!-- ===== AQUARIUM EVENT LOGBOOK PANEL (004-aquarium-events) ===== -->
<p:panel header="#{msg['aquariumevent.panel.h']}"
         styleClass="p-mb-3" style="margin-top:1rem;">

    <!-- Per-tank scoped messages -->
    <p:messages id="aquariumEventMessages_#{tank.id}" closable="true">
        <p:autoUpdate/>
    </p:messages>

    <!-- Hidden field to carry the event id for edit vs. create -->
    <h:form id="eventForm_#{tank.id}">
        <h:inputHidden id="eventId_#{tank.id}"
                       value="#{aquariumEventView.getEditFormForTank(tank.id).id}"/>

        <p:panelGrid columns="2" style="border:0; width:100%;">

            <p:outputLabel for="eventDate_#{tank.id}"
                           value="#{msg['aquariumevent.field.date.l']}"/>
            <p:datePicker id="eventDate_#{tank.id}"
                          value="#{aquariumEventView.getEditFormForTank(tank.id).eventDate}"
                          pattern="dd.MM.yyyy" showTime="false"
                          locale="#{userSession.locale}"
                          showOtherMonths="true" yearNavigator="true"
                          readonlyInput="true" required="true"
                          requiredMessage="#{msg['aquariumevent.validation.date.required.t']}"/>

            <p:outputLabel for="eventDuration_#{tank.id}"
                           value="#{msg['aquariumevent.field.duration.l']}"/>
            <p:inputText id="eventDuration_#{tank.id}"
                         value="#{aquariumEventView.getEditFormForTank(tank.id).durationHours}"
                         placeholder="#{msg['aquariumevent.field.duration.placeholder.t']}">
                <f:convertNumber minFractionDigits="0" locale="#{userSession.locale}"/>
            </p:inputText>

            <p:outputLabel for="eventDesc_#{tank.id}"
                           value="#{msg['aquariumevent.field.description.l']}"/>
            <p:inputTextarea id="eventDesc_#{tank.id}"
                             value="#{aquariumEventView.getEditFormForTank(tank.id).description}"
                             rows="4" autoResize="true" required="true"
                             requiredMessage="#{msg['aquariumevent.validation.description.required.t']}"
                             placeholder="#{msg['aquariumevent.field.description.placeholder.t']}"/>

        </p:panelGrid>

        <div style="margin-top:0.5rem; display:flex; gap:0.5rem;">
            <p:commandButton value="#{msg['common.save.b']}"
                             action="#{aquariumEventView.saveEvent(tank.id)}"
                             icon="pi pi-save"
                             update="eventForm_#{tank.id} eventList_#{tank.id}"
                             ajax="true"/>
            <p:commandButton value="#{msg['common.new_record.b']}"
                             action="#{aquariumEventView.resetForm(tank.id)}"
                             icon="pi pi-refresh"
                             update="eventForm_#{tank.id}"
                             ajax="true" immediate="true"/>
        </div>

        <p:staticMessage severity="warn" summary="INFO"
                         detail="#{msg['aquariumevent.mode.editrecord.l']}"
                         rendered="#{aquariumEventView.getEditFormForTank(tank.id).id != null}"/>
        <p:staticMessage severity="info" summary="INFO"
                         detail="#{msg['aquariumevent.mode.newrecord.l']}"
                         rendered="#{aquariumEventView.getEditFormForTank(tank.id).id == null}"/>

    </h:form>

    <!-- Event list -->
    <h:form id="eventList_#{tank.id}">
        <p:dataTable id="eventDT_#{tank.id}"
                     var="event"
                     value="#{aquariumEventView.getEventsForTank(tank.id)}"
                     emptyMessage="#{msg['aquariumevent.list.empty.t']}"
                     styleClass="p-mt-2">

            <p:column headerText="#{msg['aquariumevent.column.date.h']}" style="width:110px;">
                <p:outputLabel value="#{event.eventDate}">
                    <f:convertDateTime locale="#{userSession.locale}" type="localDate"/>
                </p:outputLabel>
            </p:column>

            <p:column headerText="#{msg['aquariumevent.column.duration.h']}" style="width:80px;">
                <h:outputText value="#{event.durationHours} h"
                              rendered="#{event.durationHours != null}"/>
            </p:column>

            <p:column headerText="#{msg['aquariumevent.column.description.h']}">
                <!-- outputText with escape="false" is NOT used; preserve newlines with CSS white-space:pre-wrap -->
                <p:outputLabel value="#{event.description}"
                               style="white-space:pre-wrap; word-break:break-word;"/>
            </p:column>

            <!-- Edit button -->
            <p:column style="width:3rem; text-align:center;">
                <p:commandButton icon="pi pi-pencil"
                                 title="#{msg['common.edit.b']}"
                                 action="#{aquariumEventView.startEditEvent(tank.id, event)}"
                                 update="eventForm_#{tank.id}"
                                 ajax="true"/>
            </p:column>

            <!-- Delete button + per-row confirmation dialog -->
            <p:column style="width:3rem; text-align:center;">
                <p:commandButton icon="pi pi-trash"
                                 styleClass="ui-button-danger"
                                 title="#{msg['common.delete.b']}"
                                 ajax="true"
                                 oncomplete="PF('confirmDeleteEvent_#{event.id}').show()"/>
                <p:confirmDialog widgetVar="confirmDeleteEvent_#{event.id}"
                                 header="#{msg['aquariumevent.delete.confirm.header']}"
                                 message="#{msg['aquariumevent.delete.confirm.message']}"
                                 modal="true">
                    <p:commandButton value="#{msg['common.confirm.b']}"
                                     action="#{aquariumEventView.deleteEvent(tank.id, event.id)}"
                                     update="eventList_#{tank.id}"
                                     oncomplete="PF('confirmDeleteEvent_#{event.id}').hide()"
                                     ajax="true"
                                     styleClass="ui-button-danger"/>
                    <p:commandButton value="#{msg['common.cancel.b']}"
                                     onclick="PF('confirmDeleteEvent_#{event.id}').hide()"
                                     type="button"
                                     styleClass="ui-button-secondary"/>
                </p:confirmDialog>
            </p:column>

        </p:dataTable>
    </h:form>

</p:panel>
<!-- ===== END AQUARIUM EVENT LOGBOOK PANEL ===== -->
```

### 5.7 XHTML: `userProfile.xhtml` — Include Events Checkbox

Inside the `<h:form id="reportLinkRow_#{tank.id}">` form, add **after the existing `validUntil` display row and before** the `p:commandButton` for "Link widerrufen":

```xml
<!-- 004-aquarium-events: include events opt-in -->
<p:panelGrid columns="3" styleClass="ui-noborder" style="margin-top:0.5rem;">

    <p:outputLabel for="includeEventsCb_#{tank.id}"
                   value="#{msg['aquariumevent.includeevents.label.l']}"/>

    <p:selectBooleanCheckbox id="includeEventsCb_#{tank.id}"
                             value="#{userProfileView.includeEventsMap[tank.id]}"/>

    <p:commandButton value="#{msg['common.save.b']}"
                     action="#{userProfileView.saveIncludeEvents(tank.id)}"
                     ajax="false"
                     styleClass="ui-button-primary"/>

</p:panelGrid>
```

### 5.8 XHTML: `houseReefReport.xhtml` — Events Section

Add an Events section **after the vitaldata charts section** and **before** the closing `</div>` of `report-body`:

```xml
<!-- 004-aquarium-events: Events section (rendered only when includeEvents = true) -->
<ui:fragment rendered="#{houseReefReportView.report.recentEvents != null}">
    <div style="margin-top:2rem;">
        <h2 style="color:#075985; font-size:1.15rem; border-bottom:2px solid #bae6fd; padding-bottom:0.4rem;">
            #{msg['aquariumevent.report.section.h']}
        </h2>

        <h:panelGroup rendered="#{empty houseReefReportView.report.recentEvents}">
            <p style="color:#64748b; font-style:italic;">
                #{msg['aquariumevent.report.no.events.t']}
            </p>
        </h:panelGroup>

        <h:panelGroup rendered="#{not empty houseReefReportView.report.recentEvents}">
            <ui:repeat value="#{houseReefReportView.report.recentEvents}" var="ev">
                <div style="border:1px solid #e0f2fe; border-radius:6px; padding:0.75rem; margin-bottom:0.6rem; background:#f0f9ff;">
                    <div style="display:flex; gap:1rem; align-items:baseline; margin-bottom:0.3rem;">
                        <span style="font-weight:600; color:#075985;">
                            <h:outputText value="#{ev.eventDate}">
                                <f:convertDateTime locale="#{userSession.locale}" type="localDate"/>
                            </h:outputText>
                        </span>
                        <h:panelGroup rendered="#{ev.durationHours != null}">
                            <span style="color:#64748b; font-size:0.88rem;">
                                #{ev.durationHours} #{msg['aquariumevent.report.hours.suffix.t']}
                            </span>
                        </h:panelGroup>
                    </div>
                    <p style="margin:0; white-space:pre-wrap; word-break:break-word; color:#1e293b;">
                        #{ev.description}
                    </p>
                </div>
            </ui:repeat>
        </h:panelGroup>
    </div>
</ui:fragment>
<!-- end 004-aquarium-events events section -->
```

### 5.9 i18n Keys

Add all keys below to **all 6 files**: `messages.properties`, `messages_de.properties`, `messages_en.properties`, `messages_es.properties`, `messages_fr.properties`, `messages_it.properties`.

#### Key table

| Key | EN value | DE value |
|---|---|---|
| `aquariumevent.panel.h` | Events Logbook | Ereignis-Logbuch |
| `aquariumevent.field.date.l` | Date | Datum |
| `aquariumevent.field.date.placeholder.t` | DD.MM.YYYY | TT.MM.JJJJ |
| `aquariumevent.field.duration.l` | Duration (hours) | Dauer (Stunden) |
| `aquariumevent.field.duration.placeholder.t` | e.g. 2.5 | z.B. 2,5 |
| `aquariumevent.field.description.l` | Description | Beschreibung |
| `aquariumevent.field.description.placeholder.t` | Describe the event... | Beschreibe das Ereignis... |
| `aquariumevent.validation.date.required.t` | Date is required. | Datum ist ein Pflichtfeld. |
| `aquariumevent.validation.description.required.t` | Description is required. | Beschreibung ist ein Pflichtfeld. |
| `aquariumevent.validation.duration.positive.t` | Duration must be greater than zero. | Dauer muss größer als 0 sein. |
| `aquariumevent.mode.newrecord.l` | New event – fill in the form and save. | Neues Ereignis – Formular ausfüllen und speichern. |
| `aquariumevent.mode.editrecord.l` | Editing existing event – save to apply changes. | Vorhandenes Ereignis wird bearbeitet – speichern zum Übernehmen. |
| `aquariumevent.list.empty.t` | No events recorded yet. | Noch keine Ereignisse erfasst. |
| `aquariumevent.column.date.h` | Date | Datum |
| `aquariumevent.column.duration.h` | Duration | Dauer |
| `aquariumevent.column.description.h` | Description | Beschreibung |
| `aquariumevent.delete.confirm.header` | Delete Event | Ereignis löschen |
| `aquariumevent.delete.confirm.message` | Are you sure you want to permanently delete this event? | Möchtest du dieses Ereignis wirklich dauerhaft löschen? |
| `aquariumevent.save.success.t` | Event saved. | Ereignis gespeichert. |
| `aquariumevent.delete.success.t` | Event deleted. | Ereignis gelöscht. |
| `aquariumevent.includeevents.label.l` | Include events in public report | Ereignisse im öffentlichen Report anzeigen |
| `aquariumevent.includeevents.saved.t` | Setting saved. | Einstellung gespeichert. |
| `aquariumevent.report.section.h` | Events Logbook | Ereignis-Logbuch |
| `aquariumevent.report.no.events.t` | No events recorded for this aquarium in the past 12 months. | Keine Ereignisse in den letzten 12 Monaten erfasst. |
| `aquariumevent.report.hours.suffix.t` | h | Std. |
| `common.confirm.b` | Confirm | Bestätigen |
| `common.cancel.b` | Cancel | Abbrechen |

> **Note**: `common.confirm.b` and `common.cancel.b` should be added only if they don't already exist in the bundles.

---

## 6. Security

### Ownership Check Pattern

All event CRUD operations verify ownership via a two-step chain identical to the existing `MeasurementController` / `TankService` pattern:

```
HTTP request → AquariumEventController
  → AquariumEventServiceImpl.resolveOwnedAquarium(aquariumId, principal.getName())
    → userRepository.findByEmail(email)
    → aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, userId)
  → If null: return HTTP 403 (FORBIDDEN)
  → Otherwise: proceed with the requested operation
```

For update/delete, a second check uses:
```
aquariumEventRepository.findByIdAndAquariumId(eventId, aquariumId)
```
This ensures an event cannot be modified even if the eventId is valid but belongs to a different aquarium (cross-aquarium attack).

### Public Report Events

- Events in the public report are assembled server-side inside `PublicReportServiceImpl.getReport()`.
- The 365-day filter is applied with a `LocalDate cutoff = LocalDate.now().minusDays(365)` server-side JPA query.
- No dedicated public event endpoint is created (FR-016, C-2).
- Events are only reachable via an existing valid share token.

### Authentication Guards

- All `AquariumEventController` endpoints require a valid JWT (enforced by the existing `JWTAuthenticationFilter`).
- Spring Security injects a valid `Principal` before any controller method executes.
- The existing `/api/public/**` security bypass does NOT apply to event CRUD endpoints.

---

## 7. Dependency Order

Implement in this order to ensure each layer has its prerequisites:

| Step | Component | Depends on |
|---|---|---|
| 1 | Flyway migration V1_6_0_1 (aquarium_event table) | — |
| 2 | Flyway migration V1_6_0_2 (include_events column) | — |
| 3 | `AquariumEventTo` DTO (sabi-boundary) | — |
| 4 | `PublicReportLinkTo` extension (add `includeEvents`) | — |
| 5 | `PublicReefReportTo` extension (add `recentEvents`) | Step 3 |
| 6 | `Endpoint.java` addition (`TANK_EVENTS`) | — |
| 7 | `AquariumEventEntity` (extends `Auditable`) | Step 1 |
| 8 | `PublicReportLinkEntity` extension (add `includeEvents`) | Step 2 |
| 9 | `AquariumEventRepository` | Step 7 |
| 10 | `AquariumEventMapper` (MapStruct) | Steps 3, 7 |
| 11 | `AquariumEventService` interface + `AquariumEventServiceImpl` | Steps 4, 8, 9, 10 |
| 12 | `PublicReportService` + `PublicReportServiceImpl` extensions | Steps 3, 5, 8, 9, 10 |
| 13 | `AquariumEventController` | Step 11 |
| 14 | `PublicReportController` extension | Step 12 |
| 15 | Webclient `AquariumEventService` interface + `AquariumEventServiceImpl` | Steps 3, 6 |
| 16 | Webclient `PublicReportService` + `PublicReportServiceImpl` extension | Steps 4, 5 |
| 17 | `AquariumEventView` CDI bean | Step 15 |
| 18 | `UserProfileView` extension (`saveIncludeEvents`, `includeEventsMap`) | Step 16 |
| 19 | `tankView.xhtml` Events panel | Step 17 |
| 20 | `userProfile.xhtml` checkbox + save button | Step 18 |
| 21 | `houseReefReport.xhtml` Events section | Step 5, 16 |
| 22 | i18n keys (all 6 bundles) | Steps 19–21 |

---

## 8. Key Existing Files — Summary of Modifications

| File | Reason / Change |
|---|---|
| `sabi-database/…/version1_5_0/V1_5_0_11__addPublicReportLink.sql` | Reference only — pattern for V1_6_0_* migrations |
| `sabi-boundary/…/model/PublicReportLinkTo.java` | Add `boolean includeEvents = false` |
| `sabi-boundary/…/model/PublicReefReportTo.java` | Add `List<AquariumEventTo> recentEvents` |
| `sabi-boundary/…/api/Endpoint.java` | Add `TANK_EVENTS` entry |
| `sabi-server/…/persistence/model/PublicReportLinkEntity.java` | Add `boolean includeEvents` column mapping |
| `sabi-server/…/services/PublicReportService.java` | Add `updateIncludeEvents()` method |
| `sabi-server/…/services/PublicReportServiceImpl.java` | Implement `updateIncludeEvents()`; inject `AquariumEventRepository` + `AquariumEventMapper`; populate `recentEvents` in `getReport()`; propagate `includeEvents` in link-to-DTO mapping |
| `sabi-server/…/rest/controller/PublicReportController.java` | Add `PUT /api/report/link/{aquariumId}/include-events` endpoint |
| `sabi-webclient/…/apigateway/PublicReportService.java` | Add `updateIncludeEventsFlag()` |
| `sabi-webclient/…/apigateway/PublicReportServiceImpl.java` | Implement `updateIncludeEventsFlag()` with `PUT` REST call |
| `sabi-webclient/…/controller/UserProfileView.java` | Add `includeEventsMap`, populate in `init()`, add `saveIncludeEvents(Long)` |
| `sabi-webclient/…/resources/secured/tankView.xhtml` | Add Events logbook panel per tank (form + datatable + confirmDialog) |
| `sabi-webclient/…/resources/secured/userProfile.xhtml` | Add `includeEvents` checkbox + Save button inside `reportLinkRow_#{tank.id}` form |
| `sabi-webclient/…/resources/houseReefReport.xhtml` | Add Events section (conditional on `recentEvents != null`) |
| All 6 `messages*.properties` files | Add 25 new i18n keys |

---

## 9. Testing Notes

### Integration Tests (sabi-server)
- **CRUD ownership**: test `POST /api/tank/{tankId}/events` with correct user → HTTP 201; with wrong user → HTTP 403.
- **12-month filter**: insert one event at `–6 months` and one at `–13 months`; call `getReport()` with `includeEvents = true`; assert only the `–6 months` event is in `recentEvents`.
- **Optimistic locking**: simulate concurrent update by sending PUT with stale optlock; assert HTTP 409.

### Unit Tests
- `AquariumEventServiceImpl.createEvent()` with null aquarium (ownership fail).
- `PublicReportServiceImpl.updateIncludeEvents()` when no link exists → `false` returned.

### i18n Completeness
Following the pattern from feature 002, verify all 25 new keys are non-empty in all 6 bundles.

---

*End of plan — ready to implement.*

