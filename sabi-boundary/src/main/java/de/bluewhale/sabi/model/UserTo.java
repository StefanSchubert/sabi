/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO of the user which will be used during registration process, or during maintenance of users properties.
 * User: Stefan
 * Date: 29.08.15
 */
@Data
@NoArgsConstructor
public class UserTo implements Serializable {

    private Long id;
    @Schema(description =  "Will be used only during registration process, when the user validates his email address.", required = false)
    private String validationToken;

    @Schema(description =  "Users Emailaddress", required = true)
    private String email;

    @Schema(description =  "Userlogin", required = true)
    private String username;

    private String password;

    @Schema(description =  "Needs to be a valid code otherwise you won't be registered - this helps avoiding simple DOS-attacks in future", required = false)
    private String captchaCode;

    @Schema(description =  "ISO-639-1 language code - used for i18n in communication. Must be set togehter with country. Defaults to 'en'", required = true)
    private String language = "en"; // default

    @Schema(description =  "ISO-3166-1 alpha-2 country code - used for i18n in communication. Default 'US'", required = true)
    private String country = "US"; // default

    private boolean validated;


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

    public boolean isValidated() {
        return this.validated;
    }

}
