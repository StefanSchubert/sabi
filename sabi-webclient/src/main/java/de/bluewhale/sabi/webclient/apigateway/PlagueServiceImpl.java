/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.PlagueTo;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
        // TODO STS (02.10.22): ImplMe or drop it
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
