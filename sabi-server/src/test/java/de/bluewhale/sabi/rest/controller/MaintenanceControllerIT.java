/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.CarbonMode;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.model.WaterChangeTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.WaterChangeEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.TankMaintenanceProfileRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.persistence.repositories.WaterChangeRepository;
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
    WaterChangeRepository waterChangeRepository;

    @Autowired
    JsonMapper objectMapper;

    // ---- GET: full block, defaults, 403 on reads (SC-006) ----

    @Test
    void getMaintenance_asOwner_returns200WithProfileAndRecords() throws Exception {
        mockOwnedAquarium();
        given(profileRepository.findByAquariumId(AQUARIUM_ID))
                .willReturn(Optional.of(profile(21, 25, true, CarbonMode.INTERVAL, 28)));
        given(waterChangeRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of(
                waterChange(RECORD_ID, LocalDate.of(2026, 10, 20), 25, "partial change")));

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
        assertEquals(1, body.getWaterChanges().size());
        assertEquals(LocalDate.of(2026, 10, 20), body.getWaterChanges().get(0).getChangedOn());
        assertEquals("partial change", body.getWaterChanges().get(0).getNote());
    }

    @Test
    void getMaintenance_withoutProfileRow_returnsDefaults() throws Exception {
        mockOwnedAquarium();
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.empty());
        given(waterChangeRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of());

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
        assertTrue(body.getWaterChanges().isEmpty());
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

    // ---- POST water-changes: 201, 403, 400 validation ----

    @Test
    void createWaterChange_asOwner_returns201() throws Exception {
        mockOwnedAquarium();
        given(waterChangeRepository.save(any())).willAnswer(invocation -> {
            WaterChangeEntity entity = invocation.getArgument(0);
            entity.setId(RECORD_ID);
            return entity;
        });

        var response = restClient.post()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes")
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(validWaterChange()))
                .retrieve()
                .toEntity(String.class);

        assertEquals(201, response.getStatusCode().value());
        WaterChangeTo created = objectMapper.readValue(response.getBody(), WaterChangeTo.class);
        assertEquals(RECORD_ID, created.getId());
        assertEquals(AQUARIUM_ID, created.getAquariumId());
    }

    @Test
    void createWaterChange_asNonOwner_returns403() {
        mockForeignAquarium();

        try {
            restClient.post()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes")
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(validWaterChange()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(waterChangeRepository, never()).save(any());
    }

    @Test
    void createWaterChange_withFutureDate_returns400() throws Exception {
        WaterChangeTo future = validWaterChange();
        future.setChangedOn(LocalDate.now().plusDays(1));

        expect400("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes", future);
        verify(waterChangeRepository, never()).save(any());
    }

    @Test
    void createWaterChange_withPercentageOutOfRange_returns400() throws Exception {
        WaterChangeTo tooLow = validWaterChange();
        tooLow.setPercentage(0);
        WaterChangeTo tooHigh = validWaterChange();
        tooHigh.setPercentage(101);

        String uri = "/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes";
        expect400(uri, tooLow);
        expect400(uri, tooHigh);
        verify(waterChangeRepository, never()).save(any());
    }

    @Test
    void createWaterChange_withNoteTooLong_returns400() throws Exception {
        WaterChangeTo longNote = validWaterChange();
        longNote.setNote("x".repeat(201));

        expect400("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes", longNote);
        verify(waterChangeRepository, never()).save(any());
    }

    // ---- PUT water-changes/{recordId}: 200, 403 record-level, 409 stale optlock ----

    @Test
    void updateWaterChange_asOwner_returns200() throws Exception {
        mockOwnedAquarium();
        given(waterChangeRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID))
                .willReturn(Optional.of(waterChange(RECORD_ID, LocalDate.of(2026, 10, 3), 50, null)));
        given(waterChangeRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        WaterChangeTo update = validWaterChange();
        update.setChangedOn(LocalDate.of(2026, 10, 21));
        update.setPercentage(30);

        var response = restClient.put()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes/" + RECORD_ID)
                .headers(headers -> headers.addAll(headers()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(update))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        WaterChangeTo updated = objectMapper.readValue(response.getBody(), WaterChangeTo.class);
        assertEquals(LocalDate.of(2026, 10, 21), updated.getChangedOn());
        assertEquals(30, updated.getPercentage());
    }

    @Test
    void updateWaterChange_recordNotInThisTank_returns403() {
        mockOwnedAquarium();
        given(waterChangeRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID)).willReturn(Optional.empty());

        try {
            restClient.put()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes/" + RECORD_ID)
                    .headers(headers -> headers.addAll(headers()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(validWaterChange()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a record that does not belong to this tank");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(waterChangeRepository, never()).save(any());
    }

    @Test
    void updateWaterChange_staleOptlock_returns409() {
        mockOwnedAquarium();
        given(waterChangeRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID))
                .willReturn(Optional.of(waterChange(RECORD_ID, LocalDate.of(2026, 10, 3), 50, null)));
        given(waterChangeRepository.save(any())).willThrow(
                new ObjectOptimisticLockingFailureException(WaterChangeEntity.class, RECORD_ID));

        try {
            restClient.put()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes/" + RECORD_ID)
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

    // ---- DELETE water-changes/{recordId}: 200, 403 ----

    @Test
    void deleteWaterChange_asOwner_returns200() {
        mockOwnedAquarium();
        given(waterChangeRepository.findByIdAndAquariumId(RECORD_ID, AQUARIUM_ID))
                .willReturn(Optional.of(waterChange(RECORD_ID, LocalDate.of(2026, 10, 3), 50, null)));

        var response = restClient.delete()
                .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes/" + RECORD_ID)
                .headers(headers -> headers.addAll(headers()))
                .retrieve()
                .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
        verify(waterChangeRepository).delete(any());
    }

    @Test
    void deleteWaterChange_asNonOwner_returns403() {
        mockForeignAquarium();

        try {
            restClient.delete()
                    .uri("/api/tank/" + AQUARIUM_ID + "/maintenance/water-changes/" + RECORD_ID)
                    .headers(headers -> headers.addAll(headers()))
                    .retrieve()
                    .toEntity(String.class);
            fail("Expected 403 for a non-owner");
        } catch (HttpClientErrorException e) {
            assertEquals(403, e.getStatusCode().value());
        }
        verify(waterChangeRepository, never()).delete(any());
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

    private WaterChangeTo validWaterChange() {
        WaterChangeTo to = new WaterChangeTo();
        to.setChangedOn(LocalDate.of(2026, 10, 26));
        to.setPercentage(25);
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

    private WaterChangeEntity waterChange(Long id, LocalDate changedOn, Integer percentage, String note) {
        WaterChangeEntity entity = new WaterChangeEntity();
        entity.setId(id);
        entity.setAquariumId(AQUARIUM_ID);
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
