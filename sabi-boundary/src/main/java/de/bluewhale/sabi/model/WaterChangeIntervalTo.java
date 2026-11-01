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
import lombok.Data;

import java.io.Serializable;

/**
 * Request body for updating (or clearing) the regular water exchange interval of an aquarium.
 * Both fields must be set together, or both must be null to clear the interval.
 */
@Data
public class WaterChangeIntervalTo implements Serializable {

    @Min(1)
    @Max(365)
    @Schema(description = "Days between regular water changes (1-365). Must be set together with percentage; null to clear.")
    private Integer intervalDays;

    @Min(1)
    @Max(100)
    @Schema(description = "Percentage of tank volume for regular water changes (1-100). Must be set together with intervalDays; null to clear.")
    private Integer percentage;

    @JsonIgnore
    @AssertTrue(message = "Both interval days and percentage must be set, or both must be null to clear the regular interval.")
    public boolean isIntervalComplete() {
        return (intervalDays == null) == (percentage == null);
    }
}
