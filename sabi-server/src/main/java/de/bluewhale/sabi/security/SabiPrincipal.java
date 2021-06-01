/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.security.Principal;

/**
 * Sabis own principal Object
 * Might be later used to check specific roles (admin functions etc...)
 *
 * @author Stefan Schubert
 */
public class SabiPrincipal implements Principal, Serializable {

    private final String name;


    /**
     * We set users email as unique ID for principal name.
     * @param pUserEmailAsID
     */
    public SabiPrincipal(String pUserEmailAsID) {
        name = pUserEmailAsID;
    }

    @Override
    /**
     * Name of the principal. In our case this will be users Email-Address.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean implies(Subject subject) {
        return Principal.super.implies(subject);
    }
}
