/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * DTO of a users profile
 */
public class UserProfileTo implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private Long userId;
    private String language ="en"; // default
    private String country = "US"; // default

// --------------------------- CONSTRUCTORS ---------------------------

    public UserProfileTo() {
    }

    public UserProfileTo(Long pUserID, String pLanguage, String pCountry) {
        this.userId = pUserID;
        this.language = pLanguage;
        this.country = pCountry;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @ApiModelProperty(notes = "ISO-3166-1 alpha-2 country code - used for i18n in communication. Default 'US'", required = true)
    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setId(Long pUserID) {
        this.userId = pUserID;
    }

    @ApiModelProperty(notes="ISO-639-1 language code - used for i18n in communication. Must be set togehter with country. Defaults to 'en'",required = true)
    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
