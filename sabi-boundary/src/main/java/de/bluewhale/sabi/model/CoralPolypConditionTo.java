/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Transfer object for a coral polyp condition observation.
 * Editable fields: observedOn and condition (FR-040).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Data
public class CoralPolypConditionTo implements Serializable {

    private Long id;
    private Long coralStockEntryId;

    @NotNull
    @PastOrPresent
    private LocalDate observedOn;

    @NotNull
    private PolypCondition condition;
}

