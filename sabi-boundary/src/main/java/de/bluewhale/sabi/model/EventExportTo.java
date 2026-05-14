/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents a single aquarium logbook event in the AI Chatbot Data Export.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
@Data
public class EventExportTo implements Serializable {

    /** Date of the event (ISO-8601, e.g. "2026-05-14"). */
    private String eventDate;

    /** Optional duration in decimal hours (e.g. 1.5 = 1 hour 30 minutes). */
    private BigDecimal durationHours;

    /** Free-text description of the event (multi-line possible). */
    private String description;
}

