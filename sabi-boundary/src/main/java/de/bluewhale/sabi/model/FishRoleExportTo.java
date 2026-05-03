/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents a single fish role entry in the AI Chatbot Data Export.
 * Contains the programmatic enum key plus the localized (English) name and description
 * so AI chatbots can understand the ecological function of the fish.
 *
 * @author Stefan Schubert
 */
@Data
public class FishRoleExportTo implements Serializable {

    /** Programmatic enum key of the role (e.g. INDICATOR_FISH). */
    private String enumKey;

    /** Localized (English) display name of the role. */
    private String name;

    /** Localized (English) description of the role's ecological function. */
    private String description;
}

