/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer object for an invertebrate catalogue entry.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Data
public class InvertebrateCatalogueEntryTo implements Serializable {
    private Long id;
    @NotBlank
    private String scientificName;
    private InvertebrateTaxonomicCategory taxonomicCategory;
    private CoralCareLevel careLevel;
    private FishCatalogueStatus status;
    private Long proposerUserId;
    private LocalDate proposalDate;
    @Valid
    private List<InvertebrateCatalogueI18nTo> i18nEntries = new ArrayList<>();
}
