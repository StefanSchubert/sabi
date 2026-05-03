/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Lightweight search result for fish catalogue dropdown (FR-020).
 * Language-specific common name and reference URL are resolved server-side.
 *
 * @author Stefan Schubert
 */
@Data
public class FishCatalogueSearchResultTo implements Serializable {

    private Long id;

    @Schema(description = "Scientific name of the fish species.")
    private String scientificName;

    @Schema(description = "Common name in the requested language (with fallback).")
    private String commonName;

    @Schema(description = "Reference URL in the requested language (with fallback).")
    private String referenceUrl;

    @Schema(description = "UGC status: PENDING | PUBLIC | REJECTED")
    private FishCatalogueStatus status;

}

