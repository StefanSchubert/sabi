/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level document for the AI Chatbot Data Export.
 * Contains a metadata block and a list of all user's aquariums with their nested sub-data.
 *
 * @author Stefan Schubert
 */
@Data
public class ReefDataExportTo implements Serializable {

    /** Current schema version — increment manually on breaking export schema changes (C-3). */
    public static final String SCHEMA_VERSION = "1.0";

    /**
     * Metadata block.
     * The leading underscore in the JSON key is required by spec (C-3) and enforced via @JsonProperty.
     */
    @JsonProperty("_meta")
    private ExportMetaTo meta;

    /** All aquariums owned by the user, each containing nested sub-data. */
    private List<AquariumExportTo> aquariums = new ArrayList<>();
}
