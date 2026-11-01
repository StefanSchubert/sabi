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
 * Water change part of the per-aquarium maintenance export block.
 * {@code regularInterval} is null when no interval is configured; {@code records}
 * is an empty list when no water change events were recorded.
 */
@Data
public class WaterChangesExportTo implements Serializable {

    private RegularIntervalExportTo regularInterval;
    private List<WaterChangeRecordExportTo> records = new ArrayList<>();
}
