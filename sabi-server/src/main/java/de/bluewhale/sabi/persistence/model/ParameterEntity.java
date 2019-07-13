/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.model;

import javax.persistence.*;

/**
 *
 * User: Stefan
 * Date: 12.03.15
 */
@Table(name = "parameter", schema = "sabi")
@Entity
public class ParameterEntity extends Auditable {
// ------------------------------ FIELDS ------------------------------

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    private String description;

    private int usedThresholdUnitId;

    private Float minThreshold;

    private Float maxThreshold;


// --------------------- GETTER / SETTER METHODS ---------------------

    @javax.persistence.Column(name = "description", nullable = false, insertable = true, updatable = true, length = 255, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @javax.persistence.Column(name = "id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @javax.persistence.Column(name = "max_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public Float getMaxThreshold() {
        return maxThreshold;
    }

    public void setMaxThreshold(Float maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    @javax.persistence.Column(name = "min_threshold", nullable = true, insertable = true, updatable = true, length = 12, precision = 0)
    @Basic
    public Float getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(Float minThreshold) {
        this.minThreshold = minThreshold;
    }

    @javax.persistence.Column(name = "used_threshold_unit_id", nullable = false, insertable = true, updatable = true, length = 10, precision = 0)
    @Basic
    public int getUsedThresholdUnitId() {
        return usedThresholdUnitId;
    }

    public void setUsedThresholdUnitId(int usedThresholdUnitId) {
        this.usedThresholdUnitId = usedThresholdUnitId;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        ParameterEntity that = (ParameterEntity) o;

        if (this.usedThresholdUnitId != that.usedThresholdUnitId) return false;
        if (!this.id.equals(that.id)) return false;
        if (!this.description.equals(that.description)) return false;
        if (this.minThreshold != null ? !this.minThreshold.equals(that.minThreshold) : that.minThreshold != null) return false;
        return this.maxThreshold != null ? this.maxThreshold.equals(that.maxThreshold) : that.maxThreshold == null;
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.description.hashCode();
        result = 31 * result + this.usedThresholdUnitId;
        result = 31 * result + (this.minThreshold != null ? this.minThreshold.hashCode() : 0);
        result = 31 * result + (this.maxThreshold != null ? this.maxThreshold.hashCode() : 0);
        return result;
    }
}
