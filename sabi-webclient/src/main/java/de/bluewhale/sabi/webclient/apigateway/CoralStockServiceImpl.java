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
 * API-Gateway implementation for coral stock management.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class CoralStockServiceImpl extends APIServiceImpl implements CoralStockService {

    @Override
    public List<CoralStockEntryTo> getCoralsForTank(Long aquariumId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + aquariumId + "/list";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            CoralStockEntryTo[] items = objectMapper.readValue(response.getBody(), CoralStockEntryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse coral list from {}", uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public CoralStockEntryTo getCoralById(Long coralId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId;
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        if (response.getBody() == null || response.getBody().isBlank()) return null;
        try {
            return objectMapper.readValue(response.getBody(), CoralStockEntryTo.class);
        } catch (JacksonException e) {
            log.error("Failed to parse coral entry {} from {}", coralId, uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<CoralStockEntryTo> addCoral(CoralStockEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/";
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
                    log.warn("Could not extract id from addCoral response: {}", parseEx.getMessage());
                }
            }
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to add coral", e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<CoralStockEntryTo> updateCoral(CoralStockEntryTo entry, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + entry.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(entry);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(entry, null);
        } catch (Exception e) {
            log.error("Failed to update coral {}", entry.getId(), e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<CoralStockEntryTo> recordDeparture(Long coralId, CoralDepartureRecordTo record, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/departure";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(record);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(null, null);
        } catch (Exception e) {
            log.error("Failed to record departure for coral {}", coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteCoral(Long coralId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, String.class);
            renewBackendToken(response);
            if (response.getStatusCode().value() == 409) {
                log.warn("Cannot delete coral {} - has departure record", coralId);
                throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
            }
        } catch (RestClientException e) {
            log.error("Failed to delete coral {}", coralId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    @Override
    public ResultTo<CoralStockEntryTo> removeCatalogueLink(Long coralId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/catalogue-link";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(null, null);
        } catch (Exception e) {
            log.error("Failed to remove catalogue link for coral {}", coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void uploadPhoto(Long coralId, byte[] bytes, String contentType, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token, MediaType.MULTIPART_FORM_DATA);
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
            log.error("Failed to upload photo for coral {}", coralId, e);
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
    public byte[] getPhoto(Long coralId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, byte[].class);
            return response.getBody() != null ? response.getBody() : new byte[0];
        } catch (Exception e) {
            log.warn("Could not load photo for coral {}: {}", coralId, e.getMessage());
            return new byte[0];
        }
    }

    @Override
    public List<CoralGrowthHistoryTo> getGrowthHistory(Long coralId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/growth";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            CoralGrowthHistoryTo[] items = objectMapper.readValue(response.getBody(), CoralGrowthHistoryTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse growth history for coral {}", coralId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ResultTo<CoralGrowthHistoryTo> addGrowthRecord(Long coralId, CoralGrowthHistoryTo record, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/growth";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(record);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(record, null);
        } catch (Exception e) {
            log.error("Failed to add growth record for coral {}", coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<CoralGrowthHistoryTo> updateGrowthRecord(Long coralId, CoralGrowthHistoryTo record, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/growth/" + record.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(record);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(record, null);
        } catch (Exception e) {
            log.error("Failed to update growth record {} for coral {}", record.getId(), coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteGrowthRecord(Long coralId, Long recordId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/growth/" + recordId;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, String.class);
        } catch (Exception e) {
            log.error("Failed to delete growth record {} for coral {}", recordId, coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public List<CoralPolypConditionTo> getPolypHistory(Long coralId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/polyp";
        ResponseEntity<String> response = getAPIResponseFor(uri, token, HttpMethod.GET);
        try {
            CoralPolypConditionTo[] items = objectMapper.readValue(response.getBody(), CoralPolypConditionTo[].class);
            return Arrays.asList(items);
        } catch (JacksonException e) {
            log.error("Failed to parse polyp history for coral {}", coralId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ResultTo<CoralPolypConditionTo> addPolypObservation(Long coralId, CoralPolypConditionTo obs, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/polyp";
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(obs);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(obs, null);
        } catch (Exception e) {
            log.error("Failed to add polyp observation for coral {}", coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public ResultTo<CoralPolypConditionTo> updatePolypObservation(Long coralId, CoralPolypConditionTo obs, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/polyp/" + obs.getId();
        RestTemplate restTemplate = new RestTemplate();
        try {
            String body = objectMapper.writeValueAsString(obs);
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, String.class);
            renewBackendToken(response);
            return new ResultTo<>(obs, null);
        } catch (Exception e) {
            log.error("Failed to update polyp observation {} for coral {}", obs.getId(), coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public void deletePolypObservation(Long coralId, Long recordId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.CORAL_STOCK.getPath() + "/" + coralId + "/polyp/" + recordId;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, String.class);
        } catch (Exception e) {
            log.error("Failed to delete polyp observation {} for coral {}", recordId, coralId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }
}

