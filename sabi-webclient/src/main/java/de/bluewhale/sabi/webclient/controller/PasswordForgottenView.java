/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.RequestNewPasswordTo;
import de.bluewhale.sabi.model.ResetPasswordTo;
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
 * Manages password forgotten use case
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope // ViewScope would be better, but bean will be renewed on captcha errors
@Slf4j
public class PasswordForgottenView implements Serializable {

    static String PASSWORD_FORGOTTON_PAGE = "pwreset";

    @Value("${captcha.backend.url}")
    private String captchaBackendUrl;

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Autowired
    private UserSession userSession;

    private ChallengeTo challenge;

    private RestTemplate restTemplate = new RestTemplate();
    private RequestNewPasswordTo pwReqModel = new RequestNewPasswordTo();
    private ResetPasswordTo pwResetModel = new ResetPasswordTo();
    // May be used to conditionally render the next part of the form.
    private boolean captchaAccepted = false;

    public RequestNewPasswordTo getPwReqModel() {
        return this.pwReqModel;
    }

    public void setPwReqModel(final RequestNewPasswordTo pwReqModel) {
        this.pwReqModel = pwReqModel;
    }

    public ResetPasswordTo getPwResetModel() {
        return this.pwResetModel;
    }

    public void setPwResetModel(final ResetPasswordTo pwResetModel) {
        this.pwResetModel = pwResetModel;
    }

    public boolean isCaptchaAccepted() {
        return this.captchaAccepted;
    }

    public ChallengeTo getChallenge() {
        return this.challenge;
    }


    public String reqPasswordResetMail() {

        String reqPWResetURI = sabiBackendUrl + "/api/auth/req_pwd_reset";

        // Preliminary checks before sending requests to the backend.
        String userAnswer = pwReqModel.getCaptchaToken();
        if (Strings.isEmpty(userAnswer)) {
            MessageUtil.error("captcha", "register.captcha_missing.t", userSession.getLocale());
            return PASSWORD_FORGOTTON_PAGE;
        }

        // Continue processing by calling the backend
        HttpHeaders headers = RestHelper.buildHttpHeader();

        String requestJson = null;
        try {
            requestJson = objectMapper.writeValueAsString(pwReqModel);
        } catch (JsonProcessingException e) {
            log.error("Coudn't convert form data into JSON reprasentation. {}", e);
            MessageUtil.fatal("captcha", "common.error.backend_unreachable.l", userSession.getLocale());
            return PASSWORD_FORGOTTON_PAGE;
        }

        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(reqPWResetURI, entity, String.class);

            if (HttpStatus.ACCEPTED.equals(responseEntity.getStatusCode())) {
                captchaAccepted = true; // signals to proceed with the form.
                MessageUtil.info("captcha", "pw_forgotten.reset_request_accepted.t", userSession.getLocale());
                return PASSWORD_FORGOTTON_PAGE;
            }

        } catch (HttpClientErrorException e) {
            if (HttpStatus.FAILED_DEPENDENCY.equals(e.getStatusCode())) {
                log.warn("Wrong Captcha answer during registration attempt.");
                MessageUtil.warn("captcha", "register.captcha_wrongAnswer.t", userSession.getLocale());
            }
            if (HttpStatus.NOT_ACCEPTABLE.equals(e.getStatusCode())) {
                log.warn("Not Acceptable - email is not registered.\n");
                MessageUtil.warn("email", "register.email_invalid.t", userSession.getLocale());

            }
            fetchNewCaptchaChallenge();
            return PASSWORD_FORGOTTON_PAGE;

        } catch (RestClientException e) {
            log.error("Backend processing error. {}", e);
            MessageUtil.fatal("commonFailure", "common.error.backend_unreachable.l", userSession.getLocale());
            return PASSWORD_FORGOTTON_PAGE;
        }

        // any unhandled case? stays on the same page!
        return PASSWORD_FORGOTTON_PAGE;
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
            log.error("Coudn't reach captcha backend. {}", e);
            String message = MessageUtil.getFromMessageProperties("common.error.backend_unreachable.l", userSession.getLocale());
            MessageUtil.fatal("captcha", message);
        }
    }

}
