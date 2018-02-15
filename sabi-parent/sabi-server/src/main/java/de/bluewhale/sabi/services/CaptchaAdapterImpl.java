/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${captcha.check.url}")
    String captchaService;

    @Override
    public Boolean isCaptchaValid(String captchaAnswer) throws IOException {

        boolean isValid;
        RestTemplate restTemplate = new RestTemplate();

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
            // TODO STS (22.10.17): replace with proper Logging
            String reason = "Could not connect to the captcha service " + checkURI;
            System.out.println(reason);
            throw new IOException(reason);
        }

        return isValid;
    }
}
