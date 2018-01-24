/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 *
 * User: Stefan
 * Date: 05.01.16
 */
public class MeasurementTo {
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
}
