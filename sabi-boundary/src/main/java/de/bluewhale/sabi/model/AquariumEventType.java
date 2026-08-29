/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Distinguishes generic aquarium logbook events from structured dosing/addition records.
 */
public enum AquariumEventType implements Serializable {
    GENERIC,
    MANUAL_ADDITION,
    AUTOMATED_DOSING;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static AquariumEventType fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
