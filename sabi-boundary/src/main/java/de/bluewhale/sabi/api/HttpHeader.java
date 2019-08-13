/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.api;

/**
 * Contains Header Values used by Backend and Frontend
 *
 * @author Stefan Schubert
 */
public class HttpHeader {

    public static final String TOKEN_PREFIX = "Bearer "; // Don't touch the space.

    /**
     * This is the Header field for submitting and requesting the
     * JWT AuthToken (following the Bearer Scheme), to pass
     * authentication info to the stateless rest-backend.
     * see https://github.com/StefanSchubert/sabi/wiki/06.-Runtime-View
     */
    public static final String AUTH_TOKEN = "Authorization";

}
