/*
 * Copyright (c) 2017 by Stefan Schubert
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
    private long id;

    private Timestamp measuredOn;

    private float measuredValue;

    private int unitId;

    private long aquariumId;

// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(long aquariumId) {
        this.aquariumId = aquariumId;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getId() {
        return id;
    }

    public void setId(long id) {
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
    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MeasurementEntity that = (MeasurementEntity) o;

        if (aquariumId != that.aquariumId) return false;
        if (id != that.id) return false;
        if (Float.compare(that.measuredValue, measuredValue) != 0) return false;
        if (unitId != that.unitId) return false;
        if (measuredOn != null ? !measuredOn.equals(that.measuredOn) : that.measuredOn != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (measuredOn != null ? measuredOn.hashCode() : 0);
        result = 31 * result + (measuredValue != +0.0f ? Float.floatToIntBits(measuredValue) : 0);
        result = 31 * result + unitId;
        result = 31 * result + (int) (aquariumId ^ (aquariumId >>> 32));
        return result;
    }
}
