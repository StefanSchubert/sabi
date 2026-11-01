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
import lombok.Data;

import java.io.Serializable;

/**
 * Request body for updating the activated carbon setting of an aquarium.
 * When disabled, mode and replacement interval are ignored (stored as null).
 */
@Data
public class ActivatedCarbonTo implements Serializable {

    @NotNull
    @Schema(description = "Whether activated carbon filtering is used.", required = true)
    private Boolean enabled;

    @Schema(description = "Carbon mode, required when enabled. CONTINUOUS or INTERVAL.")
    private CarbonMode mode;

    @Min(1)
    @Max(365)
    @Schema(description = "Replacement interval in days (1-365), required when mode is INTERVAL.")
    private Integer replacementIntervalDays;

    @JsonIgnore
    @AssertTrue(message = "A carbon mode is required when activated carbon is enabled.")
    public boolean isModeRequiredWhenEnabled() {
        return !Boolean.TRUE.equals(enabled) || mode != null;
    }

    @JsonIgnore
    @AssertTrue(message = "A replacement interval is required for INTERVAL mode.")
    public boolean isReplacementIntervalRequiredForIntervalMode() {
        return mode == null || mode != CarbonMode.INTERVAL || replacementIntervalDays != null;
    }
}
