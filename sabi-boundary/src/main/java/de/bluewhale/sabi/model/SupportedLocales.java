/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Listing of languages that are currently supported by sabi.
 *
 * @author Stefan Schubert
 */
public enum SupportedLocales {
    German(Locale.GERMANY),
    English(Locale.ENGLISH);

// ------------------------------ FIELDS ------------------------------

    private Locale locale;

// --------------------------- CONSTRUCTORS ---------------------------

    SupportedLocales(Locale locale) {
        this.locale = locale;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @JsonValue
    public Locale getLocale() {
        return this.locale;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        return locale.getISO3Language();
    }
}
