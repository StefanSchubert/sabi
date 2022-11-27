/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Table which contains the translated plague names.
 */
@Table(name = "localized_plague", schema = "sabi")
@Entity
@Data
public class LocalizedPlagueEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20)
    @Basic
    private Long id;

    @Column(name = "plague_id", nullable = false, insertable = true, updatable = true)
    @Basic
    private Integer plague_id;

    @Column(name = "common_name", nullable = true, insertable = true, updatable = true, length = 80)
    @Basic
    private String commonName;

    @Column(name = "language", nullable = true, insertable = true, updatable = true, length = 3)
    @Basic
    private String language;

}
