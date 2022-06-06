/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class PlagueTo implements Serializable {

    @Schema(name = "References the unique plague.", required = true)
    private Integer id;

    @Schema(name = "i18n Name of the plague like 'Cyano Alge' in English.", required = true)
    private String commonName;

    @Schema(name = "unique Scientific Name of the plague", required = false)
    private String ScientificName;

}
