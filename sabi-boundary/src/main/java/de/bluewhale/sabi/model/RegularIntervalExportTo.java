/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * The regular water exchange interval of an aquarium in the AI-JSON export.
 */
@Data
public class RegularIntervalExportTo implements Serializable {

    private Integer intervalDays;
    private Integer percentage;
}
