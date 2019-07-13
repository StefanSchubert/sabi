/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Used for i18n metric support, for th ease of the users.
 *
 * @author Stefan Schubert
 */
public enum SizeUnit {
    LITER("l"),
    GALLONS("gal");

// ------------------------------ FIELDS ------------------------------

    private String unitSign;

// --------------------------- CONSTRUCTORS ---------------------------

    SizeUnit(String pUnitSign) {
        this.unitSign = pUnitSign;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @JsonValue
    public String getUnitSign() {
        return this.unitSign;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        return unitSign;
    }
}
