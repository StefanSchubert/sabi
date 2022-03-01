/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the User credentials as provided by the login.
 *
 * @author Stefan Schubert
 */
public class RequestNewPasswordTo {
// ------------------------------ FIELDS ------------------------------

    private String emailAddress;
    private String captchaToken;

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getCaptchaToken() {
        return this.captchaToken;
    }

    @Schema(name = "Answer token of the captcha challenge.", example="kSzu6", required = true)
    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    @Schema(name = "Users email address.", example="john.doe@bluewhale.de", required = true)
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
