/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.PublicReefReportTo;
import de.bluewhale.sabi.model.PublicReportLinkTo;
import de.bluewhale.sabi.services.PublicReportService;
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
import java.time.LocalDateTime;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * REST controller for public HouseReef report share links.
 *
 * Public endpoint (no auth): GET /api/public/report/{token}
 * Auth-protected endpoints:
 *   GET    /api/report/link/{aquariumId}  – get current link for a tank
 *   POST   /api/report/link/{aquariumId}  – create / replace link
 *   DELETE /api/report/link/{aquariumId}  – delete link
 *
 * @author Stefan Schubert
 */
@RestController
@Slf4j
public class PublicReportController {

    @Autowired
    PublicReportService publicReportService;

    // ---- Public endpoint (no authentication required) ----

    @Operation(summary = "Fetch the public HouseReef report for the given share token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report returned. Check linkExpired flag."),
            @ApiResponse(responseCode = "404", description = "Token unknown.")
    })
    @GetMapping(value = "/api/public/report/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicReefReportTo> getReport(
            @PathVariable("token") String token,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {
        log.debug("GET /api/public/report/{}", token);
        PublicReefReportTo report = publicReportService.getReport(token, lang);
        return ResponseEntity.ok(report);
    }

    // ---- Authenticated endpoints for link management ----

    @Operation(summary = "Get the active public share link for the given aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link returned."),
            @ApiResponse(responseCode = "204", description = "No active link for this aquarium."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping(value = "/api/report/link/{aquariumId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicReportLinkTo> getLinkForTank(
            @PathVariable Long aquariumId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/report/link/{} for {}", aquariumId, principal.getName());
        PublicReportLinkTo link = publicReportService.getLinkForTank(aquariumId, principal.getName());
        if (link == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(link);
    }

    @Operation(summary = "Create or replace the public share link for the given aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Link created/replaced."),
            @ApiResponse(responseCode = "403", description = "Aquarium does not belong to requesting user."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/api/report/link/{aquariumId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicReportLinkTo> createOrReplaceLink(
            @PathVariable Long aquariumId,
            @RequestParam(value = "validUntil", required = false) String validUntilStr,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("POST /api/report/link/{} for {}", aquariumId, principal.getName());
        LocalDateTime validUntil = null;
        if (validUntilStr != null && !validUntilStr.isBlank()) {
            try {
                validUntil = LocalDateTime.parse(validUntilStr);
            } catch (Exception e) {
                log.warn("Invalid validUntil value '{}': {}", validUntilStr, e.getMessage());
            }
        }
        PublicReportLinkTo link = publicReportService.createOrReplaceLink(aquariumId, validUntil, principal.getName());
        if (link == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @Operation(summary = "Delete the public share link for the given aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Link deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/api/report/link/{aquariumId}")
    public ResponseEntity<Void> deleteLink(
            @PathVariable Long aquariumId,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("DELETE /api/report/link/{} for {}", aquariumId, principal.getName());
        publicReportService.deleteLink(aquariumId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ---- Public photo endpoints (no authentication, validated by share token) ----

    @Operation(summary = "Returns the aquarium photo for a valid share token.")
    @GetMapping(value = "/api/public/report/{token}/photo")
    public ResponseEntity<byte[]> getAquariumPhoto(@PathVariable("token") String token) {
        log.debug("GET /api/public/report/{}/photo", token);
        byte[] bytes = publicReportService.getAquariumPhotoBytes(token);
        if (bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    @Operation(summary = "Returns a fish photo for a valid share token.")
    @GetMapping(value = "/api/public/report/{token}/fish/{fishId}/photo")
    public ResponseEntity<byte[]> getFishPhoto(
            @PathVariable("token") String token,
            @PathVariable("fishId") Long fishId) {
        log.debug("GET /api/public/report/{}/fish/{}/photo", token, fishId);
        byte[] bytes = publicReportService.getFishPhotoBytes(token, fishId);
        if (bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    // ---- 004-aquarium-events: include-events flag management ----

    @Operation(summary = "Set or clear the includeEvents flag for the active report link of the given aquarium.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flag updated."),
        @ApiResponse(responseCode = "403", description = "Aquarium does not belong to user or no link exists."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PutMapping(value = "/api/report/link/{aquariumId}/include-events")
    public ResponseEntity<Void> updateIncludeEvents(
            @PathVariable Long aquariumId,
            @RequestParam(value = "includeEvents") boolean includeEvents,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("PUT /api/report/link/{}/include-events?includeEvents={} for user_id=?", aquariumId, includeEvents);
        boolean success = publicReportService.updateIncludeEvents(aquariumId, includeEvents, principal.getName());
        return success ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
