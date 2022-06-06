/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO of the just registered user which will be send back for registration confirmation.
 * User: Stefan
 * Date: 29.08.15
 */
@Data
public class UserRegConfirmationTo implements Serializable {

    @Schema(name = "Users Emailaddress", required = true)
    private String email;

    @Schema(name = "Userlogin", required = true)
    private String username;

    @Schema(name="ISO-639-1 language code - used for i18n in communication. Must be set togehter with country. Defaults to 'en'",required = true)
    private String language ="en"; // default

    @Schema(name = "ISO-3166-1 alpha-2 country code - used for i18n in communication. Default 'US'", required = true)
    private String country = "US"; // default

    private boolean validated;

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

    public boolean isValidated() {
        return this.validated;
    }

}
