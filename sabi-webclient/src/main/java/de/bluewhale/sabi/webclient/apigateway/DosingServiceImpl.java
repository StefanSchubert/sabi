/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;

/**
 * REST gateway for standalone aquarium dosing records.
 */
@Named
@RequestScope
@Slf4j
public class DosingServiceImpl extends APIServiceImpl implements DosingService {

    @Override
    public List<DosingTo> listDosingsForTank(Long aquariumId, String token) throws BusinessException {
        String uri = dosingUri(aquariumId);
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            return Arrays.asList(objectMapper.readValue(response.getBody(), DosingTo[].class));
        } catch (JacksonException e) {
            log.error("Failed to parse dosing list for aquarium_id={}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public DosingTo createDosing(Long aquariumId, DosingTo dosing, String token) throws BusinessException {
        return exchange(dosingUri(aquariumId), HttpMethod.POST, aquariumId, dosing, token);
    }

    @Override
    public DosingTo updateDosing(Long aquariumId, Long dosingId, DosingTo dosing, String token) throws BusinessException {
        return exchange(dosingUri(aquariumId) + "/" + dosingId, HttpMethod.PUT, aquariumId, dosing, token);
    }

    @Override
    public void deleteDosing(Long aquariumId, Long dosingId, String token) throws BusinessException {
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            ResponseEntity<String> response = new RestTemplate().exchange(
                    dosingUri(aquariumId) + "/" + dosingId, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            renewBackendToken(response);
        } catch (RestClientException e) {
            log.error("Failed to delete dosing_id={} for aquarium_id={}", dosingId, aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    private DosingTo exchange(String uri, HttpMethod method, Long aquariumId, DosingTo dosing, String token)
            throws BusinessException {
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            String body = objectMapper.writeValueAsString(dosing);
            ResponseEntity<String> response = new RestTemplate().exchange(
                    uri, method, new HttpEntity<>(body, headers), String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), DosingTo.class);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to save dosing for aquarium_id={}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    private String dosingUri(Long aquariumId) {
        return sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/dosings";
    }
}
