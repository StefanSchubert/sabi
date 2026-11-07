/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Types of recorded maintenance events. Water changes carry a percentage of the tank
 * volume that was changed; carbon replacements are date + note only.
 */
@Schema(description = "Type of a recorded maintenance event.")
public enum MaintenanceEventType {

    WATER_CHANGE,
    CARBON_REPLACEMENT;

    @JsonCreator
    public static MaintenanceEventType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (MaintenanceEventType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
