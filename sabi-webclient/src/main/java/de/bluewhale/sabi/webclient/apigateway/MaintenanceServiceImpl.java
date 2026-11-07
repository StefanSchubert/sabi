/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.MaintenanceEventTo;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
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

/**
 * REST gateway for the per-aquarium maintenance data. The URI is built on top of the existing
 * TANKS endpoint (dosing precedent), no new Endpoint enum entry.
 */
@Named
@RequestScope
@Slf4j
public class MaintenanceServiceImpl extends APIServiceImpl implements MaintenanceService {

    @Override
    public MaintenanceTo getMaintenanceForTank(Long aquariumId, String token) throws BusinessException {
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            ResponseEntity<String> response = new RestTemplate().exchange(
                    maintenanceUri(aquariumId), HttpMethod.GET, new HttpEntity<>(headers), String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), MaintenanceTo.class);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to load maintenance for aquarium_id={}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    @Override
    public MaintenanceProfileTo updateWaterChangeInterval(Long aquariumId, WaterChangeIntervalTo interval, String token)
            throws BusinessException {
        return exchange(maintenanceUri(aquariumId) + "/water-change-interval", HttpMethod.PUT,
                aquariumId, interval, MaintenanceProfileTo.class, token);
    }

    @Override
    public MaintenanceProfileTo updateActivatedCarbon(Long aquariumId, ActivatedCarbonTo carbon, String token)
            throws BusinessException {
        return exchange(maintenanceUri(aquariumId) + "/activated-carbon", HttpMethod.PUT,
                aquariumId, carbon, MaintenanceProfileTo.class, token);
    }

    @Override
    public MaintenanceEventTo createMaintenanceEvent(Long aquariumId, MaintenanceEventTo event, String token)
            throws BusinessException {
        return exchange(maintenanceUri(aquariumId) + "/events", HttpMethod.POST,
                aquariumId, event, MaintenanceEventTo.class, token);
    }

    @Override
    public MaintenanceEventTo updateMaintenanceEvent(Long aquariumId, Long eventId, MaintenanceEventTo event, String token)
            throws BusinessException {
        return exchange(maintenanceUri(aquariumId) + "/events/" + eventId, HttpMethod.PUT,
                aquariumId, event, MaintenanceEventTo.class, token);
    }

    @Override
    public void deleteMaintenanceEvent(Long aquariumId, Long eventId, String token) throws BusinessException {
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            ResponseEntity<String> response = new RestTemplate().exchange(
                    maintenanceUri(aquariumId) + "/events/" + eventId, HttpMethod.DELETE,
                    new HttpEntity<>(headers), String.class);
            renewBackendToken(response);
        } catch (RestClientException e) {
            log.error("Failed to delete maintenance event_id={} for aquarium_id={}", eventId, aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    private <T> T exchange(String uri, HttpMethod method, Long aquariumId, Object body,
                            Class<T> responseType, String token) throws BusinessException {
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = new RestTemplate().exchange(
                    uri, method, new HttpEntity<>(json, headers), String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), responseType);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to update maintenance for aquarium_id={}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    private String maintenanceUri(Long aquariumId) {
        return sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/maintenance";
    }
}
