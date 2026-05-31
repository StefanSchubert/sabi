/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Transfer object for recording a coral departure.
 * Part of 005-coral-stock (FR-003 to FR-006).
 *
 * @author Stefan Schubert
 */
@Data
public class CoralDepartureRecordTo implements Serializable {

    @NotNull
    private LocalDate departureDate;

    @NotNull
    private CoralDepartureReason departureReason;

    @Size(max = 500)
    private String departureNote;
}

