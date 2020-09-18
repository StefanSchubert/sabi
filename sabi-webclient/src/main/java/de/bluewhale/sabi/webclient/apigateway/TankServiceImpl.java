/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.webclient.rest.exceptions.TankMessageCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;
import static de.bluewhale.sabi.api.HttpHeader.TOKEN_PREFIX;

/**
 * Calls Sabi Backend to retrieve the list of users aquariums.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class TankServiceImpl implements TankService {

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper


    @Override
    public @NotNull List<AquariumTo> getUsersTanks(@NotNull String JWTBackendAuthtoken) throws BusinessException {

        String listTankUri = sabiBackendUrl + "/api/tank/list";

        RestTemplate restTemplate = new RestTemplate();
        List<AquariumTo> tankList = new ArrayList<>();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(AUTH_TOKEN, TOKEN_PREFIX + JWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            responseEntity = restTemplate.exchange(listTankUri, HttpMethod.GET, requestEntity, String.class);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",listTankUri),e.getLocalizedMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

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
    public void deleteTankById(@NotNull Long tankId, @NotNull String JWTBackendAuthtoken) throws BusinessException {
        String tankUri = sabiBackendUrl + "/api/tank/"+tankId;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(AUTH_TOKEN, TOKEN_PREFIX + JWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            responseEntity = restTemplate.exchange(tankUri, HttpMethod.DELETE, requestEntity, String.class);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",tankUri),e.getLocalizedMessage());
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()){
            if (responseEntity.getStatusCodeValue()==409) {
                log.warn("Tried to delete non existing tank",tankId);
                throw new BusinessException(Message.error(TankMessageCodes.NO_SUCH_TANK));
            }
            if (responseEntity.getStatusCodeValue()==401) {
                log.warn("Invalid Token when trying to delete tank",tankId);
                throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
            }
        }
    }

    @Override
    public void save(AquariumTo tank, String JWTBackendAuthtoken) throws BusinessException {

        String updateTankURI = sabiBackendUrl + "/api/tank/"; // PUT here
        String createTankURI = sabiBackendUrl + "/api/tank/create"; // POST here
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(tank);
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert tank object to jason: ",tank);
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }


        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(AUTH_TOKEN, TOKEN_PREFIX + JWTBackendAuthtoken);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson,headers);

        if (tank.getId() == null) {
            // Save case
            try {
                responseEntity = restTemplate.postForEntity(createTankURI, requestEntity, String.class);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",createTankURI),e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }
            if (!responseEntity.getStatusCode().is2xxSuccessful()){
                if (responseEntity.getStatusCodeValue()==409) {
                    log.info("Tried to create the same tank twice. Will be just ignored as we favour idempotent behavior. Tank ID: ",tank.getId());
                }
                if (responseEntity.getStatusCodeValue()==401) {
                    log.warn("Invalid Token when trying to update tank: ",tank.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }

        } else {
            // update case
            try {
                responseEntity = restTemplate.exchange(updateTankURI, HttpMethod.PUT, requestEntity, String.class);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",updateTankURI),e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }

            if (!responseEntity.getStatusCode().is2xxSuccessful()){
                if (responseEntity.getStatusCodeValue()==409) {
                    log.warn("Tried to update non existing tank or internal error. Tank ID: ",tank.getId());
                    throw new BusinessException(Message.error(TankMessageCodes.NO_SUCH_TANK));
                }
                if (responseEntity.getStatusCodeValue()==401) {
                    log.warn("Invalid Token when trying to update tank: ",tank.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }

        }
    }
}
