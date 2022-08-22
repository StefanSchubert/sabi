/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ParameterTo contains additional detail information about measurement units such as threshold values.
 * Might have been modeled as static class/enum but I prefer have it ready for a later admin GUI for maintenance.
 */
@Data
public class ParameterTo implements Serializable {

    @Schema(description =  "References the unique unit in which a measurement has been taken.", required = true)
    private Integer id;
    @Schema(description =  "References the belonging measurement Unit.", required = true)
    private Integer belongingUnitId;
    @Schema(description =  "Recommendation according natural seawater composition. Measurement value should not fall below this threshold.", required = true)
    private Float minThreshold;
    @Schema(description =  "Recommendation according natural seawater composition. Measurement value should not be higher than this threshold.", required = true)
    private Float maxThreshold;
    @Schema(description =  "Short description of the unit.", required = true)
    private String description;

}
