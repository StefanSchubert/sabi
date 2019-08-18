/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.model.ChallengeTo;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.faces.view.ViewScoped;
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
@ViewScoped
public class CaptchaClient implements Serializable {
// ------------------------------ FIELDS ------------------------------

    static Logger logger = LoggerFactory.getLogger(CaptchaClient.class);

    @Value("${captcha.backend.url}")
    private String captchaBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Autowired
    private UserSession userSession;

    private ChallengeTo challenge;

    private String choosenAnswer = "N/A";

    private RestTemplate restTemplate = new RestTemplate();

// --------------------- GETTER / SETTER METHODS ---------------------

    public ChallengeTo getChallenge() {
        return this.challenge;
    }

    public String getChoosenAnswer() {
        return this.choosenAnswer;
    }

    public void setChoosenAnswer(final String choosenAnswer) {
        this.choosenAnswer = choosenAnswer;
    }

// -------------------------- OTHER METHODS --------------------------

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
            String message = MessageUtil.getFromMessageProperties("common.error_backend_unreachable.lab",userSession.getLocale());
            MessageUtil.fatal("captcha", message);
        }
    }
}
