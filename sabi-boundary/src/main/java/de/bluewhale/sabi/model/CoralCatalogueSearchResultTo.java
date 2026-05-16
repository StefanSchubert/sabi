/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Lightweight search result for the coral catalogue (for autocomplete in the stock entry form).
 * Language-resolved: commonName and referenceUrl are for the requested language.
 * Part of 005-coral-stock (FR-030).
 *
 * @author Stefan Schubert
 */
@Data
public class CoralCatalogueSearchResultTo implements Serializable {

    private Long id;
    private String scientificName;
    /** Resolved for the requested language. */
    private String commonName;
    private CoralClassification classification;
    private CoralCareLevel careLevel;
    /** Resolved for the requested language. */
    private String referenceUrl;
    /** Reuses FishCatalogueStatus (PENDING / PUBLIC / REJECTED). */
    private FishCatalogueStatus status;
}

