/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.CarbonMode;
import de.bluewhale.sabi.model.MaintenanceEventTo;
import de.bluewhale.sabi.model.MaintenanceEventType;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MaintenanceEventEntity;
import de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.MaintenanceEventRepository;
import de.bluewhale.sabi.persistence.repositories.TankMaintenanceProfileRepository;
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
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Controller integration tests for the maintenance endpoints (US1/US3).
 * Follows the DosingControllerIT pattern: repositories fully mocked, real service + mapper.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MaintenanceControllerIT extends CommonTestController {

    private static final String OWNER_EMAIL = "owner.maintenance@test.invalid";
    private static final Long AQUARIUM_ID = 10L;
    private static final Long RECORD_ID = 77L;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    AquariumRepository aquariumRepository;

    @MockitoBean
    TankMaintenanceProfileRepository profileRepository;

    @MockitoBean
    MaintenanceEventRepository eventRepository;

    @Autowired
    JsonMapper objectMapper;

    // ---- GET: full block, defaults, 403 on reads (SC-006) ----

    @Test
    void getMaintenance_asOwner_returns200WithProfileAndEvents() throws Exception {
        mockOwnedAquarium();
        given(profileRepository.findByAquariumId(AQUARIUM_ID))
                .willReturn(Optional.of(profile(21, 25, true, CarbonMode.INTERVAL, 28)));
        given(eventRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of(
                maintenanceEvent(RECORD_ID, MaintenanceEventType.WATER_CHANGE, LocalDate.of(2026, 10, 20), 25, "partial change"),
                maintenanceEvent(78L, MaintenanceEventType.CARBON_REPLACEMENT, LocalDate.of(2026, 10, 3), null, "new media")));

        var response = restClient.get()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance")
                .headers(headers -> headers.addAll(headers()))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        MaintenanceTo body = objectMapper.readValue(response.getBody(), MaintenanceTo.class);
        assertEquals(21, body.getProfile().getRegularIntervalDays());
        assertEquals(25, body.getProfile().getRegularPercentage());
        assertTrue(body.getProfile().getCarbonEnabled());
        assertEquals(CarbonMode.INTERVAL, body.getProfile().getCarbonMode());
        assertEquals(28, body.getProfile().getCarbonReplacementIntervalDays());
        assertEquals(2, body.getEvents().size());
        assertEquals(MaintenanceEventType.WATER_CHANGE, body.getEvents().get(0).getMaintenanceType());
        assertEquals(LocalDate.of(2026, 10, 20), body.getEvents().get(0).getChangedOn());
        assertEquals("partial change", body.getEvents().get(0).getNote());
        assertEquals(MaintenanceEventType.CARBON_REPLACEMENT, body.getEvents().get(1).getMaintenanceType());
        assertNull(body.getEvents().get(1).getPercentage());
    }

    @Test
    void getMaintenance_withoutProfileRow_returnsDefaults() throws Exception {
        mockOwnedAquarium();
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.empty());
        given(eventRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of());

        var response = restClient.get()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance")
                .headers(headers -> headers.addAll(headers()))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        MaintenanceTo body = objectMapper.readValue(response.getBody(), MaintenanceTo.class);
        assertNull(body.getProfile().getRegularIntervalDays());
        assertNull(body.getProfile().getRegularPercentage());
        assertFalse(body.getProfile().getCarbonEnabled());
        assertNull(body.getProfile().getCarbonMode());
        assertNull(body.getProfile().getCarbonReplacementIntervalDays());
        assertTrue(body.getEvents().isEmpty());
    }

    @Test
    void getMaintenance_asNonOwner_returns403() {
        mockForeignAquarium();

        try {
            restClient.get()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance")
                    .headers(headers -> headers.addAll(headers()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner on GET (SC-006)");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
    }

    // ---- POST events: 201, 403, 400 validation (generic + type-specific) ----

    @Test
    void createWaterChange_asOwner_returns201() throws Exception {
        mockOwnedAquarium();
        given(eventRepository.saveAndFlush(any())).willAnswer(invocation -> {
            MaintenanceEventEntity entity = invocation.getArgument(0);
            entity.setId(RECORD_ID);
            return entity;
        });

        var response = restClient.post()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events")
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(validWaterChange()))
                .retrieve()
                .toEntity(String.class);

        assertEquals(201, response.getStatusCode().value());
        MaintenanceEventTo created = objectMapper.readValue(response.getBody(), MaintenanceEventTo.class);
        assertEquals(RECORD_ID, created.getId());
        assertEquals(AQUARIUM_ID, created.getAquariumId());
        assertEquals(MaintenanceEventType.WATER_CHANGE, created.getMaintenanceType());
    }

    @Test
    void createCarbonReplacement_asOwner_returns201WithoutPercentage() throws Exception {
        mockOwnedAquarium();
        given(eventRepository.saveAndFlush(any())).willAnswer(invocation -> {
            MaintenanceEventEntity entity = invocation.getArgument(0);
            entity.setId(RECORD_ID);
            return entity;
        });

        var response = restClient.post()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events")
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(validCarbonReplacement()))
                .retrieve()
                .toEntity(String.class);

        assertEquals(201, response.getStatusCode().value());
        MaintenanceEventTo created = objectMapper.readValue(response.getBody(), MaintenanceEventTo.class);
        assertEquals(MaintenanceEventType.CARBON_REPLACEMENT, created.getMaintenanceType());
        assertNull(created.getPercentage(), "carbon replacements must not carry a percentage");
    }

    @Test
    void createMaintenanceEvent_asNonOwner_returns403() {
        mockForeignAquarium();

        try {
            restClient.post()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events")
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(validWaterChange()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void createMaintenanceEvent_withFutureDate_returns400() throws Exception {
        MaintenanceEventTo future = validWaterChange();
        future.setChangedOn(LocalDate.now().plusDays(1));

        expect400("/api/tank/" + AQUARIUM_ID + "/maintenance/events", future);
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void createWaterChange_withoutPercentage_returns400() throws Exception {
        MaintenanceEventTo noPercentage = validWaterChange();
        noPercentage.setPercentage(null);

        expect400("/api/tank/" + AQUARIUM_ID + "/maintenance/events", noPercentage);
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCarbonReplacement_withPercentage_returns400() throws Exception {
        MaintenanceEventTo withPercentage = validCarbonReplacement();
        withPercentage.setPercentage(50);

        expect400("/api/tank/" + AQUARIUM_ID + "/maintenance/events", withPercentage);
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void createMaintenanceEvent_withPercentageOutOfRange_returns400() throws Exception {
        MaintenanceEventTo tooLow = validWaterChange();
        tooLow.setPercentage(0);
        MaintenanceEventTo tooHigh = validWaterChange();
        tooHigh.setPercentage(101);

        String uri = "/api/tank/" + AQUARIUM_ID + "/maintenance/events";
        expect400(uri, tooLow);
        expect400(uri, tooHigh);
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void createMaintenanceEvent_withNoteTooLong_returns400() throws Exception {
        MaintenanceEventTo longNote = validWaterChange();
        longNote.setNote("x".repeat(201));

        expect400("/api/tank/" + AQUARIUM_ID + "/maintenance/events", longNote);
        verify(eventRepository, never()).saveAndFlush(any());
    }

    // ---- PUT events/{eventId}: 200, 403 record-level, 409 stale optlock ----

    @Test
    void updateWaterChange_asOwner_returns200() throws Exception {
        mockOwnedAquarium();
        given(eventRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID))
                .willReturn(Optional.of(maintenanceEvent(RECORD_ID, MaintenanceEventType.WATER_CHANGE, LocalDate.of(2026, 10, 3), 50, null)));
        given(eventRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        MaintenanceEventTo update = validWaterChange();
        update.setChangedOn(LocalDate.of(2026, 10, 21));
        update.setPercentage(30);

        var response = restClient.put()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events/" + RECORD_ID)
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(update))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        MaintenanceEventTo updated = objectMapper.readValue(response.getBody(), MaintenanceEventTo.class);
        assertEquals(LocalDate.of(2026, 10, 21), updated.getChangedOn());
        assertEquals(30, updated.getPercentage());
    }

    @Test
    void updateCarbonReplacement_withPercentage_returns400() throws Exception {
        // A CARBON_REPLACEMENT payload that carries a percentage is rejected at the TO level by
        // @AssertTrue isCarbonReplacementPercentageAbsent (400) before the service runs, so the
        // service-level strip path is unreachable via the API. That defense-in-depth path is
        // covered by MaintenanceServiceTest.updateCarbonReplacement_stripsPercentageFromPayload.
        MaintenanceEventTo update = validCarbonReplacement();
        update.setChangedOn(LocalDate.of(2026, 10, 21));
        update.setPercentage(30);

        expect400(org.springframework.http.HttpMethod.PUT, "/api/tank/" + AQUARIUM_ID + "/maintenance/events/" + RECORD_ID, update);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateWaterChange_withCarbonTypedPayload_missingPercentage_returns403() {
        mockOwnedAquarium();
        given(eventRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID))
                .willReturn(Optional.of(maintenanceEvent(RECORD_ID, MaintenanceEventType.WATER_CHANGE, LocalDate.of(2026, 10, 3), 50, null)));

        // Payload typed as CARBON_REPLACEMENT carries no percentage; the stored record is a
        // WATER_CHANGE that must keep its percentage, so the update is rejected.
        try {
            restClient.put()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events/" + RECORD_ID)
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(validCarbonReplacement()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 when a water change would lose its percentage");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateMaintenanceEvent_recordNotInThisTank_returns403() {
        mockOwnedAquarium();
        given(eventRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID)).willReturn(Optional.empty());

        try {
            restClient.put()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events/" + RECORD_ID)
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(validWaterChange()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a record that does not belong to this tank");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateMaintenanceEvent_staleOptlock_returns409() {
        mockOwnedAquarium();
        given(eventRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID))
                .willReturn(Optional.of(maintenanceEvent(RECORD_ID, MaintenanceEventType.WATER_CHANGE, LocalDate.of(2026, 10, 3), 50, null)));
        given(eventRepository.save(any())).willThrow(
                new ObjectOptimisticLockingFailureException(MaintenanceEventEntity.class, RECORD_ID));

        try {
            restClient.put()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events/" + RECORD_ID)
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(validWaterChange()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 409 for a stale optlock");
        } catch (HttpClientErrorException e) {
            assertEquals(409, e.getStatusCode().value());
        }
    }

    // ---- DELETE events/{eventId}: 200, 403 ----

    @Test
    void deleteMaintenanceEvent_asOwner_returns200() {
        mockOwnedAquarium();
        given(eventRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID))
                .willReturn(Optional.of(maintenanceEvent(RECORD_ID, MaintenanceEventType.WATER_CHANGE, LocalDate.of(2026, 10, 3), 50, null)));

        var response = restClient.delete()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events/" + RECORD_ID)
                .headers(headers -> headers.addAll(headers()))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        verify(eventRepository).delete(any());
    }

    @Test
    void deleteMaintenanceEvent_asNonOwner_returns403() {
        mockForeignAquarium();

        try {
            restClient.delete()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/events/" + RECORD_ID)
                    .headers(headers -> headers.addAll(headers()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(eventRepository, never()).delete(any());
    }

    // ---- PUT water-change-interval: 200 upsert, 400 both-or-neither, 403 ----

    @Test
    void updateWaterChangeInterval_asOwner_returns200() throws Exception {
        mockOwnedAquarium();
        given(profileRepository.findByAquariumId(AQUARIUM_ID))
                .willReturn(Optional.of(profile(14, 10, false, null, null)));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        WaterChangeIntervalTo interval = new WaterChangeIntervalTo();
        interval.setIntervalDays(21);
        interval.setPercentage(25);

        var response = restClient.put()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-change-interval")
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(interval))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        MaintenanceProfileTo profile = objectMapper.readValue(response.getBody(), MaintenanceProfileTo.class);
        assertEquals(21, profile.getRegularIntervalDays());
        assertEquals(25, profile.getRegularPercentage());
    }

    @Test
    void updateWaterChangeInterval_halfSet_returns400() throws Exception {
        WaterChangeIntervalTo half = new WaterChangeIntervalTo();
        half.setIntervalDays(21); // percentage missing → both-or-neither violated

        expect400(org.springframework.http.HttpMethod.PUT, "/api/tank/" + AQUARIUM_ID + "/maintenance/water-change-interval", half);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateWaterChangeInterval_asNonOwner_returns403() {
        mockForeignAquarium();

        try {
            restClient.put()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-change-interval")
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(interval(21, 25)))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(profileRepository, never()).save(any());
    }

    // ---- PUT activated-carbon: 200, 400 validation, 403 ----

    @Test
    void updateActivatedCarbon_asOwner_returns200() throws Exception {
        mockOwnedAquarium();
        given(profileRepository.findByAquariumId(AQUARIUM_ID))
                .willReturn(Optional.of(profile(21, 25, false, null, null)));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ActivatedCarbonTo carbon = new ActivatedCarbonTo();
        carbon.setEnabled(true);
        carbon.setMode(CarbonMode.INTERVAL);
        carbon.setReplacementIntervalDays(28);

        var response = restClient.put()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/activated-carbon")
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(carbon))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        MaintenanceProfileTo profile = objectMapper.readValue(response.getBody(), MaintenanceProfileTo.class);
        assertTrue(profile.getCarbonEnabled());
        assertEquals(CarbonMode.INTERVAL, profile.getCarbonMode());
        assertEquals(28, profile.getCarbonReplacementIntervalDays());
    }

    @Test
    void updateActivatedCarbon_enabledWithoutMode_returns400() throws Exception {
        ActivatedCarbonTo carbon = new ActivatedCarbonTo();
        carbon.setEnabled(true); // mode missing → validation failure

        expect400(org.springframework.http.HttpMethod.PUT, "/api/tank/" + AQUARIUM_ID + "/maintenance/activated-carbon", carbon);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateActivatedCarbon_asNonOwner_returns403() {
        mockForeignAquarium();

        try {
            restClient.put()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/activated-carbon")
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(carbon(true, CarbonMode.CONTINUOUS, null)))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(profileRepository, never()).save(any());
    }

    // ---- helpers ----

    private void mockOwnedAquarium() {
        UserEntity owner = owner();
        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(AQUARIUM_ID);
        aquarium.setUser(owner);
        given(userRepository.getByEmail(OWNER_EMAIL)).willReturn(owner);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, owner.getId())).willReturn(aquarium);
    }

    private void mockForeignAquarium() {
        given(userRepository.getByEmail(OWNER_EMAIL)).willReturn(owner());
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 1L)).willReturn(null);
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

    private MaintenanceEventTo validWaterChange() {
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.WATER_CHANGE);
        to.setChangedOn(LocalDate.of(2026, 10, 26));
        to.setPercentage(25);
        return to;
    }

    private MaintenanceEventTo validCarbonReplacement() {
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.CARBON_REPLACEMENT);
        to.setChangedOn(LocalDate.of(2026, 10, 26));
        return to;
    }

    private TankMaintenanceProfileEntity profile(Integer intervalDays, Integer percentage,
                                                  Boolean carbonEnabled, CarbonMode mode, Integer replacementDays) {
        TankMaintenanceProfileEntity entity = new TankMaintenanceProfileEntity();
        entity.setId(5L);
        entity.setAquariumId(AQUARIUM_ID);
        entity.setRegularIntervalDays(intervalDays);
        entity.setRegularPercentage(percentage);
        entity.setCarbonEnabled(carbonEnabled);
        entity.setCarbonMode(mode);
        entity.setCarbonReplacementIntervalDays(replacementDays);
        return entity;
    }

    private MaintenanceEventEntity maintenanceEvent(Long id, MaintenanceEventType type, LocalDate changedOn,
                                                    Integer percentage, String note) {
        MaintenanceEventEntity entity = new MaintenanceEventEntity();
        entity.setId(id);
        entity.setAquariumId(AQUARIUM_ID);
        entity.setMaintenanceType(type.name());
        entity.setChangedOn(changedOn);
        entity.setPercentage(percentage);
        entity.setNote(note);
        return entity;
    }

    private WaterChangeIntervalTo interval(Integer intervalDays, Integer percentage) {
        WaterChangeIntervalTo to = new WaterChangeIntervalTo();
        to.setIntervalDays(intervalDays);
        to.setPercentage(percentage);
        return to;
    }

    private ActivatedCarbonTo carbon(Boolean enabled, CarbonMode mode, Integer replacementDays) {
        ActivatedCarbonTo to = new ActivatedCarbonTo();
        to.setEnabled(enabled);
        to.setMode(mode);
        to.setReplacementIntervalDays(replacementDays);
        return to;
    }

    private void expect400(String uri, Object body) throws Exception {
        expect400(org.springframework.http.HttpMethod.POST, uri, body);
    }

    private void expect400(org.springframework.http.HttpMethod method, String uri, Object body) throws Exception {
        try {
            restClient.method(method)
                    .uri(uri)
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 400 for an invalid payload");
        } catch (HttpClientErrorException e) {
            assertEquals(400, e.getStatusCode().value());
        }
    }
}
