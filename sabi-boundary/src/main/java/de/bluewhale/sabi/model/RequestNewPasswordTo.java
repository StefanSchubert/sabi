/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

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

    @ApiModelProperty(notes = "Answer token of the captcha challenge.", example="kSzu6", required = true)
    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    @ApiModelProperty(notes = "Users email address.", example="john.doe@bluewhale.de", required = true)
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
