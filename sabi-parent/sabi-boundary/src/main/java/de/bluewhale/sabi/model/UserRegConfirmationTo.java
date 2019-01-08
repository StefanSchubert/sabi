/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * DTO of the just registered user which will be send back for registration confirmation.
 * User: Stefan
 * Date: 29.08.15
 */
public class UserRegConfirmationTo implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private String email;

    private String username;

    private String language ="en"; // default

    private String country = "US"; // default

    private boolean validated;

// --------------------------- CONSTRUCTORS ---------------------------

    public UserRegConfirmationTo() {
    }

    public UserRegConfirmationTo(String pEmail, String pUsername) {
        this.email = pEmail;
        this.username = pUsername;
    }

    public UserRegConfirmationTo(String pEmail, String pUsername, String pLanguage, String pCountry) {
        this.email = pEmail;
        this.username = pUsername;
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

    @ApiModelProperty(notes = "Users Emailaddress", required = true)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @ApiModelProperty(notes="ISO-639-1 language code - used for i18n in communication. Must be set togehter with country. Defaults to 'en'",required = true)
    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @ApiModelProperty(notes = "Userlogin", required = true)
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isValidated() {
        return this.validated;
    }

    public void setValidated(final boolean pValidated) {
        validated = pValidated;
    }
}
