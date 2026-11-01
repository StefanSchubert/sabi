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
 * Transfer object for a recorded water change event (date-only precision, back-dating allowed).
 */
@Data
public class WaterChangeTo implements Serializable {

    @Schema(description = "Internal PK, null when creating a new water change record.")
    private Long id;

    @Schema(description = "ID of the aquarium this water change belongs to. Set by the server from the URL.")
    private Long aquariumId;

    @NotNull
    @Schema(description = "Date of the water change (date-only, no time component). Must not be in the future.",
            required = true)
    private LocalDate changedOn;

    @NotNull
    @Min(1)
    @Max(100)
    @Schema(description = "Percentage of tank volume that was changed (integer 1-100).", required = true)
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
    @AssertTrue(message = "A water change cannot be recorded for a future date.")
    public boolean isChangedOnNotInFuture() {
        return changedOn == null || !changedOn.isAfter(LocalDate.now());
    }
}
