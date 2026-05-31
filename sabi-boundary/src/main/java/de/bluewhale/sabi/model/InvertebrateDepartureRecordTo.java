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
 * Transfer object for recording an invertebrate departure.
 * Part of 006-invertebrate-tracking.
 * <p>
 * Reuses {@link CoralDepartureReason} (same values: DIED/SOLD/GIVEN_AWAY/MOVED_TO_OTHER_TANK/OTHER).
 *
 * @author Stefan Schubert
 */
@Data
public class InvertebrateDepartureRecordTo implements Serializable {

    @NotNull
    private LocalDate departedOn;

    @NotNull
    private CoralDepartureReason departureReason;

    @Size(max = 500)
    private String departureNote;
}
