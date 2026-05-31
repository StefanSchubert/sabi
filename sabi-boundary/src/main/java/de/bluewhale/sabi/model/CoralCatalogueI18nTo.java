/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Transfer object for a localised coral catalogue entry (one per language).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Data
public class CoralCatalogueI18nTo implements Serializable {

    private Long id;
    /** ISO 639-1 two-letter code: de, en, es, fr, it */
    private String languageCode;
    private String commonName;
    @Size(max = 2000)
    private String description;
    private String referenceUrl;
}

