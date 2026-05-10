/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.model.PublicReefReportTo;
import de.bluewhale.sabi.model.PublicReportLinkTo;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * API gateway implementation for public HouseReef report share links.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class PublicReportServiceImpl extends APIServiceImpl implements PublicReportService {

    @Override
    public PublicReportLinkTo getLinkForTank(Long aquariumId, String jwtBackendAuthToken) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.REPORT_LINK.getPath() + "/" + aquariumId;
        try {
            ResponseEntity<String> response = getAPIResponseFor(uri, jwtBackendAuthToken, HttpMethod.GET);
            if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getBody() == null || response.getBody().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.getBody(), PublicReportLinkTo.class);
        } catch (JacksonException e) {
            log.error("Failed to parse report link from {}", uri, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    @Override
    public PublicReportLinkTo createOrReplaceLink(Long aquariumId, LocalDateTime validUntil, String jwtBackendAuthToken) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.REPORT_LINK.getPath() + "/" + aquariumId;
        if (validUntil != null) {
            uri += "?validUntil=" + validUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(jwtBackendAuthToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
            renewBackendToken(response);
            return objectMapper.readValue(response.getBody(), PublicReportLinkTo.class);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to create report link for aquarium {}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    @Override
    public void deleteLink(Long aquariumId, String jwtBackendAuthToken) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.REPORT_LINK.getPath() + "/" + aquariumId;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(jwtBackendAuthToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, request, String.class);
            renewBackendToken(response);
        } catch (RestClientException e) {
            log.error("Failed to delete report link for aquarium {}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }

    @Override
    public PublicReefReportTo getReport(String token, String lang) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.PUBLIC_REPORT.getPath() + "/" + token + "?lang=" + lang;
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, request, String.class);
            return objectMapper.readValue(response.getBody(), PublicReefReportTo.class);
        } catch (RestClientException | JacksonException e) {
            log.error("Failed to fetch public report for token {}", token, e);
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }
    }
}
