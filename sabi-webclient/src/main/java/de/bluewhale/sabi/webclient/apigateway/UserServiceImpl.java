/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.api.HttpHeader;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.utils.I18nUtil;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.SessionScope;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
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

    @Inject
    FacesContext facesContext;

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

        HttpEntity<?> entity = new HttpEntity<>(requestJson, requestHeaders); // for request

        ResponseEntity<String> responseEntity = null;
        HttpHeaders responseHeaders = null;
        try {
            responseEntity = restTemplate.exchange(sabiBackendUrl + Endpoint.LOGIN, HttpMethod.POST, entity, String.class);
            String result = responseEntity.getBody();
            responseHeaders = responseEntity.getHeaders();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.UNKNOWN_USERNAME));
            } else {
                log.error("Couldn't talk proper to sabiBackend. {}", e.getMessage());
                return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.BACKEND_TEMPORARILY_UNAVAILABLE));
            }
        }

        if (responseEntity.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            // We need to extract and remember the header for subsequent REST request
            String jwtSabiBackendToken = responseHeaders.getFirst(HttpHeader.AUTH_TOKEN);
            userSession.setSabiBackendToken(jwtSabiBackendToken);

            UserProfileTo userProfileTo = requestUserProfile(jwtSabiBackendToken);

            Locale supportedLocale;
            if (userProfileTo != null && !Strings.isNullOrEmpty(userProfileTo.getLanguage())) {
                /* Locale from stored user profile */
                supportedLocale = i18nUtil.getEnsuredSupportedLocale(userProfileTo.getLanguage());
            } else {
                /* Locale derived from browser */
                Locale browsersLocale = LocaleContextHolder.getLocale(); // used by spring
                supportedLocale = i18nUtil.getEnsuredSupportedLocale(browsersLocale.getLanguage());
            }

            LocaleContextHolder.setLocale(supportedLocale); // Used by spring
            userSession.setLocale(supportedLocale);
            userSession.setUserName(loginData.getUsername());

            return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.SIGNIN_SUCCEEDED));
        } else {
            // catchall - won't happen, as the 401 will we thrown as exception catched above.
            return new ResultTo<String>(pEmail, Message.info(AuthMessageCodes.UNKNOWN_USERNAME));
        }
    }

    /**
     * Used to query users profile
     *
     * @param sabiBackendToken
     * @return userProfile if everything went fine, null in case of any problems
     */
    private UserProfileTo requestUserProfile(String sabiBackendToken) {

        String userProfileURL = sabiBackendUrl + Endpoint.USER_PROFILE.getPath();
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(sabiBackendToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity;
        UserProfileTo userProfileTo = null;

        try {
            responseEntity = restTemplate.exchange(userProfileURL, HttpMethod.GET, requestEntity, String.class);
            userProfileTo = objectMapper.readValue(responseEntity.getBody(), UserProfileTo.class);

        } catch (Exception e) {
            log.error("Could not access Users Profile, Reason {}",e.getMessage());
            e.printStackTrace();
        }

        return userProfileTo;
    }

    @Override
    public void requestPasswordReset(RequestNewPasswordTo requestData) throws BusinessException {
        throw new UnsupportedOperationException("void requestPasswordReset([requestData])");
    }

    @Override
    public void resetPassword(ResetPasswordTo requestData) throws BusinessException {
        throw new UnsupportedOperationException("void resetPassword([requestData])");
    }

    @Override
    public void updateUsersProfile(@NotNull UserProfileTo pUserProfile, @NotNull String JWTBackendAuthtoken) throws BusinessException {

        String updateUserProfileURL = sabiBackendUrl + Endpoint.USER_PROFILE.getPath();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(JWTBackendAuthtoken, MediaType.APPLICATION_JSON);
            String requestJson = objectMapper.writeValueAsString(pUserProfile);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> responseEntity;

            RestTemplate restTemplate = new RestTemplate();
            responseEntity = restTemplate.exchange(updateUserProfileURL, HttpMethod.PUT, requestEntity, String.class);

        } catch (RestClientException e) {
            log.error("Couldn't reach {} reason {}", updateUserProfileURL, e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        } catch (Exception e) {
            log.error("Problem occurred while updating user profile, reason {}", e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }


    }
}
