/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.AquariumEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * REST controller for aquarium logbook events (CRUD).
 * Feature: 004-aquarium-events.
 *
 * Endpoints:
 *   GET    /api/tank/{tankId}/events            – list events (newest first)
 *   POST   /api/tank/{tankId}/events            – create event (→ HTTP 201)
 *   PUT    /api/tank/{tankId}/events/{eventId}  – update event (→ HTTP 200)
 *   DELETE /api/tank/{tankId}/events/{eventId}  – delete event (→ HTTP 200)
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping("api/tank")
@Slf4j
public class AquariumEventController {

    @Autowired
    AquariumEventService aquariumEventService;

    // ---- GET /api/tank/{tankId}/events ----

    @Operation(summary = "List all events for a tank, newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event list returned."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{tankId}/events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<AquariumEventTo>> listEvents(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId,
            Principal principal) {
        List<AquariumEventTo> events = aquariumEventService.listEventsForTank(tankId, principal.getName());
        return ResponseEntity.ok(events);
    }

    // ---- POST /api/tank/{tankId}/events  → HTTP 201 ----

    @Operation(summary = "Create a new event for the given tank.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Event created."),
        @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
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
            log.warn("createEvent rejected for aquarium_id={}", tankId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.getValue());
    }

    // ---- PUT /api/tank/{tankId}/events/{eventId}  → HTTP 200 ----

    @Operation(summary = "Update an existing event.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event updated."),
        @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or event not found."),
        @ApiResponse(responseCode = "409", description = "Optimistic locking conflict."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
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
                log.warn("updateEvent rejected for aquarium_id={} event_id={}", tankId, eventId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(result.getValue());
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict updating event_id={} in aquarium_id={}", eventId, tankId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // ---- DELETE /api/tank/{tankId}/events/{eventId}  → HTTP 200 ----

    @Operation(summary = "Delete an event permanently.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event deleted."),
        @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or event not found."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{tankId}/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> deleteEvent(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId,
            @PathVariable Long eventId,
            Principal principal) {
        ResultTo<AquariumEventTo> result = aquariumEventService.deleteEvent(tankId, eventId, principal.getName());
        if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
            log.warn("deleteEvent rejected for aquarium_id={} event_id={}", tankId, eventId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().build();
    }
}

