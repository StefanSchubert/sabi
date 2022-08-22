/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * User: Stefan
 * Date: 05.01.16
 */
@Data
public class MeasurementTo implements Serializable {

    private Long id;

    @Schema(description =  "Point in time when the measurement has been taken.", required = true)
    private LocalDateTime measuredOn;

    @Schema(description =  "Decimal value of the measurement.", required = true)
    private float measuredValue;

    @Schema(description =  "References the used unit this measurement belongs to.", required = true)
    private int unitId;

    @Schema(description =  "References the Aquarium this measurement belongs to.", required = true)
    private Long aquariumId;

}
