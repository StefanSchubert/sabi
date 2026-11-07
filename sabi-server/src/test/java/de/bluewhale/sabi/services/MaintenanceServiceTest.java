/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.MaintenanceMapperImpl;
import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.CarbonMode;
import de.bluewhale.sabi.model.MaintenanceEventTo;
import de.bluewhale.sabi.model.MaintenanceEventType;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MaintenanceEventEntity;
import de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.MaintenanceEventRepository;
import de.bluewhale.sabi.persistence.repositories.TankMaintenanceProfileRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the ownership-safe maintenance operations (US1/US3).
 * Uses the real MapStruct-generated mapper so merge semantics are verified against entity state.
 */
@ExtendWith(MockitoExtension.class)
@Tag("ServiceTest")
class MaintenanceServiceTest {

    private static final String USER_EMAIL = "owner.maintenance@test.invalid";
    private static final Long AQUARIUM_ID = 10L;

    @Mock
    TankMaintenanceProfileRepository profileRepository;

    @Mock
    MaintenanceEventRepository eventRepository;

    @Mock
    AquariumRepository aquariumRepository;

    @Mock
    UserRepository userRepository;

    MaintenanceServiceImpl maintenanceService;

    @BeforeEach
    void setUp() {
        maintenanceService = new MaintenanceServiceImpl(profileRepository, eventRepository,
                aquariumRepository, userRepository, new MaintenanceMapperImpl());
    }

    // ---- ownership (403 on every operation, including reads) ----

    @Test
    void getMaintenanceForTank_forForeignAquarium_returnsErrorAndNeverQueriesRecords() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<MaintenanceTo> result = maintenanceService.getMaintenanceForTank(AQUARIUM_ID, USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        assertNull(result.getValue());
        verify(eventRepository, never()).findByAquariumIdOrderByChangedOnDesc(any());
    }

    @Test
    void createMaintenanceEvent_forForeignAquarium_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<MaintenanceEventTo> result = maintenanceService.createMaintenanceEvent(AQUARIUM_ID, validWaterChange(), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateWaterChangeInterval_forForeignAquarium_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(21, 25), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateActivatedCarbon_forForeignAquarium_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateActivatedCarbon(AQUARIUM_ID, carbon(true, CarbonMode.INTERVAL, 28), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateMaintenanceEvent_recordNotInThisTank_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(eventRepository.findByIdAndAquariumId(77L, AQUARIUM_ID)).willReturn(Optional.empty());

        ResultTo<MaintenanceEventTo> result = maintenanceService.updateMaintenanceEvent(AQUARIUM_ID, 77L, validWaterChange(), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void deleteMaintenanceEvent_recordNotInThisTank_returnsErrorAndNeverDeletes() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(eventRepository.findByIdAndAquariumId(77L, AQUARIUM_ID)).willReturn(Optional.empty());

        ResultTo<MaintenanceEventTo> result = maintenanceService.deleteMaintenanceEvent(AQUARIUM_ID, 77L, USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(eventRepository, never()).delete(any());
    }

    // ---- read: profile + events, defaults when no profile row ----

    @Test
    void getMaintenanceForTank_asOwner_returnsProfileAndEvents() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID))
                .willReturn(Optional.of(profile(21, 25, true, CarbonMode.INTERVAL, 28)));
        given(eventRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of(
                maintenanceEvent(1L, MaintenanceEventType.WATER_CHANGE, LocalDate.of(2026, 10, 20), 25, "partial change"),
                maintenanceEvent(2L, MaintenanceEventType.CARBON_REPLACEMENT, LocalDate.of(2026, 10, 3), null, "new filter media")));

        ResultTo<MaintenanceTo> result = maintenanceService.getMaintenanceForTank(AQUARIUM_ID, USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        MaintenanceTo to = result.getValue();
        assertEquals(21, to.getProfile().getRegularIntervalDays());
        assertEquals(25, to.getProfile().getRegularPercentage());
        assertTrue(to.getProfile().getCarbonEnabled());
        assertEquals(CarbonMode.INTERVAL, to.getProfile().getCarbonMode());
        assertEquals(28, to.getProfile().getCarbonReplacementIntervalDays());
        assertEquals(2, to.getEvents().size());
        assertEquals(MaintenanceEventType.WATER_CHANGE, to.getEvents().get(0).getMaintenanceType());
        assertEquals(LocalDate.of(2026, 10, 20), to.getEvents().get(0).getChangedOn());
        assertEquals("partial change", to.getEvents().get(0).getNote());
        assertEquals(MaintenanceEventType.CARBON_REPLACEMENT, to.getEvents().get(1).getMaintenanceType());
        assertNull(to.getEvents().get(1).getPercentage());
    }

    @Test
    void getMaintenanceForTank_withoutProfileRow_returnsDefaults() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.empty());
        given(eventRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of());

        ResultTo<MaintenanceTo> result = maintenanceService.getMaintenanceForTank(AQUARIUM_ID, USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        MaintenanceProfileTo profile = result.getValue().getProfile();
        assertNull(profile.getRegularIntervalDays());
        assertNull(profile.getRegularPercentage());
        assertFalse(profile.getCarbonEnabled());
        assertNull(profile.getCarbonMode());
        assertNull(profile.getCarbonReplacementIntervalDays());
        assertTrue(result.getValue().getEvents().isEmpty());
    }

    // ---- regular interval: upsert / clear / lazy profile creation ----

    @Test
    void updateWaterChangeInterval_upsertsBothFields() {
        TankMaintenanceProfileEntity existing = profile(14, 10, false, null, null);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(21, 25), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(21, captor.getValue().getRegularIntervalDays());
        assertEquals(25, captor.getValue().getRegularPercentage());
        // untouched fields stay as they were
        assertFalse(captor.getValue().getCarbonEnabled());
    }

    @Test
    void updateWaterChangeInterval_clearsBothFieldsWhenBothNull() {
        TankMaintenanceProfileEntity existing = profile(21, 25, false, null, null);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(null, null), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertNull(captor.getValue().getRegularIntervalDays());
        assertNull(captor.getValue().getRegularPercentage());
    }

    @Test
    void updateWaterChangeInterval_createsProfileRowWhenMissing() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.empty());
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(21, 25), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(AQUARIUM_ID, captor.getValue().getAquariumId());
        assertEquals(21, captor.getValue().getRegularIntervalDays());
        assertEquals(25, captor.getValue().getRegularPercentage());
    }

    // ---- activated carbon: FR-011 deterministic clearing on disable ----

    @Test
    void updateActivatedCarbon_disabledNullsModeAndReplacementInterval() {
        TankMaintenanceProfileEntity existing = profile(21, 25, true, CarbonMode.INTERVAL, 28);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateActivatedCarbon(AQUARIUM_ID, carbon(false, null, null), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertFalse(captor.getValue().getCarbonEnabled());
        assertNull(captor.getValue().getCarbonMode());
        assertNull(captor.getValue().getCarbonReplacementIntervalDays());
    }

    @Test
    void updateActivatedCarbon_enabledContinuous_clearsReplacementInterval() {
        TankMaintenanceProfileEntity existing = profile(21, 25, true, CarbonMode.INTERVAL, 28);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateActivatedCarbon(AQUARIUM_ID, carbon(true, CarbonMode.CONTINUOUS, null), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertTrue(captor.getValue().getCarbonEnabled());
        assertEquals(CarbonMode.CONTINUOUS, captor.getValue().getCarbonMode());
        assertNull(captor.getValue().getCarbonReplacementIntervalDays());
    }

    // ---- maintenance events: path-derived aquariumId, trim-to-null, type-specific rules ----

    @Test
    void createWaterChange_setsAquariumIdFromPathAndNormalizesNote() {
        MaintenanceEventTo to = validWaterChange();
        to.setAquariumId(999L); // must be ignored, taken from the path
        to.setNote("   ");

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(eventRepository.saveAndFlush(any())).willAnswer(invocation -> {
            MaintenanceEventEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return entity;
        });

        ResultTo<MaintenanceEventTo> result = maintenanceService.createMaintenanceEvent(AQUARIUM_ID, to, USER_EMAIL);

        ArgumentCaptor<MaintenanceEventEntity> captor = ArgumentCaptor.forClass(MaintenanceEventEntity.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        assertEquals(AQUARIUM_ID, captor.getValue().getAquariumId());
        assertNull(captor.getValue().getNote(), "blank note must be normalized to null");
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(42L, result.getValue().getId());
    }

    @Test
    void createCarbonReplacement_neverPersistsAPercentage() {
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.CARBON_REPLACEMENT);
        to.setChangedOn(LocalDate.of(2026, 10, 26));
        to.setPercentage(50); // must be stripped by the service

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(eventRepository.saveAndFlush(any())).willAnswer(invocation -> {
            MaintenanceEventEntity entity = invocation.getArgument(0);
            entity.setId(43L);
            return entity;
        });

        ResultTo<MaintenanceEventTo> result = maintenanceService.createMaintenanceEvent(AQUARIUM_ID, to, USER_EMAIL);

        ArgumentCaptor<MaintenanceEventEntity> captor = ArgumentCaptor.forClass(MaintenanceEventEntity.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getPercentage(), "carbon replacements must not carry a percentage");
        assertEquals(MaintenanceEventType.CARBON_REPLACEMENT.name(), captor.getValue().getMaintenanceType());
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
    }

    @Test
    void updateWaterChange_withCarbonTypedPayload_missingPercentage_isRejected() {
        MaintenanceEventEntity existing = maintenanceEvent(5L, MaintenanceEventType.WATER_CHANGE,
                LocalDate.of(2026, 10, 20), 25, null);

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(eventRepository.findByIdAndAquariumId(5L, AQUARIUM_ID)).willReturn(Optional.of(existing));

        // Payload typed as CARBON_REPLACEMENT carries no percentage; the stored record is a
        // WATER_CHANGE and must keep its percentage, so the update is rejected.
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.CARBON_REPLACEMENT);
        to.setChangedOn(LocalDate.of(2026, 10, 21));

        ResultTo<MaintenanceEventTo> result = maintenanceService.updateMaintenanceEvent(AQUARIUM_ID, 5L, to, USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateCarbonReplacement_stripsPercentageFromPayload() {
        MaintenanceEventEntity existing = maintenanceEvent(6L, MaintenanceEventType.CARBON_REPLACEMENT,
                LocalDate.of(2026, 10, 20), null, "old media");

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(eventRepository.findByIdAndAquariumId(6L, AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(eventRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.CARBON_REPLACEMENT);
        to.setChangedOn(LocalDate.of(2026, 10, 21));
        to.setPercentage(30); // must be stripped

        ResultTo<MaintenanceEventTo> result = maintenanceService.updateMaintenanceEvent(AQUARIUM_ID, 6L, to, USER_EMAIL);

        ArgumentCaptor<MaintenanceEventEntity> captor = ArgumentCaptor.forClass(MaintenanceEventEntity.class);
        verify(eventRepository).save(captor.capture());
        assertNull(captor.getValue().getPercentage());
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
    }

    @Test
    void futureDatedMaintenanceEvent_failsValidationPredicate() {
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.WATER_CHANGE);
        to.setChangedOn(LocalDate.now().plusDays(1));
        to.setPercentage(25);

        assertFalse(to.isChangedOnNotInFuture(), "future dates must fail the Bean Validation predicate (400 in the IT)");
    }

    // ---- helpers ----

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(USER_EMAIL);
        return user;
    }

    private AquariumEntity aquarium() {
        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(AQUARIUM_ID);
        return aquarium;
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

    private MaintenanceEventTo validWaterChange() {
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.WATER_CHANGE);
        to.setChangedOn(LocalDate.of(2026, 10, 26));
        to.setPercentage(25);
        return to;
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
}
