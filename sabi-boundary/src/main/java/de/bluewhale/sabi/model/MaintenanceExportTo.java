/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Per-aquarium maintenance block of the AI-JSON export (spec 007, FR-013/FR-016).
 * The block is always present: nested objects are initialized by default so that
 * aquariums without maintenance data still serialize the full structure.
 */
@Data
public class MaintenanceExportTo implements Serializable {

    private WaterChangesExportTo waterChanges = new WaterChangesExportTo();
    private ActivatedCarbonExportTo activatedCarbon = new ActivatedCarbonExportTo();
}
