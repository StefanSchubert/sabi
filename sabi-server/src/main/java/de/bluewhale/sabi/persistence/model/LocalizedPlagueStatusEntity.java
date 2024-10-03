/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Table which contains the translated plague status.
 */
@Table(name = "localized_plague_status", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(callSuper=false)
public class LocalizedPlagueStatusEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @Column(name = "plague_status_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Integer plague_status_id;

    @Column(name = "language", nullable = true, insertable = true, updatable = true, length = 3)
    @Basic
    private String language;

    @Column(name = "description", nullable = true, insertable = true, updatable = true, length = 80)
    @Basic
    private String description;

}
