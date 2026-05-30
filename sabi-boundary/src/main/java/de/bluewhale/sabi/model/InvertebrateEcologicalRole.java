/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Ecological role of an invertebrate in the tank ecosystem.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateEcologicalRole implements Serializable {
    CLEANUP_CREW,
    NEUTRAL,
    DETRIMENTAL;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static InvertebrateEcologicalRole fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
