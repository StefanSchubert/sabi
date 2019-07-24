/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.api.HttpHeader;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Named;

/**
 * Responsible to handle auth and profile operations that are directly related to the user.
 *
 * @author Stefan Schubert
 */
@Named
public class UserServiceImpl implements UserService {

    static Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    // FIXME: 2019-07-24 JSF2.3 way would be to work with a SessionMap, refactor this
    @Autowired
    private UserSession userSession;

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
            logger.error("Object Mapper failed with Login Data - No Logins possible!", e);
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
                e.printStackTrace();
            }
        }

        if (responseEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            // We need to extract and remember the header for subsequent REST request
            String jwtSabiBackendToken = responseHeaders.getFirst(HttpHeader.AUTH_TOKEN);
            userSession.setSabiBackendToken(jwtSabiBackendToken);
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
