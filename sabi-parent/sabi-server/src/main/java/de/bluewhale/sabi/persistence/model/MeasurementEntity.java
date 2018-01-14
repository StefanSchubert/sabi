/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "measurement", schema = "sabi")
@Entity
public class MeasurementEntity {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Timestamp measuredOn;

    private float measuredValue;

    private Integer unitId;

    private Long aquariumId;

// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(Long aquariumId) {
        this.aquariumId = aquariumId;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "measured_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getMeasuredOn() {
        return measuredOn;
    }

    public void setMeasuredOn(Timestamp measuredOn) {
        this.measuredOn = measuredOn;
    }

    @javax.persistence.Column(name = "measured_value", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public float getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(float measuredValue) {
        this.measuredValue = measuredValue;
    }

    @javax.persistence.Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
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
        if (this.measuredOn != null ? !this.measuredOn.equals(that.measuredOn) : that.measuredOn != null) return false;
        if (this.unitId != null ? !this.unitId.equals(that.unitId) : that.unitId != null) return false;
        return this.aquariumId.equals(that.aquariumId);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + (this.measuredOn != null ? this.measuredOn.hashCode() : 0);
        result = 31 * result + (this.measuredValue != +0.0f ? Float.floatToIntBits(this.measuredValue) : 0);
        result = 31 * result + (this.unitId != null ? this.unitId.hashCode() : 0);
        result = 31 * result + this.aquariumId.hashCode();
        return result;
    }
}
