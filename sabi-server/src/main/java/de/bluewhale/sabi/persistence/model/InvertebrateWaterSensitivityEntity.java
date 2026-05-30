/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Water parameter sensitivity link for an invertebrate stock entry.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Table(name = "invertebrate_water_sensitivity", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class InvertebrateWaterSensitivityEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "invertebrate_stock_id", nullable = false)
    @Basic
    private Long invertebrateStockId;

    @Column(name = "unit_id", nullable = false)
    @Basic
    private Integer unitId;
}
