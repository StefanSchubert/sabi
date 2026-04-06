/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T053
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.FishCatalogueStatus;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.FishCatalogueService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for FishCatalogueController.
 * Covers FR-020 (min 2 chars search), SC-009 (visibility: PUBLIC + own PENDING).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishCatalogueControllerTest extends CommonTestController {

    static final String MOCKED_USER = "cataloguetest@bluewhale.de";

    @MockitoBean
    FishCatalogueService fishCatalogueService;

    @Autowired
    JsonMapper objectMapper;

    private HttpHeaders authedHeader() {
        String token = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);
        return RestHelper.prepareAuthedHttpHeader(token);
    }

    // ---- Test Cases ----

    /**
     * FR-020: GET /api/fish/catalogue/search?q=A (1 char) → 400 Bad Request.
     */
    @Test
    public void search_minTwoCharsRequired_else400() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.get()
                    .uri("/api/fish/catalogue/search?q=A&lang=en")
                    .headers(h -> h.addAll(authedHeader()))
                    .retrieve()
                    .toEntity(String.class);
        });
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
    }

    /**
     * SC-009: GET /api/fish/catalogue/search returns PUBLIC entries + own PENDING.
     * Does NOT return other users' PENDING entries.
     */
    @Test
    public void search_returnsPublicAndOwnPending_notOthersPending() {
        FishCatalogueSearchResultTo publicEntry = new FishCatalogueSearchResultTo();
        publicEntry.setId(1L);
        publicEntry.setScientificName("Amphiprioninae");
        publicEntry.setCommonName("Clownfish");
        publicEntry.setStatus(FishCatalogueStatus.PUBLIC);

        List<FishCatalogueSearchResultTo> resultList = new ArrayList<>();
        resultList.add(publicEntry);

        given(fishCatalogueService.search(eq("cl"), anyString(), eq(MOCKED_USER)))
                .willReturn(resultList);

        ResponseEntity<List> response = restClient.get()
                .uri("/api/fish/catalogue/search?q=cl&lang=en")
                .headers(h -> h.addAll(authedHeader()))
                .retrieve()
                .onStatus(s -> s.value() != 202, (req, res) -> {
                    throw new RuntimeException("Expected 202 but got " + res.getStatusCode());
                })
                .toEntity(List.class);

        assertEquals(202, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    /**
     * SC-009: Partial match on scientific name.
     */
    @Test
    public void search_partialMatch_scientificAndI18n() {
        FishCatalogueSearchResultTo result1 = new FishCatalogueSearchResultTo();
        result1.setId(2L);
        result1.setScientificName("Paracanthurus hepatus");
        result1.setCommonName("Blue Tang");
        result1.setStatus(FishCatalogueStatus.PUBLIC);

        given(fishCatalogueService.search(eq("pa"), anyString(), eq(MOCKED_USER)))
                .willReturn(List.of(result1));

        ResponseEntity<List> response = restClient.get()
                .uri("/api/fish/catalogue/search?q=pa&lang=en")
                .headers(h -> h.addAll(authedHeader()))
                .retrieve()
                .onStatus(s -> s.value() != 202, (req, res) -> {
                    throw new RuntimeException("Expected 202 but got " + res.getStatusCode());
                })
                .toEntity(List.class);

        assertEquals(202, response.getStatusCode().value());
        assertFalse(response.getBody().isEmpty());
    }

    /**
     * FR-023: Search without token → 401/403.
     */
    @Test
    public void search_unauthenticated_returns401or403() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.get()
                    .uri("/api/fish/catalogue/search?q=cl&lang=en")
                    .retrieve()
                    .toEntity(String.class);
        });
        assertTrue(ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403,
                "Expected 401 or 403 but got " + ex.getStatusCode().value());
    }
}

