/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.sql.Timestamp;

/**
 *
 * User: Stefan
 * Date: 05.01.16
 */
public class MeasurementTo {
// ------------------------------ FIELDS ------------------------------

    private long id;

    private Timestamp measuredOn;

    private float measuredValue;

    private int unitId;

    private long aquariumId;

// --------------------- GETTER / SETTER METHODS ---------------------

    @ApiModelProperty(notes = "References the Aquarium this measurement belongs to.", required = true)
    public long getAquariumId() {
        return aquariumId;
    }

    public void setAquariumId(long aquariumId) {
        this.aquariumId = aquariumId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ApiModelProperty(notes = "Point in time when the description has been taken.", required = true)
    public Timestamp getMeasuredOn() {
        return measuredOn;
    }

    public void setMeasuredOn(Timestamp measuredOn) {
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
}
