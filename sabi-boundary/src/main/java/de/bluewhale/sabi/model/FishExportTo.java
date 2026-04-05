/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents a single fish entry in the AI Chatbot Data Export.
 *
 * @author Stefan Schubert
 */
@Data
public class FishExportTo implements Serializable {

    private Long fishCatalogueId;
    private String scientificName;
    private String addedOn;
    private String observedBehavior;
}
