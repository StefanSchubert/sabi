/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Transport Object for Aquarium
 *
 * @author Stefan Schubert
 */
public class AquariumTo implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private Long id;

    private Integer size;

    private SizeUnit sizeUnit;

    private String description;

    private Boolean active;

    private Long userId;

// --------------------- GETTER / SETTER METHODS ---------------------

    @SuppressWarnings("UnqualifiedFieldAccess")
    public Boolean getActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @ApiModelProperty(notes = "Description or Name of the tank, so the user can distinguish them.", required = true)
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @ApiModelProperty(notes = "ID this object for further reference.")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @ApiModelProperty(notes = "Tanks volume", required = true)
    public Integer getSize() {
        return size;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }


    @ApiModelProperty(notes = "Unit of tanks volume size.", required = true)
    public SizeUnit getSizeUnit() {
        return sizeUnit;
    }

    public void setSizeUnit(SizeUnit sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @ApiModelProperty(notes = "UserID - will be ignored. Set through processing.", required = false)
    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

// ------------------------ CANONICAL METHODS ------------------------


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        AquariumTo that = (AquariumTo) o;

        if (this.id != null ? !this.id.equals(that.id) : that.id != null) return false;
        if (this.size != null ? !this.size.equals(that.size) : that.size != null) return false;
        if (this.sizeUnit != that.sizeUnit) return false;
        if (this.description != null ? !this.description.equals(that.description) : that.description != null) return false;
        if (this.active != null ? !this.active.equals(that.active) : that.active != null) return false;
        return this.userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.size != null ? this.size.hashCode() : 0);
        result = 31 * result + (this.sizeUnit != null ? this.sizeUnit.hashCode() : 0);
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        result = 31 * result + (this.active != null ? this.active.hashCode() : 0);
        result = 31 * result + this.userId.hashCode();
        return result;
    }
}
