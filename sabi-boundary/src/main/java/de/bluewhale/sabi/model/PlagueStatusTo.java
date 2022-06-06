/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class PlagueStatusTo implements Serializable {

    @Schema(name = "References the plague status.", required = true)
    private Integer id;

    @Schema(name = "i18n description  of the plague status", required = true)
    private String description;

}
