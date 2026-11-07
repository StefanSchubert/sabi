/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Activated carbon setting of an aquarium in the AI-JSON export (FR-013).
 * {@code mode} and {@code replacementIntervalDays} are null when carbon is disabled
 * or running in continuous mode; {@code mode} is "INTERVAL" only for interval mode.
 */
@Data
public class ActivatedCarbonExportTo implements Serializable {

    private Boolean enabled = false;
    private String mode;
    private Integer replacementIntervalDays;
}
