/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import de.bluewhale.sabi.model.DosingType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for a separate aquarium dosing record.
 */
@Table(name = "aquarium_dosing", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class DosingEntity extends Auditable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    @Basic
    private Long id;

    @Column(name = "aquarium_id", nullable = false)
    @Basic
    private Long aquariumId;

    @Column(name = "recorded_on", nullable = false)
    @Basic
    private LocalDateTime recordedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "dosing_type", nullable = false, length = 32)
    private DosingType dosingType = DosingType.MANUAL_ADDITION;

    @Column(name = "product_name", nullable = false, length = 255)
    @Basic
    private String productName;

    @Column(name = "amount", nullable = false, precision = 10, scale = 3)
    @Basic
    private BigDecimal amount;

    @Column(name = "amount_unit", nullable = false, length = 30)
    @Basic
    private String amountUnit;

    @Column(name = "category", length = 80)
    @Basic
    private String category;

    @Column(name = "dosing_interval", length = 40)
    @Basic
    private String dosingInterval;

    @Column(name = "dosing_method", length = 80)
    @Basic
    private String dosingMethod;

    @Column(name = "solution_description", columnDefinition = "TEXT")
    @Basic
    private String solutionDescription;

    @Column(name = "note", columnDefinition = "TEXT")
    @Basic
    private String note;

    @Column(name = "dosing_end_on")
    @Basic
    private LocalDateTime dosingEndOn;
}
