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
import java.util.Map;

/**
 * API-Gateway for fish catalogue admin operations (webclient to backend).
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class FishCatalogueAdminServiceImpl extends APIServiceImpl implements FishCatalogueAdminService {

    @Override
    public List<FishCatalogueEntryTo> getPendingProposals(String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_CATALOGUE_ADMIN.getPath() + "/pending";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            FishCatalogueEntryTo[] items = objectMapper.readValue(response.getBody(), FishCatalogueEntryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse pending proposals", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public FishCatalogueEntryTo approveEntry(Long id, FishCatalogueEntryTo edits, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_CATALOGUE_ADMIN.getPath() + "/" + id + "/approve";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = edits != null ? objectMapper.writeValueAsString(edits) : "{}";
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return edits;
        } catch (Exception e) {
            log.error("Failed to approve entry {}", id, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void rejectEntry(Long id, String reason, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_CATALOGUE_ADMIN.getPath() + "/" + id + "/reject";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(Map.of("reason", reason != null ? reason : ""));
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
        } catch (Exception e) {
            log.error("Failed to reject entry {}", id, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }
}
