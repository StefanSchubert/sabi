/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Transfer object representing a fish stock entry in a user's tank.
 * Replaces the deprecated {@link FishTo} (002-fish-stock-catalogue).
 *
 * @author Stefan Schubert
 */
@Data
public class FishStockEntryTo implements Serializable {

    private Long id;

    @Schema(description = "ID of the aquarium this fish belongs to.", required = true)
    private Long aquariumId;

    @NotBlank(message = "fishstock.form.commonname.required")
    @Schema(description = "Common (free-text) name of the fish species.", required = true)
    private String commonName;

    @Schema(description = "Scientific name snapshot from fish catalogue (optional).")
    private String scientificName;

    @Schema(description = "Personal nickname for this fish (optional).")
    private String nickname;

    @Pattern(regexp = "^(https?://.*)?$", message = "fishstock.form.refurl.invalid")
    @Schema(description = "Optional external reference URL for this fish.")
    private String externalRefUrl;

    @NotNull(message = "fishstock.form.entrydate.required")
    @PastOrPresent
    @Schema(description = "Date when the fish was added to the tank.", required = true)
    private LocalDate addedOn;

    @Schema(description = "Date when the fish left the tank (null = still present).")
    private LocalDate exodusOn;

    @Schema(description = "Reason for departure (null if still present).")
    private DepartureReason departureReason;

    @Schema(description = "Optional remark recorded at departure time.")
    private String departureNote;

    @Schema(description = "Optional behaviour observations by the owner.")
    private String observedBehavior;

    @Schema(description = "Optional link to fish catalogue entry.")
    private Long fishCatalogueId;

    @Schema(description = "True if a photo has been uploaded for this fish entry.")
    private boolean hasPhoto;

}

