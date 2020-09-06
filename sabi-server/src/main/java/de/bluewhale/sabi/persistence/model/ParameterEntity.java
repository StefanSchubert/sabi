/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import lombok.Data;

import javax.persistence.*;

@Table(name = "parameter", schema = "sabi")
@Entity
@Data
public class ParameterEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private Integer id;

    @javax.persistence.Column(name = "description", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    private String description;

    @javax.persistence.Column(name = "used_threshold_unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private int usedThresholdUnitId;

    @javax.persistence.Column(name = "min_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private Float minThreshold;

    @javax.persistence.Column(name = "max_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private Float maxThreshold;

}
