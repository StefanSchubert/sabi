/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T040
 */

package de.bluewhale.sabi.rest.controller;

import tools.jackson.databind.json.JsonMapper;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.FishStockMessageCodes;
import de.bluewhale.sabi.services.FishStockService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for FishStockController.
 * Verifies authentication, authorization (FR-011, FR-023), and business constraints (FR-024, FR-025).
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishStockControllerTest extends CommonTestController {

    static final String MOCKED_USER = "fishtest@bluewhale.de";
    static final String OTHER_USER  = "other@bluewhale.de";

    @MockitoBean
    FishStockService fishStockService;

    @Autowired
    JsonMapper objectMapper;

    // -------- Helpers --------

    private HttpHeaders authedHeader() {
        String token = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);
        return RestHelper.prepareAuthedHttpHeader(token);
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private FishStockEntryTo validFishEntry() {
        FishStockEntryTo entry = new FishStockEntryTo();
        entry.setAquariumId(1L);
        entry.setCommonName("Clownfish");
        entry.setAddedOn(LocalDate.now().minusDays(10));
        return entry;
    }

    // -------- Test Cases --------

    /**
     * FR-002: POST /api/fish/ with valid entry → 201 Created.
     */
    @Test
    public void addFish_validEntry_returns201() throws Exception {
        FishStockEntryTo entry = validFishEntry();
        FishStockEntryTo saved = validFishEntry();
        saved.setId(42L);
        given(fishStockService.addFishToTank(any(FishStockEntryTo.class), eq(MOCKED_USER)))
                .willReturn(new ResultTo<>(saved, Message.info(FishStockMessageCodes.FISH_CREATED, 42L)));

        ResponseEntity<String> response = restClient.post().uri("/api/fish/")
                .headers(h -> h.addAll(authedHeader()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(entry))
                .retrieve()
                .onStatus(status -> status.value() != 201, (req, res) -> {
                    throw new RuntimeException("Expected 201 but got " + res.getStatusCode());
                })
                .toEntity(String.class);

        assertEquals(201, response.getStatusCode().value());
    }

    /**
     * FR-003: POST /api/fish/ without commonName → 400 Bad Request (Bean Validation).
     */
    @Test
    public void addFish_missingCommonName_returns400() {
        FishStockEntryTo entry = new FishStockEntryTo();
        entry.setAquariumId(1L);
        // commonName deliberately missing
        entry.setAddedOn(LocalDate.now());

        assertThrows(HttpClientErrorException.class, () -> {
            restClient.post().uri("/api/fish/")
                    .headers(h -> h.addAll(authedHeader()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(entry))
                    .retrieve()
                    .toEntity(String.class);
        });
    }

    /**
     * FR-023: POST /api/fish/ without token → 401/403 Unauthorized.
     */
    @Test
    public void addFish_unauthenticated_returns401() throws Exception {
        FishStockEntryTo entry = validFishEntry();

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.post().uri("/api/fish/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toJson(entry))
                    .retrieve()
                    .toEntity(String.class);
        });
        assertTrue(ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403,
                "Expected 401 or 403 but got " + ex.getStatusCode().value());
    }

    /**
     * FR-011: GET /api/fish/{aquariumId}/list for another user's aquarium → empty list (service checks ownership).
     */
    @Test
    public void getFishForTank_otherUsersAquarium_returns202WithEmptyList() {
        given(fishStockService.getFishForTank(eq(99L), eq(MOCKED_USER)))
                .willReturn(new ArrayList<>());

        ResponseEntity<List> response = restClient.get().uri("/api/fish/99/list")
                .headers(h -> {
                    h.addAll(authedHeader());
                })
                .retrieve()
                .onStatus(status -> status.value() != 202, (req, res) -> {
                    throw new RuntimeException("Expected 202 but got " + res.getStatusCode());
                })
                .toEntity(List.class);

        assertEquals(202, response.getStatusCode().value());
        assertTrue(response.getBody() == null || response.getBody().isEmpty());
    }

    /**
     * FR-024: DELETE /api/fish/{id} when fish has departure record → 409 Conflict.
     */
    @Test
    public void deleteFish_withDepartureRecord_returns409() throws Exception {
        FishStockEntryTo fish = validFishEntry();
        fish.setId(7L);
        given(fishStockService.deletePhysically(eq(7L), eq(MOCKED_USER)))
                .willReturn(new ResultTo<>(fish,
                        Message.error(FishStockMessageCodes.FISH_HAS_DEPARTURE_RECORD, 7L)));

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.delete().uri("/api/fish/7")
                    .headers(h -> h.addAll(authedHeader()))
                    .retrieve()
                    .toEntity(String.class);
        });
        assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());
    }

    /**
     * FR-008: POST /api/fish/{id}/photo without auth → 401/403.
     */
    @Test
    public void uploadPhoto_unauthenticated_returns401or403() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.post().uri("/api/fish/1/photo")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(new byte[10])
                    .retrieve()
                    .toEntity(String.class);
        });
        assertTrue(ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403);
    }

    /**
     * FR-025: GET /api/fish/{id}/photo for a fish that has no photo → 404.
     */
    @Test
    public void getPhoto_noPhoto_returns404() {
        given(fishStockService.getPhotoBytes(eq(99L), eq(MOCKED_USER)))
                .willReturn(new byte[0]);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.get().uri("/api/fish/99/photo")
                    .headers(h -> h.addAll(authedHeader()))
                    .retrieve()
                    .toEntity(byte[].class);
        });
        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getStatusCode().value());
    }
}

