/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;

/**
 * API gateway implementation for aquarium logbook events.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class AquariumEventServiceImpl extends APIServiceImpl implements AquariumEventService {

    @Override
    public List<AquariumEventTo> listEventsForTank(Long aquariumId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, org.springframework.http.HttpMethod.GET);
        try {
            AquariumEventTo[] items = objectMapper.readValue(response.getBody(), AquariumEventTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse event list from {}", uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public AquariumEventTo createEvent(Long aquariumId, AquariumEventTo event, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(event);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), AquariumEventTo.class);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to create event for aquarium_id={}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    @Override
    public AquariumEventTo updateEvent(Long aquariumId, Long eventId, AquariumEventTo event, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events/" + eventId;
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(event);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, request, String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), AquariumEventTo.class);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to update event_id={} for aquarium_id={}", eventId, aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    @Override
    public void deleteEvent(Long aquariumId, Long eventId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/events/" + eventId;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
        HttpEntity<String> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, request, String.class);
            renewBackendToken(response);
        } catch (RestClientException e) {
            log.error("Failed to delete event_id={} for aquarium_id={}", eventId, aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }
}

