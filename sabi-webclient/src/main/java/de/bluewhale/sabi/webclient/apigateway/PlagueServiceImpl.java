/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
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
public class PlagueServiceImpl extends APIServiceImpl implements PlagueService {


    @Override
    public List<PlagueTo> getPlagueCatalogue(String pJWTBackendAuthtoken, String pLanguage) throws BusinessException {
        List<PlagueTo> plagueToList;
        String listPlagueUri = sabiBackendUrl + Endpoint.PLAGUE_CENTER_SERVICE + "/type/list";
        ResponseEntity<String> responseEntity = getAPIResponseFor(listPlagueUri + "/" + pLanguage, pJWTBackendAuthtoken, HttpMethod.GET);

        try {
            PlagueTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), PlagueTo[].class);
            plagueToList = (myObjects == null ? Collections.emptyList() : Arrays.asList(myObjects));
        } catch (JsonProcessingException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s", listPlagueUri, e.getMessage()), e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        // Fallback to en if requested language was not available
        if (!pLanguage.equalsIgnoreCase("en") && plagueToList.isEmpty()) {
            plagueToList = getPlagueCatalogue(pJWTBackendAuthtoken, "en");
        }
        return plagueToList;
    }

    @Override
    public List<PlagueStatusTo> getPlagueStatusList(String pJWTBackendAuthtoken, String language) throws BusinessException {

        List<PlagueStatusTo> plagueStatusToList;
        String listPlagueUri = sabiBackendUrl + Endpoint.PLAGUE_CENTER_SERVICE + "/status/list";

        ResponseEntity<String> responseEntity = getAPIResponseFor(listPlagueUri + "/" + language, pJWTBackendAuthtoken, HttpMethod.GET);

        try {
            PlagueStatusTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), PlagueStatusTo[].class);
            plagueStatusToList = (myObjects == null ? Collections.emptyList() : Arrays.asList(myObjects));
        } catch (JsonProcessingException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s", listPlagueUri, e.getMessage()), e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        // Fallback to en if requested language was not available
        if (!language.equalsIgnoreCase("en") && plagueStatusToList.isEmpty()) {
            plagueStatusToList = getPlagueStatusList(pJWTBackendAuthtoken, "en");
        }

        return plagueStatusToList;

    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTank(String pJWTBackendAuthtoken, Long tankId) throws BusinessException {
        // TODO STS (02.10.22): ImplMe or drop it
        return Collections.emptyList();
    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTankFilteredByPlague(String pJWTBackendAuthtoken, Long tankId, Integer plagueId) throws BusinessException {
        // TODO STS (02.10.22): ImplMe or drop it
        return Collections.emptyList();
    }

    @Override
    public void deletePlagueRecordById(Long plagueRecordId, String pJWTBackendAuthtoken) throws BusinessException {
        // TODO STS (02.10.22): ImplMe or drop it
    }

    @Override
    public void save(PlagueRecordTo plagueRecord, String pJWTBackendAuthtoken) throws BusinessException {

        String addPlageRecordURI = sabiBackendUrl + Endpoint.PLAGUE_CENTER_SERVICE+"/record"; // POST here
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(plagueRecord);
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert plage record object to json: {}",plagueRecord);
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson,headers);

        try {
            responseEntity = restTemplate.postForEntity(addPlageRecordURI, requestEntity, String.class);
            renewBackendToken(responseEntity);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",addPlageRecordURI),e.getLocalizedMessage());
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()){
            /* FIXME STS (05.10.22): Fix status code mapping to real backend API  */
            if (responseEntity.getStatusCode().equals(HttpStatusCode.valueOf(500))) {
                log.error("Could not create plague record: {}",plagueRecord.toString());
                throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
            }
            if (responseEntity.getStatusCode().equals(HttpStatusCode.valueOf(401))) {
                log.warn("Invalid Token when trying to add a plague record: {}",plagueRecord.toString());
                throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
            }
        }

    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUserTanks(String pJWTBackendAuthtoken) throws BusinessException {

        List<PlagueRecordTo> plagueRecordTos;
        String listPlagueUri = sabiBackendUrl + Endpoint.PLAGUE_CENTER_SERVICE + "/record/list";

        ResponseEntity<String> responseEntity = getAPIResponseFor(listPlagueUri, pJWTBackendAuthtoken, HttpMethod.GET);

        try {
            PlagueRecordTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), PlagueRecordTo[].class);
            plagueRecordTos = (myObjects == null ? Collections.emptyList() : Arrays.asList(myObjects));
        } catch (JsonProcessingException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s", listPlagueUri, e.getMessage()), e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        return plagueRecordTos;
    }


}
