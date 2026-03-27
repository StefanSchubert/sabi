/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Response DTO for OIDC login endpoint (200 OK).
 * Contains the Sabi JWT and basic user information.
 *
 * @author Stefan Schubert
 */
@Data
public class OidcLoginResponseTo implements Serializable {

    @Schema(description = "Sabi JWT — identical format to password-login JWT.")
    private String token;

    @Schema(description = "Verified email address from the Google ID token.")
    private String email;

    @Schema(description = "The Sabi username (existing or newly provisioned).")
    private String username;

    @Schema(description = "true if a new Sabi account was auto-created; false if an existing account was used/linked.")
    private boolean provisioned;

}

