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
import de.bluewhale.sabi.exception.CommonMessageCodes;
import de.bluewhale.sabi.model.MeasurementReminderTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ParameterTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.webclient.rest.exceptions.MeasurementMessageCodes;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.*;

/**
 * Calls Sabi Backend to manage users measurements.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class MeasurementServiceImpl extends APIServiceImpl implements MeasurementService {

    static List<UnitTo> cachedAvailableMeasurementUnits = Collections.emptyList();
    // TODO STS (04.05.21): Revisit and check after i18n of ParameterTo...
    static Map<Integer, ParameterTo> cachedUnitParameterMap = new HashMap<>();

    @Override
    public List<MeasurementReminderTo> getMeasurementRemindersForUser(String pJWTBackendAuthtoken) throws BusinessException {

        String listMeasurementReminderUri = sabiBackendUrl + Endpoint.MEASUREMENTS + "/reminder/list";
        ResponseEntity<String> responseEntity = getAPIResponseFor(listMeasurementReminderUri, pJWTBackendAuthtoken, HttpMethod.GET);
        MeasurementReminderTo[] measurementReminderTos;
        try {
            measurementReminderTos = objectMapper.readValue(responseEntity.getBody(), MeasurementReminderTo[].class);
        } catch (JsonProcessingException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s", listMeasurementReminderUri, e.getMessage()), e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
        return Arrays.asList(measurementReminderTos);
    }

    @Override
    public void addMeasurementReminder(MeasurementReminderTo measurementReminderTo, String pJWTBackendAuthtoken) throws BusinessException {
        String addMeasurementReminderUri = sabiBackendUrl + Endpoint.MEASUREMENTS + "/reminder/";

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(measurementReminderTo);
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert measurement object to json: {}", measurementReminderTo);
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);

        // initial creation (POST)
        try {
            responseEntity = restTemplate.postForEntity(addMeasurementReminderUri, requestEntity, String.class);
            renewBackendToken(responseEntity);
        } catch (RestClientException e) {
            log.error("Couldn't reach backend {} to add a measurementReminder reason was {}", addMeasurementReminderUri, e.getLocalizedMessage());
            throw BusinessException.with(CommonMessageCodes.NETWORK_PROBLEM);
        } catch (Exception e) {
            log.error("Couln'd save measurment reminder. Reason: {}", e);
            throw BusinessException.with(CommonMessageCodes.BACKEND_API_PROBLEM);
        }
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            if (responseEntity.getStatusCodeValue() == 409) {
                log.info("Tried to create the same measurement twice. Will be just ignored as we favour idempotent behavior. Measurement Unit was: {}", measurementReminderTo.getUnitName());
            }
            if (responseEntity.getStatusCodeValue() == 401) {
                log.warn("Invalid Token when trying to add measurement reminder for userID: {}", measurementReminderTo.getUserId());
                throw BusinessException.with(AuthMessageCodes.TOKEN_EXPIRED);
            }
        }
    }


        @Override
        public @NotNull List<UnitTo> getAvailableMeasurementUnits (@NotNull String pJWTBackendAuthtoken) throws
        BusinessException {

            String listMeasurementUnitsUri = sabiBackendUrl + Endpoint.UNITS + "/list";

            if (cachedAvailableMeasurementUnits.isEmpty()) {

                ResponseEntity<String> responseEntity = getAPIResponseFor(listMeasurementUnitsUri, pJWTBackendAuthtoken, HttpMethod.GET);

                try {
                    UnitTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), UnitTo[].class);
                    cachedAvailableMeasurementUnits = Arrays.asList(myObjects);
                } catch (JsonProcessingException e) {
                    log.error(String.format("Didn't understand response from %s got parsing exception %s", listMeasurementUnitsUri, e.getMessage()), e.getMessage());
                    e.printStackTrace();
                    throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
                }
            }

            return cachedAvailableMeasurementUnits;
        }

        @Override
        public ParameterTo getParameterFor (Integer selectedUnitId, String pJWTBackendAuthtoken) throws
        BusinessException {

            if (selectedUnitId != null && cachedUnitParameterMap.containsKey(selectedUnitId)) {
                log.debug("Access cached parameterTo");
                return cachedUnitParameterMap.get(selectedUnitId);
            } else {

                String listUnitsParameterUri = sabiBackendUrl + Endpoint.UNITS + "/parameter/" + selectedUnitId;

                ResponseEntity<String> responseEntity = getAPIResponseFor(listUnitsParameterUri, pJWTBackendAuthtoken, HttpMethod.GET);

                try {
                    ParameterTo parameterTo = objectMapper.readValue(responseEntity.getBody(), ParameterTo.class);
                    // cache result for next request
                    if (parameterTo != null) cachedUnitParameterMap.put(selectedUnitId, parameterTo);
                    log.debug("Retrieved ParameterTo {}", parameterTo);
                    return parameterTo;
                } catch (JsonProcessingException e) {
                    log.error(String.format("Didn't understand response from %s got parsing exception %s", listUnitsParameterUri, e.getMessage()), e.getMessage());
                    e.printStackTrace();
                    throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
                }
            }
        }

        @Override
        public @NotNull List<MeasurementTo> getMeasurementsTakenByUser (@NotNull String
        pJWTBackendAuthtoken, @NotNull Integer maxResultCount) throws BusinessException {

            List<MeasurementTo> usersMeasurements;

            String listOfUsersMeasurements = sabiBackendUrl + "/api/measurement/list/" + maxResultCount;

            ResponseEntity<String> responseEntity = getAPIResponseFor(listOfUsersMeasurements, pJWTBackendAuthtoken, HttpMethod.GET);

            try {
                MeasurementTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), MeasurementTo[].class);
                usersMeasurements = Arrays.asList(myObjects);
            } catch (JsonProcessingException e) {
                log.error(String.format("Didn't understand response from %s got parsing exception %s", listOfUsersMeasurements, e.getMessage()), e.getMessage());
                e.printStackTrace();
                throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
            }

            return usersMeasurements;
        }

        @Override
        public @NotNull List<MeasurementTo> getMeasurementsForUsersTank (@NotNull String
        pJWTBackendAuthtoken, @NotNull Long tankId) throws BusinessException {
            throw new UnsupportedOperationException("java.util.List<de.bluewhale.sabi.model.MeasurementTo> getMeasurmentsForUsersTank([JWTBackendAuthtoken, tankId])");
        }

        @Override
        public List<MeasurementTo> getMeasurementsForUsersTankFilteredByUnit (String pJWTBackendAuthtoken, Long
        tankId, Integer unitId) throws BusinessException {

            if (tankId == null || unitId == null) return Collections.emptyList();

            List<MeasurementTo> usersMeasurements;
            String apiURL = String.format(sabiBackendUrl + "/api/measurement/tank/%s/unit/%s", tankId, unitId);
            ResponseEntity<String> responseEntity = getAPIResponseFor(apiURL, pJWTBackendAuthtoken, HttpMethod.GET);

            try {
                MeasurementTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), MeasurementTo[].class);
                usersMeasurements = Arrays.asList(myObjects);
            } catch (JsonProcessingException e) {
                log.error("Didn't understand response from {} got parsing exception {}", apiURL, e.getLocalizedMessage());
                e.printStackTrace();
                throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
            }
            return usersMeasurements;
        }

        @Override
        public void deleteMeasurementById (@NotNull Long measurementId, @NotNull String pJWTBackendAuthtoken) throws
        BusinessException {
            throw new UnsupportedOperationException("void deleteMeasurementById([measurementId, JWTBackendAuthtoken])");
        }

        @Override
        public void save (MeasurementTo measurement, String pJWTBackendAuthtoken) throws BusinessException {

            String saveMeasurmentURI = sabiBackendUrl + "/api/measurement/"; // PUT for update POST for create
            String requestJson;
            try {
                requestJson = objectMapper.writeValueAsString(measurement);
            } catch (JsonProcessingException e) {
                log.error("Couldn't convert measurement object to json: {}", measurement);
                e.printStackTrace();
                throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
            }

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity;
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthtoken);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);

            if (measurement.getId() == null) {
                // initial creation (POST)
                try {
                    responseEntity = restTemplate.postForEntity(saveMeasurmentURI, requestEntity, String.class);
                    renewBackendToken(responseEntity);
                } catch (RestClientException e) {
                    log.error("Couldn't reach backend {} to add a measurement reason was {}", saveMeasurmentURI, e.getLocalizedMessage());
                    throw BusinessException.with(CommonMessageCodes.NETWORK_PROBLEM);
                } catch (Exception e) {
                    log.error("Couln'd save measurment. Reason: {}", e);
                    e.printStackTrace();
                    throw BusinessException.with(CommonMessageCodes.BACKEND_API_PROBLEM);
                }
                if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                    if (responseEntity.getStatusCodeValue() == 409) {
                        log.info("Tried to create the same measurement twice. Will be just ignored as we favour idempotent behavior. MeasurementID: {}", measurement.getId());
                    }
                    if (responseEntity.getStatusCodeValue() == 401) {
                        log.warn("Invalid Token when trying to create measurement: {}", measurement.getId());
                        throw BusinessException.with(AuthMessageCodes.TOKEN_EXPIRED);
                    }
                }
            } else {
                // update case (PUT)
                try {
                    responseEntity = restTemplate.exchange(saveMeasurmentURI, HttpMethod.PUT, requestEntity, String.class);
                    renewBackendToken(responseEntity);
                } catch (RestClientException e) {
                    log.error("Couldn't backend {} to update a measurement reason was {}", saveMeasurmentURI, e.getLocalizedMessage());
                    throw BusinessException.with(CommonMessageCodes.NETWORK_PROBLEM);
                }
                if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                    if (responseEntity.getStatusCodeValue() == 409) {
                        log.warn("Tried to update non existing measurement or internal error. Measurement ID: {}", measurement.getId());
                        throw BusinessException.with(MeasurementMessageCodes.NO_SUCH_MEAUREMENT);
                    }
                    if (responseEntity.getStatusCodeValue() == 401) {
                        log.warn("Invalid Token when trying to update measurement: {}", measurement.getId());
                        throw BusinessException.with(AuthMessageCodes.TOKEN_EXPIRED);
                    }
                }
            }
        }
    }
