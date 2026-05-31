/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Lightweight search result for invertebrate catalogue autocomplete.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Data
public class InvertebrateCatalogueSearchResultTo implements Serializable {
    private Long id;
    private String scientificName;
    private String commonName;
    private InvertebrateTaxonomicCategory taxonomicCategory;
    private CoralCareLevel careLevel;
    private String referenceUrl;
    private FishCatalogueStatus status;
}
