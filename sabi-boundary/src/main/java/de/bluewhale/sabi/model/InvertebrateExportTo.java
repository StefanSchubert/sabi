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
 * Represents a single invertebrate entry in the AI Chatbot Data Export.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Data
public class InvertebrateExportTo implements Serializable {

    private Long catalogueId;
    private String scientificName;
    private String speciesName;
    private String taxonomicCategory;
    private String mobility;
    private List<String> ecologicalRoles = new ArrayList<>();
    private String activityPattern;
    /** ISO date string */
    private String addedOn;
    /** ISO date string; null if still active */
    private String departedOn;
    private String departureReason;
    private String departureNote;
    private String notes;
    private List<String> waterSensitivityUnits = new ArrayList<>();
}
