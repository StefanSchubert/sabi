/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Mobility classification for an invertebrate.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateMobility implements Serializable {
    MOBILE,
    SESSILE;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static InvertebrateMobility fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
