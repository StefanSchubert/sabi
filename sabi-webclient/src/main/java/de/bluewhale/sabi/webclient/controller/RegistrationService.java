/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.NewRegistrationTO;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.model.ChallengeTo;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Named;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * Client to the used Captcha Service
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
public class RegistrationService implements Serializable {
// ------------------------------ FIELDS ------------------------------

    static Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Value("${captcha.backend.url}")
    private String captchaBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Autowired
    private UserSession userSession;

    private ChallengeTo challenge;

    private RestTemplate restTemplate = new RestTemplate();

// --------------------- GETTER / SETTER METHODS ---------------------

    public ChallengeTo getChallenge() {
        return this.challenge;
    }

// -------------------------- OTHER METHODS --------------------------

    private NewRegistrationTO model = new NewRegistrationTO();


    public NewRegistrationTO getModel() {
        return model;
    }

    public void setModel(NewRegistrationTO model) {
        this.model = model;
    }

    /**
     * Used to retrieve a new Challenge from the used Captcha Service
     */
    public void fetchNewCaptchaChallenge() {

        String checkURI = captchaBackendUrl + "/challenge/" + userSession.getLanguage();

        try {
            challenge = restTemplate.getForObject(checkURI, ChallengeTo.class);

            // jsf requires the map not in Key->Value but in Lable->Value Format for the selectOneRadio tag.
            // so we transpose the retrieved map from captcha service here.
            Map<String,String> answers_transposed = new HashMap<String, String>(challenge.getAnswers().size());
            challenge.getAnswers().forEach((key,value)->answers_transposed.put(value,key));
            challenge.setAnswers(answers_transposed);

        } catch (RestClientException e) {
            logger.error("Coudn't reach captcha backend.", e);
            String message = MessageUtil.getFromMessageProperties("common.error_backend_unreachable.l",userSession.getLocale());
            MessageUtil.fatal("captcha", message);
        }
    }

    public String doRegister() {
        String outcome = "register"; // default is to stay on the registration page
        // check if captcha is valid

        // check if user does not exists already

        // create new user and redirect him to the pre-register page.
        outcome = "preregistration";

        return outcome;
    }

}
