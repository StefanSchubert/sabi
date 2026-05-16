/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * A single polyp condition observation for a coral stock entry.
 * No soft-delete — cascade from parent TankCoralStockEntity.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Table(name = "coral_polyp_condition", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"coralStock"}, callSuper = false)
@ToString(exclude = {"coralStock"})
public class CoralPolypConditionEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "coral_stock_id", nullable = false, insertable = true, updatable = false)
    @Basic
    private Long coralStockId;

    @Column(name = "observed_on", nullable = false)
    @Basic
    private LocalDate observedOn;

    @Column(name = "polyp_condition", nullable = false, length = 30)
    @Basic
    private String polypCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coral_stock_id", insertable = false, updatable = false)
    private TankCoralStockEntity coralStock;
}

