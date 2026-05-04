/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.model;
import lombok.Data;
import java.io.Serializable;
/**
 * Represents a single size measurement entry for a fish in the AI Chatbot Data Export.
 *
 * @author Stefan Schubert
 */
@Data
public class FishSizeHistoryExportTo implements Serializable {
    /** Date the size was measured (ISO-8601 date). */
    private String measuredOn;
    /** Measured body length in centimeters. */
    private Double sizeCm;
}
