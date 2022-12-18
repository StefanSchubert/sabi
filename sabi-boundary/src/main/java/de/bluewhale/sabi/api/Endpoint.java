/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.api;

/**
 * Listing of API-Endpoints provided by Sabi-Backend
 *
 * @author Stefan Schubert
 */
public enum Endpoint {
    // requires complete path after root context, as this one is also used by HttpSecurityConfig
    LOGIN("/api/auth/login"),
    REGISTER("/api/auth/register"),
    PW_RESET_REQUEST("/api/auth/req_pwd_reset"),
    PW_RESET("/api/auth/pwd_reset"),
    PARTICIPANT_STATS("/api/stats/participants"),
    HEALTH_STATS("/api/stats/healthcheck"),
    TANK_STATS("/api/stats/tanks"),
    MEASUREMENT_STATS("/api/stats/measurements"),
    MEASUREMENTS("/api/measurement"),
    PLAGUE_STATS("/api/stats/plagues"),
    IOT_API("/api/aquarium_iot"),
    TANKS("/api/tank"),
    UNITS("/api/units"),
    USER_PROFILE("/api/userprofile"),
    PLAGUE_CENTER_SERVICE("/api/plagues");
    ;

// ------------------------------ FIELDS ------------------------------

    private String path;

// --------------------------- CONSTRUCTORS ---------------------------

    Endpoint(String pPath) {
        this.path = pPath;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getPath() {
        return this.path;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        return path;
    }
}
