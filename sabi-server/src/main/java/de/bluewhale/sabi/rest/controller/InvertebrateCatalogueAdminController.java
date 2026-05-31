/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.InvertebrateCatalogueServiceImpl;
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
 * REST controller for admin-only invertebrate catalogue operations (approve / reject / admin-update).
 * All endpoints require JWT + admin role (verified in service via sabi.admin.users property).
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Tag(name = "Invertebrate Catalogue Admin", description = "Admin operations for invertebrate catalogue proposals")
@RestController
@RequestMapping(value = "api/admin/invertebrate-catalogue")
@Slf4j
public class InvertebrateCatalogueAdminController {

    @Autowired
    private InvertebrateCatalogueServiceImpl invertebrateCatalogueService;

    @Operation(summary = "List ALL invertebrate catalogue entries (any status) for admin view.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Full catalogue returned."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InvertebrateCatalogueEntryTo>> listAll(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        return ResponseEntity.ok(invertebrateCatalogueService.listAllForAdmin(principal.getName()));
    }

    @Operation(summary = "List all pending invertebrate catalogue proposals.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending list returned."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InvertebrateCatalogueEntryTo>> listPending(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        return ResponseEntity.ok(invertebrateCatalogueService.listPending(principal.getName()));
    }

    @Operation(summary = "Approve an invertebrate catalogue proposal (sets status to PUBLIC).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry approved."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "404", description = "Entry not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateCatalogueEntryTo>> approve(
            @PathVariable Long id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateCatalogueEntryTo> result = invertebrateCatalogueService.approveEntry(id, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @Operation(summary = "Reject an invertebrate catalogue proposal (sets status to REJECTED).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry rejected."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "404", description = "Entry not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateCatalogueEntryTo>> reject(
            @PathVariable Long id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateCatalogueEntryTo> result = invertebrateCatalogueService.rejectEntry(id, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @Operation(summary = "Admin update of any invertebrate catalogue entry regardless of status.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry updated."),
            @ApiResponse(responseCode = "403", description = "Not an admin."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateCatalogueEntryTo>> adminUpdate(
            @PathVariable Long id,
            @Valid @RequestBody InvertebrateCatalogueEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateCatalogueEntryTo> result = invertebrateCatalogueService.adminUpdateEntry(id, entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
}
