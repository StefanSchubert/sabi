/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer object for a coral stock entry.
 * Represents a single coral in a user's marine aquarium.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Data
public class CoralStockEntryTo implements Serializable {

    private Long id;

    @NotNull
    private Long aquariumId;

    @NotBlank
    private String speciesName;

    /** Snapshot from catalogue at link time; user-editable. */
    private String scientificName;

    /** LPS / SPS snapshot; user-editable. */
    private CoralClassification classification;

    /** EASY / MODERATE / DEMANDING snapshot. */
    private CoralCareLevel careLevel;

    @Pattern(regexp = "^(https?://.*)?$", message = "{coralstock.form.refurl.invalid}")
    private String externalRefUrl;

    private String notes;

    @NotNull
    @PastOrPresent
    private LocalDate addedOn;

    private LocalDate departedOn;

    private CoralDepartureReason departureReason;

    @Size(max = 500)
    private String departureNote;

    /** Optional link to the coral catalogue. */
    private Long coralCatalogueId;

    private boolean hasPhoto;

    private List<CoralGrowthHistoryTo> growthHistory = new ArrayList<>();

    private List<CoralPolypConditionTo> polypConditionHistory = new ArrayList<>();
}

