/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
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

    @Schema(name = "ID this object for further reference.")
    private Long id;

    @Schema(name = "Tanks volume", required = true)
    private Integer size;

    @Schema(name = "Unit of tanks volume size.", required = true)
    private SizeUnit sizeUnit;

    @Schema(name = "Description or Name of the tank, so the user can distinguish them.", required = true)
    private String description;

    @Schema(name = "Flag telling if this tank is still in used, or if it meanwhile has been disolved.", required = false)
    private Boolean active;

    @Schema(name = "UserID - will be ignored. Set through processing.", required = false)
    private Long userId;

    @Schema(name = "Date since when this tank is up and running.", required = false)
    private Date inceptionDate;

}
