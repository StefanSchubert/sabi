/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Table(name = "parameter", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = "localizedParameterEntities")
public class ParameterEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    private Integer id;

    @Column(name = "belonging_unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private int belongingUnitId;

    @Column(name = "min_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private Float minThreshold;

    @Column(name = "max_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private Float maxThreshold;

    // Unidirectional for now - as this contains more static data, we won't provide an admin gui for it.
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="parameter_id")
    private List<LocalizedParameterEntity> localizedParameterEntities = new ArrayList<LocalizedParameterEntity>();

}
