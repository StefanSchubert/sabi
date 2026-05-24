/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer object for a coral catalogue entry (proposal / full view).
 * Reuses FishCatalogueStatus for the status field (PENDING / PUBLIC / REJECTED).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Data
public class CoralCatalogueEntryTo implements Serializable {

    private Long id;

    @NotBlank
    private String scientificName;

    @NotNull
    private CoralClassification classification;

    @NotNull
    private CoralCareLevel careLevel;

    /** Reuses FishCatalogueStatus (PENDING / PUBLIC / REJECTED). */
    private FishCatalogueStatus status;

    private Long proposerUserId;

    private LocalDate proposalDate;

    @Valid
    private List<CoralCatalogueI18nTo> i18nEntries = new ArrayList<>();
}

