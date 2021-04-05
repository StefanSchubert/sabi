/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.webclient.rest.exceptions.MeasurementMessageCodes;
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
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Calls Sabi Backend to manage users measurements.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class MeasurementServiceImpl implements MeasurementService {


    static List<UnitTo> cachedAvailableMeasurementUnits = Collections.emptyList();

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    @Override
    public @NotNull List<UnitTo> getAvailableMeasurementUnits(@NotNull String JWTBackendAuthtoken) throws BusinessException {

        String listMeasurementUnitsUri = sabiBackendUrl + "/api/measurement/units/list";

        if (cachedAvailableMeasurementUnits.isEmpty()) {

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity;

            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(JWTBackendAuthtoken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            try {
                // Notice the that the controller defines a list, the resttemplate will get it as array.
                responseEntity = restTemplate.exchange(listMeasurementUnitsUri, HttpMethod.GET, requestEntity, String.class);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",listMeasurementUnitsUri),e.getLocalizedMessage());
                e.printStackTrace();
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }

            try {
                UnitTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), UnitTo[].class);
                cachedAvailableMeasurementUnits = Arrays.asList(myObjects);
            } catch (JsonProcessingException e) {
                log.error(String.format("Didn't understand response from %s got parsing exception %s",listMeasurementUnitsUri,e.getMessage()),e.getMessage());
                e.printStackTrace();
                throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
            }
        }

        return cachedAvailableMeasurementUnits;
    }


    @Override
    public @NotNull List<MeasurementTo> getMeasurementsTakenByUser(@NotNull String JWTBackendAuthtoken, @NotNull Integer maxResultCount) throws BusinessException {

        List<MeasurementTo> usersMeasurements;

        String listOfUsersMeasurements = sabiBackendUrl + "/api/measurement/list/" + maxResultCount;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(JWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            // Notice the that the controller defines a list, the resttemplate will get it as array.
            responseEntity = restTemplate.exchange(listOfUsersMeasurements, HttpMethod.GET, requestEntity, String.class);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",listOfUsersMeasurements),e.getLocalizedMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

        try {
            MeasurementTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), MeasurementTo[].class);
            usersMeasurements = Arrays.asList(myObjects);
        } catch (JsonProcessingException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s",listOfUsersMeasurements,e.getMessage()),e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        return usersMeasurements;
    }

    @Override
    public @NotNull List<MeasurementTo> getMeasurmentsForUsersTank(@NotNull String JWTAuthtoken, @NotNull Long tankId) throws BusinessException {
        throw new UnsupportedOperationException("java.util.List<de.bluewhale.sabi.model.MeasurementTo> getMeasurmentsForUsersTank([JWTAuthtoken, tankId])");
    }

    @Override
    public void deleteMeasurementById(@NotNull Long measurementId, @NotNull String JWTBackendAuthtoken) throws BusinessException {
        throw new UnsupportedOperationException("void deleteMeasurementById([measurementId, JWTBackendAuthtoken])");
    }

    @Override
    public void save(MeasurementTo measurement, String JWTBackendAuthtoken) throws BusinessException {

        String saveMeasurmentURI = sabiBackendUrl + "/api/measurement/"; // PUT for update POST for create
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(measurement);
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert measurement object to json: {}",measurement);
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(JWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson,headers);

        if (measurement.getId() == null) {
            // initial creation (POST)
            try {
                responseEntity = restTemplate.postForEntity(saveMeasurmentURI, requestEntity, String.class);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",saveMeasurmentURI),e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }
            if (!responseEntity.getStatusCode().is2xxSuccessful()){
                if (responseEntity.getStatusCodeValue()==409) {
                    log.info("Tried to create the same measurement twice. Will be just ignored as we favour idempotent behavior. MeasurementID: {}",measurement.getId());
                }
                if (responseEntity.getStatusCodeValue()==401) {
                    log.warn("Invalid Token when trying to create measurement: {}",measurement.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }
        } else {
            // update case (PUT)
            try {
                responseEntity = restTemplate.exchange(saveMeasurmentURI, HttpMethod.PUT, requestEntity, String.class);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s", saveMeasurmentURI), e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                if (responseEntity.getStatusCodeValue() == 409) {
                    log.warn("Tried to update non existing measurement or internal error. Measurement ID: {}", measurement.getId());
                    throw new BusinessException(Message.error(MeasurementMessageCodes.NO_SUCH_MEAUREMENT));
                }
                if (responseEntity.getStatusCodeValue() == 401) {
                    log.warn("Invalid Token when trying to update measurement: {}", measurement.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }
        }
    }
}