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
 * The complete maintenance block of an aquarium: the profile (regular interval + activated carbon)
 * and all recorded water change events, newest first.
 */
@Data
public class MaintenanceTo implements Serializable {

    @Schema(description = "The maintenance profile of the aquarium.")
    private MaintenanceProfileTo profile = new MaintenanceProfileTo();

    @Schema(description = "Recorded water change events, newest first.")
    private List<WaterChangeTo> waterChanges = new ArrayList<>();
}
