/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single growth measurement for a coral stock entry.
 * measurement_type is immutable after creation (FR-039).
 * No soft-delete — cascade from parent TankCoralStockEntity.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Table(name = "coral_growth_history", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"coralStock"}, callSuper = false)
@ToString(exclude = {"coralStock"})
public class CoralGrowthHistoryEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "coral_stock_id", nullable = false, insertable = true, updatable = false)
    @Basic
    private Long coralStockId;

    @Column(name = "measured_on", nullable = false)
    @Basic
    private LocalDate measuredOn;

    /** Immutable after creation (FR-039). */
    @Column(name = "measurement_type", nullable = false, length = 30)
    @Basic
    private String measurementType;

    @Column(name = "measurement_value", nullable = false, precision = 8, scale = 1)
    @Basic
    private BigDecimal measurementValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coral_stock_id", insertable = false, updatable = false)
    private TankCoralStockEntity coralStock;
}

