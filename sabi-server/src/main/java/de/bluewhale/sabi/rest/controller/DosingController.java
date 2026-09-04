/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.DosingService;
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
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * REST controller for standalone manual and automated dosing records.
 */
@RestController
@RequestScope
@RequestMapping("api/tank")
public class DosingController {

    private final DosingService dosingService;

    public DosingController(DosingService dosingService) {
        this.dosingService = dosingService;
    }

    @Operation(summary = "List dosing records for a tank, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dosing records returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{tankId}/dosings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DosingTo>> listDosings(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, Principal principal) {
        return ResponseEntity.ok(dosingService.listDosingsForTank(tankId, principal.getName()));
    }

    @Operation(summary = "Create a standalone dosing record.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dosing record created."),
            @ApiResponse(responseCode = "400", description = "Invalid dosing payload."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user.")
    })
    @PostMapping(value = "/{tankId}/dosings", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DosingTo> createDosing(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @PathVariable Long tankId, @RequestBody @Valid DosingTo dosingTo, Principal principal) {
        ResultTo<DosingTo> result = dosingService.createDosing(tankId, dosingTo, principal.getName());
        return errorOr(result, HttpStatus.CREATED);
    }

    @Operation(summary = "Update a standalone dosing record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dosing record updated."),
            @ApiResponse(responseCode = "400", description = "Invalid dosing payload."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or record was not found."),
            @ApiResponse(responseCode = "409", description = "Optimistic locking conflict.")
    })
    @PutMapping(value = "/{tankId}/dosings/{dosingId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DosingTo> updateDosing(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token, @PathVariable Long tankId,
            @PathVariable Long dosingId, @RequestBody @Valid DosingTo dosingTo, Principal principal) {
        try {
            ResultTo<DosingTo> result = dosingService.updateDosing(tankId, dosingId, dosingTo, principal.getName());
            return errorOr(result, HttpStatus.OK);
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(summary = "Delete a standalone dosing record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dosing record deleted."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or record was not found.")
    })
    @DeleteMapping(value = "/{tankId}/dosings/{dosingId}")
    public ResponseEntity<Void> deleteDosing(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token, @PathVariable Long tankId,
            @PathVariable Long dosingId, Principal principal) {
        ResultTo<DosingTo> result = dosingService.deleteDosing(tankId, dosingId, principal.getName());
        if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<DosingTo> errorOr(ResultTo<DosingTo> result, HttpStatus successStatus) {
        if (Message.CATEGORY.ERROR.equals(result.getMessage().getType())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(successStatus).body(result.getValue());
    }
}
