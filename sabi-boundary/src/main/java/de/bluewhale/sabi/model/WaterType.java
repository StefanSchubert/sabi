/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Used to distinguish the tank types and therefore the measurement ecologic reference system.
 *
 * @author Stefan Schubert
 */
public enum WaterType {

    SEA_WATER("seawater"),
    FRESH_WATER("freshwater");


    WaterType(final String waterType) {
        this.waterType = waterType;
    }
    @JsonValue
    public String getWaterType() {
        return this.waterType;
    }

    private String waterType;

    @Override
    public String toString() {
        return waterType;
    }
}
