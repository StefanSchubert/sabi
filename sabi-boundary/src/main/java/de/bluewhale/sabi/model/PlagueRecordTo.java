/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PlagueRecordTo implements Serializable {

    @Schema(description =  "Primary Key", required = true)
    private Long id;

    @Schema(description =  "Point in time when the observation has been taken.", required = true)
    private LocalDateTime observedOn;

    @Schema(description =  "References the observed plague.", required = true)
    private Integer plagueId;

    @Schema(description =  "References the observed plague status.", required = true)
    private Integer plagueStatusId;

    @Schema(description =  "References the Aquarium this measurement belongs to.", required = true)
    private Long aquariumId;

}
