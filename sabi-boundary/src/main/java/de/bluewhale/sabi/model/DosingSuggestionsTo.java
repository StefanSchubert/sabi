/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggestion values for the dosing form: distinct product names, categories and dosing
 * methods that the user has already recorded in any of their tanks. Each list is empty
 * when no value exists yet; lists are sorted alphabetically and case-insensitive.
 */
@Data
public class DosingSuggestionsTo implements Serializable {

    @Schema(description = "Distinct product names already used by the user.")
    private List<String> productNames = new ArrayList<>();

    @Schema(description = "Distinct categories already used by the user.")
    private List<String> categories = new ArrayList<>();

    @Schema(description = "Distinct dosing methods already used by the user.")
    private List<String> dosingMethods = new ArrayList<>();
}
