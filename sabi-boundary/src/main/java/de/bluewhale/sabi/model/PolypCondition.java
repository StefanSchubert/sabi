/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Polyp condition state for a coral observation.
 * Part of 005-coral-stock (FR-018).
 *
 * @author Stefan Schubert
 */
public enum PolypCondition implements Serializable {
    VITAL,
    TISSUE_LOSS,
    PALE,
    LIMP,
    SIGNIFICANT_GROWTH;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static PolypCondition fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}

