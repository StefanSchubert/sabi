/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;

/**
 * Status of a fish catalogue entry in the UGC workflow.
 * Part of 002-fish-stock-catalogue (FR-013–FR-017).
 *
 * @author Stefan Schubert
 */
public enum FishCatalogueStatus implements Serializable {
    PENDING,
    PUBLIC,
    REJECTED;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static FishCatalogueStatus fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}

