/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Export-optimised representation of a coral growth measurement record.
 * Part of 005-coral-stock AI-JSON export (FR-035).
 *
 * @author Stefan Schubert
 */
@Data
public class CoralGrowthHistoryExportTo implements Serializable {

    /** ISO date string, e.g. 2026-04-15 */
    private String measuredOn;
    private String measurementType;
    private BigDecimal measurementValue;
}

