/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Transport Object for Aquarium
 *
 * @author Stefan Schubert
 */
@Data
public class AquariumTo implements Serializable {

    @Schema(description =  "ID this object for further reference.")
    private Long id;

    @Schema(description =  "Tanks volume", required = true)
    private Integer size;

    @Schema(description =  "Unit of tanks volume size.", required = true)
    private SizeUnit sizeUnit;

    @Schema(description =  "Fresh, Sea- or other Water Type.", required = true)
    private WaterType waterType;

    @Schema(description =  "Description or Name of the tank, so the user can distinguish them.", required = true)
    private String description;

    @Schema(description =  "API-Key which can be used to submit temperature measurements for this tank by an IoT device.", required = false)
    private String temperatureApiKey;

    @Schema(description =  "Flag telling if this tank is still in used, or if it meanwhile has been disolved.", required = false)
    private Boolean active;

    @Schema(description =  "UserID - will be ignored. Set through processing.", required = false)
    private Long userId;

    @Schema(description =  "Date since when this tank is up and running.", required = false)
    private Date inceptionDate;

}
