/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.FishCatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * REST-Controller for fish catalogue search and proposal.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping(value = "api/fish/catalogue")
@Slf4j
public class FishCatalogueController {

    @Autowired
    FishCatalogueService fishCatalogueService;

    // ---- Search (T049, US3) ----

    @Operation(summary = "Search fish catalogue (PUBLIC + own PENDING). Requires min 2 chars (FR-020).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Search results returned."),
            @ApiResponse(responseCode = "400", description = "Query too short (< 2 chars)."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FishCatalogueSearchResultTo>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/fish/catalogue/search?q={}&lang={} for {}", query, lang, principal.getName());
        if (query == null || query.length() < 2) {
            return ResponseEntity.badRequest().build();
        }
        List<FishCatalogueSearchResultTo> results =
                fishCatalogueService.search(query, lang, principal.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(results);
    }

    // ---- Get single entry (T049) ----

    @Operation(summary = "Get a single fish catalogue entry (PUBLIC or own PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Not found.")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FishCatalogueEntryTo> getEntry(
            @PathVariable Long id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/fish/catalogue/{} for {}", id, principal.getName());
        FishCatalogueEntryTo entry = fishCatalogueService.getEntry(id, principal.getName());
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(entry);
    }

    // ---- Propose entry (T055, US4) ----

    @Operation(summary = "Propose a new fish catalogue entry (status=PENDING).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proposal submitted."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<FishCatalogueEntryTo>> proposeEntry(
            @RequestBody @Valid FishCatalogueEntryTo entryTo,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("POST /api/fish/catalogue/ for {}", principal.getName());
        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.proposeEntry(entryTo, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ---- Update entry (T069, US6) ----

    @Operation(summary = "Update a fish catalogue entry (Creator for PENDING/PUBLIC, Admin for PUBLIC).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Entry updated."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Not yours."),
            @ApiResponse(responseCode = "409", description = "REJECTED entries are read-only (FR-019).")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<FishCatalogueEntryTo>> updateEntry(
            @PathVariable Long id,
            @RequestBody @Valid FishCatalogueEntryTo entryTo,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("PUT /api/fish/catalogue/{} for {}", id, principal.getName());
        entryTo.setId(id);
        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.updateEntry(entryTo, principal.getName());
        if (result.getMessage() != null) {
            if (result.getMessage().getType() == Message.CATEGORY.ERROR) {
                String codeStr = result.getMessage().getCode() != null
                        ? result.getMessage().getCode().toString() : "";
                if ("CATALOGUE_REJECTED_READ_ONLY".equals(codeStr)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
            }
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }
}

