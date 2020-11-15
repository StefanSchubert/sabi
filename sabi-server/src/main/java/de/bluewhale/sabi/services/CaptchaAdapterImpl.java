/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * using Bluewhales own simplified captcha service.
 *
 * @author Stefan Schubert
 */
@Service
@Slf4j
public class CaptchaAdapterImpl implements CaptchaAdapter {

    static int CONNECT_TIMEOUT_IN_MILLIS = 20000;

    @Value("${captcha.check.url}")
    String captchaService;

    @Override
    public Boolean isCaptchaValid(String captchaAnswer) throws IOException {

        boolean isValid = false;
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
        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_ACCEPTABLE.equals(e.getStatusCode())) {
                log.warn("captcha token not accepted.");
                isValid = false;
            }
        }
        catch (RestClientException e) {
            String reason = "Could not connect to the captcha service " + checkURI;
            log.error(reason,e);
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
