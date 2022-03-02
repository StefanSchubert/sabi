/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Just a Message of Today (motd)
 */
public class MotdTo implements Serializable {
// ------------------------------ FIELDS ------------------------------

    private String modt;

// --------------------------- CONSTRUCTORS ---------------------------

    public MotdTo() {
        this.modt = "";
    }

    public MotdTo(final String modt) {
        this.modt = modt;
    }

// -------------------------- OTHER METHODS --------------------------

    @Schema(name = "Message of today which may contain maintenance announcements.", required = true)
    public String getMotd() {
        return modt;
    }

    public void setMotd(String pMotd) {
        this.modt = pMotd;
    }
}
