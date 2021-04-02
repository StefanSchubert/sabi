/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.api.HttpHeader;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.SessionScope;

import javax.inject.Named;
import java.util.Locale;

/**
 * Responsible to handle auth and profile operations that are directly related to the user.
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Slf4j
public class UserServiceImpl implements UserService {

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Autowired
    private UserSession userSession;

    @Autowired
    private I18nUtil i18nUtil;

    /* Does not work this way currently with joinfaces
       Tried to use ist, to get browsers locale, but reefactored the code to use springs context instead via LocaleContextHolder()
    @Inject
    private FacesContext facesContext;
    */

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public ResultTo<UserTo> registerNewUser(NewRegistrationTO newUser) {
        throw new UnsupportedOperationException("de.bluewhale.sabi.model.ResultTo<de.bluewhale.sabi.model.UserTo> registerNewUser([newUser])");
    }

    @Override
    public ResultTo<String> signIn(String pEmail, String pClearTextPassword) {

        AccountCredentialsTo loginData = new AccountCredentialsTo();
        loginData.setPassword(pClearTextPassword);
        loginData.setUsername(pEmail);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);

        String requestJson = null;
        try {
            requestJson = objectMapper.writeValueAsString(loginData);
        } catch (JsonProcessingException e) {
            log.error("Object Mapper failed with Login Data - No Logins possible! {}", e);
            return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.BACKEND_TEMPORARILY_UNAVAILABLE));
        }

        HttpEntity<?> entity = new HttpEntity<>( requestJson, requestHeaders ); // for request

        ResponseEntity<String> responseEntity = null;
        HttpHeaders responseHeaders = null;
        try {

            responseEntity = restTemplate.exchange(sabiBackendUrl + Endpoint.LOGIN, HttpMethod.POST, entity, String.class);
            String result= responseEntity.getBody();
            responseHeaders = responseEntity.getHeaders();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.UNKNOWN_USERNAME));
            } else {
                log.error("Couldn't talk proper to sabiBackend. {}",e);
                return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.BACKEND_TEMPORARILY_UNAVAILABLE));
            }
        }

        if (responseEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            // We need to extract and remember the header for subsequent REST request
            String jwtSabiBackendToken = responseHeaders.getFirst(HttpHeader.AUTH_TOKEN);
            userSession.setSabiBackendToken(jwtSabiBackendToken);

            // TODO STS (29.12.19): Should be done only, if he or she hasn't set it explicitly.
            // In addition we determine users locale here.
            Locale browsersLocale = LocaleContextHolder.getLocale();
            Locale supportedLocale = i18nUtil.getEnsuredSupportedLocale(browsersLocale.getLanguage());
            LocaleContextHolder.setLocale(supportedLocale);
            userSession.setLocale(supportedLocale);

            // TODO STS (29.12.19): Refactor this to contain the chosen username instead of the email.
            userSession.setUserName(loginData.getUsername());

            return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.SIGNIN_SUCCEEDED));
        } else {
            // catchall - won't happen, as the 401 will we thrown as exception catched above.
            return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.UNKNOWN_USERNAME));
        }
    }

    @Override
    public void requestPasswordReset(RequestNewPasswordTo requestData) throws BusinessException {
        throw new UnsupportedOperationException("void requestPasswordReset([requestData])");
    }

    @Override
    public void resetPassword(ResetPasswordTo requestData) throws BusinessException {
        throw new UnsupportedOperationException("void resetPassword([requestData])");
    }
}
