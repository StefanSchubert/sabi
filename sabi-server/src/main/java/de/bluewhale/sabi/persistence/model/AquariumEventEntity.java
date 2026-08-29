/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import de.bluewhale.sabi.model.AquariumEventType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private AquariumEventType eventType = AquariumEventType.GENERIC;

    @Column(name = "event_time", nullable = true, length = 5)
    @Basic
    private String eventTime;

    /** Optional; positive decimal. Validated at controller level before persist. */
    @Column(name = "duration_hours", nullable = true, precision = 6, scale = 2)
    @Basic
    private BigDecimal durationHours;

    @Column(name = "amount", nullable = true, precision = 10, scale = 3)
    @Basic
    private BigDecimal amount;

    @Column(name = "amount_unit", nullable = true, length = 30)
    @Basic
    private String amountUnit;

    @Column(name = "product_name", nullable = true, length = 255)
    @Basic
    private String productName;

    @Column(name = "category", nullable = true, length = 80)
    @Basic
    private String category;

    @Column(name = "dosing_interval", nullable = true, length = 40)
    @Basic
    private String dosingInterval;

    @Column(name = "dosing_method", nullable = true, length = 80)
    @Basic
    private String dosingMethod;

    @Column(name = "solution_description", nullable = true, columnDefinition = "TEXT")
    @Basic
    private String solutionDescription;

    @Column(name = "note", nullable = true, columnDefinition = "TEXT")
    @Basic
    private String note;

    @Column(name = "dosing_end_on", nullable = true)
    @Basic
    private LocalDateTime dosingEndOn;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @Basic
    private String description;
}
