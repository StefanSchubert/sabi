/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Transport Objekt which references the fish with a catalogue, to support
 * a common fish base. This is the required common attribute. Without it
 * we would't be able to analyse treatments between the different tanks.
 *
 * @author Stefan Schubert
 */
@Data
public class FishCatalogueTo implements Serializable {

    private Long id;
    @Schema(description =  "Scientific name of the fish, which might ease the fish lookup from the catalogue.", required = true)
    private String scientificName;
    @Schema(description =  "Short description of the fish (or localized name?), the detailed description relies in public wikis.", required = false)
    private String description;
    @Schema(description =  "Link to a public wiki which describes the fish in detail.", required = false)
    private String meerwasserwikiUrl;
}
