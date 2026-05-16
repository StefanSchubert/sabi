/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.CoralCatalogueEntryTo;
import de.bluewhale.sabi.model.CoralCatalogueSearchResultTo;
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
 * API-Gateway implementation for coral catalogue operations.
 * Part of 005-coral-stock.
 */
@Named
@RequestScope
@Slf4j
public class CoralCatalogueServiceImpl extends APIServiceImpl implements CoralCatalogueService {

    @Override
    public List<CoralCatalogueSearchResultTo> search(String query, String lang, String token) throws BusinessException {
        if (query == null || query.length() < 2) return Collections.emptyList();
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE.getPath() + "/search?q=" + query + "&lang=" + lang;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            CoralCatalogueSearchResultTo[] items =
                    objectMapper.readValue(response.getBody(), CoralCatalogueSearchResultTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse coral catalogue search results", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public List<CoralCatalogueSearchResultTo> listAll(String lang, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE.getPath() + "?lang=" + lang;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            CoralCatalogueSearchResultTo[] items =
                    objectMapper.readValue(response.getBody(), CoralCatalogueSearchResultTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse coral catalogue listAll results", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public CoralCatalogueEntryTo getById(Long id, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE.getPath() + "/" + id;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        if (response.getBody() == null || response.getBody().isBlank()) return null;
        try {
            return objectMapper.readValue(response.getBody(), CoralCatalogueEntryTo.class);
        } catch (JacksonException e) {
            log.error("Failed to parse coral catalogue entry {}", id, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo proposeEntry(CoralCatalogueEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE.getPath() + "/";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to propose coral catalogue entry: {}", e.getMessage(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo updateEntry(CoralCatalogueEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_CATALOGUE.getPath() + "/" + entry.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to update coral catalogue entry {}", entry.getId(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }
}

