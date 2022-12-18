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
 * Contains a "remind me to measure something" record, which the user sets in his profile.
 */
@Data
public class MeasurementReminderTo implements Serializable {

    @Schema(description =  "How many days shall past after the last measurement of the unit until a reminder will be created.", required = true)
    private int pastDays;

    @Schema(description =  "Date of next measurement.", required = false)
    private LocalDateTime nextMeasureDate;

    @Schema(description =  "References the unit this reminder belongs to.", required = true)
    private int unitId;

    @Schema(description =  "Name of the unit for drop down.", required = true)
    private String unitName;

    @Schema(description =  "References the user this reminder belongs to.", required = true)
    private long userId;

    @Schema(description =  "Is the setting active?", defaultValue = "true", required = false)
    private boolean active;

}
