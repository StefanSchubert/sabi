/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * DTO of the user which will be used during registration process.
 * User: Stefan
 * Date: 08.01.19
 */
public class NewRegistrationTO implements Serializable {
// ------------------------------ FIELDS ------------------------------

    @ApiModelProperty(notes = "Users Emailaddress.", required = true)
    private String email;

    @ApiModelProperty(notes = "Loginname. Uniquely identifies the user account.", required = true)
    private String username;

    @ApiModelProperty(notes = "Users secret", required = true)
    private String password;

    @ApiModelProperty(notes = "Needs to be a valid code otherwise you won't be registered - this helps avoiding " +
            "simple DOS-attacks in future", required = true)
    private String captchaCode;

    @ApiModelProperty(notes="ISO-639-1 language code - used for i18n in communication. " +
            "Must be set together with country. SmallCAPS / Defaults to 'en'",required = true)
    private String language ="en"; // default

    @ApiModelProperty(notes = "ISO-3166-1 alpha-2 country code - used for i18n in communication. " +
            "Default 'US'", required = true)
    private String country = "US"; // default

// --------------------------- CONSTRUCTORS ---------------------------

    public NewRegistrationTO() {
    }

    public NewRegistrationTO(String pEmail, String pUsername, String pPassword) {
        this.email = pEmail;
        this.password = pPassword;
        this.username = pUsername;
    }

    public NewRegistrationTO(String pEmail, String pUsername, String pPassword, String pLanguage, String pCountry) {
        this.email = pEmail;
        this.username = pUsername;
        this.password = pPassword;
        this.language = pLanguage;
        this.country = pCountry;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(final String pCaptchaCode) {
        captchaCode = pCaptchaCode;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "NewRegistrationTO{" +
                "email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", captchaCode='" + captchaCode + '\'' +
                ", language='" + language + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
