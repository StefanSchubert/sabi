/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;
import static de.bluewhale.sabi.api.HttpHeader.TOKEN_PREFIX;

/**
 * Everything that can be reused and helps in communication with the backend.
 *
 * @author Stefan Schubert
 */
public class RestHelper {

    /**
     * Contains Mediatype Application Json and User Agent
     * @return
     */
    public static HttpHeaders buildHttpHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("user-agent", "Junit Driver");
        return headers;
    }

    /**
     * Contains Mediatype Application Json, User Agent and Auth-Informations
     * @param JWTBackendAuthtoken valid Auth-Token for the Backend-Session
     * @return http header required to mak calls against secured API
     */
    public static HttpHeaders prepareAuthedHttpHeader(String JWTBackendAuthtoken) {
        HttpHeaders headers = buildHttpHeader();
        headers.add(AUTH_TOKEN, TOKEN_PREFIX + JWTBackendAuthtoken);
        return headers;
    }

}
