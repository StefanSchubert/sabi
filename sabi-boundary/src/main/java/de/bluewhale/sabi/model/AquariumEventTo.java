/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transfer object for a single aquarium logbook event.
 * Used by the authenticated CRUD API and by public report assembly.
 * Feature: 004-aquarium-events.
 */
@Data
public class AquariumEventTo implements Serializable {

    @Schema(description = "Internal PK, null when creating a new event.")
    private Long id;

    @NotNull
    @Schema(description = "ID of the aquarium this event belongs to.", required = true)
    private Long aquariumId;

    @NotNull
    @Schema(description = "Calendar date the event occurred (no time-of-day).", required = true)
    private LocalDate eventDate;

    @Positive
    @Schema(description = "Optional duration in hours; must be > 0 when present.", required = false)
    private BigDecimal durationHours;

    @NotNull
    @Schema(description = "Free-form multi-line description. Line breaks are preserved.", required = true)
    private String description;

    @Schema(description = "Timestamp when this record was created (server-set; ignored on POST/PUT).")
    private LocalDateTime createdOn;

    @Schema(description = "Timestamp of last modification (server-set; ignored on POST/PUT).")
    private LocalDateTime updatedOn;   // maps to DB column lastmod_on via entity

    @Schema(description = "Optimistic lock version (server-set; must be sent back on PUT to detect concurrent edits).")
    private long optlock;
}

