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
 * Reduced invertebrate view for the public HouseReef report.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Data
public class PublicReefReportInvertebrateTo implements Serializable {
    private String speciesName;
    private InvertebrateTaxonomicCategory taxonomicCategory;
    private InvertebrateMobility mobility;
    private List<InvertebrateEcologicalRole> ecologicalRoles = new ArrayList<>();
    private InvertebrateActivityPattern activityPattern;
    private List<String> waterSensitivityUnitNames;
}
