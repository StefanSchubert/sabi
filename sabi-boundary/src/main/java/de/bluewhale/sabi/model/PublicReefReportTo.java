/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The complete public HouseReef report for an aquarium, served via a share-token link.
 * Contains anonymised owner info, tank parameters, current fish inhabitants, and
 * the last 3 months of measurement data grouped by unit.
 *
 * @author Stefan Schubert
 */
@Data
public class PublicReefReportTo implements Serializable {

    @Schema(description = "Display name (username) of the tank owner.")
    private String ownerUsername;

    @Schema(description = "Tank (aquarium) data.")
    private AquariumTo tank;

    @Schema(description = "Timestamp when this report snapshot was generated.")
    private LocalDateTime reportGeneratedAt;

    @Schema(description = "True when the share link has expired (validUntil is in the past). " +
            "If true all data lists are empty.")
    private boolean linkExpired;

    @Schema(description = "Current fish inhabitants (exodusOn == null).")
    private List<FishStockEntryTo> inhabitants = new ArrayList<>();

    /**
     * Measurements for the last 3 months, keyed by unit ID.
     * Each entry contains the list of measurements for that unit sorted by measuredOn ascending.
     */
    @Schema(description = "Measurements for the last 3 months grouped by unitId.")
    private Map<Integer, List<MeasurementTo>> measurementsByUnit;

    /**
     * Unit meta-data needed to render chart labels (unitId → UnitTo).
     */
    @Schema(description = "Unit metadata (sign, description) keyed by unitId.")
    private Map<Integer, UnitTo> unitMap;

    @Schema(description = "Events from the past 365 days; null when includeEvents = false for this report link.")
    private List<AquariumEventTo> recentEvents;   // null = not opted-in; empty list = opted-in but no events

    /**
     * Returns the report timestamp as UTC epoch milliseconds so the browser
     * can format it in the viewer's local timezone via {@code new Date(ms).toLocaleString()}.
     * Uses {@link ZoneId#systemDefault()} so the conversion is correct regardless of whether
     * the server runs in UTC (production Docker) or a local timezone (dev Mac).
     * Not included in JSON transport — only used by JSF/EL on the webclient side.
     */
    @JsonIgnore
    public long getReportGeneratedAtEpochMillis() {
        if (reportGeneratedAt == null) return 0L;
        return reportGeneratedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
