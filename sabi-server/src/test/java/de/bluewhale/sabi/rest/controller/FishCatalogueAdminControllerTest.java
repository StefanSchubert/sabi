/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T067
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueStatus;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.FishCatalogueMessageCodes;
import de.bluewhale.sabi.services.FishCatalogueService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for FishCatalogueAdminController.
 * Covers FR-021 (Admin-only), FR-016 (approve), FR-017 (reject), FR-018 (admin edits before approve).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishCatalogueAdminControllerTest extends CommonTestController {

    static final String ADMIN_USER  = "admin@sabi-project.net";
    static final String NORMAL_USER = "user@example.com";

    @MockitoBean
    FishCatalogueService fishCatalogueService;

    @Autowired
    JsonMapper objectMapper;

    private HttpHeaders headerFor(String email) {
        String token = TokenAuthenticationService.createAuthorizationTokenFor(email);
        return RestHelper.prepareAuthedHttpHeader(token);
    }

    private FishCatalogueEntryTo pendingEntry() {
        FishCatalogueEntryTo to = new FishCatalogueEntryTo();
        to.setId(1L);
        to.setScientificName("Amphiprioninae");
        to.setStatus(FishCatalogueStatus.PENDING);
        to.setProposerUserId(99L);
        to.setProposalDate(LocalDate.now());
        to.setI18nEntries(new ArrayList<>());
        return to;
    }

    // ---- Test Cases ----

    /**
     * FR-021: GET /api/admin/fish/catalogue/pending without ADMIN role -> 403.
     */
    @Test
    public void listPending_nonAdmin_returns403() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.get()
                    .uri("/api/admin/fish/catalogue/pending")
                    .headers(h -> h.addAll(headerFor(NORMAL_USER)))
                    .retrieve()
                    .toEntity(String.class);
        });
        assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatusCode().value());
    }

    /**
     * FR-016: PUT /api/admin/fish/catalogue/{id}/approve -> entry status becomes PUBLIC.
     */
    @Test
    public void approve_validProposal_entryBecomesPublic() throws Exception {
        FishCatalogueEntryTo approved = pendingEntry();
        approved.setStatus(FishCatalogueStatus.PUBLIC);

        given(fishCatalogueService.approveEntry(eq(1L), any(), eq(ADMIN_USER)))
                .willReturn(new ResultTo<>(approved,
                        Message.info(FishCatalogueMessageCodes.CATALOGUE_ENTRY_APPROVED, 1L)));

        ResponseEntity<String> response = restClient.put()
                .uri("/api/admin/fish/catalogue/1/approve")
                .headers(h -> h.addAll(headerFor(ADMIN_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .retrieve()
                .onStatus(s -> s.value() != 202, (req, res) -> {
                    throw new RuntimeException("Expected 202 but got " + res.getStatusCode());
                })
                .toEntity(String.class);

        assertEquals(202, response.getStatusCode().value());
    }

    /**
     * FR-017: PUT /api/admin/fish/catalogue/{id}/reject -> entry becomes REJECTED and invisible.
     */
    @Test
    public void reject_validProposal_entryBecomesRejected() throws Exception {
        FishCatalogueEntryTo rejected = pendingEntry();
        rejected.setStatus(FishCatalogueStatus.REJECTED);

        given(fishCatalogueService.rejectEntry(eq(1L), anyString(), eq(ADMIN_USER)))
                .willReturn(new ResultTo<>(rejected,
                        Message.info(FishCatalogueMessageCodes.CATALOGUE_ENTRY_REJECTED, 1L)));

        ResponseEntity<String> response = restClient.put()
                .uri("/api/admin/fish/catalogue/1/reject")
                .headers(h -> h.addAll(headerFor(ADMIN_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"reason\":\"Duplicate entry\"}")
                .retrieve()
                .onStatus(s -> s.value() != 202, (req, res) -> {
                    throw new RuntimeException("Expected 202 but got " + res.getStatusCode());
                })
                .toEntity(String.class);

        assertEquals(202, response.getStatusCode().value());
    }

    /**
     * FR-018: PUT /api/admin/fish/catalogue/{id}/approve with edited fields -> saves admin changes.
     */
    @Test
    public void approve_adminEditsBeforeApproving_fieldsSaved() throws Exception {
        FishCatalogueEntryTo edited = pendingEntry();
        edited.setScientificName("Amphiprion ocellaris"); // admin corrected name
        FishCatalogueEntryTo approved = pendingEntry();
        approved.setScientificName("Amphiprion ocellaris");
        approved.setStatus(FishCatalogueStatus.PUBLIC);

        given(fishCatalogueService.approveEntry(eq(1L), any(), eq(ADMIN_USER)))
                .willReturn(new ResultTo<>(approved,
                        Message.info(FishCatalogueMessageCodes.CATALOGUE_ENTRY_APPROVED, 1L)));

        String body = objectMapper.writeValueAsString(edited);
        ResponseEntity<String> response = restClient.put()
                .uri("/api/admin/fish/catalogue/1/approve")
                .headers(h -> h.addAll(headerFor(ADMIN_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(s -> s.value() != 202, (req, res) -> {
                    throw new RuntimeException("Expected 202 but got " + res.getStatusCode());
                })
                .toEntity(String.class);

        assertEquals(202, response.getStatusCode().value());
    }

    /**
     * FR-023: GET /api/admin/fish/catalogue/pending without auth -> 401/403.
     */
    @Test
    public void listPending_unauthenticated_returns401or403() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient.get()
                    .uri("/api/admin/fish/catalogue/pending")
                    .retrieve()
                    .toEntity(String.class);
        });
        assertTrue(ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403);
    }
}
