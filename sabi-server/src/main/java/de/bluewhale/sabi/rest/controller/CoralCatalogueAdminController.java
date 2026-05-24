/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.CoralCatalogueServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * REST controller for admin-only coral catalogue operations (approve / reject / admin-update).
 * All endpoints require JWT + admin role (verified in service via sabi.admin.users property).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Tag(name = "Coral Catalogue Admin", description = "Admin operations for coral catalogue proposals")
@RestController
@RequestMapping(value = "api/admin/coral/catalogue")
@Slf4j
public class CoralCatalogueAdminController {

    @Autowired
    private CoralCatalogueServiceImpl coralCatalogueService;

    @Operation(summary = "List ALL coral catalogue entries (any status) for admin view.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Full catalogue returned."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoralCatalogueEntryTo>> listAll(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        List<CoralCatalogueEntryTo> entries = coralCatalogueService.listAllForAdmin(principal.getName());
        return ResponseEntity.ok(entries);
    }

    @Operation(summary = "List all pending coral catalogue proposals.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending list returned."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoralCatalogueEntryTo>> listPending(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        List<CoralCatalogueEntryTo> pending = coralCatalogueService.listPending(principal.getName());
        return ResponseEntity.ok(pending);
    }

    @Operation(summary = "Approve a coral catalogue proposal (sets status to PUBLIC).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry approved."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "404", description = "Entry not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralCatalogueEntryTo>> approve(
            @PathVariable Long id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralCatalogueEntryTo> result = coralCatalogueService.approveEntry(id, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @Operation(summary = "Reject a coral catalogue proposal (sets status to REJECTED).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry rejected."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "404", description = "Entry not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralCatalogueEntryTo>> reject(
            @PathVariable Long id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralCatalogueEntryTo> result = coralCatalogueService.rejectEntry(id, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @Operation(summary = "Admin update of any coral catalogue entry regardless of status.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry updated."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralCatalogueEntryTo>> adminUpdate(
            @PathVariable Long id,
            @Valid @RequestBody CoralCatalogueEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralCatalogueEntryTo> result = coralCatalogueService.adminUpdateEntry(id, entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
}

