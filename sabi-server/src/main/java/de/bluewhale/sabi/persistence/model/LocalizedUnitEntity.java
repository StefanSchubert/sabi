/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Table which contains the translated parameter descriptions.
 */
@Table(name = "localized_unit", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper=false)
public class LocalizedUnitEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20)
    @Basic
    private Long id;

    @Column(name = "unit_id", nullable = false, insertable = true, updatable = true)
    @Basic
    private Integer unitId;

    @Column(name = "description", nullable = true, insertable = true, updatable = true, length = 80)
    @Basic
    private String description;

    @Column(name = "language", nullable = true, insertable = true, updatable = true, length = 3)
    @Basic
    private String language;

}
