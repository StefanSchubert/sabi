/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-aquarium maintenance block of the AI-JSON export (spec 007, FR-013/FR-016).
 * The block is always present: nested objects are initialized by default so that
 * aquariums without maintenance data still serialize the full structure.
 * {@code regularInterval} is null when no interval is configured; {@code events}
 * is an empty list when no maintenance events were recorded.
 */
@Data
public class MaintenanceExportTo implements Serializable {

    private RegularIntervalExportTo regularInterval;
    private List<MaintenanceEventRecordExportTo> events = new ArrayList<>();
    private ActivatedCarbonExportTo activatedCarbon = new ActivatedCarbonExportTo();
}
