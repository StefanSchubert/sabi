/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.InvertebrateCatalogueService;
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
 * REST controller for invertebrate catalogue management (read + propose + update).
 * Admin operations are handled by {@link InvertebrateCatalogueAdminController}.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Tag(name = "Invertebrate Catalogue", description = "Community-maintained invertebrate catalogue with UGC workflow")
@RestController
@RequestMapping(value = "api/invertebrate-catalogue")
@Slf4j
public class InvertebrateCatalogueController {

    @Autowired
    private InvertebrateCatalogueService invertebrateCatalogueService;

    @Operation(summary = "List all accessible invertebrate catalogue entries (PUBLIC + own PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InvertebrateCatalogueEntryTo>> listAll(
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        return ResponseEntity.ok(invertebrateCatalogueService.listAll(principal.getName(), lang));
    }

    @Operation(summary = "Search invertebrate catalogue by scientific or common name (min 2 chars).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned."),
            @ApiResponse(responseCode = "400", description = "Query too short (< 2 chars)."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InvertebrateCatalogueSearchResultTo>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(invertebrateCatalogueService.search(query, lang, principal.getName()));
    }

    @Operation(summary = "Get an invertebrate catalogue entry by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entry returned."),
            @ApiResponse(responseCode = "404", description = "Not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvertebrateCatalogueEntryTo> getById(
            @PathVariable Long id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        InvertebrateCatalogueEntryTo entry = invertebrateCatalogueService.getById(id, principal.getName());
        if (entry == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(entry);
    }

    @Operation(summary = "Propose a new invertebrate catalogue entry (status = PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entry proposed."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateCatalogueEntryTo>> proposeEntry(
            @Valid @RequestBody InvertebrateCatalogueEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateCatalogueEntryTo> result = invertebrateCatalogueService.proposeEntry(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Update an existing invertebrate catalogue entry (proposer or admin).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry updated."),
            @ApiResponse(responseCode = "403", description = "Not your entry or entry is REJECTED."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateCatalogueEntryTo>> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody InvertebrateCatalogueEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        entry.setId(id);
        ResultTo<InvertebrateCatalogueEntryTo> result = invertebrateCatalogueService.updateEntry(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
}
