/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Instead of static enums I prefer to be able to add a new unit without the need of a redeployment.
 * User: Stefan Schubert
 * Date: 12.03.15
 */
@Data
public class UnitTo implements Serializable {

    @Schema(name = "References the unique unit in which a measurement has been taken.", required = true)
    private Integer id;

    @Schema(name = "Sign (Abbreviation) of measurements unit like 'PO4'", required = true)
    private String unitSign;

    @Schema(name = "Short description of the unit might contain threshold information.", required = true)
    private String description;


}
