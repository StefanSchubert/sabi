/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A standalone dosing record included in a user's reef data export.
 */
@Data
public class DosingExportTo implements Serializable {

    private String recordedOn;
    private String dosingType;
    private String productName;
    private BigDecimal amount;
    private String amountUnit;
    private String category;
    private String dosingInterval;
    private String dosingMethod;
    private String solutionDescription;
    private String note;
    private String dosingEndOn;
}
