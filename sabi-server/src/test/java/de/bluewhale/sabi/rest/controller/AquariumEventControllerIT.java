/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import tools.jackson.databind.json.JsonMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.mapper.AquariumEventMapper;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.AquariumEventEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumEventRepository;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link AquariumEventController}: ownership enforcement and optimistic locking.
 * Feature: 004-aquarium-events / T-033.
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AquariumEventControllerIT extends CommonTestController {

    private static final String USER_A_EMAIL = "usera.events@bluewhale.de";
    private static final String USER_B_EMAIL = "userb.events@bluewhale.de";

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    AquariumRepository aquariumRepository;

    @MockitoBean
    AquariumEventRepository aquariumEventRepository;

    @Autowired
    JsonMapper objectMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    AquariumEventMapper aquariumEventMapper;

    // ---- Helpers ----

    private UserEntity buildUserEntity(String email, Long id) {
        UserTo userTo = new UserTo(email, "TestUser", "pw123");
        userTo.setId(id);
        return userMapper.mapUserTo2Entity(userTo);
    }

    private AquariumEntity buildAquariumEntity(Long aquariumId, UserEntity owner) {
        AquariumEntity entity = new AquariumEntity();
        entity.setId(aquariumId);
        entity.setDescription("Test Tank");
        entity.setUser(owner);
        return entity;
    }

    private AquariumEventEntity buildEventEntity(Long eventId, Long aquariumId) {
        AquariumEventEntity entity = new AquariumEventEntity();
        entity.setId(eventId);
        entity.setAquariumId(aquariumId);
        entity.setEventDate(LocalDate.now());
        entity.setDescription("Test event description");
        // optlock defaults to 0 (managed by @Version in Auditable)
        return entity;
    }

    // ---- T-033 Test 1: POST as correct owner → HTTP 201 ----

    @Test
    public void testCreateEvent_asOwner_returns201() throws Exception {
        // Given
        Long aquariumId = 10L;
        UserEntity userA = buildUserEntity(USER_A_EMAIL, 1L);
        AquariumEntity tankX = buildAquariumEntity(aquariumId, userA);
        AquariumEventEntity savedEntity = buildEventEntity(42L, aquariumId);

        given(userRepository.getByEmail(USER_A_EMAIL)).willReturn(userA);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, userA.getId())).willReturn(tankX);
        given(aquariumEventRepository.save(any())).willReturn(savedEntity);

        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(USER_A_EMAIL);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        AquariumEventTo eventTo = new AquariumEventTo();
        eventTo.setAquariumId(aquariumId);
        eventTo.setEventDate(LocalDate.now());
        eventTo.setDescription("Test event");
        String requestBody = objectMapper.writeValueAsString(eventTo);

        // When / Then
        var response = restClient.post()
                .uri(Endpoint.TANKS.getPath() + "/" + aquariumId + "/events")
                .headers(h -> h.addAll(headers))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(status -> status.value() != 201, (req, resp) -> {
                    throw new RuntimeException("Expected 201 but got: " + resp.getStatusCode());
                })
                .toEntity(String.class);

        assertNotNull(response.getBody());
        AquariumEventTo created = objectMapper.readValue(response.getBody(), AquariumEventTo.class);
        assertNotNull(created.getId(), "Created event should have a generated ID");
        assertEquals(42L, created.getId());
    }

    // ---- T-033 Test 2: POST as wrong user → HTTP 403 ----

    @Test
    public void testCreateEvent_asNonOwner_returns403() throws Exception {
        // Given
        Long aquariumId = 10L;
        UserEntity userB = buildUserEntity(USER_B_EMAIL, 2L);

        given(userRepository.getByEmail(USER_B_EMAIL)).willReturn(userB);
        // Ownership check fails: aquarium does not belong to user B
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, userB.getId())).willReturn(null);

        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(USER_B_EMAIL);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        AquariumEventTo eventTo = new AquariumEventTo();
        eventTo.setAquariumId(aquariumId);
        eventTo.setEventDate(LocalDate.now());
        eventTo.setDescription("Unauthorized attempt");
        String requestBody = objectMapper.writeValueAsString(eventTo);

        // When / Then — expect HTTP 403
        try {
            restClient.post()
                    .uri(Endpoint.TANKS.getPath() + "/" + aquariumId + "/events")
                    .headers(h -> h.addAll(headers))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected HttpClientErrorException for 403");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value(), "Expected HTTP 403 for non-owner");
        }

        // Verify no event was persisted
        verify(aquariumEventRepository, never()).save(any());
    }

    // ---- T-033 Test 3: PUT with stale optlock → HTTP 409 ----

    @Test
    public void testUpdateEvent_staleOptlock_returns409() throws Exception {
        // Given
        Long aquariumId = 10L;
        Long eventId = 42L;
        UserEntity userA = buildUserEntity(USER_A_EMAIL, 1L);
        AquariumEntity tankX = buildAquariumEntity(aquariumId, userA);
        AquariumEventEntity existingEntity = buildEventEntity(eventId, aquariumId);

        given(userRepository.getByEmail(USER_A_EMAIL)).willReturn(userA);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, userA.getId())).willReturn(tankX);
        given(aquariumEventRepository.findByIdAndAquariumId(eventId, aquariumId)).willReturn(Optional.of(existingEntity));
        // Simulate optimistic lock failure on save
        given(aquariumEventRepository.save(any())).willThrow(
                new ObjectOptimisticLockingFailureException(AquariumEventEntity.class, eventId));

        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(USER_A_EMAIL);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        AquariumEventTo update = new AquariumEventTo();
        update.setId(eventId);
        update.setAquariumId(aquariumId);
        update.setEventDate(LocalDate.now());
        update.setDescription("Conflicting update");
        update.setOptlock(0L); // stale value
        String requestBody = objectMapper.writeValueAsString(update);

        // When / Then — expect HTTP 409
        try {
            restClient.put()
                    .uri(Endpoint.TANKS.getPath() + "/" + aquariumId + "/events/" + eventId)
                    .headers(h -> h.addAll(headers))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected HttpClientErrorException for 409");
        } catch (HttpClientErrorException e) {
            assertEquals(409, e.getStatusCode().value(), "Expected HTTP 409 for stale optlock");
        }
    }
}


