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
 * Transfer object for an invertebrate stock entry.
 * Represents a single invertebrate in a user's marine aquarium.
 * Part of 006-invertebrate-tracking.
 * <p>
 * Reuses {@link CoralCareLevel} (EASY/MODERATE/DEMANDING — identical values).
 * Reuses {@link CoralDepartureReason} (DIED/SOLD/GIVEN_AWAY/MOVED_TO_OTHER_TANK/OTHER — identical values).
 *
 * @author Stefan Schubert
 */
@Data
public class InvertebrateStockEntryTo implements Serializable {

    private Long id;

    @NotNull
    private Long aquariumId;

    @NotBlank
    private String speciesName;

    /** Snapshot from catalogue at link time; user-editable. */
    private String scientificName;

    /** CRUSTACEAN / MOLLUSC / ECHINODERM / WORM — required. */
    @NotNull
    private InvertebrateTaxonomicCategory taxonomicCategory;

    /** EASY / MODERATE / DEMANDING — reuses CoralCareLevel. */
    private CoralCareLevel careLevel;

    /** MOBILE / SESSILE — optional. */
    private InvertebrateMobility mobility;

    /** CLEANUP_CREW / NEUTRAL / DETRIMENTAL — optional. */
    private InvertebrateEcologicalRole ecologicalRole;

    /** DIURNAL / NOCTURNAL / BOTH — optional. */
    private InvertebrateActivityPattern activityPattern;

    /** Unit IDs the invertebrate is sensitive to. Empty list = none specified. */
    private List<Integer> waterSensitivityUnitIds = new ArrayList<>();

    @Pattern(regexp = "^(https?://.*)?$", message = "{invertebratestock.form.refurl.invalid}")
    private String externalRefUrl;

    private String notes;

    @NotNull
    @PastOrPresent
    private LocalDate addedOn;

    private LocalDate departedOn;

    /** Reuses CoralDepartureReason (same values as coral). */
    private CoralDepartureReason departureReason;

    @Size(max = 500)
    private String departureNote;

    /** Optional link to the invertebrate catalogue. */
    private Long invertebrateCatalogueId;

    private boolean hasPhoto;
}
