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
public class PlagueServiceImpl extends APIServiceImpl implements PlagueService  {

    
    @Override
    public List<PlagueTo> getPlagueCatalogue(String pJWTBackendAuthtoken) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTank(String pJWTBackendAuthtoken, Long tankId) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTankFilteredByPlague(String pJWTBackendAuthtoken, Long tankId, Integer plagueId) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public void deletePlagueRecordById(Long plagueRecordId, String pJWTBackendAuthtoken) throws BusinessException {

    }

    @Override
    public void save(PlagueRecordTo plagueRecord, String pJWTBackendAuthtoken) throws BusinessException {

    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUserTanks(String pJWTBackendAuthtoken) throws BusinessException {

        List<PlagueRecordTo> plagueRecordTos;
        String listPlagueUri = sabiBackendUrl + Endpoint.PLAGUE_CENTER_SERVICE+ "/record/list";

        ResponseEntity<String> responseEntity = getAPIResponseFor(listPlagueUri,pJWTBackendAuthtoken,HttpMethod.GET);
        
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
    

}
