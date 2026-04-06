/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Localized fields for a fish catalogue entry.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Data
public class FishCatalogueI18nTo implements Serializable {

    /** Nullable for new entries (before persist). */
    private Long id;

    /** Language code: de | en | es | fr | it */
    private String languageCode;

    private String commonName;

    @Size(max = 2000, message = "fishcatalogue.i18n.description.maxlength.error")
    private String description;

    private String referenceUrl;

}

