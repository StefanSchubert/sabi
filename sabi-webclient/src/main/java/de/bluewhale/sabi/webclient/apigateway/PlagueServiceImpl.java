/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Calls Sabi Backend to manage users plague records.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class PlagueServiceImpl implements PlagueService {

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Autowired
    private UserSession userSession;

    @Override
    public List<PlagueTo> getPlagueCatalogue(String JWTBackendAuthtoken) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTank(String JWTAuthtoken, Long tankId) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTankFilteredByUnit(String JWTAuthtoken, Long tankId, Integer plagueId) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public void deletePlagueRecordById(Long plagueRecordId, String JWTBackendAuthtoken) throws BusinessException {

    }

    @Override
    public void save(PlagueRecordTo plagueRecord, String JWTBackendAuthtoken) throws BusinessException {

    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUserTanks(String JWTBackendAuthtoken) throws BusinessException {

        List<PlagueRecordTo> plagueRecordTos;
        String listPlagueUri = sabiBackendUrl + Endpoint.PLAGUE_CENTER_SERVICE+ "/list";

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(JWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            responseEntity = restTemplate.exchange(listPlagueUri, HttpMethod.GET, requestEntity, String.class);
            renewBackendToken(responseEntity);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",listPlagueUri),e.getLocalizedMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

        try {
            PlagueRecordTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), PlagueRecordTo[].class);
            plagueRecordTos = Arrays.asList(myObjects);
        } catch (JsonProcessingException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s", listPlagueUri, e.getMessage()), e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        return plagueRecordTos;
    }

    private void renewBackendToken(ResponseEntity<String> responseEntity) {
        if( responseEntity.getHeaders().containsKey(HttpHeaders.AUTHORIZATION) ) {
            userSession.setSabiBackendToken(responseEntity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        }
    }

}
