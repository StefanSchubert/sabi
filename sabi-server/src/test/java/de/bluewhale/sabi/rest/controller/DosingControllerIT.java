/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.mapper.DosingMapper;
import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.model.DosingType;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.DosingEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.DosingRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DosingControllerIT extends CommonTestController {

    private static final String OWNER_EMAIL = "owner.dosing@test.invalid";

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    AquariumRepository aquariumRepository;

    @MockitoBean
    DosingRepository dosingRepository;

    @Autowired
    JsonMapper objectMapper;

    @Autowired
    DosingMapper dosingMapper;

    @Test
    void createDosing_asOwner_returns201() throws Exception {
        Long aquariumId = 10L;
        UserEntity owner = owner();
        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(aquariumId);
        aquarium.setUser(owner);

        given(userRepository.getByEmail(OWNER_EMAIL)).willReturn(owner);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, owner.getId())).willReturn(aquarium);
        given(dosingRepository.save(any())).willAnswer(invocation -> {
            DosingEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return entity;
        });

        var response = restClient.post()
                .uri("/api/tank/" + aquariumId + "/dosings")
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(validManualDosing()))
                .retrieve()
                .toEntity(String.class);

        DosingTo created = objectMapper.readValue(response.getBody(), DosingTo.class);
        assertEquals(201, response.getStatusCode().value());
        assertEquals(42L, created.getId());
        assertEquals(aquariumId, created.getAquariumId());
    }

    @Test
    void createDosing_asNonOwner_returns403() throws Exception {
        Long aquariumId = 10L;
        UserEntity owner = owner();
        given(userRepository.getByEmail(OWNER_EMAIL)).willReturn(owner);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, owner.getId())).willReturn(null);

        try {
            restClient.post()
                    .uri("/api/tank/" + aquariumId + "/dosings")
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(validManualDosing()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(dosingRepository, never()).save(any());
    }

    @Test
    void createAutomatedDosing_withoutInterval_returns400() throws Exception {
        DosingTo invalidDosing = validManualDosing();
        invalidDosing.setDosingType(DosingType.AUTOMATED_DOSING);

        try {
            restClient.post()
                    .uri("/api/tank/10/dosings")
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(invalidDosing))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 400 for automated dosing without an interval");
        } catch (HttpClientErrorException e) {
            assertEquals(400, e.getStatusCode().value());
        }
        verify(dosingRepository, never()).save(any());
    }

    private UserEntity owner() {
        UserEntity owner = new UserEntity();
        owner.setId(1L);
        owner.setEmail(OWNER_EMAIL);
        return owner;
    }

    private HttpHeaders headers() {
        return RestHelper.prepareAuthedHttpHeader(TokenAuthenticationService.createAuthorizationTokenFor(OWNER_EMAIL));
    }

    private DosingTo validManualDosing() {
        DosingTo dosing = new DosingTo();
        dosing.setRecordedOn(LocalDateTime.of(2026, 9, 4, 10, 30));
        dosing.setDosingType(DosingType.MANUAL_ADDITION);
        dosing.setProductName("Magnesium");
        dosing.setAmount(new BigDecimal("5.000"));
        dosing.setAmountUnit("ml");
        return dosing;
    }
}
