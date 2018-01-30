/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */

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
public class MeasurementEntity extends TracableEntity {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private LocalDateTime measuredOn;

    private float measuredValue;

    private Integer unitId;

    /**
     * Owner-side of the relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private AquariumEntity aquarium;

    @Embedded
    private EntityState entityState;

// --------------------- GETTER / SETTER METHODS ---------------------

    public AquariumEntity getAquarium() {
        return this.aquarium;
    }

    public void setAquarium(AquariumEntity aquariumEntity) {
        this.aquarium = aquariumEntity;
    }

    @Override
    public EntityState getEntityState() {
        return this.entityState;
    }

    @Override
    public void setEntityState(EntityState entityState) {
        this.entityState = entityState;
    }

    @Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "measured_on", nullable = false, insertable = true, updatable = true)
    @Basic
    public LocalDateTime getMeasuredOn() {
        return measuredOn;
    }

    public void setMeasuredOn(LocalDateTime measuredOn) {
        this.measuredOn = measuredOn;
    }

    @Column(name = "measured_value", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public float getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(float measuredValue) {
        this.measuredValue = measuredValue;
    }

    @Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public Integer getUnitId() {
        return unitId;
    }

    public void setUnitId(Integer unitId) {
        this.unitId = unitId;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        MeasurementEntity that = (MeasurementEntity) o;

        if (Float.compare(that.measuredValue, this.measuredValue) != 0) return false;
        if (!this.id.equals(that.id)) return false;
        if (!this.measuredOn.equals(that.measuredOn)) return false;
        if (!this.unitId.equals(that.unitId)) return false;
        if (!this.aquarium.equals(that.aquarium)) return false;
        return this.entityState != null ? this.entityState.equals(that.entityState) : that.entityState == null;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.measuredOn.hashCode();
        result = 31 * result + (this.measuredValue != +0.0f ? Float.floatToIntBits(this.measuredValue) : 0);
        result = 31 * result + this.unitId.hashCode();
        result = 31 * result + this.aquarium.hashCode();
        result = 31 * result + (this.entityState != null ? this.entityState.hashCode() : 0);
        return result;
    }
}
