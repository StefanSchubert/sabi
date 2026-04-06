/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Transfer object for recording a fish departure (FR-005, FR-006).
 *
 * @author Stefan Schubert
 */
@Data
public class FishDepartureRecordTo implements Serializable {

    @NotNull(message = "fishstock.departure.date.label")
    private LocalDate departureDate;

    @NotNull
    private DepartureReason departureReason;

}

