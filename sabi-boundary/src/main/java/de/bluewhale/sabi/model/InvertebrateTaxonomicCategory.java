/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Taxonomic category of an invertebrate.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateTaxonomicCategory implements Serializable {
    CRUSTACEAN,
    MOLLUSC,
    ECHINODERM,
    WORM;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static InvertebrateTaxonomicCategory fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
