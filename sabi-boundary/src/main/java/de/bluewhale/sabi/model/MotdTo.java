/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Just a Message of Today (motd)
 */
@Data
public class MotdTo implements Serializable {

    @Schema(description =  "Message of today which may contain maintenance announcements.", required = true)
    private String modt;

    public MotdTo(final String modt) {
        this.modt = modt;
    }

    public MotdTo() {
        this.modt = "";
    }

}
