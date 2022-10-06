/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls Sabi Backend to manage users plague records.
 *
 * @author Stefan Schubert
 */
@Slf4j
public abstract class APIServiceImpl {

    @Value("${sabi.backend.url}")
    protected String sabiBackendUrl;

    @Autowired
    protected ObjectMapper objectMapper;  // json mapper

    @Autowired
    protected UserSession userSession;

    protected  ResponseEntity<String> getAPIResponseFor(String pEndpointURL, String pJWTBackendAuthToken, HttpMethod pHttpMethod) throws BusinessException {

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            responseEntity = restTemplate.exchange(pEndpointURL, HttpMethod.GET, requestEntity, String.class);
            renewBackendToken(responseEntity);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",pEndpointURL),e.getLocalizedMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

        return responseEntity;

    }

    protected void renewBackendToken(ResponseEntity<String> responseEntity) {
        if( responseEntity.getHeaders().containsKey(HttpHeaders.AUTHORIZATION) ) {
            userSession.setSabiBackendToken(responseEntity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        }
    }

}
