/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Type of growth measurement for a coral entry.
 * measurement_type is immutable after creation (FR-039).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public enum CoralGrowthType implements Serializable {
    SURFACE_AREA_CM2,
    SIZE_CM,
    VOLUME_CM3,
    BRANCH_COUNT;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static CoralGrowthType fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}

