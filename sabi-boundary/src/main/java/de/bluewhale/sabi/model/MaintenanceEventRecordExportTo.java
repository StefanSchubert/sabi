/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * A single recorded maintenance event in the AI-JSON export.
 * {@code type} is "WATER_CHANGE" or "CARBON_REPLACEMENT"; {@code date} is an ISO-8601
 * date-only string (yyyy-MM-dd); {@code percentage} is null for carbon replacements.
 */
@Data
public class MaintenanceEventRecordExportTo implements Serializable {

    private String type;
    private String date;
    private Integer percentage;
    private String note;
}
