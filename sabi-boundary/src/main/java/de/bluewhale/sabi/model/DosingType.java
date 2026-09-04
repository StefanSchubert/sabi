/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.Locale;

/**
 * Specifies whether a dosing record describes a one-time manual addition or an automated schedule.
 */
public enum DosingType implements Serializable {
    MANUAL_ADDITION,
    AUTOMATED_DOSING;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static DosingType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
