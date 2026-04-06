/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
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
 * API-Gateway implementation for fish catalogue operations.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class FishCatalogueServiceImpl extends APIServiceImpl implements FishCatalogueService {

    @Override
    public List<FishCatalogueSearchResultTo> search(String query, String lang, String token) throws BusinessException {
        if (query == null || query.length() < 2) return Collections.emptyList();
        String uri = sabiBackendUrl + Endpoint.FISH_CATALOGUE.getPath() + "/search?q="
                + query + "&lang=" + lang;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            FishCatalogueSearchResultTo[] items =
                    objectMapper.readValue(response.getBody(), FishCatalogueSearchResultTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse catalogue search results", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo propose(FishCatalogueEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_CATALOGUE.getPath() + "/";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), ResultTo.class);
        } catch (Exception e) {
            log.error("Failed to propose catalogue entry", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo updateEntry(FishCatalogueEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_CATALOGUE.getPath() + "/" + entry.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), ResultTo.class);
        } catch (Exception e) {
            log.error("Failed to update catalogue entry {}", entry.getId(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public boolean isDuplicate(String scientificName, String token) throws BusinessException {
        try {
            List<FishCatalogueSearchResultTo> results = search(scientificName, "en", token);
            return results.stream()
                    .anyMatch(r -> scientificName.equalsIgnoreCase(r.getScientificName()));
        } catch (Exception e) {
            log.warn("isDuplicate check failed for {}: {}", scientificName, e.getMessage());
            return false;
        }
    }
}

