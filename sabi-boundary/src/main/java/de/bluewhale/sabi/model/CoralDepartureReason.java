/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Reason why a coral left a tank.
 * Part of 005-coral-stock (FR-005).
 *
 * @author Stefan Schubert
 */
public enum CoralDepartureReason implements Serializable {
    DIED,
    SOLD,
    GIVEN_AWAY,
    MOVED_TO_OTHER_TANK,
    OTHER;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static CoralDepartureReason fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}

