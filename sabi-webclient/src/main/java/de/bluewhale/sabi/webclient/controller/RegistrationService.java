/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.NewRegistrationTO;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.webclient.CDIBeans.ApplicationInfo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.model.ChallengeTo;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
@Slf4j
public class RegistrationService implements Serializable {
// ------------------------------ FIELDS ------------------------------

    // TODO STS (15.09.20): Separate View Controller and apigateway here

    static String REGISTER_PAGE = "register";
    static String PREREGISTER_PAGE = "preregistration";

    @Value("${captcha.backend.url}")
    private String captchaBackendUrl;

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Autowired
    private UserSession userSession;

    @Autowired
    private ApplicationInfo applicationInfo;

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

        String registerURI = sabiBackendUrl + "/api/auth/register";

        // Preliminary checks before sending requests to the backend.
        String userAnswer = model.getCaptchaCode();
        if (Strings.isEmpty(userAnswer)) {
            String message = MessageUtil.getFromMessageProperties("register.captcha_missing.t", userSession.getLocale());
            MessageUtil.error("captcha", message);
            return REGISTER_PAGE;
        }

        // For registration we take the language settings from guessed environment
        model.setLanguage(userSession.getLanguage());
        model.setCountry(userSession.getLocale().getCountry());

        // Continue processing by calling the backend
        HttpHeaders headers = RestHelper.buildHttpHeader();

        String requestJson = null;
        try {
            requestJson = objectMapper.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            log.error("Coudn't convert form data into JSON reprasentation.", e);
            String message = MessageUtil.getFromMessageProperties("common.error.backend_unreachable.l", userSession.getLocale());
            MessageUtil.fatal("captcha", message);
            return REGISTER_PAGE;
        }

        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);


        try {
            ResponseEntity<UserTo> responseEntity = restTemplate.postForEntity(registerURI, entity, UserTo.class);

            if (HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
                return PREREGISTER_PAGE;
            }

        } catch (HttpClientErrorException e) {
            if (HttpStatus.PRECONDITION_FAILED.equals(e.getStatusCode())) {
                log.warn("Wrong Captcha answer during registration attempt.");
                String message = MessageUtil.getFromMessageProperties("register.captcha_wrongAnswer.t", userSession.getLocale());
                MessageUtil.warn("captcha", message);
            }
            if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
                log.warn("Register attempt with already existing username or email.");
                String message = MessageUtil.getFromMessageProperties("register.conflict.t", userSession.getLocale());
                MessageUtil.warn("username", message);
                MessageUtil.warn("email", message);

            }
            fetchNewCaptchaChallenge();
            return REGISTER_PAGE;
        } catch (RestClientException e) {
            log.error("Backend processing error.", e);
            String message = MessageUtil.getFromMessageProperties("common.error.backend_unreachable.l", userSession.getLocale());
            MessageUtil.fatal("commonFailure", message);
            return REGISTER_PAGE;
        }

        // any unhandled case? stays on the same page!
        return REGISTER_PAGE;
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
            log.error("Coudn't reach captcha backend.", e);
            String message = MessageUtil.getFromMessageProperties("common.error.backend_unreachable.l", userSession.getLocale());
            MessageUtil.fatal("captcha", message);
        }
    }

}
