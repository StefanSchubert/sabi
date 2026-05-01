/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishRoleTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.FishStockExceptionCodes;
import de.bluewhale.sabi.services.FishStockMessageCodes;
import de.bluewhale.sabi.services.FishStockService;
import de.bluewhale.sabi.services.FishRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * REST-Controller for fish stock management.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping(value = "api/fish")
@Slf4j
public class FishStockController {

    @Autowired
    FishStockService fishStockService;

    @Autowired
    FishRoleService fishRoleService;

    // ---- List fish roles (localized) ----

    @Operation(summary = "List all available fish roles with localized names and descriptions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role list returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FishRoleTo>> getFishRoles(
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token) {
        log.debug("GET /api/fish/roles?lang={}", lang);
        List<FishRoleTo> roles = fishRoleService.getFishRoles(lang);
        return ResponseEntity.ok(roles);
    }

    // ---- List fish for a tank ----

    @Operation(summary = "List all fish (active and departed) for a given aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Fish list returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to requesting user.")
    })
    @GetMapping(value = "/{aquariumId}/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<FishStockEntryTo>> getFishForTank(
            @PathVariable Long aquariumId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/fish/{}/list for {}", aquariumId, principal.getName());
        List<FishStockEntryTo> fish = fishStockService.getFishForTank(aquariumId, principal.getName());
        return new ResponseEntity<>(fish, HttpStatus.ACCEPTED);
    }

    // ---- Add fish ----

    @Operation(summary = "Add a new fish entry to the authenticated user's tank.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fish entry created."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultTo<FishStockEntryTo>> addFish(
            @RequestBody @Valid FishStockEntryTo fishEntry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("POST /api/fish/ for {}", principal.getName());
        ResultTo<FishStockEntryTo> result = fishStockService.addFishToTank(fishEntry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ---- Get single fish ----

    @Operation(summary = "Get a single fish entry by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Fish entry returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Not your fish.")
    })
    @GetMapping(value = "/{fishId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<FishStockEntryTo> getFish(
            @PathVariable Long fishId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/fish/{} for {}", fishId, principal.getName());
        FishStockEntryTo fish = fishStockService.getFishById(fishId, principal.getName());
        if (fish == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(fish);
    }

    // ---- Update fish ----

    @Operation(summary = "Update an existing fish entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Fish entry updated."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Not your fish.")
    })
    @PutMapping(value = "/{fishId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ResultTo<FishStockEntryTo>> updateFish(
            @PathVariable Long fishId,
            @RequestBody @Valid FishStockEntryTo fishEntry,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("PUT /api/fish/{} for {}", fishId, principal.getName());
        fishEntry.setId(fishId);
        ResultTo<FishStockEntryTo> result = fishStockService.updateFishEntry(fishEntry, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Delete fish ----

    @Operation(summary = "Physically delete a fish entry (only allowed without departure record, FR-024).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fish deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Not your fish."),
            @ApiResponse(responseCode = "409", description = "Fish has a departure record, cannot delete.")
    })
    @DeleteMapping(value = "/{fishId}")
    public ResponseEntity<Void> deleteFish(
            @PathVariable Long fishId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("DELETE /api/fish/{} for {}", fishId, principal.getName());
        ResultTo<FishStockEntryTo> result = fishStockService.deletePhysically(fishId, principal.getName());
        if (result.getMessage() == null) {
            return ResponseEntity.noContent().build();
        }
        if (FishStockMessageCodes.FISH_HAS_DEPARTURE_RECORD.name().equals(
                result.getMessage().getCode() != null ? result.getMessage().getCode().toString() : "")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if (result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Record departure (US2, T044) ----

    @Operation(summary = "Record a departure event for an active fish (FR-006).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Departure recorded."),
            @ApiResponse(responseCode = "400", description = "Missing required fields."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "422", description = "Departure date before entry date.")
    })
    @PutMapping(value = "/{fishId}/departure", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ResultTo<FishStockEntryTo>> recordDeparture(
            @PathVariable Long fishId,
            @RequestBody @Valid FishDepartureRecordTo departureRecord,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("PUT /api/fish/{}/departure for {}", fishId, principal.getName());
        ResultTo<FishStockEntryTo> result = fishStockService.recordDeparture(fishId, departureRecord, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            // FR-006: date before addedOn → 422
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Remove catalogue link ----

    @Operation(summary = "Remove the catalogue link from a fish entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Catalogue link removed."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Not your fish.")
    })
    @DeleteMapping(value = "/{fishId}/catalogue-link", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ResultTo<FishStockEntryTo>> removeCatalogueLink(
            @PathVariable Long fishId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("DELETE /api/fish/{}/catalogue-link for {}", fishId, principal.getName());
        ResultTo<FishStockEntryTo> result = fishStockService.removeCatalogueLink(fishId, principal.getName());
        if (result.getMessage() != null && result.getMessage().getType() == Message.CATEGORY.ERROR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // ---- Upload photo ----

    @Operation(summary = "Upload a photo for a fish entry (max 5 MB, JPEG/PNG/WebP/GIF).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo uploaded."),
            @ApiResponse(responseCode = "400", description = "Invalid format or file too large."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/{fishId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhoto(
            @PathVariable Long fishId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "contentType", defaultValue = "image/jpeg") String contentType,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("POST /api/fish/{}/photo for {}", fishId, principal.getName());
        try {
            String resolvedContentType = (file.getContentType() != null) ? file.getContentType() : contentType;
            byte[] fileBytes = file.getBytes();
            fishStockService.uploadPhoto(fishId, fileBytes, resolvedContentType, principal.getName());
        } catch (Exception e) {
            log.warn("Photo upload failed for fish {}: {}", fishId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Download photo ----

    @Operation(summary = "Download the photo of a fish entry (ownership check, FR-025).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo bytes returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Not your fish or no photo."),
            @ApiResponse(responseCode = "404", description = "No photo for this fish.")
    })
    @GetMapping(value = "/{fishId}/photo")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable Long fishId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/fish/{}/photo for {}", fishId, principal.getName());
        byte[] bytes = fishStockService.getPhotoBytes(fishId, principal.getName());
        if (bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"fish-" + fishId + ".jpg\"");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    // ---- Delete photo ----

    @Operation(summary = "Delete the photo of a fish entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{fishId}/photo")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long fishId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("DELETE /api/fish/{}/photo for {}", fishId, principal.getName());
        fishStockService.deletePhoto(fishId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ---- Helpers ----

    private FishStockEntryTo createLookupEntry(Long fishId) {
        FishStockEntryTo e = new FishStockEntryTo();
        e.setId(fishId);
        return e;
    }
}

