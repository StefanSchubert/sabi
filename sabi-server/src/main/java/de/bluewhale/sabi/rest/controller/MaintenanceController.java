/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.MaintenanceEventTo;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.services.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import java.security.Principal;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * REST controller for the per-aquarium maintenance data: regular water exchange interval,
 * recorded maintenance events (water changes and carbon replacements) and the activated carbon
 * setting. Ownership is enforced on every endpoint, including reads (SC-006).
 */
@RestController
@RequestScope
@RequestMapping("api/tank")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Operation(summary = "Return the full maintenance block (profile + events, newest first) for a tank.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance block returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user.")
    })
    @GetMapping(value = "/{tankId}/maintenance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MaintenanceTo> getMaintenance(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, Principal principal) {
        ResultTo<MaintenanceTo> result = maintenanceService.getMaintenanceForTank(tankId, principal.getName());
        return errorOr(result, HttpStatus.OK);
    }

    @Operation(summary = "Upsert or clear the regular water exchange interval for a tank.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interval updated or cleared."),
            @ApiResponse(responseCode = "400", description = "Invalid interval payload (both-or-neither, ranges)."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user.")
    })
    @PutMapping(value = "/{tankId}/maintenance/water-change-interval", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MaintenanceProfileTo> updateWaterChangeInterval(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, @RequestBody @Valid WaterChangeIntervalTo intervalTo, Principal principal) {
        ResultTo<MaintenanceProfileTo> result =
                maintenanceService.updateWaterChangeInterval(tankId, intervalTo, principal.getName());
        return errorOr(result, HttpStatus.OK);
    }

    @Operation(summary = "Upsert the activated carbon setting for a tank. Disabling clears mode and replacement interval.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carbon setting updated."),
            @ApiResponse(responseCode = "400", description = "Invalid carbon payload (enabled requires mode, INTERVAL requires replacement interval)."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user.")
    })
    @PutMapping(value = "/{tankId}/maintenance/activated-carbon", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MaintenanceProfileTo> updateActivatedCarbon(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, @RequestBody @Valid ActivatedCarbonTo carbonTo, Principal principal) {
        ResultTo<MaintenanceProfileTo> result =
                maintenanceService.updateActivatedCarbon(tankId, carbonTo, principal.getName());
        return errorOr(result, HttpStatus.OK);
    }

    @Operation(summary = "Create a maintenance event for a tank (water change or carbon replacement). "
            + "Back-dating allowed, future dates rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Maintenance event created."),
            @ApiResponse(responseCode = "400", description = "Invalid event payload (date, type-specific percentage rules, note length)."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user.")
    })
    @PostMapping(value = "/{tankId}/maintenance/events", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MaintenanceEventTo> createMaintenanceEvent(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, @RequestBody @Valid MaintenanceEventTo eventTo, Principal principal) {
        ResultTo<MaintenanceEventTo> result = maintenanceService.createMaintenanceEvent(tankId, eventTo, principal.getName());
        return errorOr(result, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing maintenance event of a tank. The client must echo the optlock version. "
            + "The maintenance type of a record cannot be changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance event updated."),
            @ApiResponse(responseCode = "400", description = "Invalid event payload (date, type-specific percentage rules, note length)."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or record was not found."),
            @ApiResponse(responseCode = "409", description = "Optimistic locking conflict.")
    })
    @PutMapping(value = "/{tankId}/maintenance/events/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MaintenanceEventTo> updateMaintenanceEvent(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, @PathVariable Long eventId,
            @RequestBody @Valid MaintenanceEventTo eventTo, Principal principal) {
        try {
            ResultTo<MaintenanceEventTo> result =
                    maintenanceService.updateMaintenanceEvent(tankId, eventId, eventTo, principal.getName());
            return errorOr(result, HttpStatus.OK);
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "Delete a maintenance event of a tank.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Maintenance event deleted."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or record was not found.")
    })
    @DeleteMapping(value = "/{tankId}/maintenance/events/{eventId}")
    public ResponseEntity<Void> deleteMaintenanceEvent(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, @PathVariable Long eventId, Principal principal) {
        ResultTo<MaintenanceEventTo> result = maintenanceService.deleteMaintenanceEvent(tankId, eventId, principal.getName());
        if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().build();
    }

    private <T> ResponseEntity<T> errorOr(ResultTo<T> result, HttpStatus successStatus) {
        if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(successStatus).body(result.getValue());
    }
}
