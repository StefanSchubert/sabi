/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Represents the User credentials as provided by the login.
 *
 * @author Stefan Schubert
 */
public class AccountCredentialsTo implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private String username;
    private String password;

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getPassword() {
        return this.password;
    }

    @Schema(name = "Users password.", example="kSzu65#@!$§g642", required = true)
    public void setPassword(String password) {
        this.password = password;
    }

    @Schema(name = "We use users email address as unique username.", example="sabi@bluewhale.de", required = true)
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
