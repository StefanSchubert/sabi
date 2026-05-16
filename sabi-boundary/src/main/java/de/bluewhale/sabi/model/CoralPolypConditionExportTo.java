/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Export-optimised representation of a coral polyp condition observation.
 * Part of 005-coral-stock AI-JSON export (FR-035).
 *
 * @author Stefan Schubert
 */
@Data
public class CoralPolypConditionExportTo implements Serializable {

    /** ISO date string, e.g. 2026-04-15 */
    private String observedOn;
    private String condition;
}

