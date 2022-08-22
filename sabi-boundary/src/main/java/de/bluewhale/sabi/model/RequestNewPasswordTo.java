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
public class RequestNewPasswordTo implements Serializable  {

    @Schema(description =  "Users email address.", example="john.doe@bluewhale.de", required = true)
    private String emailAddress;
    @Schema(description =  "Answer token of the captcha challenge.", example="kSzu6", required = true)
    private String captchaToken;
}
