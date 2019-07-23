/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * useing Bluewhales own simplified captcha service.
 *
 * @author Stefan Schubert
 */
@Service
public class CaptchaAdapterImpl implements CaptchaAdapter {

    static Logger logger = LoggerFactory.getLogger(CaptchaAdapterImpl.class);
    static int CONNECT_TIMEOUT_IN_MILLIS = 20000;

    @Value("${captcha.check.url}")
    String captchaService;

    @Override
    public Boolean isCaptchaValid(String captchaAnswer) throws IOException {

        boolean isValid;
        RestTemplate restTemplate = new RestTemplate();
        setTimeout(restTemplate,CONNECT_TIMEOUT_IN_MILLIS);

        Map params = new HashMap<String, String>(1);
        params.put("code", captchaAnswer);

        String checkURI = captchaService + "/{code}";

        try {
            final String checkresult = restTemplate.getForObject(checkURI, String.class, params);

            if ("Accepted".equals(checkresult)) {
                isValid = true;
            } else {
                isValid = false;
            }
        } catch (RestClientException e) {
            String reason = "Could not connect to the captcha service " + checkURI;
            logger.error(reason,e);
            throw new IOException(reason);
        }

        return isValid;
    }

    private void setTimeout(RestTemplate restTemplate, int timeoutInMilliSeconds) {
        //Explicitly setting ClientHttpRequestFactory instance to
        //SimpleClientHttpRequestFactory instance to leverage
        //set*Timeout methods
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory());
        SimpleClientHttpRequestFactory rf = (SimpleClientHttpRequestFactory) restTemplate
                .getRequestFactory();
        rf.setReadTimeout(timeoutInMilliSeconds);
        rf.setConnectTimeout(timeoutInMilliSeconds);
    }
}
