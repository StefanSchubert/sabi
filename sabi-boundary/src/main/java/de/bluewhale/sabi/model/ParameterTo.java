/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * ParameterTo contains additional detail information about measurement units such as threshold values.
 * Might have been modeled as static class/enum but I prefer have it ready for a later admin GUI for maintenance.
 */
public class ParameterTo {

    private Integer id;
    private Integer belongingUnitId;
    private Float minThreshold;
    private Float maxThreshold;
    private String description;

    @ApiModelProperty(notes = "References the belonging measurement Unit.", required = true)
    public Integer getBelongingUnitId() {
        return this.belongingUnitId;
    }

    public void setBelongingUnitId(final Integer belongingUnitId) {
        this.belongingUnitId = belongingUnitId;
    }

    @ApiModelProperty(notes = "Recommendation according natural seawater composition. Measurement value should not fall below this threshold.", required = true)
    public Float getMinThreshold() {
        return this.minThreshold;
    }

    public void setMinThreshold(final Float minThreshold) {
        this.minThreshold = minThreshold;
    }

    @ApiModelProperty(notes = "Recommendation according natural seawater composition. Measurement value should not be higher than this threshold.", required = true)
    public Float getMaxThreshold() {
        return this.maxThreshold;
    }

    public void setMaxThreshold(final Float maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    @ApiModelProperty(notes = "Short description of the unit.", required = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        this.description = pDescription;
    }

    @ApiModelProperty(notes = "References the unique unit in which a measurement has been taken.", required = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterTo)) return false;

        final ParameterTo that = (ParameterTo) o;

        if (!this.id.equals(that.id)) return false;
        return this.belongingUnitId.equals(that.belongingUnitId);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.belongingUnitId.hashCode();
        return result;
    }
}
