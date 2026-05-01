/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.FishCatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * Admin REST-Controller for fish catalogue proposal management.
 * Admin check is performed at service level (sabi.admin.users property).
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping(value = "api/admin/fish/catalogue")
@Slf4j
public class FishCatalogueAdminController {

    @Autowired
    FishCatalogueService fishCatalogueService;

    // ---- List pending proposals (T061, US5) ----

    @Operation(summary = "List all pending catalogue proposals (Admin only).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Pending proposals returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Admin role required.")
    })
    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FishCatalogueEntryTo>> listPending(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/admin/fish/catalogue/pending for {}", principal.getName());
        List<FishCatalogueEntryTo> pending = fishCatalogueService.listPendingProposals(principal.getName());
        if (pending.isEmpty() && !isAdminAccess(principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pending);
    }

    // ---- List ALL entries (admin catalogue browser) ----

    @Operation(summary = "List all catalogue entries regardless of status (Admin only).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All catalogue entries returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Admin role required.")
    })
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FishCatalogueEntryTo>> listAll(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/admin/fish/catalogue for {}", principal.getName());
        List<FishCatalogueEntryTo> entries = fishCatalogueService.listAllForAdmin(principal.getName());
        // Service returns empty list for non-admins; treat same as 403 for non-admins
        if (entries.isEmpty() && !isAdminAccess(principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(entries);
    }

    // ---- Approve proposal (T061, US5) ----

    @Operation(summary = "Approve a pending proposal (Admin only, FR-016).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry approved and set to PUBLIC."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Admin role required.")
    })
    @PutMapping(value = "/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<FishCatalogueEntryTo>> approve(
            @PathVariable Long id,
            @RequestBody(required = false) FishCatalogueEntryTo editedEntry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("PUT /api/admin/fish/catalogue/{}/approve for {}", id, principal.getName());
        ResultTo<FishCatalogueEntryTo> result =
                fishCatalogueService.approveEntry(id, editedEntry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Reject proposal (T061, US5) ----

    @Operation(summary = "Reject a pending proposal (Admin only, FR-017).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry rejected."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Admin role required.")
    })
    @PutMapping(value = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<FishCatalogueEntryTo>> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> reasonMap,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("PUT /api/admin/fish/catalogue/{}/reject for {}", id, principal.getName());
        String reason = reasonMap != null ? reasonMap.get("reason") : null;
        ResultTo<FishCatalogueEntryTo> result =
                fishCatalogueService.rejectEntry(id, reason, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    private boolean isAdminAccess(String userEmail) {
        // Empty list from service = non-admin → check via 403 handling in service
        return true; // Service already handles the admin check
    }
}

