/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.bluewhale.sabi.exception.*;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.webclient.rest.exceptions.TankMessageCodes;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;

/**
 * Calls Sabi Backend to manage users aquariums.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class TankServiceImpl extends APIServiceImpl implements TankService {

    @Override
    public @NotNull List<AquariumTo> getUsersTanks(@NotNull String pJWTBackendAuthtoken) throws BusinessException {

        String listTankUri = sabiBackendUrl + "/api/tank/list";
        List<AquariumTo> tankList;
        ResponseEntity<String> responseEntity = getAPIResponseFor(listTankUri, pJWTBackendAuthtoken, HttpMethod.GET);

        try {
            AquariumTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), AquariumTo[].class);
            tankList = Arrays.asList(myObjects);
        } catch (JsonProcessingException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s",listTankUri,e.getMessage()),e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
        return tankList;
    }


    @Override
    public void deleteTankById(@NotNull Long tankId, @NotNull String pJWTBackendAuthtoken) throws BusinessException {
        String tankUri = sabiBackendUrl + "/api/tank/"+tankId;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            responseEntity = restTemplate.exchange(tankUri, HttpMethod.DELETE, requestEntity, String.class);
            renewBackendToken(responseEntity);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",tankUri),e.getLocalizedMessage());
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()){
            if (responseEntity.getStatusCodeValue()==409) {
                log.warn("Tried to delete non existing tank {}",tankId);
                throw new BusinessException(Message.error(TankMessageCodes.NO_SUCH_TANK));
            }
            if (responseEntity.getStatusCodeValue()==401) {
                log.warn("Invalid Token when trying to delete tank {}",tankId);
                throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
            }
        }
    }

    @Override
    public String reCreateTemperatureAPIKey(Long tankID, String pJWTBackendAuthtoken) throws BusinessException {
        String requestTempAPIKeyURI = sabiBackendUrl + "/api/tank/"+tankID+"/tempApiKey";

        ResponseEntity<String> responseEntity = getAPIResponseFor(requestTempAPIKeyURI,pJWTBackendAuthtoken,HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatusCode.valueOf(200)) {
            log.error(String.format("Couldn't process %s",requestTempAPIKeyURI),responseEntity.getStatusCode());
            throw new BusinessException(TankExceptionCodes.TANK_NOT_FOUND_OR_DOES_NOT_BELONG_TO_USER);
        }

        AquariumTo myTankWithAPIKey;
        try {
            myTankWithAPIKey = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
        } catch (JsonProcessingException e) {
            log.error("Couldn't parse servers response",e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        return myTankWithAPIKey.getTemperatueApiKey();
    }

    @Override
    public void save(AquariumTo tank, String pJWTBackendAuthtoken) throws BusinessException {

        String updateTankURI = sabiBackendUrl + "/api/tank/"; // PUT here
        String createTankURI = sabiBackendUrl + "/api/tank/create"; // POST here
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(tank);
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert tank object to json: {}",tank);
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }


        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson,headers);

        if (tank.getId() == null) {
            // Save case
            try {
                responseEntity = restTemplate.postForEntity(createTankURI, requestEntity, String.class);
                renewBackendToken(responseEntity);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",createTankURI),e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }
            if (!responseEntity.getStatusCode().is2xxSuccessful()){
                if (responseEntity.getStatusCode().equals(HttpStatusCode.valueOf(409))) {
                    log.info("Tried to create the same tank twice. Will be just ignored as we favour idempotent behavior. Tank ID: {}",tank.getId());
                }
                if (responseEntity.getStatusCode().equals(HttpStatusCode.valueOf(401))) {
                    log.warn("Invalid Token when trying to create tank: {}",tank.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }

        } else {
            // update case
            try {
                responseEntity = restTemplate.exchange(updateTankURI, HttpMethod.PUT, requestEntity, String.class);
                renewBackendToken(responseEntity);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",updateTankURI),e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }

            if (!responseEntity.getStatusCode().is2xxSuccessful()){
                if (responseEntity.getStatusCodeValue()==409) {
                    log.warn("Tried to update non existing tank or internal error. Tank ID: {}",tank.getId());
                    throw new BusinessException(Message.error(TankMessageCodes.NO_SUCH_TANK));
                }
                if (responseEntity.getStatusCodeValue()==401) {
                    log.warn("Invalid Token when trying to update tank: {}",tank.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }

        }
    }

}
