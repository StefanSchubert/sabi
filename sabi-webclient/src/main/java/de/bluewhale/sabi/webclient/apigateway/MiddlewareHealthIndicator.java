/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Offers healthcheck endpoint to monitor the pi stack, if all components are up and running.
 * Required by #sabi-124
 *
 * @author Stefan Schubert
 */
@Component
@Slf4j
public class MiddlewareHealthIndicator implements HealthIndicator {

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private ObjectMapper objectMapper;  // json mapper

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * We will call a api from blackpearl and see if it's responsive.
     * Thus we know that not only our frontend is up and running but that
     * blackpearl has survived the lastet power outage or unattended upgrade and
     * restarted properly.
     * @return
     */
    @Override
    public Health health() {
        Health.Builder status = Health.up(); // default assuming good case

        ResponseEntity<String> responseEntity = null;
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        String requestJson = null;
        HttpEntity<?> entity = new HttpEntity<>(requestJson, requestHeaders); // for request
        try {
            responseEntity = restTemplate.exchange(sabiBackendUrl + Endpoint.HEALTH_STATS, HttpMethod.GET,entity, String.class);
            String result = responseEntity.getBody();

        } catch (HttpClientErrorException e) {
                status = Health.down();
                status.withException(e);
                log.info("Couldn't reach Middleware PI, Healthcheck was {}",status.build());
        }

        return status.build();
    }

}
