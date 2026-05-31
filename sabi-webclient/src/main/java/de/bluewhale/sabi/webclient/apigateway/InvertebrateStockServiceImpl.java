/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * API-Gateway implementation for invertebrate stock management.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class InvertebrateStockServiceImpl extends APIServiceImpl implements InvertebrateStockService {

    @Override
    public List<InvertebrateStockEntryTo> getInvertebratesForTank(Long aquariumId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + aquariumId + "/list";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            InvertebrateStockEntryTo[] items = objectMapper.readValue(response.getBody(), InvertebrateStockEntryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse invertebrate list from {}", uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public InvertebrateStockEntryTo getInvertebrateById(Long invertebrateId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + invertebrateId;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        if (response.getBody() == null || response.getBody().isBlank()) return null;
        try {
            return objectMapper.readValue(response.getBody(), InvertebrateStockEntryTo.class);
        } catch (JacksonException e) {
            log.error("Failed to parse invertebrate entry {} from {}", invertebrateId, uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<InvertebrateStockEntryTo> addInvertebrate(InvertebrateStockEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            if (response.getBody() != null) {
                try {
                    tools.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
                    tools.jackson.databind.JsonNode idNode = root.path("value").path("id");
                    if (!idNode.isMissingNode() && !idNode.isNull()) {
                        entry.setId(idNode.asLong());
                    }
                } catch (Exception parseEx) {
                    log.warn("Could not extract id from addInvertebrate response: {}", parseEx.getMessage());
                }
            }
            return new ResultTo<>(entry, null);
        } catch (RestClientException e) {
            log.error("Failed to add invertebrate", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<InvertebrateStockEntryTo> updateInvertebrate(InvertebrateStockEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + entry.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to update invertebrate {}", entry.getId(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<InvertebrateStockEntryTo> recordDeparture(Long invertebrateId, InvertebrateDepartureRecordTo record, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + invertebrateId + "/departure";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(record);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(null, null);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to record departure for invertebrate {}", invertebrateId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteInvertebrate(Long invertebrateId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + invertebrateId;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, Void.class);
        } catch (RestClientException e) {
            log.error("Failed to delete invertebrate {}", invertebrateId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<InvertebrateStockEntryTo> removeCatalogueLink(Long invertebrateId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + invertebrateId + "/catalogue-link";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(null, null);
        } catch (RestClientException e) {
            log.error("Failed to remove catalogue link for invertebrate {}", invertebrateId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void uploadPhoto(Long invertebrateId, byte[] bytes, String contentType, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + invertebrateId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            org.springframework.util.LinkedMultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("file", new org.springframework.core.io.ByteArrayResource(bytes) {
                @Override
                public String getFilename() { return "photo"; }
            });
            HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
            restTemplate.exchange(uri, HttpMethod.POST, requestEntity, Void.class);
        } catch (RestClientException e) {
            log.error("Failed to upload photo for invertebrate {}", invertebrateId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public byte[] getPhoto(Long invertebrateId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.INVERTEBRATE_STOCK.getPath() + "/" + invertebrateId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, byte[].class);
            return response.getBody() != null ? response.getBody() : new byte[0];
        } catch (Exception e) {
            log.warn("Could not load photo for invertebrate {}: {}", invertebrateId, e.getMessage());
            return new byte[0];
        }
    }
}
