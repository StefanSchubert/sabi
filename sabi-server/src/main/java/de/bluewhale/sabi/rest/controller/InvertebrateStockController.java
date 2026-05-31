/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.InvertebrateStockExceptionCodes;
import de.bluewhale.sabi.services.InvertebrateStockService;
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
 * REST controller for invertebrate stock management.
 * All endpoints require a valid JWT (Bearer token via AUTH_TOKEN header).
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Tag(name = "Invertebrate Stock", description = "Invertebrate stock management for marine aquariums")
@RestController
@RequestMapping(value = "api/invertebrate")
@Slf4j
public class InvertebrateStockController {

    @Autowired
    private InvertebrateStockService invertebrateStockService;

    // ---- List invertebrates for a tank ----

    @Operation(summary = "List all invertebrates (active and departed) for a given aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invertebrate list returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{aquariumId}/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InvertebrateStockEntryTo>> listForAquarium(
            @PathVariable Long aquariumId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/invertebrate/{}/list for user: {}", aquariumId, principal.getName());
        List<InvertebrateStockEntryTo> invertebrates = invertebrateStockService.listForAquarium(aquariumId, principal.getName());
        return ResponseEntity.ok(invertebrates);
    }

    // ---- Get single invertebrate ----

    @Operation(summary = "Get a single invertebrate entry by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invertebrate entry returned."),
            @ApiResponse(responseCode = "404", description = "Not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{invertebrateId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InvertebrateStockEntryTo> getById(
            @PathVariable Long invertebrateId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        InvertebrateStockEntryTo entry = invertebrateStockService.getById(invertebrateId, principal.getName());
        if (entry == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(entry);
    }

    // ---- Add invertebrate ----

    @Operation(summary = "Add a new invertebrate to a marine aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invertebrate created."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "403", description = "Not a marine aquarium or not yours."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateStockEntryTo>> addInvertebrate(
            @Valid @RequestBody InvertebrateStockEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.create(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            if (result.getMessage().getCode() != null &&
                    result.getMessage().getCode().getExceptionCode() == InvertebrateStockExceptionCodes.MARINE_ONLY) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
            }
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ---- Update invertebrate ----

    @Operation(summary = "Update an existing invertebrate entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Invertebrate updated."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "403", description = "Not your invertebrate."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{invertebrateId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateStockEntryTo>> updateInvertebrate(
            @PathVariable Long invertebrateId,
            @Valid @RequestBody InvertebrateStockEntryTo entry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        entry.setId(invertebrateId);
        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.update(entry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Delete invertebrate ----

    @Operation(summary = "Physically delete an invertebrate entry (blocked if departure record exists).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invertebrate deleted."),
            @ApiResponse(responseCode = "409", description = "Has departure record — cannot delete."),
            @ApiResponse(responseCode = "403", description = "Not your invertebrate."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{invertebrateId}")
    public ResponseEntity<ResultTo<InvertebrateStockEntryTo>> deleteInvertebrate(
            @PathVariable Long invertebrateId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.delete(invertebrateId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            if (result.getMessage().getCode() != null &&
                    result.getMessage().getCode().getExceptionCode() == InvertebrateStockExceptionCodes.INVERT_HAS_DEPARTURE_RECORD) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Record departure ----

    @Operation(summary = "Record a departure for an invertebrate entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Departure recorded."),
            @ApiResponse(responseCode = "422", description = "Departure date before entry date."),
            @ApiResponse(responseCode = "403", description = "Not your invertebrate."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/{invertebrateId}/departure", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateStockEntryTo>> recordDeparture(
            @PathVariable Long invertebrateId,
            @Valid @RequestBody InvertebrateDepartureRecordTo record,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateStockEntryTo> result =
                invertebrateStockService.recordDeparture(invertebrateId, record, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            if (result.getMessage().getCode() != null &&
                    result.getMessage().getCode().getExceptionCode() == InvertebrateStockExceptionCodes.DEPARTURE_DATE_BEFORE_ENTRY) {
                return ResponseEntity.unprocessableEntity().body(result);
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Remove catalogue link ----

    @Operation(summary = "Remove the catalogue link from an invertebrate entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Catalogue link removed."),
            @ApiResponse(responseCode = "403", description = "Not your invertebrate."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{invertebrateId}/catalogue-link", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateStockEntryTo>> removeCatalogueLink(
            @PathVariable Long invertebrateId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateStockEntryTo> result =
                invertebrateStockService.removeCatalogueLink(invertebrateId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Photo endpoints ----

    @Operation(summary = "Upload a photo for an invertebrate entry (max 5 MB).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo uploaded."),
            @ApiResponse(responseCode = "400", description = "Photo too large or invalid format."),
            @ApiResponse(responseCode = "403", description = "Not your invertebrate."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/{invertebrateId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<InvertebrateStockEntryTo>> uploadPhoto(
            @PathVariable Long invertebrateId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) throws IOException {
        ResultTo<InvertebrateStockEntryTo> result =
                invertebrateStockService.uploadPhoto(invertebrateId, file, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get the photo for an invertebrate entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo bytes returned."),
            @ApiResponse(responseCode = "404", description = "No photo."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/{invertebrateId}/photo")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable Long invertebrateId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        byte[] bytes = invertebrateStockService.getPhotoBytes(invertebrateId, principal.getName());
        if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes);
    }

    @Operation(summary = "Delete the photo for an invertebrate entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo deleted."),
            @ApiResponse(responseCode = "403", description = "Not your invertebrate."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{invertebrateId}/photo")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long invertebrateId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        ResultTo<InvertebrateStockEntryTo> result =
                invertebrateStockService.deletePhoto(invertebrateId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }
}
