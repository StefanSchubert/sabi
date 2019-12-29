/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
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
    GALLONS("gal");

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
