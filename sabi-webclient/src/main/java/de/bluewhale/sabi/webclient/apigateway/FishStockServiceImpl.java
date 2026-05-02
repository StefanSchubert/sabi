/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishRoleTo;
import de.bluewhale.sabi.model.FishSizeHistoryTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
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
 * API-Gateway implementation for fish stock management.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class FishStockServiceImpl extends APIServiceImpl implements FishStockService {

    @Override
    public List<FishStockEntryTo> getFishForTank(Long aquariumId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + aquariumId + "/list";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            FishStockEntryTo[] items = objectMapper.readValue(response.getBody(), FishStockEntryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse fish list from {}", uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public FishStockEntryTo getFishById(Long fishId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        if (response.getBody() == null || response.getBody().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(response.getBody(), FishStockEntryTo.class);
        } catch (JacksonException e) {
            log.error("Failed to parse fish entry {} from {}", fishId, uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<FishStockEntryTo> addFish(FishStockEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            // ResultTo JSON structure: {"value": {...}, "message": {...}}
            // Key is "value" (matches ResultTo.getValue()), NOT "resultObject"
            if (response.getBody() != null) {
                try {
                    tools.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
                    tools.jackson.databind.JsonNode idNode = root.path("value").path("id");
                    if (!idNode.isMissingNode() && !idNode.isNull()) {
                        entry.setId(idNode.asLong());
                    }
                } catch (Exception parseEx) {
                    log.warn("Could not extract id from addFish response: {}", parseEx.getMessage());
                }
            }
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to add fish", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo updateFish(FishStockEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + entry.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to update fish {}", entry.getId(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo recordDeparture(Long fishId, FishDepartureRecordTo record, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId + "/departure";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(record);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(record, null);
        } catch (Exception e) {
            log.error("Failed to record departure for fish {}", fishId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteFish(Long fishId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, String.class);
            renewBackendToken(response);
            if (response.getStatusCode().value() == 409) {
                log.warn("Cannot delete fish {} - has departure record", fishId);
                throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
            }
        } catch (RestClientException e) {
            log.error("Failed to delete fish {}", fishId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    @Override
    public ResultTo removeCatalogueLink(Long fishId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId + "/catalogue-link";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(null, null);
        } catch (Exception e) {
            log.error("Failed to remove catalogue link for fish {}", fishId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void uploadPhoto(Long fishId, byte[] bytes, String contentType, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token, MediaType.MULTIPART_FORM_DATA);
            // Wrap byte[] in ByteArrayResource so RestTemplate creates a proper multipart file part
            org.springframework.core.io.ByteArrayResource resource =
                    new org.springframework.core.io.ByteArrayResource(bytes) {
                        @Override
                        public String getFilename() {
                            return "photo" + extensionFor(contentType);
                        }
                    };
            org.springframework.util.MultiValueMap<String, Object> parts =
                    new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("file", resource);
            parts.add("contentType", contentType);
            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(parts, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
        } catch (Exception e) {
            log.error("Failed to upload photo for fish {}", fishId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    private String extensionFor(String contentType) {
        if (contentType == null) return ".jpg";
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    @Override
    public List<FishRoleTo> getFishRoles(String language, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_ROLES.getPath() + "?lang=" + language;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            FishRoleTo[] roles = objectMapper.readValue(response.getBody(), FishRoleTo[].class);
            return Arrays.asList(roles);
        } catch (JacksonException e) {
            log.error("Failed to parse fish roles from {}", uri, e);
            return Collections.emptyList();
        }
    }

    @Override
    public byte[] getPhoto(Long fishId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, byte[].class);
            renewBackendToken(ResponseEntity.ok().build()); // type mismatch workaround
            return response.getBody() != null ? response.getBody() : new byte[0];
        } catch (Exception e) {
            log.warn("Could not load photo for fish {}: {}", fishId, e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public List<FishSizeHistoryTo> getSizeHistory(Long fishId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId + "/size";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            FishSizeHistoryTo[] items = objectMapper.readValue(response.getBody(), FishSizeHistoryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse size history for fish {}", fishId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ResultTo<FishSizeHistoryTo> addSizeRecord(Long fishId, FishSizeHistoryTo record, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.FISH_STOCK.getPath() + "/" + fishId + "/size";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(record);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(record, null);
        } catch (Exception e) {
            log.error("Failed to add size record for fish {}", fishId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }
}

