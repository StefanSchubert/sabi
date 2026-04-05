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
 * Represents a single aquarium (tank) with all nested sub-data in the AI Chatbot Data Export.
 *
 * @author Stefan Schubert
 */
@Data
public class AquariumExportTo implements Serializable {

    private Long id;
    private String description;
    private String waterType;
    private Integer size;
    private String sizeUnit;
    private Boolean active;
    private String inceptionDate;
    private List<MeasurementExportTo> measurements = new ArrayList<>();
    private List<PlagueRecordExportTo> plagueRecords = new ArrayList<>();
    private List<FishExportTo> fish = new ArrayList<>();
    private List<CoralExportTo> corals = new ArrayList<>();
    private List<TreatmentExportTo> treatments = new ArrayList<>();
}
