/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.utils;

import de.bluewhale.sabi.webclient.CDIBeans.ApplicationInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
}
