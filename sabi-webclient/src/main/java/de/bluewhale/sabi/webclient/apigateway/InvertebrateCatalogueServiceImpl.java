/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.InvertebrateCatalogueEntryTo;
import de.bluewhale.sabi.model.InvertebrateCatalogueSearchResultTo;
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
 * API-Gateway implementation for invertebrate catalogue operations.
 * Part of 006-invertebrate-tracking.
 */
@Named
@RequestScope
@Slf4j
public class InvertebrateCatalogueServiceImpl extends APIServiceImpl implements InvertebrateCatalogueService {

    @Override
    public List<InvertebrateCatalogueSearchResultTo> search(String query, String lang, String token) throws BusinessException {
        if (query == null || query.length() < 2) return Collections.emptyList();
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_CATALOGUE.getPath() + "/search?q=" + query + "&lang=" + lang;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            InvertebrateCatalogueSearchResultTo[] items =
                    objectMapper.readValue(response.getBody(), InvertebrateCatalogueSearchResultTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse invertebrate catalogue search results", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public List<InvertebrateCatalogueSearchResultTo> listAll(String lang, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_CATALOGUE.getPath() + "/?lang=" + lang;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            InvertebrateCatalogueSearchResultTo[] items =
                    objectMapper.readValue(response.getBody(), InvertebrateCatalogueSearchResultTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse invertebrate catalogue listAll results", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public InvertebrateCatalogueEntryTo getById(Long id, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_CATALOGUE.getPath() + "/" + id;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        if (response.getBody() == null || response.getBody().isBlank()) return null;
        try {
            return objectMapper.readValue(response.getBody(), InvertebrateCatalogueEntryTo.class);
        } catch (JacksonException e) {
            log.error("Failed to parse invertebrate catalogue entry {}", id, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo proposeEntry(InvertebrateCatalogueEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_CATALOGUE.getPath() + "/";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to propose invertebrate catalogue entry: {}", e.getMessage(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo updateEntry(InvertebrateCatalogueEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_CATALOGUE.getPath() + "/" + entry.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to update invertebrate catalogue entry {}", entry.getId(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }
}
