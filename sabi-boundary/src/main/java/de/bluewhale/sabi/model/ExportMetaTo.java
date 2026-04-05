/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Metadata block for the AI Chatbot Data Export.
 * Carries schema version, generation timestamp and a fixed English description.
 *
 * @author Stefan Schubert
 */
@Data
public class ExportMetaTo implements Serializable {

    /** ISO-8601 UTC timestamp of when this export was generated. */
    private String exportedAt;

    /** Export schema version constant; currently "1.0". */
    private String sabiSchemaVersion;

    /** Fixed English description for AI context. */
    private String description;
}

