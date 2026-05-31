/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents a single coral entry in the public House Reef Report.
 * Part of 005-coral-stock (FR-032).
 *
 * @author Stefan Schubert
 */
@Data
public class PublicReefReportCoralTo implements Serializable {

    /** DB ID of the coral stock entry — used to fetch the photo via the public photo endpoint. */
    private Long id;

    private String speciesName;
    private String classification;

    /** True when a photo has been uploaded for this coral. */
    private boolean hasPhoto;
    /** Latest growth measurement value per measurement type (e.g. SURFACE_AREA_CM2 → 45.5). */
    private Map<String, BigDecimal> latestGrowthByType;
    /** Latest polyp condition string, or null if no observations exist. */
    private String latestPolypCondition;
}

