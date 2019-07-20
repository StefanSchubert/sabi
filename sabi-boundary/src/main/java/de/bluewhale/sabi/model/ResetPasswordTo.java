/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents the User credentials as provided by the login.
 *
 * @author Stefan Schubert
 */
public class ResetPasswordTo {
// ------------------------------ FIELDS ------------------------------

    private String emailAddress;
    private String resetToken;
    private String newPassword;

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getResetToken() {
        return this.resetToken;
    }

    @ApiModelProperty(notes = "Reset token as provided via password reset request confirmation mail.", example="kSzu6", required = true)
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    @ApiModelProperty(notes = "Users email address.", example="john.doe@bluewhale.de", required = true)
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    @ApiModelProperty(notes = "Users new password.", example="You_Never_Know_:-)", required = true)
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ResetPasswordTo{" +
                "emailAddress='" + emailAddress + '\'' +
                ", resetToken='" + resetToken + '\'' +
                '}';
    }
}
