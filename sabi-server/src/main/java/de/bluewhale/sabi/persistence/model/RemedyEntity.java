/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "remedy", schema = "sabi")
@Entity
@Data
public class RemedyEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @jakarta.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @jakarta.persistence.Column(name = "productname", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    private String productname;

    @jakarta.persistence.Column(name = "vendor", nullable = true, insertable = true, updatable = true, length = 60, precision = 0)
    @Basic
    private String vendor;

}
