/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transfer object for a recorded maintenance event (date-only precision, back-dating allowed).
 * Two types exist: {@link MaintenanceEventType#WATER_CHANGE} (requires a percentage of 1-100)
 * and {@link MaintenanceEventType#CARBON_REPLACEMENT} (date + note only, no percentage).
 */
@Data
public class MaintenanceEventTo implements Serializable {

    @Schema(description = "Internal PK, null when creating a new maintenance event.")
    private Long id;

    @Schema(description = "ID of the aquarium this event belongs to. Set by the server from the URL.")
    private Long aquariumId;

    @NotNull
    @Schema(description = "Type of the maintenance event.", required = true)
    private MaintenanceEventType maintenanceType;

    @NotNull
    @Schema(description = "Date of the event (date-only, no time component). Must not be in the future.",
            required = true)
    private LocalDate changedOn;

    @Min(1)
    @Max(100)
    @Schema(description = "Percentage of tank volume that was changed (integer 1-100). Required for "
            + "WATER_CHANGE, must be null for CARBON_REPLACEMENT.")
    private Integer percentage;

    @Size(max = 200)
    @Schema(description = "Optional note, at most 200 characters.")
    private String note;

    @Schema(description = "Timestamp when this record was created (server-set).")
    private LocalDateTime createdOn;

    @Schema(description = "Timestamp of last modification (server-set).")
    private LocalDateTime updatedOn;

    @Schema(description = "Optimistic lock version (server-set; send it back on PUT).")
    private long optlock;

    @JsonIgnore
    @AssertTrue(message = "A maintenance event cannot be recorded for a future date.")
    public boolean isChangedOnNotInFuture() {
        return changedOn == null || !changedOn.isAfter(LocalDate.now());
    }

    @JsonIgnore
    @AssertTrue(message = "A water change must carry a percentage between 1 and 100.")
    public boolean isWaterChangePercentagePresent() {
        if (maintenanceType != MaintenanceEventType.WATER_CHANGE) {
            return true;
        }
        return percentage != null && percentage >= 1 && percentage <= 100;
    }

    @JsonIgnore
    @AssertTrue(message = "A carbon replacement must not carry a percentage.")
    public boolean isCarbonReplacementPercentageAbsent() {
        if (maintenanceType != MaintenanceEventType.CARBON_REPLACEMENT) {
            return true;
        }
        return percentage == null;
    }
}
