/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * DTO of the user which will be used during registration process.
 * User: Stefan
 * Date: 08.01.19
 */
@Data
public class NewRegistrationTO implements Serializable {

    @Schema(name = "Users Emailaddress.", required = true)
    private String email;

    @Schema(name = "Loginname. Uniquely identifies the user account.", required = true)
    private String username;

    @Schema(name = "Users secret", required = true)
    private String password;

    @Schema(name = "Needs to be a valid code otherwise you won't be registered - this helps avoiding " +
            "simple DOS-attacks in future", required = true)
    private String captchaCode;

    @Schema(name="ISO-639-1 language code - used for i18n in communication. " +
            "Must be set together with country. SmallCAPS / Defaults to 'en'",required = true)
    private String language ="en"; // default

    @Schema(name = "ISO-3166-1 alpha-2 country code - used for i18n in communication. " +
            "Default 'US'", required = true)
    private String country = "US"; // default


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

}
