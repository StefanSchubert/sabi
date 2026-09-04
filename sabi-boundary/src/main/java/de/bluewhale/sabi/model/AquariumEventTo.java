/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
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

    @Schema(description = "Optional event time in 24h HH:mm format. Required for dosing/addition records.")
    private String eventTime;

    @NotNull
    @Schema(description = "Type of logbook entry.", required = true)
    private AquariumEventType eventType = AquariumEventType.GENERIC;

    @Positive
    @Schema(description = "Optional duration in hours; must be > 0 when present.", required = false)
    private BigDecimal durationHours;

    @Schema(description = "Free-form multi-line description for generic events. For structured dosing records the backend stores a generated summary here.")
    private String description;

    @Positive
    @Schema(description = "Added/dosed amount of the product or solution.", required = false)
    private BigDecimal amount;

    @Schema(description = "Unit of the added/dosed amount, e.g. ml, g, tablets, drops.", required = false)
    private String amountUnit;

    @Schema(description = "Product, substance or user-defined solution name.", required = false)
    private String productName;

    @Schema(description = "Optional user-defined category, e.g. nutrient, calcium, trace elements.", required = false)
    private String category;

    @Schema(description = "Optional interval/frequency description for automated dosing, e.g. day.", required = false)
    private String dosingInterval;

    @Schema(description = "Optional dosing method, e.g. dosing pump.", required = false)
    private String dosingMethod;

    @Schema(description = "Optional solution or concentration description.", required = false)
    private String solutionDescription;

    @Schema(description = "Optional note for structured dosing/addition records.", required = false)
    private String note;

    @Schema(description = "Optional end timestamp for automated dosing.", required = false)
    private LocalDateTime dosingEndOn;

    @Schema(description = "Timestamp when this record was created (server-set; ignored on POST/PUT).")
    private LocalDateTime createdOn;

    @Schema(description = "Timestamp of last modification (server-set; ignored on POST/PUT).")
    private LocalDateTime updatedOn;   // maps to DB column lastmod_on via entity

    @Schema(description = "Optimistic lock version (server-set; must be sent back on PUT to detect concurrent edits).")
    private long optlock;

    @JsonIgnore
    @AssertTrue(message = "A generic event requires a description.")
    public boolean isGenericDescriptionValid() {
        return getResolvedEventType() != AquariumEventType.GENERIC
                || (description != null && !description.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "Dosing/addition records require product, amount, amount unit and time.")
    public boolean isAdditionPayloadValid() {
        if (getResolvedEventType() == AquariumEventType.GENERIC) {
            return true;
        }
        return productName != null && !productName.isBlank()
                && amount != null
                && amount.signum() > 0
                && amountUnit != null && !amountUnit.isBlank()
                && eventTime != null && !eventTime.isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "Automated dosing requires a dosing interval/frequency.")
    public boolean isAutomatedDosingIntervalValid() {
        return getResolvedEventType() != AquariumEventType.AUTOMATED_DOSING
                || (dosingInterval != null && !dosingInterval.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "eventTime must use 24h HH:mm format.")
    public boolean isEventTimeFormatValid() {
        return eventTime == null || eventTime.isBlank() || eventTime.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$");
    }

    @JsonIgnore
    public AquariumEventType getResolvedEventType() {
        return eventType != null ? eventType : AquariumEventType.GENERIC;
    }
}
