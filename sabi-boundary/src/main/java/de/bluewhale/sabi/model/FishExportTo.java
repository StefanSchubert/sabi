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
 * Represents a single fish entry in the AI Chatbot Data Export.
 *
 * @author Stefan Schubert
 */
@Data
public class FishExportTo implements Serializable {

    private Long fishCatalogueId;
    private String scientificName;
    /** Common name stored by the user (free text, may differ from catalogue). */
    private String commonName;
    /** Optional personal nickname for this individual fish. */
    private String nickname;
    private String addedOn;
    /** Date the fish left the aquarium (departure/exodus). Null if still present. */
    private String exodusOn;
    /** Reason for departure (e.g. DEATH, SOLD, GIVEN_AWAY). Null if still present. */
    private String departureReason;
    private String observedBehavior;
    /** Chronological size history entries (most recent first). May be empty. */
    private List<FishSizeHistoryExportTo> sizeHistory = new ArrayList<>();
    /** Ecological roles assigned to this fish (e.g. INDICATOR_FISH, ALGAE_CLEANER). May be empty. */
    private List<FishRoleExportTo> roles = new ArrayList<>();
}
