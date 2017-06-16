/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.rest.model;

/**
 *
 * Author: Stefan Schubert
 * Date: 27.09.15
 */
@Deprecated // todo trash it / has been removed to boundary modul.
public class User {

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String email;
    private String password;

}
