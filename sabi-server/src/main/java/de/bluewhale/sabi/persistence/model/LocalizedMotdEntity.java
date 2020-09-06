/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import lombok.Data;

import javax.persistence.*;

/**
 * Table which contains the translated message of today content.
 */
@Table(name = "localized_motd", schema = "sabi")
@Entity
@Data
public class LocalizedMotdEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @Column(name = "motd_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long motd_id;

    @Column(name = "language", nullable = true, insertable = true, updatable = true, length = 3)
    @Basic
    private String language;

    @Column(name = "text", nullable = true, insertable = true, updatable = true, length = 255)
    @Basic
    private String text;

}
