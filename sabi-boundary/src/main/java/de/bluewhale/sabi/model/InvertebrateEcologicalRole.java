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
 * A single invertebrate may fulfil zero, one, or multiple roles simultaneously.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateEcologicalRole implements Serializable {
    /** Removes algae, biofilm, or other unwanted growth. */
    CLEANER,
    /** Actively grazes algae growth. */
    GRAZER,
    /** Processes detritus and organic deposits. */
    DETRITIVORE,
    /** Consumes leftover food and carrion. */
    SCAVENGER,
    /** Works or aerates the sandbed. */
    SAND_MAINTAINER,
    /** Filters particles from the water column. */
    FILTER_FEEDER,
    /** Cleans other animals or lives in a cleaning symbiosis. */
    SYMBIOTIC_CLEANER,
    /** Actively modifies the environment. */
    ECOSYSTEM_ENGINEER,
    /** Reacts sensitively to environmental changes; serves as a biological indicator. */
    INDICATOR_SPECIES,
    /** Directly influences corals – positively or negatively. */
    CORAL_INTERACTOR;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static InvertebrateEcologicalRole fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
