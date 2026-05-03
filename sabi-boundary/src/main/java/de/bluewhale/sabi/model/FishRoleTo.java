/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Transfer object representing a fish role with its localized name and description.
 *
 * @author Stefan Schubert
 */
@Data
public class FishRoleTo implements Serializable {

    @Schema(description = "Numeric ID of the fish role.")
    private Integer id;

    @Schema(description = "Program-internal enum key of the role (e.g. INDICATOR_FISH).")
    private String enumKey;

    @Schema(description = "Localized display name of the role.")
    private String localizedName;

    @Schema(description = "Localized description of the role.")
    private String localizedDescription;

}
