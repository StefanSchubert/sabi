/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transfer object for a coral growth measurement record.
 * Note: measurement_type is IMMUTABLE after creation (FR-039).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Data
public class CoralGrowthHistoryTo implements Serializable {

    private Long id;
    private Long coralStockEntryId;

    @NotNull
    @PastOrPresent
    private LocalDate measuredOn;

    /** Immutable after creation (FR-039). */
    @NotNull
    private CoralGrowthType measurementType;

    @NotNull
    @DecimalMin("0.1")
    private BigDecimal measurementValue;
}

