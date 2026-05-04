/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Describes the ecosystem type of a seawater aquarium.
 *
 * @author Stefan Schubert
 */
public enum EcosystemType {

    UNKNOWN("unknown"),
    SPS("sps"),
    LPS("lps"),
    MIXED_CORAL_FISH("mixed_coral_fish"),
    MIXED_SPS_LPS("mixed_sps_lps");

    private final String type;

    EcosystemType(final String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return type;
    }
}
