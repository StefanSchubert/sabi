/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents a single treatment record in the AI Chatbot Data Export.
 *
 * @author Stefan Schubert
 */
@Data
public class TreatmentExportTo implements Serializable {

    private String givenOn;
    private Float amount;
    private Integer unitId;
    private String unitSign;
    private String unitName;
    private Long remedyId;
    private String productName;
    private String vendor;
    private String description;
}
