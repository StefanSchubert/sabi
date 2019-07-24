/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.CDIBeans;

import org.springframework.web.context.annotation.SessionScope;

import javax.inject.Named;
import java.io.Serializable;

/**
 * Container for mandatory things which we need to keep in a user session.
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
public class UserSession implements Serializable {

    private String sabiBackendToken;

    /**
     * After being successful authenticated against the sabi backend,
     * this token will be submitted via HTTP-Header whenever calling
      * sabis rest api backend.
     * @return auth token, see {@link de.bluewhale.sabi.api.HttpHeader#AUTH_TOKEN}
     */
    public String getSabiBackendToken() {
        return sabiBackendToken;
    }

    /**
     * After being successful authenticated against the sabi backend,
     * this token will be submitted via HTTP-Header whenever calling
     * sabis rest api backend.
     * @param sabiBackendToken
     */
    public void setSabiBackendToken(String sabiBackendToken) {
        this.sabiBackendToken = sabiBackendToken;
    }
}
