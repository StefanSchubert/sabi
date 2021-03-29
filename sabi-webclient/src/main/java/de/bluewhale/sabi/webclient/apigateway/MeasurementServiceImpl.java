/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UnitTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;
import static de.bluewhale.sabi.api.HttpHeader.TOKEN_PREFIX;

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(AUTH_TOKEN, TOKEN_PREFIX + JWTBackendAuthtoken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            try {
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
    public @NotNull List<MeasurementTo> getMeasurmentsTakenByUser(@NotNull String JWTBackendAuthtoken, @NotNull Integer maxResultCount) throws BusinessException {
        throw new UnsupportedOperationException("java.util.List<de.bluewhale.sabi.model.MeasurementTo> getMeasurmentsTakenByUser([JWTBackendAuthtoken, maxResultCount])");
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
        throw new UnsupportedOperationException("void save([measurement, JWTBackendAuthtoken])");
    }
}
