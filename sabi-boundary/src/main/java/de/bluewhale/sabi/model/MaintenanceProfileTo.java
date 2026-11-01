/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * The maintenance profile of an aquarium: regular water exchange interval and activated carbon setting.
 * Defaults when no profile row exists: all interval fields null, carbon disabled.
 */
@Data
public class MaintenanceProfileTo implements Serializable {

    @Schema(description = "Days between regular water changes (1-365), null when no regular interval is set.")
    private Integer regularIntervalDays;

    @Schema(description = "Percentage of tank volume for regular water changes (1-100), null when no regular interval is set.")
    private Integer regularPercentage;

    @Schema(description = "Whether activated carbon filtering is used.")
    private Boolean carbonEnabled = false;

    @Schema(description = "Carbon mode, null when carbon is disabled.")
    private CarbonMode carbonMode;

    @Schema(description = "Carbon replacement interval in days, null unless mode is INTERVAL.")
    private Integer carbonReplacementIntervalDays;
}
