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
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.SessionScope;

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
@SessionScope // ViewScope would be better, but bean will be renewed on captcha errors
public class RegistrationService implements Serializable {
// ------------------------------ FIELDS ------------------------------

    static Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    static String REGISTER_PAGE = "register";
    static String PREREGISTER_PAGE = "preregister";

    @Value("${captcha.backend.url}")
    private String captchaBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Autowired
    private UserSession userSession;

    private ChallengeTo challenge;

    private RestTemplate restTemplate = new RestTemplate();
    private NewRegistrationTO model = new NewRegistrationTO();

// --------------------- GETTER / SETTER METHODS ---------------------

    public ChallengeTo getChallenge() {
        return this.challenge;
    }

    public NewRegistrationTO getModel() {
        return model;
    }

    public void setModel(NewRegistrationTO model) {
        this.model = model;
    }

// -------------------------- OTHER METHODS --------------------------

    public String doRegister() {

        // check if captcha is valid
        String userAnswer = model.getCaptchaCode();
        if (Strings.isEmpty(userAnswer)) {
            String message = MessageUtil.getFromMessageProperties("register.captcha_missing.t", userSession.getLocale());
            MessageUtil.error("captcha", message);
            return REGISTER_PAGE;
        }

        String checkURI = captchaBackendUrl + "/check/" + userAnswer;
        ResponseEntity<String> response = null;

        try {
            response = restTemplate.getForEntity(checkURI, String.class);
            // returns 202 for valid code or 406 (which comes as a RestClientException) for wrong answer.
            if (HttpStatus.ACCEPTED.equals(response.getStatusCode())) {
                logger.debug("CaptchaCode has been successfully verified.");
            } else {
                logger.error("Unexpected return code from Captcha validation.");
                String message = MessageUtil.getFromMessageProperties("register.captcha_wrongAnswer.t", userSession.getLocale());
                MessageUtil.warn("captcha", message);
                fetchNewCaptchaChallenge();
                return REGISTER_PAGE;
            }
        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_ACCEPTABLE.equals(e.getStatusCode())) {
                logger.warn("Wrong Captcha answer during registration attempt.");
                String message = MessageUtil.getFromMessageProperties("register.captcha_wrongAnswer.t", userSession.getLocale());
                MessageUtil.warn("captcha", message);
                fetchNewCaptchaChallenge();
                return REGISTER_PAGE;
            }
        } catch (RestClientException e) {
            logger.error("Coudn't reach captcha backend.", e);
            String message = MessageUtil.getFromMessageProperties("common.error_backend_unreachable.l", userSession.getLocale());
            MessageUtil.fatal("captcha", message);
        }

        // Now that the captcha was correct - check if user does not exists already

        // create new user and redirect him to the pre-register page.
        // outcome = "preregistration";

        return PREREGISTER_PAGE;
    }

    /**
     * Used to retrieve a new Challenge from the used Captcha Service
     */
    public void fetchNewCaptchaChallenge() {
        String challengeURI = captchaBackendUrl + "/challenge/" + userSession.getLanguage();

        try {
            challenge = restTemplate.getForObject(challengeURI, ChallengeTo.class);

            // jsf requires the map not in Key->Value but in Lable->Value Format for the selectOneRadio tag.
            // so we transpose the retrieved map from captcha service here.
            Map<String, String> answers_transposed = new HashMap<String, String>(challenge.getAnswers().size());
            challenge.getAnswers().forEach((key, value) -> answers_transposed.put(value, key));
            challenge.setAnswers(answers_transposed);
        } catch (RestClientException e) {
            logger.error("Coudn't reach captcha backend.", e);
            String message = MessageUtil.getFromMessageProperties("common.error_backend_unreachable.l", userSession.getLocale());
            MessageUtil.fatal("captcha", message);
        }
    }
}
