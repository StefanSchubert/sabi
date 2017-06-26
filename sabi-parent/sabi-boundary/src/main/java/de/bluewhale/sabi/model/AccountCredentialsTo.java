/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents the User credentials as provided by the login.
 *
 * @author Stefan Schubert
 */
public class AccountCredentialsTo {
// ------------------------------ FIELDS ------------------------------

    private String username;
    private String password;

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getPassword() {
        return this.password;
    }

    @ApiModelProperty(notes = "Users password.", example="kSzu65#@!$§g642", required = true)
    public void setPassword(String password) {
        this.password = password;
    }

    @ApiModelProperty(notes = "We use users email address as unique username.", example="sabi@bluewhale.de", required = true)
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
