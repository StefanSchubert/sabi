/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.CoralStockExceptionCodes;
import de.bluewhale.sabi.services.CoralStockService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * REST controller for coral stock management.
 * All endpoints require a valid JWT (Bearer token via AUTH_TOKEN header).
 * PII rule: principal.getName() only at DEBUG level.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Tag(name = "Coral Stock", description = "Coral stock management for marine aquariums")
@RestController
@RequestMapping(value = "api/coral")
@Slf4j
public class CoralStockController {

    @Autowired
    private CoralStockService coralStockService;

    // ---- List corals for a tank ----

    @Operation(summary = "List all corals (active and departed) for a given aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coral list returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{aquariumId}/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoralStockEntryTo>> getCoralsForTank(
            @PathVariable Long aquariumId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/coral/{}/list for user: {}", aquariumId, principal.getName());
        List<CoralStockEntryTo> corals = coralStockService.getCoralsForTank(aquariumId, principal.getName());
        return ResponseEntity.ok(corals);
    }

    // ---- Get single coral ----

    @Operation(summary = "Get a single coral entry by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coral entry returned."),
            @ApiResponse(responseCode = "404", description = "Not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{coralId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CoralStockEntryTo> getCoralById(
            @PathVariable Long coralId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        CoralStockEntryTo coral = coralStockService.getCoralById(coralId, principal.getName());
        if (coral == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(coral);
    }

    // ---- Add coral ----

    @Operation(summary = "Add a new coral to a marine aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Coral created."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "403", description = "Not a marine aquarium or not yours."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralStockEntryTo>> addCoralToTank(
            @Valid @RequestBody CoralStockEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralStockEntryTo> result = coralStockService.addCoralToTank(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            if (result.getMessage().getCode() != null &&
                    result.getMessage().getCode().getExceptionCode() == CoralStockExceptionCodes.MARINE_ONLY) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
            }
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ---- Update coral ----

    @Operation(summary = "Update an existing coral entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Coral updated."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "403", description = "Not your coral."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{coralId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralStockEntryTo>> updateCoralEntry(
            @PathVariable Long coralId,
            @Valid @RequestBody CoralStockEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        entry.setId(coralId);
        ResultTo<CoralStockEntryTo> result = coralStockService.updateCoralEntry(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Delete coral ----

    @Operation(summary = "Physically delete a coral entry (blocked if departure record exists).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Coral deleted."),
            @ApiResponse(responseCode = "409", description = "Has departure record — cannot delete."),
            @ApiResponse(responseCode = "403", description = "Not your coral."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{coralId}")
    public ResponseEntity<ResultTo<CoralStockEntryTo>> deleteCoralPhysically(
            @PathVariable Long coralId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralStockEntryTo> result = coralStockService.deletePhysically(coralId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            if (result.getMessage().getCode() != null &&
                    result.getMessage().getCode().getExceptionCode() == CoralStockExceptionCodes.CORAL_HAS_DEPARTURE_RECORD) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Record departure ----

    @Operation(summary = "Record a departure for a coral entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Departure recorded."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "422", description = "Departure date before entry date."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{coralId}/departure", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralStockEntryTo>> recordDeparture(
            @PathVariable Long coralId,
            @Valid @RequestBody CoralDepartureRecordTo record,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralStockEntryTo> result = coralStockService.recordDeparture(coralId, record, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            if (result.getMessage().getCode() != null &&
                    result.getMessage().getCode().getExceptionCode() == CoralStockExceptionCodes.DEPARTURE_DATE_BEFORE_ENTRY) {
                return ResponseEntity.unprocessableEntity().body(result);
            }
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Remove catalogue link ----

    @Operation(summary = "Remove the catalogue link from a coral entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Catalogue link removed."),
            @ApiResponse(responseCode = "403", description = "Not your coral."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{coralId}/catalogue-link", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralStockEntryTo>> removeCatalogueLink(
            @PathVariable Long coralId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralStockEntryTo> result = coralStockService.removeCatalogueLink(coralId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Photo management ----

    @Operation(summary = "Upload a photo for a coral entry (max 5 MB, JPEG/PNG/WebP/GIF).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo uploaded."),
            @ApiResponse(responseCode = "400", description = "Photo too large or invalid format."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/{coralId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhoto(
            @PathVariable Long coralId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) throws IOException {
        ResultTo<CoralStockEntryTo> result = coralStockService.uploadPhoto(coralId, file, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get the photo bytes for a coral entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo bytes returned."),
            @ApiResponse(responseCode = "404", description = "No photo found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{coralId}/photo", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable Long coralId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        byte[] bytes = coralStockService.getPhotoBytes(coralId, principal.getName());
        if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(bytes);
    }

    @Operation(summary = "Delete the photo of a coral entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo deleted."),
            @ApiResponse(responseCode = "403", description = "Not your coral."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{coralId}/photo")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long coralId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralStockEntryTo> result = coralStockService.deletePhoto(coralId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Growth history ----

    @Operation(summary = "Get growth measurement history for a coral.")
    @GetMapping(value = "/{coralId}/growth", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoralGrowthHistoryTo>> getGrowthHistory(
            @PathVariable Long coralId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        return ResponseEntity.ok(coralStockService.getGrowthHistory(coralId, principal.getName()));
    }

    @Operation(summary = "Add a growth measurement for a coral.")
    @PostMapping(value = "/{coralId}/growth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralGrowthHistoryTo>> addGrowthRecord(
            @PathVariable Long coralId,
            @Valid @RequestBody CoralGrowthHistoryTo record,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralGrowthHistoryTo> result = coralStockService.addGrowthRecord(coralId, record, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.unprocessableEntity().body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Update a growth measurement record (date and value only; type is immutable).")
    @PutMapping(value = "/{coralId}/growth/{recordId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralGrowthHistoryTo>> updateGrowthRecord(
            @PathVariable Long coralId,
            @PathVariable Long recordId,
            @RequestBody CoralGrowthHistoryTo record,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        record.setId(recordId);
        ResultTo<CoralGrowthHistoryTo> result = coralStockService.updateGrowthRecord(coralId, record, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.unprocessableEntity().body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @Operation(summary = "Delete a growth measurement record.")
    @DeleteMapping(value = "/{coralId}/growth/{recordId}")
    public ResponseEntity<Void> deleteGrowthRecord(
            @PathVariable Long coralId,
            @PathVariable Long recordId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralGrowthHistoryTo> result = coralStockService.deleteGrowthRecord(coralId, recordId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Polyp condition history ----

    @Operation(summary = "Get polyp condition observation history for a coral.")
    @GetMapping(value = "/{coralId}/polyp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CoralPolypConditionTo>> getPolypHistory(
            @PathVariable Long coralId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        return ResponseEntity.ok(coralStockService.getPolypHistory(coralId, principal.getName()));
    }

    @Operation(summary = "Add a polyp condition observation for a coral.")
    @PostMapping(value = "/{coralId}/polyp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralPolypConditionTo>> addPolypObservation(
            @PathVariable Long coralId,
            @Valid @RequestBody CoralPolypConditionTo record,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralPolypConditionTo> result = coralStockService.addPolypObservation(coralId, record, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.unprocessableEntity().body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Update a polyp condition observation.")
    @PutMapping(value = "/{coralId}/polyp/{recordId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<CoralPolypConditionTo>> updatePolypObservation(
            @PathVariable Long coralId,
            @PathVariable Long recordId,
            @RequestBody CoralPolypConditionTo record,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        record.setId(recordId);
        ResultTo<CoralPolypConditionTo> result = coralStockService.updatePolypObservation(coralId, record, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.unprocessableEntity().body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @Operation(summary = "Delete a polyp condition observation.")
    @DeleteMapping(value = "/{coralId}/polyp/{recordId}")
    public ResponseEntity<Void> deletePolypObservation(
            @PathVariable Long coralId,
            @PathVariable Long recordId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<CoralPolypConditionTo> result = coralStockService.deletePolypObservation(coralId, recordId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }
}

