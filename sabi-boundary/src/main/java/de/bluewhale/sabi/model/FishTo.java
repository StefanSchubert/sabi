/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Transportobject describing a fish
 *
 * @author Stefan Schubert
 */
@Data
public class FishTo implements Serializable  {

    private Long id;
    private Long aquariumId;
    @Schema(description =  "Loose coupled reference to the fish catalogue which identifies the inhabitant within the community.", required = true)
    private Long fishCatalogueId;
    @Schema(description =  "Telling when the fish was added to the tank.", required = true)
    private LocalDate addedOn;
    @Schema(description =  "Telling when the fish died. By what reason ever.", required = false)
    private LocalDate exodusOn;
    @Schema(description =  "For owners sake. Especially thought for being able to distinguish them when you have more of a kind.", required = true)
    private String nickname;
    @Schema(description =  "Might be used by the owner to describe the observed behavior, so he or she might comapre it against public descriptions.", required = false)
    private String observedBehavior;

}
