/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.webclient.model.ChallengeTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;

/**
 * Client to the used Captcha Service
 *
 * @author Stefan Schubert
 */
@Named
@ViewScoped
public class CaptchaClient implements Serializable {
// ------------------------------ FIELDS ------------------------------

    static Logger logger = LoggerFactory.getLogger(CaptchaClient.class);

    @Value("${captcha.backend.url}")
    private String captchaBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    private ChallengeTo challenge;

    private RestTemplate restTemplate = new RestTemplate();

// --------------------- GETTER / SETTER METHODS ---------------------

    public ChallengeTo getChallenge() {
        return this.challenge;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Used to retrieve a new Challenge from the used Captcha Service
     */
    public void fetchNewCaptchaChallenge() {
        // todo fetching captcha challenge from UserContext
        // make sure to check it against Suppoeted Languages and use englisch as fallback
        String lang = "de";

        String checkURI = captchaBackendUrl + "/challenge/" + lang;

        try {
            challenge = restTemplate.getForObject(checkURI, ChallengeTo.class);
        } catch (RestClientException e) {
            logger.error("Coudn't reach captcha backend.", e);
            // TODO STS (2019-08-17): Fill jsf error context, if backend error occured.
        }
    }
}
