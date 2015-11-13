package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "parameter", schema = "sabi")
@Entity
public class ParameterEntity extends TracableEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private String description;

    @javax.persistence.Column(name = "description", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private int usedThresholdUnitId;

    @javax.persistence.Column(name = "used_threshold_unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public int getUsedThresholdUnitId() {
        return usedThresholdUnitId;
    }

    public void setUsedThresholdUnitId(int usedThresholdUnitId) {
        this.usedThresholdUnitId = usedThresholdUnitId;
    }

    private Float minThreshold;

    @javax.persistence.Column(name = "min_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public Float getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(Float minThreshold) {
        this.minThreshold = minThreshold;
    }

    private Float maxThreshold;

    @javax.persistence.Column(name = "max_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public Float getMaxThreshold() {
        return maxThreshold;
    }

    public void setMaxThreshold(Float maxThreshold) {
        this.maxThreshold = maxThreshold;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterEntity that = (ParameterEntity) o;

        if (id != that.id) return false;
        if (usedThresholdUnitId != that.usedThresholdUnitId) return false;
        if (createdOn != null ? !createdOn.equals(that.createdOn) : that.createdOn != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (maxThreshold != null ? !maxThreshold.equals(that.maxThreshold) : that.maxThreshold != null) return false;
        if (minThreshold != null ? !minThreshold.equals(that.minThreshold) : that.minThreshold != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + usedThresholdUnitId;
        result = 31 * result + (minThreshold != null ? minThreshold.hashCode() : 0);
        result = 31 * result + (maxThreshold != null ? maxThreshold.hashCode() : 0);
        result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
        return result;
    }
}
