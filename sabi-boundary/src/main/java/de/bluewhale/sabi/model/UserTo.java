/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * DTO of the user which will be used during registration process, or during maintenance of users properties.
 * User: Stefan
 * Date: 29.08.15
 */
public class UserTo implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private Long id;
    private String validationToken;

    private String email;

    private String username;

    private String password;

    private String captchaCode;

    private String language ="en"; // default

    private String country = "US"; // default

    private boolean validated;

// --------------------------- CONSTRUCTORS ---------------------------

    public UserTo() {
    }

    public UserTo(String pEmail, String pUsername, String pPassword) {
        this.email = pEmail;
        this.password = pPassword;
        this.username = pUsername;
    }

    public UserTo(String pEmail, String pUsername, String pPassword, String pLanguage, String pCountry) {
        this.email = pEmail;
        this.username = pUsername;
        this.password = pPassword;
        this.language = pLanguage;
        this.country = pCountry;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @ApiModelProperty(notes = "Needs to be a valid code otherwise you won't be registered - this helps avoiding simple DOS-attacks in future", required = false)
    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(final String pCaptchaCode) {
        captchaCode = pCaptchaCode;
    }

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

    public Long getId() {
        return this.id;
    }

    public void setId(Long pId) {
        this.id = pId;
    }

    @ApiModelProperty(notes="ISO-639-1 language code - used for i18n in communication. Must be set togehter with country. Defaults to 'en'",required = true)
    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @ApiModelProperty(notes = "Users secret", required = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ApiModelProperty(notes = "Userlogin", required = true)
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @ApiModelProperty(notes = "Will be used only during registration process, when the user validates his email address.", required = false)
    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

    public boolean isValidated() {
        return this.validated;
    }

    public void setValidated(final boolean pValidated) {
        validated = pValidated;
    }
}
