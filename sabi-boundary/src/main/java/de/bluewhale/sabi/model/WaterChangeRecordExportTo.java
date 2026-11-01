/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * A single recorded water change event in the AI-JSON export.
 * {@code date} is an ISO-8601 date-only string (yyyy-MM-dd).
 */
@Data
public class WaterChangeRecordExportTo implements Serializable {

    private String date;
    private Integer percentage;
    private String note;
}
