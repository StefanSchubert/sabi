/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents a single plague record in the AI Chatbot Data Export.
 *
 * @author Stefan Schubert
 */
@Data
public class PlagueRecordExportTo implements Serializable {

    private String observedOn;
    private Integer plagueId;
    private String plagueName;
    private Boolean plagueNameResolved;
    private Integer plagueStatusId;
    private String plagueStatusName;
    private Boolean plagueStatusResolved;
    private Integer plagueIntervallId;
}
