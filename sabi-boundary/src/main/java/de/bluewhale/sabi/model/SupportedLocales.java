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
    German(Locale.GERMAN),
    English(Locale.ENGLISH);

    private Locale locale;

    SupportedLocales(Locale locale) {
        this.locale = locale;
    }

    @JsonValue
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public String toString() {
        return locale.getISO3Language();
    }

}
