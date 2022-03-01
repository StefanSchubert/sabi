/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Instead of static enums I prefer to be able to add a new unit without the need of a redeployment.
 * User: Stefan Schubert
 * Date: 12.03.15
 */
public class UnitTo  {
// ------------------------------ FIELDS ------------------------------

    private Integer id;

    private String unitSign;

    private String description;


// --------------------- GETTER / SETTER METHODS ---------------------

    @Schema(name = "Short description of the unit might contain threshold information.", required = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        this.description = pDescription;
    }

    @Schema(name = "References the unique unit in which a measurement has been taken.", required = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Schema(name = "Sign (Abbreviation) of measurements unit like 'PO4'", required = true)
    public String getUnitSign() {
        return unitSign;
    }

    public void setUnitSign(String pUnitSign) {
        this.unitSign = pUnitSign;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        UnitTo unitTo = (UnitTo) o;

        if (!this.id.equals(unitTo.id)) return false;
        if (!this.unitSign.equals(unitTo.unitSign)) return false;
        return this.description.equals(unitTo.description);
    }

    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.unitSign.hashCode();
        result = 31 * result + this.description.hashCode();
        return result;
    }
}
