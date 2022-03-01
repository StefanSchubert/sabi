/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * Transport Object for Aquarium
 *
 * @author Stefan Schubert
 */
public class AquariumTo implements Serializable {

    private Long id;

    private Integer size;

    private SizeUnit sizeUnit;

    private String description;

    private Boolean active;

    private Long userId;

    private Date inceptionDate;

    @SuppressWarnings("UnqualifiedFieldAccess")
    @Schema(name = "Date since when this tank is up and running.", required = false)
    public Date getInceptionDate() {
        return this.inceptionDate;
    }

    public void setInceptionDate(final Date inceptionDate) {
        this.inceptionDate = inceptionDate;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @Schema(name = "Flag telling if this tank is still in used, or if it meanwhile has been disolved.", required = false)
    public Boolean getActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @Schema(name = "Description or Name of the tank, so the user can distinguish them.", required = true)
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @Schema(name = "ID this object for further reference.")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @Schema(name = "Tanks volume", required = true)
    public Integer getSize() {
        return size;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }


    @Schema(name = "Unit of tanks volume size.", required = true)
    public SizeUnit getSizeUnit() {
        return sizeUnit;
    }

    public void setSizeUnit(SizeUnit sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    @SuppressWarnings("UnqualifiedFieldAccess")
    @Schema(name = "UserID - will be ignored. Set through processing.", required = false)
    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

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
