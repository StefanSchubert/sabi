/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.utils;

import de.bluewhale.sabi.webclient.CDIBeans.ApplicationInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

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
        headers.add("user-agent", "Sabi JSF Webclient "+ ApplicationInfo.buildVersion);
        return headers;
    }

    /**
     * Contains Mediatype Application Json, User Agent and Auth-Informations
     * @param JWTBackendAuthtoken valid Auth-Token for the Backend-Session
     * @return http header required to mak calls against secured API
     */
    public static HttpHeaders prepareAuthedHttpHeader(String JWTBackendAuthtoken) {
        HttpHeaders headers = buildHttpHeader();
        headers.add(AUTH_TOKEN, JWTBackendAuthtoken);
        return headers;
    }

    /**
     * Contains Mediatype Application Json, User Agent and Auth-Informations
     * @param JWTBackendAuthtoken valid Auth-Token (incl. Bearer Prefix) for the Backend-Session
     * @param contentTypeHeader sets the header 'Content-Type'
     * @return http header required to mak calls against secured API
     */
    public static HttpHeaders prepareAuthedHttpHeader(String JWTBackendAuthtoken, MediaType contentTypeHeader) {
        HttpHeaders headers = buildHttpHeader();
        headers.add(AUTH_TOKEN, JWTBackendAuthtoken);
        headers.setContentType(contentTypeHeader);
        return headers;
    }

}
