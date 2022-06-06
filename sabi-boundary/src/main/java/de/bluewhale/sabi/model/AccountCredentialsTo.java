/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Represents the User credentials as provided by the login.
 *
 * @author Stefan Schubert
 */
@Data
public class AccountCredentialsTo implements Serializable {

    @Schema(name = "We use users email address as unique username.", example="sabi@bluewhale.de", required = true)
    private String username;
    @Schema(name = "Users password.", example="kSzu65#@!$§g642", required = true)
    private String password;

}
