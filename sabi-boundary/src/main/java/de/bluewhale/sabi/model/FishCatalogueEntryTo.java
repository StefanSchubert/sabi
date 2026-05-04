/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer object for a fish catalogue entry (with UGC workflow and i18n support).
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Data
public class FishCatalogueEntryTo implements Serializable {

    private Long id;

    @NotBlank(message = "fishcatalogue.scientificname.required")
    @Schema(description = "Scientific name of the fish species.", required = true)
    private String scientificName;

    @Schema(description = "UGC status: PENDING | PUBLIC | REJECTED")
    private FishCatalogueStatus status;

    @Schema(description = "ID of the user who proposed this entry (admin context only).")
    private Long proposerUserId;

    @Schema(description = "Date the proposal was submitted.")
    private LocalDate proposalDate;

    @Valid
    @Schema(description = "Localized fields for this catalogue entry.")
    private List<FishCatalogueI18nTo> i18nEntries = new ArrayList<>();

}

