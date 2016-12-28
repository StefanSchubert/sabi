/*
 * Copyright (c) 2016. by Stefan Schubert
 */

package de.bluewhale.sabi.model;

/**
 * Used for i18n metric support, for th ease of the users.
 *
 * @author Stefan Schubert
 */
public enum SizeUnit {

    LITER("l"),
    GALLONS("gal");

    private String unitSign;

    SizeUnit(String pUnitSign) {
        this.unitSign = pUnitSign;
    }

    @Override
    public String toString() {
        return unitSign;
    }
}
