package de.bluewhale.sabi.persistence;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 * Time: 21:05
 * To change this template use File | Settings | File Templates.
 */
@javax.persistence.Table(name = "measurement", schema = "", catalog = "sabi")
@Entity
public class MeasurementEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private long id;

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private Timestamp measuredOn;

    @javax.persistence.Column(name = "measured_on", nullable = false, insertable = true, updatable = true, length = 19, precision = 0)
    @Basic
    public Timestamp getMeasuredOn() {
        return measuredOn;
    }

    public void setMeasuredOn(Timestamp measuredOn) {
        this.measuredOn = measuredOn;
    }

    private float measuredValue;

    @javax.persistence.Column(name = "measured_value", nullable = false, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public float getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(float measuredValue) {
        this.measuredValue = measuredValue;
    }

    private int unitId;

    @javax.persistence.Column(name = "unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    private int parameterId;

    @javax.persistence.Column(name = "parameter_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public int getParameterId() {
        return parameterId;
    }

    public void setParameterId(int parameterId) {
        this.parameterId = parameterId;
    }

    private long aquariumId;

    @javax.persistence.Column(name = "aquarium_id", nullable = false, insertable = true, updatable = true, length = 20, precision = 0)
    @Basic
    public long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(long aquariumId) {
        this.aquariumId = aquariumId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MeasurementEntity that = (MeasurementEntity) o;

        if (aquariumId != that.aquariumId) return false;
        if (id != that.id) return false;
        if (Float.compare(that.measuredValue, measuredValue) != 0) return false;
        if (parameterId != that.parameterId) return false;
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
        result = 31 * result + parameterId;
        result = 31 * result + (int) (aquariumId ^ (aquariumId >>> 32));
        return result;
    }
}
