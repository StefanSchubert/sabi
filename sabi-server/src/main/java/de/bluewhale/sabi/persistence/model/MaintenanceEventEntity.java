/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * JPA entity for a recorded maintenance event (date-only precision, back-dating allowed).
 * Water changes carry a percentage of the tank volume; carbon replacements have a NULL percentage.
 */
@Table(name = "aquarium_maintenance", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class MaintenanceEventEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "maintenance_type", nullable = false, length = 32)
    @Basic
    private String maintenanceType;

    @Column(name = "changed_on", nullable = false)
    @Basic
    private LocalDate changedOn;

    @Column(name = "percentage")
    @Basic
    private Integer percentage;

    @Column(name = "note", length = 200)
    @Basic
    private String note;
}
