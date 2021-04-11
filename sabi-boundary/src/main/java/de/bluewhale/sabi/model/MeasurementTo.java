/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * User: Stefan
 * Date: 05.01.16
 */
public class MeasurementTo implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private Long id;

    private LocalDateTime measuredOn;

    private float measuredValue;

    private int unitId;

    private Long aquariumId;

// --------------------- GETTER / SETTER METHODS ---------------------

    @ApiModelProperty(notes = "References the Aquarium this measurement belongs to.", required = true)
    public Long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(Long aquariumId) {
        this.aquariumId = aquariumId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ApiModelProperty(notes = "Point in time when the description has been taken.", required = true)
    public LocalDateTime getMeasuredOn() {
        return measuredOn;
    }

    public void setMeasuredOn(LocalDateTime measuredOn) {
        this.measuredOn = measuredOn;
    }

    @ApiModelProperty(notes = "Decimal value of the measurement.", required = true)
    public float getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(float measuredValue) {
        this.measuredValue = measuredValue;
    }

    @ApiModelProperty(notes = "References the used unit this measurement belongs to.", required = true)
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
        if (o == null || this.getClass() != o.getClass()) return false;

        MeasurementTo that = (MeasurementTo) o;

        if (Float.compare(that.measuredValue, this.measuredValue) != 0) return false;
        if (this.unitId != that.unitId) return false;
        if (!this.id.equals(that.id)) return false;
        if (!this.measuredOn.equals(that.measuredOn)) return false;
        return this.aquariumId.equals(that.aquariumId);
    }

    @Override
    public int hashCode() {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + this.measuredOn.hashCode();
        result = 31 * result + (this.measuredValue != +0.0f ? Float.floatToIntBits(this.measuredValue) : 0);
        result = 31 * result + this.unitId;
        result = 31 * result + this.aquariumId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MeasurementTo{" +
                ", measuredOn=" + measuredOn +
                ", measuredValue=" + measuredValue +
                ", unitId=" + unitId +
                ", aquariumId=" + aquariumId +
                '}';
    }
}
