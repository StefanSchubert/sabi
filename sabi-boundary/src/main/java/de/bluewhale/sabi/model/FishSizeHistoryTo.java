/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transfer object for a fish size history entry.
 * Used to track fish growth over time.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Data
public class FishSizeHistoryTo implements Serializable {

    @Schema(description = "ID of the size record (null for new records).")
    private Long id;

    @Schema(description = "ID of the fish stock entry this size belongs to.")
    private Long fishStockEntryId;

    @NotNull(message = "fishstock.form.size.date.required")
    @Schema(description = "Date when the fish was measured.", required = true)
    private LocalDate measuredOn;

    @NotNull(message = "fishstock.form.size.cm.required")
    @DecimalMin(value = "0.1", message = "fishstock.form.size.cm.min")
    @Schema(description = "Measured size in centimetres.", required = true)
    private BigDecimal sizeCm;
}
