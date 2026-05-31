/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.CoralCatalogueService;
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
 * REST controller for coral catalogue management (read + propose + update).
 * Admin operations are handled by {@link CoralCatalogueAdminController}.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Tag(name = "Coral Catalogue", description = "Community-maintained coral catalogue with UGC workflow")
@RestController
@RequestMapping(value = "api/coral/catalogue")
@Slf4j
public class CoralCatalogueController {

    @Autowired
    private CoralCatalogueService coralCatalogueService;

    @Operation(summary = "List all accessible coral catalogue entries (PUBLIC + own PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoralCatalogueEntryTo>> listAll(
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        return ResponseEntity.ok(coralCatalogueService.listAll(principal.getName(), lang));
    }

    @Operation(summary = "Search coral catalogue by scientific or common name (min 2 chars).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned."),
            @ApiResponse(responseCode = "400", description = "Query too short (< 2 chars)."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoralCatalogueSearchResultTo>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(coralCatalogueService.search(query, lang, principal.getName()));
    }

    @Operation(summary = "Get a coral catalogue entry by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entry returned."),
            @ApiResponse(responseCode = "404", description = "Not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CoralCatalogueEntryTo> getById(
            @PathVariable Long id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        CoralCatalogueEntryTo entry = coralCatalogueService.getById(id, principal.getName());
        if (entry == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(entry);
    }

    @Operation(summary = "Propose a new coral catalogue entry (status = PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entry proposed; ResultTo may contain a duplicate-name warning."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralCatalogueEntryTo>> proposeEntry(
            @Valid @RequestBody CoralCatalogueEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralCatalogueEntryTo> result = coralCatalogueService.proposeEntry(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Update an existing coral catalogue entry (proposer or admin).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry updated."),
            @ApiResponse(responseCode = "403", description = "Not your entry or entry is REJECTED."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralCatalogueEntryTo>> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody CoralCatalogueEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        entry.setId(id);
        ResultTo<CoralCatalogueEntryTo> result = coralCatalogueService.updateEntry(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
}

