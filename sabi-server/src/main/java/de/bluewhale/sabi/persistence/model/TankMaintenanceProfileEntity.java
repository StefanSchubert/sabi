/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import de.bluewhale.sabi.model.CarbonMode;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * JPA entity for the per-aquarium maintenance profile: regular water exchange interval
 * and activated carbon setting. The row is created lazily on first update; reads fall back
 * to defaults (no regular interval, carbon disabled) when the row is missing.
 */
@Table(name = "aquarium_maintenance_profile", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class TankMaintenanceProfileEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "regular_interval_days")
    @Basic
    private Integer regularIntervalDays;

    @Column(name = "regular_percentage")
    @Basic
    private Integer regularPercentage;

    @Column(name = "carbon_enabled", nullable = false)
    @Basic
    private Boolean carbonEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "carbon_mode", length = 32)
    private CarbonMode carbonMode;

    @Column(name = "carbon_replacement_interval_days")
    @Basic
    private Integer carbonReplacementIntervalDays;
}
