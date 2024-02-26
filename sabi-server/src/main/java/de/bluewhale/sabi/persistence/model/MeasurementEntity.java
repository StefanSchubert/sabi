/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


@NamedQueries({@NamedQuery(name = "Measurement.getMeasurement",
        query = "select a from MeasurementEntity a, AquariumEntity t where a.id = :pMeasurementId " +
                "and a.aquarium.id = :pTankID " +
                "and a.aquarium.id = t.id " +
                "and t.user.id = :pUserID"),
        @NamedQuery(name = "Measurement.getAllMeasurementsForTank",
                query = "select a from MeasurementEntity a where a.aquarium.id = :pTankID"),
        @NamedQuery(name = "Measurement.getUsersMeasurements",
                query = "select a FROM MeasurementEntity a, AquariumEntity t " +
                        "where a.aquarium.id = t.id " +
                        "and t.user.id = :pUserID")})
@Table(name = "measurement", schema = "sabi")
@Entity
@Data
@EqualsAndHashCode(exclude = {"user", "aquarium"},callSuper = false)
public class MeasurementEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    private Long id;

    @Column(name = "measured_on", nullable = false, insertable = true, updatable = true)
    @Basic
    private LocalDateTime measuredOn;

    @Column(name = "measured_value", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    private float measuredValue;

    @Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    private Integer unitId;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private AquariumEntity aquarium;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

}
