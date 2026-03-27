/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Request DTO for OIDC login endpoint.
 * The webclient forwards the raw Google ID token after completing the OAuth2 Authorization Code Flow.
 *
 * @author Stefan Schubert
 */
@Data
public class OidcLoginRequestTo implements Serializable {

    @NotBlank
    @Schema(description = "Raw Google ID token JWT as returned by Google's token endpoint.", required = true)
    private String idToken;

    @NotBlank
    @Schema(description = "OIDC provider identifier. Currently always 'GOOGLE'.", example = "GOOGLE", required = true)
    private String provider;

}

