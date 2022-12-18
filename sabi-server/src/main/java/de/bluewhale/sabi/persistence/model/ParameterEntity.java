/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;

@Table(name = "parameter", schema = "sabi")
@Entity
@Data
public class ParameterEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @jakarta.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private Integer id;

    @jakarta.persistence.Column(name = "description", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    private String description;

    @jakarta.persistence.Column(name = "belonging_unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private int belongingUnitId;

    @jakarta.persistence.Column(name = "min_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private Float minThreshold;

    @jakarta.persistence.Column(name = "max_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private Float maxThreshold;

}
