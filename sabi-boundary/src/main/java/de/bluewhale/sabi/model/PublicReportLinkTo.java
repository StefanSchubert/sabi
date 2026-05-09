/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Transfer object representing a public share link for a HouseReef report.
 * One active link can exist per aquarium at a time.
 * Generating a new link invalidates (overwrites) the previous one.
 *
 * @author Stefan Schubert
 */
@Data
public class PublicReportLinkTo implements Serializable {

    @Schema(description = "Internal ID of this link record.")
    private Long id;

    @Schema(description = "ID of the aquarium this link belongs to.", required = true)
    private Long aquariumId;

    @Schema(description = "UUID share token that forms the public URL.", required = true)
    private String linkToken;

    @Schema(description = "Optional expiry date/time. NULL means no expiry.", required = false)
    private LocalDateTime validUntil;

    @Schema(description = "Timestamp when this link was created.")
    private LocalDateTime createdOn;
}
