/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Localised fields for one language of an invertebrate catalogue entry.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Data
public class InvertebrateCatalogueI18nTo implements Serializable {
    private Long id;
    private String languageCode;
    private String commonName;
    @Size(max = 2000)
    private String description;
    private String referenceUrl;
}
