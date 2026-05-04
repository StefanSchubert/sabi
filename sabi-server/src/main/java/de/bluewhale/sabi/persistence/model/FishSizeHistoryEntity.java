/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Records a measured size (in cm) for a fish at a given date.
 * Enables growth tracking and biomass correlation.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Entity
@Table(name = "fish_size_history", schema = "sabi")
@Data
public class FishSizeHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "fish_id", nullable = false)
    private Long fishId;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    @Column(name = "size_cm", nullable = false, precision = 5, scale = 1)
    private BigDecimal sizeCm;
}
