/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Reason why a fish left a tank.
 * Part of 002-fish-stock-catalogue (FR-005).
 *
 * @author Stefan Schubert
 */
public enum DepartureReason implements Serializable {
    DECEASED,
    REMOVED_REHOMED,
    UNKNOWN;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static DepartureReason fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}

