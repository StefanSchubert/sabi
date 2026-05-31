/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.CoralCatalogueEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * API-Gateway implementation for admin coral catalogue operations.
 * Part of 005-coral-stock.
 */
@Named
@RequestScope
@Slf4j
public class CoralCatalogueAdminServiceImpl extends APIServiceImpl implements CoralCatalogueAdminService {

    @Override
    public List<CoralCatalogueEntryTo> listPending(String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE_ADMIN.getPath() + "/pending";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            CoralCatalogueEntryTo[] items = objectMapper.readValue(response.getBody(), CoralCatalogueEntryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse pending coral catalogue entries", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<CoralCatalogueEntryTo> listAll(String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE_ADMIN.getPath();
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            CoralCatalogueEntryTo[] items = objectMapper.readValue(response.getBody(), CoralCatalogueEntryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse all coral catalogue entries", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ResultTo approve(Long id, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE_ADMIN.getPath() + "/" + id + "/approve";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(id, null);
        } catch (Exception e) {
            log.error("Failed to approve coral catalogue entry {}", id, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo reject(Long id, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE_ADMIN.getPath() + "/" + id + "/reject";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(id, null);
        } catch (Exception e) {
            log.error("Failed to reject coral catalogue entry {}", id, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo adminUpdate(Long id, CoralCatalogueEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE_ADMIN.getPath() + "/" + id;
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to admin-update coral catalogue entry {}", id, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }
}

