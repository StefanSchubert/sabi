/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity for the aquarium_event table.
 * Represents a single logbook entry for an aquarium (feature 004-aquarium-events).
 * Audit fields (createdOn, lastmodOn, optlock) are inherited from {@link Auditable}.
 *
 * @author Stefan Schubert
 */
@Table(name = "aquarium_event", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class AquariumEventEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    /** FK to aquarium.id — stored as plain Long (no JPA association object needed). */
    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "event_date", nullable = false)
    @Basic
    private LocalDate eventDate;

    /** Optional; positive decimal. Validated at controller level before persist. */
    @Column(name = "duration_hours", nullable = true, precision = 6, scale = 2)
    @Basic
    private BigDecimal durationHours;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @Basic
    private String description;
}

